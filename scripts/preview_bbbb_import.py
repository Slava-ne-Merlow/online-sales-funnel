#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import urllib.error
import urllib.request
import zipfile
from collections import Counter
from dataclasses import asdict, dataclass
from datetime import date, datetime, timedelta
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any
from xml.etree import ElementTree as ET


XLSX_NS = {
    "sheet": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "rel": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
}

DEFAULT_SHEETS = ("Певцов", "Ткачев", "Рашидов")
DEFAULT_SOURCE = "DIRECT_SALES"
DEFAULT_FALLBACK_CREATED_DATE = date(2026, 3, 13)
EXCEL_EPOCH = datetime(1899, 12, 30)

SOURCE_MAP = {
    "Прямая продажа": "DIRECT_SALES",
    "Тендер": "TENDER",
    "Сайт": "WEBSITE",
    "ГК Остек": "GK_OSTEK",
}

PT_ACTIVE_AMOUNT_COLUMNS = {
    "F": "QUALIFICATION",
    "H": "PROPOSAL",
    "J": "CONTRACTED",
    "L": "INVOICE_ISSUED",
    "N": "WAITING_FOR_PAYMENT",
}

PT_STATUS_AMOUNT_COLUMNS = {
    "P": "DONE",
    "R": "LOST",
    "T": "ON_HOLD",
}

RASHIDOV_ACTIVE_AMOUNT_COLUMNS = {
    "D": "QUALIFICATION",
    "E": "PROPOSAL",
    "F": "INVOICE_ISSUED",
    "G": "CONTRACTED",
    "H": "WAITING_FOR_PAYMENT",
}

RASHIDOV_STATUS_AMOUNT_COLUMNS = {
    "J": "DONE",
    "L": "LOST",
    "N": "ON_HOLD",
}

NUMERIC_TITLE_RE = re.compile(r"^[\d\s.,]+$")


@dataclass
class ImportPreviewRow:
    sheet: str
    row_number: int
    title: str | None
    raw_source: str | None
    source: str | None
    current_stage: str | None
    current_status: str | None
    initial_amount: str | None
    current_amount: str | None
    created_at: str | None
    comment: str | None
    issues: list[str]
    ready_for_import: bool
    skip_reason: str | None


class WorkbookReader:
    def __init__(self, path: Path) -> None:
        self.archive = zipfile.ZipFile(path)
        self.shared_strings = self._load_shared_strings()
        self.sheets = self._load_sheets()

    def close(self) -> None:
        self.archive.close()

    def _load_shared_strings(self) -> list[str]:
        if "xl/sharedStrings.xml" not in self.archive.namelist():
            return []

        root = ET.fromstring(self.archive.read("xl/sharedStrings.xml"))
        items: list[str] = []
        for string_item in root:
            items.append("".join(node.text or "" for node in string_item.iter(f"{{{XLSX_NS['sheet']}}}t")))
        return items

    def _load_sheets(self) -> dict[str, str]:
        workbook = ET.fromstring(self.archive.read("xl/workbook.xml"))
        relations = ET.fromstring(self.archive.read("xl/_rels/workbook.xml.rels"))
        relation_map = {rel.get("Id"): rel.get("Target") for rel in relations}
        relation_attr = f"{{{XLSX_NS['rel']}}}id"
        return {
            sheet.get("name"): f"xl/{relation_map[sheet.get(relation_attr)]}"
            for sheet in workbook.find("sheet:sheets", XLSX_NS)
        }

    def iter_rows(self, sheet_name: str) -> list[ET.Element]:
        root = ET.fromstring(self.archive.read(self.sheets[sheet_name]))
        return list(root.find("sheet:sheetData", XLSX_NS) or [])

    def cell_value(self, cell: ET.Element) -> str | None:
        value_node = cell.find("sheet:v", XLSX_NS)
        if value_node is None:
            inline_string = cell.find("sheet:is", XLSX_NS)
            if inline_string is None:
                return None
            return "".join(node.text or "" for node in inline_string.iter(f"{{{XLSX_NS['sheet']}}}t"))

        raw = value_node.text
        if cell.get("t") == "s":
            return self.shared_strings[int(raw)]
        return raw


def normalize_text(value: str | None) -> str | None:
    if value is None:
        return None
    cleaned = value.replace("\xa0", " ").strip()
    return cleaned or None


def parse_decimal(value: str | None) -> Decimal | None:
    text = normalize_text(value)
    if text is None:
        return None

    normalized = text.replace(" ", "").replace(",", ".")
    try:
        return Decimal(normalized)
    except InvalidOperation:
        return None


def format_decimal(value: Decimal | None) -> str | None:
    if value is None:
        return None
    return f"{value.quantize(Decimal('0.01'))}"


def parse_excel_date(raw: str | None) -> date | None:
    text = normalize_text(raw)
    if text is None:
        return None

    if re.fullmatch(r"\d+(\.\d+)?", text):
        serial = float(text)
        return (EXCEL_EPOCH + timedelta(days=serial)).date()
    return None


def is_summary_title(value: str | None) -> bool:
    title = normalize_text(value)
    if title is None:
        return False
    return bool(NUMERIC_TITLE_RE.fullmatch(title))


def resolve_source(raw_source: str | None, default_source: str) -> tuple[str, list[str]]:
    text = normalize_text(raw_source)
    if text is None:
        return default_source, ["source_defaulted"]

    mapped = SOURCE_MAP.get(text)
    if mapped is None:
        return default_source, [f"unknown_source:{text}", "source_defaulted"]
    return mapped, []


def resolve_status_stage(status: str | None, stage: str | None) -> tuple[str | None, str | None]:
    if status == "DONE":
        return "CONTRACTED", "DONE"
    if status == "LOST":
        return "PROPOSAL", "LOST"
    if status == "ON_HOLD":
        return "PROPOSAL", "ON_HOLD"
    return stage, "ACTIVE" if stage else None


def select_state(values: dict[str, str | None], amount_columns: dict[str, str], status_columns: dict[str, str]) -> tuple[str | None, list[str]]:
    filled = [
        column
        for column in (*amount_columns.keys(), *status_columns.keys())
        if normalize_text(values.get(column)) is not None
    ]
    if not filled:
        return None, ["no_stage_or_status_amount"]
    if len(filled) > 1:
        return None, [f"multiple_stage_or_status_amounts:{','.join(filled)}"]
    return filled[0], []


def preview_pt_tk_row(
    sheet: str,
    row_number: int,
    values: dict[str, str | None],
    fallback_created_date: date,
    default_source: str,
) -> ImportPreviewRow:
    title = normalize_text(values.get("C"))
    if title is None:
        return ImportPreviewRow(sheet, row_number, None, normalize_text(values.get("B")), None, None, None, None, None, None, None, ["missing_title"], False, "missing_title")
    if is_summary_title(title):
        return ImportPreviewRow(sheet, row_number, title, normalize_text(values.get("B")), None, None, None, None, None, None, None, ["summary_row"], False, "summary_row")

    state_column, state_issues = select_state(values, PT_ACTIVE_AMOUNT_COLUMNS, PT_STATUS_AMOUNT_COLUMNS)
    if state_column is None:
        return ImportPreviewRow(
            sheet,
            row_number,
            title,
            normalize_text(values.get("B")),
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            state_issues,
            False,
            state_issues[0].split(":")[0],
        )

    current_amount = parse_decimal(values.get(state_column))
    initial_amount = parse_decimal(values.get("D")) or current_amount
    created_at = parse_excel_date(values.get("A")) or fallback_created_date
    source, issues = resolve_source(values.get("B"), default_source)
    issues.extend(state_issues)

    if current_amount is None:
        issues.append("invalid_current_amount")
    if initial_amount is None:
        issues.append("missing_initial_amount")

    stage = PT_ACTIVE_AMOUNT_COLUMNS.get(state_column)
    status = PT_STATUS_AMOUNT_COLUMNS.get(state_column)
    current_stage, current_status = resolve_status_stage(status, stage)

    return ImportPreviewRow(
        sheet=sheet,
        row_number=row_number,
        title=title,
        raw_source=normalize_text(values.get("B")),
        source=source,
        current_stage=current_stage,
        current_status=current_status,
        initial_amount=format_decimal(initial_amount),
        current_amount=format_decimal(current_amount),
        created_at=f"{created_at.isoformat()}T00:00:00Z",
        comment=normalize_text(values.get("V")),
        issues=issues,
        ready_for_import=not any(issue.startswith(("missing_", "invalid_", "multiple_")) or issue == "no_stage_or_status_amount" for issue in issues),
        skip_reason=None if not any(issue.startswith(("missing_", "invalid_", "multiple_")) or issue == "no_stage_or_status_amount" for issue in issues) else next(
            issue.split(":")[0]
            for issue in issues
            if issue.startswith(("missing_", "invalid_", "multiple_")) or issue == "no_stage_or_status_amount"
        ),
    )


def preview_rashidov_row(
    row_number: int,
    values: dict[str, str | None],
    default_source: str,
) -> ImportPreviewRow:
    title = normalize_text(values.get("C"))
    if title is None:
        return ImportPreviewRow("Рашидов", row_number, None, normalize_text(values.get("B")), None, None, None, None, None, None, None, ["missing_title"], False, "missing_title")
    if is_summary_title(title):
        return ImportPreviewRow("Рашидов", row_number, title, normalize_text(values.get("B")), None, None, None, None, None, None, None, ["summary_row"], False, "summary_row")

    state_column, state_issues = select_state(values, RASHIDOV_ACTIVE_AMOUNT_COLUMNS, RASHIDOV_STATUS_AMOUNT_COLUMNS)
    if state_column is None:
        return ImportPreviewRow(
            "Рашидов",
            row_number,
            title,
            normalize_text(values.get("B")),
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            state_issues,
            False,
            state_issues[0].split(":")[0],
        )

    current_amount = parse_decimal(values.get(state_column))
    initial_amount = current_amount
    created_at = parse_excel_date(values.get("A"))
    source, issues = resolve_source(values.get("B"), default_source)
    issues.extend(state_issues)

    if current_amount is None:
        issues.append("invalid_current_amount")
    if initial_amount is None:
        issues.append("missing_initial_amount")
    if created_at is None:
        issues.append("missing_created_at")

    stage = RASHIDOV_ACTIVE_AMOUNT_COLUMNS.get(state_column)
    status = RASHIDOV_STATUS_AMOUNT_COLUMNS.get(state_column)
    current_stage, current_status = resolve_status_stage(status, stage)

    return ImportPreviewRow(
        sheet="Рашидов",
        row_number=row_number,
        title=title,
        raw_source=normalize_text(values.get("B")),
        source=source,
        current_stage=current_stage,
        current_status=current_status,
        initial_amount=format_decimal(initial_amount),
        current_amount=format_decimal(current_amount),
        created_at=f"{created_at.isoformat()}T00:00:00Z" if created_at else None,
        comment=normalize_text(values.get("P")),
        issues=issues,
        ready_for_import=not any(issue.startswith(("missing_", "invalid_", "multiple_")) or issue == "no_stage_or_status_amount" for issue in issues),
        skip_reason=None if not any(issue.startswith(("missing_", "invalid_", "multiple_")) or issue == "no_stage_or_status_amount" for issue in issues) else next(
            issue.split(":")[0]
            for issue in issues
            if issue.startswith(("missing_", "invalid_", "multiple_")) or issue == "no_stage_or_status_amount"
        ),
    )


def parse_sheet_rows(reader: WorkbookReader, sheet: str, fallback_created_date: date, default_source: str) -> list[ImportPreviewRow]:
    rows: list[ImportPreviewRow] = []
    for row in reader.iter_rows(sheet):
        row_number = int(row.get("r"))
        if sheet in ("Певцов", "Ткачев") and row_number <= 4:
            continue
        if sheet == "Рашидов" and row_number <= 3:
            continue

        values: dict[str, str | None] = {}
        for cell in row:
            cell_ref = cell.get("r")
            column = "".join(ch for ch in cell_ref if ch.isalpha())
            values[column] = reader.cell_value(cell)

        if not any(normalize_text(value) is not None for value in values.values()):
            continue

        if not any(
            normalize_text(value) is not None
            for column, value in values.items()
            if column != "B"
        ):
            continue

        if sheet in ("Певцов", "Ткачев"):
            rows.append(preview_pt_tk_row(sheet, row_number, values, fallback_created_date, default_source))
        else:
            rows.append(preview_rashidov_row(row_number, values, default_source))
    return rows


def parse_workbook(path: Path, sheets: list[str], fallback_created_date: date, default_source: str) -> dict[str, Any]:
    reader = WorkbookReader(path)
    try:
        rows: list[ImportPreviewRow] = []
        for sheet in sheets:
            if sheet not in reader.sheets:
                raise SystemExit(f"Sheet '{sheet}' not found in {path}")
            rows.extend(parse_sheet_rows(reader, sheet, fallback_created_date, default_source))

        ready_rows = [row for row in rows if row.ready_for_import]
        skipped_rows = [row for row in rows if row.skip_reason is not None]

        return {
            "file": str(path),
            "total_rows_seen": len(rows),
            "ready_rows": len(ready_rows),
            "skipped_rows": len(skipped_rows),
            "ready_by_sheet": dict(Counter(row.sheet for row in ready_rows)),
            "issues": dict(Counter(issue for row in rows for issue in row.issues)),
            "rows": [asdict(row) for row in rows],
        }
    finally:
        reader.close()


def print_summary(report: dict[str, Any], show_ready: int, show_skipped: int) -> None:
    print(f"File: {report['file']}")
    print(f"Rows parsed: {report['total_rows_seen']}")
    print(f"Ready rows: {report['ready_rows']}")
    print(f"Skipped rows: {report['skipped_rows']}")
    print(f"Ready by sheet: {report['ready_by_sheet']}")
    print(f"Issues: {report['issues']}")

    ready_rows = [row for row in report["rows"] if row["ready_for_import"]][:show_ready]
    skipped_rows = [row for row in report["rows"] if row["skip_reason"] is not None][:show_skipped]

    if ready_rows:
        print("\nReady rows:")
        for row in ready_rows:
            print(
                f"  {row['sheet']}#{row['row_number']}: {row['title']} | "
                f"stage={row['current_stage']} status={row['current_status']} "
                f"current={row['current_amount']} initial={row['initial_amount']} "
                f"createdAt={row['created_at']}"
            )

    if skipped_rows:
        print("\nSkipped rows:")
        for row in skipped_rows:
            print(
                f"  {row['sheet']}#{row['row_number']}: {row['title']} | "
                f"skip={row['skip_reason']} issues={','.join(row['issues'])}"
            )


def build_payload(report: dict[str, Any], dry_run: bool) -> dict[str, Any]:
    rows = []
    for row in report["rows"]:
        if not row["ready_for_import"]:
            continue
        rows.append(
            {
                "sheet": row["sheet"],
                "rowNumber": row["row_number"],
                "title": row["title"],
                "source": row["source"],
                "initialAmount": row["initial_amount"],
                "currentAmount": row["current_amount"],
                "globalComment": row["comment"],
                "currentStage": row["current_stage"],
                "currentStatus": row["current_status"],
                "createdAt": row["created_at"],
                "responsibleUserId": None,
            }
        )
    return {"dryRun": dry_run, "rows": rows}


def build_payload_by_sheet(report: dict[str, Any], dry_run: bool) -> dict[str, dict[str, Any]]:
    grouped: dict[str, dict[str, Any]] = {}
    for row in report["rows"]:
        if not row["ready_for_import"]:
            continue
        payload = grouped.setdefault(row["sheet"], {"dryRun": dry_run, "rows": []})
        payload["rows"].append(
            {
                "sheet": row["sheet"],
                "rowNumber": row["row_number"],
                "title": row["title"],
                "source": row["source"],
                "initialAmount": row["initial_amount"],
                "currentAmount": row["current_amount"],
                "globalComment": row["comment"],
                "currentStage": row["current_stage"],
                "currentStatus": row["current_status"],
                "createdAt": row["created_at"],
                "responsibleUserId": None,
            }
        )
    return grouped


def load_accounts(path: Path) -> dict[str, dict[str, str]]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        raise SystemExit("Accounts file must be a JSON object keyed by sheet name")

    accounts: dict[str, dict[str, str]] = {}
    for sheet, config in raw.items():
        if not isinstance(config, dict):
            raise SystemExit(f"Account config for sheet '{sheet}' must be an object")
        email = config.get("email")
        password = config.get("password")
        if not email or not password:
            raise SystemExit(f"Account config for sheet '{sheet}' must contain email and password")
        accounts[sheet] = {"email": email, "password": password}
    return accounts


def post_json(url: str, payload: dict[str, Any], token: str | None = None) -> dict[str, Any]:
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json", **({"Authorization": f"Bearer {token}"} if token else {})},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise SystemExit(f"HTTP {exc.code} for {url}: {body}") from exc


def sign_in(base_url: str, email: str, password: str) -> str:
    response = post_json(
        f"{base_url.rstrip('/')}/api/auth/sign-in",
        {"email": email, "password": password},
    )
    access_token = response.get("accessToken")
    if not access_token:
        raise SystemExit(f"Sign-in for {email} did not return accessToken")
    return access_token


def send_imports(base_url: str, report: dict[str, Any], accounts: dict[str, dict[str, str]], dry_run: bool) -> list[dict[str, Any]]:
    results: list[dict[str, Any]] = []
    payloads = build_payload_by_sheet(report, dry_run=dry_run)

    for sheet, payload in payloads.items():
        account = accounts.get(sheet)
        if account is None:
            raise SystemExit(f"No account provided for sheet '{sheet}'")

        token = sign_in(base_url, account["email"], account["password"])
        response = post_json(
            f"{base_url.rstrip('/')}/api/internal/project-import/rows",
            payload,
            token=token,
        )
        results.append(
            {
                "sheet": sheet,
                "email": account["email"],
                "response": response,
            }
        )

    return results


def parse_date(value: str) -> date:
    return date.fromisoformat(value)


def main() -> None:
    parser = argparse.ArgumentParser(description="Preview one-time import plan for bbbb.xlsx")
    parser.add_argument("xlsx", type=Path, help="Path to bbbb.xlsx")
    parser.add_argument("--sheet", dest="sheets", action="append", help="Sheet to include. Can be passed multiple times.")
    parser.add_argument("--fallback-created-date", type=parse_date, default=DEFAULT_FALLBACK_CREATED_DATE, help="Fallback createdAt for Певцов/Ткачев rows without date in YYYY-MM-DD")
    parser.add_argument("--default-source", choices=sorted(SOURCE_MAP.values()), default=DEFAULT_SOURCE, help="Fallback source when source cell is empty")
    parser.add_argument("--json", action="store_true", help="Print full JSON report")
    parser.add_argument("--payload-json", action="store_true", help="Print backend payload with only ready rows")
    parser.add_argument("--apply", action="store_true", help="Set dryRun=false in payload output or when sending to backend")
    parser.add_argument("--accounts-file", type=Path, help="JSON file with per-sheet email/password credentials")
    parser.add_argument("--api-base-url", default="http://localhost:8080", help="Backend base URL for sign-in and import")
    parser.add_argument("--show-ready", type=int, default=12, help="How many ready rows to print in text mode")
    parser.add_argument("--show-skipped", type=int, default=12, help="How many skipped rows to print in text mode")
    args = parser.parse_args()

    sheets = list(dict.fromkeys(args.sheets or list(DEFAULT_SHEETS)))
    report = parse_workbook(args.xlsx, sheets, args.fallback_created_date, args.default_source)

    if args.payload_json:
        print(json.dumps(build_payload(report, dry_run=not args.apply), ensure_ascii=False, indent=2))
        return

    if args.accounts_file:
        accounts = load_accounts(args.accounts_file)
        results = send_imports(args.api_base_url, report, accounts, dry_run=not args.apply)
        print(json.dumps(results, ensure_ascii=False, indent=2))
        return

    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return

    print_summary(report, args.show_ready, args.show_skipped)


if __name__ == "__main__":
    main()
