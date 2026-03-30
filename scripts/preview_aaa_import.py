#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
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
    "draw": "http://schemas.openxmlformats.org/drawingml/2006/main",
}

DEFAULT_SHEETS = ("Певцов", "Ткачев")
DEFAULT_GREEN_DATE = date(2026, 3, 27)
DEFAULT_REGULAR_DATE = date(2026, 3, 20)
DEFAULT_SOURCE = "DIRECT_SALES"
DEFAULT_STATUS_ONLY_STAGE = "QUALIFICATION"
EXCEL_EPOCH = datetime(1899, 12, 30)

SOURCE_MAP = {
    "Прямая продажа": "DIRECT_SALES",
    "Тендер": "TENDER",
    "Сайт": "WEBSITE",
    "ГК Остек": "GK_OSTEK",
}

ACTIVE_STAGE_BY_AMOUNT_COLUMN = {
    "F": "QUALIFICATION",
    "H": "PROPOSAL",
    "J": "INVOICE_ISSUED",
    "L": "PRODUCTION",
    "N": "WAITING_FOR_PAYMENT",
}

STATUS_BY_AMOUNT_COLUMN = {
    "P": "DONE",
    "R": "LOST",
    "T": "ON_HOLD",
}

STAGE_ROUTE = [
    "QUALIFICATION",
    "PROPOSAL",
    "NEGOTIATION",
    "INVOICE_ISSUED",
    "PRODUCTION",
    "WAITING_FOR_PAYMENT",
]

GREEN_HEX = {"00B050", "4EA72E"}
NUMERIC_TITLE_RE = re.compile(r"^[\d\s.,]+$")
NON_BLOCKING_ISSUES = {"stage_assumed_for_done_status"}


@dataclass
class ImportPreviewRow:
    sheet: str
    row_number: int
    title: str | None
    raw_type: str | None
    source: str | None
    source_issue: str | None
    state_amount_column: str | None
    current_stage: str | None
    current_status: str | None
    initial_amount: str | None
    current_amount: str | None
    import_date: str | None
    is_green: bool
    green_columns: list[str]
    workbook_created_date: str | None
    plan_contact_date: str | None
    plan_contact_raw: str | None
    comment: str | None
    issues: list[str]
    ready_for_import: bool
    advance_count_from_create: int | None
    final_status_after_create: str | None
    skip_reason: str | None


class WorkbookReader:
    def __init__(self, path: Path) -> None:
        self.archive = zipfile.ZipFile(path)
        self.shared_strings = self._load_shared_strings()
        self.sheets = self._load_sheets()
        self.theme_colors = self._load_theme_colors()
        self.fills, self.cell_xfs = self._load_styles()

    def close(self) -> None:
        self.archive.close()

    def _load_shared_strings(self) -> list[str]:
        shared_path = "xl/sharedStrings.xml"
        if shared_path not in self.archive.namelist():
            return []

        root = ET.fromstring(self.archive.read(shared_path))
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

    def _load_theme_colors(self) -> dict[str, str]:
        theme_path = "xl/theme/theme1.xml"
        if theme_path not in self.archive.namelist():
            return {}

        theme = ET.fromstring(self.archive.read(theme_path))
        color_scheme = theme.find(".//draw:clrScheme", XLSX_NS)
        if color_scheme is None:
            return {}

        colors: dict[str, str] = {}
        for index, child in enumerate(list(color_scheme)):
            if not list(child):
                continue
            node = list(child)[0]
            value = (node.get("lastClr") or node.get("val") or "").upper()
            if value:
                colors[str(index)] = value
        return colors

    def _load_styles(self) -> tuple[list[dict[str, Any]], list[ET.Element]]:
        styles = ET.fromstring(self.archive.read("xl/styles.xml"))

        fills: list[dict[str, Any]] = []
        for fill in styles.find("sheet:fills", XLSX_NS):
            pattern_fill = fill.find("sheet:patternFill", XLSX_NS)
            if pattern_fill is None:
                fills.append({})
                continue

            fg = pattern_fill.find("sheet:fgColor", XLSX_NS)
            fills.append(
                {
                    "patternType": pattern_fill.get("patternType"),
                    "fgColor": dict(fg.attrib) if fg is not None else None,
                }
            )

        return fills, list(styles.find("sheet:cellXfs", XLSX_NS))

    def iter_rows(self, sheet_name: str) -> list[ET.Element]:
        sheet_path = self.sheets[sheet_name]
        root = ET.fromstring(self.archive.read(sheet_path))
        sheet_data = root.find("sheet:sheetData", XLSX_NS)
        return list(sheet_data or [])

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

    def is_green_cell(self, cell: ET.Element) -> bool:
        style_id = cell.get("s")
        if style_id is None:
            return False

        xf = self.cell_xfs[int(style_id)]
        fill_id = int(xf.get("fillId", "0"))
        fill = self.fills[fill_id]
        if fill.get("patternType") != "solid":
            return False

        fg_color = fill.get("fgColor") or {}
        rgb = (fg_color.get("rgb") or "").upper().removeprefix("FF")
        theme = fg_color.get("theme")
        resolved_theme = self.theme_colors.get(theme or "", "")

        return rgb in GREEN_HEX or resolved_theme in GREEN_HEX or theme == "9"


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


def parse_excel_date(raw: str | None) -> str | None:
    text = normalize_text(raw)
    if text is None:
        return None

    if re.fullmatch(r"\d+(\.\d+)?", text):
        serial = float(text)
        return (EXCEL_EPOCH + timedelta(days=serial)).date().isoformat()
    return None


def is_summary_title(value: str | None) -> bool:
    title = normalize_text(value)
    if title is None:
        return False
    return bool(NUMERIC_TITLE_RE.fullmatch(title))


def build_api_plan(stage: str | None, status: str | None) -> tuple[int | None, str | None]:
    if stage is None:
        return None, None

    try:
        advance_count = STAGE_ROUTE.index(stage)
    except ValueError:
        return None, None

    final_status = None if status == "ACTIVE" else status
    return advance_count, final_status


def is_blocking_issue(issue: str) -> bool:
    if issue in NON_BLOCKING_ISSUES:
        return False
    if issue.startswith("stage_assumed_for_status_only_row:"):
        return False
    return True


def source_from_type(raw_type: str | None, default_source: str | None) -> tuple[str | None, str | None]:
    value = normalize_text(raw_type)
    if value is None:
        return default_source, None

    mapped = SOURCE_MAP.get(value)
    if mapped is None:
        return default_source, f"unknown_source:{value}"
    return mapped, None


def preview_row(
    sheet: str,
    row_number: int,
    values: dict[str, str | None],
    green_columns: list[str],
    green_date: date,
    regular_date: date,
    default_source: str | None,
    status_only_stage: str | None,
) -> ImportPreviewRow:
    title = normalize_text(values.get("C"))
    raw_type = normalize_text(values.get("B"))
    comment = normalize_text(values.get("V"))
    workbook_created_date = parse_excel_date(values.get("A"))
    plan_contact_date = parse_excel_date(values.get("U"))
    plan_contact_raw = normalize_text(values.get("U")) if plan_contact_date is None else None

    if title is None:
        return ImportPreviewRow(
            sheet=sheet,
            row_number=row_number,
            title=None,
            raw_type=raw_type,
            source=None,
            source_issue=None,
            state_amount_column=None,
            current_stage=None,
            current_status=None,
            initial_amount=None,
            current_amount=None,
            import_date=None,
            is_green=bool(green_columns),
            green_columns=green_columns,
            workbook_created_date=workbook_created_date,
            plan_contact_date=plan_contact_date,
            plan_contact_raw=plan_contact_raw,
            comment=comment,
            issues=["missing_title"],
            ready_for_import=False,
            advance_count_from_create=None,
            final_status_after_create=None,
            skip_reason="missing_title",
        )

    state_columns = [column for column in (*ACTIVE_STAGE_BY_AMOUNT_COLUMN.keys(), *STATUS_BY_AMOUNT_COLUMN.keys()) if normalize_text(values.get(column)) is not None]
    if is_summary_title(title):
        return ImportPreviewRow(
            sheet=sheet,
            row_number=row_number,
            title=title,
            raw_type=raw_type,
            source=None,
            source_issue=None,
            state_amount_column=None,
            current_stage=None,
            current_status=None,
            initial_amount=None,
            current_amount=None,
            import_date=None,
            is_green=bool(green_columns),
            green_columns=green_columns,
            workbook_created_date=workbook_created_date,
            plan_contact_date=plan_contact_date,
            plan_contact_raw=plan_contact_raw,
            comment=comment,
            issues=["summary_row"],
            ready_for_import=False,
            advance_count_from_create=None,
            final_status_after_create=None,
            skip_reason="summary_row",
        )

    if not state_columns:
        return ImportPreviewRow(
            sheet=sheet,
            row_number=row_number,
            title=title,
            raw_type=raw_type,
            source=None,
            source_issue=None,
            state_amount_column=None,
            current_stage=None,
            current_status=None,
            initial_amount=None,
            current_amount=None,
            import_date=None,
            is_green=bool(green_columns),
            green_columns=green_columns,
            workbook_created_date=workbook_created_date,
            plan_contact_date=plan_contact_date,
            plan_contact_raw=plan_contact_raw,
            comment=comment,
            issues=["no_stage_or_status_amount"],
            ready_for_import=False,
            advance_count_from_create=None,
            final_status_after_create=None,
            skip_reason="no_stage_or_status_amount",
        )

    if len(state_columns) > 1:
        return ImportPreviewRow(
            sheet=sheet,
            row_number=row_number,
            title=title,
            raw_type=raw_type,
            source=None,
            source_issue=None,
            state_amount_column=None,
            current_stage=None,
            current_status=None,
            initial_amount=None,
            current_amount=None,
            import_date=None,
            is_green=bool(green_columns),
            green_columns=green_columns,
            workbook_created_date=workbook_created_date,
            plan_contact_date=plan_contact_date,
            plan_contact_raw=plan_contact_raw,
            comment=comment,
            issues=[f"multiple_stage_or_status_amounts:{','.join(state_columns)}"],
            ready_for_import=False,
            advance_count_from_create=None,
            final_status_after_create=None,
            skip_reason="multiple_stage_or_status_amounts",
        )

    state_column = state_columns[0]
    amount = parse_decimal(values.get(state_column))
    issues: list[str] = []

    source, source_issue = source_from_type(raw_type, default_source)
    if source_issue:
        issues.append(source_issue)

    if amount is None:
        issues.append("invalid_amount")

    current_stage: str | None
    current_status: str | None
    if state_column in ACTIVE_STAGE_BY_AMOUNT_COLUMN:
        current_stage = ACTIVE_STAGE_BY_AMOUNT_COLUMN[state_column]
        current_status = "ACTIVE"
    else:
        current_status = STATUS_BY_AMOUNT_COLUMN[state_column]
        if current_status == "DONE":
            current_stage = "WAITING_FOR_PAYMENT"
            issues.append("stage_assumed_for_done_status")
        else:
            current_stage = status_only_stage
            if status_only_stage is None:
                issues.append("stage_not_resolved_for_status_only_row")
            else:
                issues.append(f"stage_assumed_for_status_only_row:{status_only_stage}")

    import_date = (green_date if green_columns else regular_date).isoformat()
    advance_count, final_status_after_create = build_api_plan(current_stage, current_status)
    blocking_issues = [issue for issue in issues if is_blocking_issue(issue)]
    ready_for_import = not blocking_issues

    return ImportPreviewRow(
        sheet=sheet,
        row_number=row_number,
        title=title,
        raw_type=raw_type,
        source=source,
        source_issue=source_issue,
        state_amount_column=state_column,
        current_stage=current_stage,
        current_status=current_status,
        initial_amount=format_decimal(amount),
        current_amount=format_decimal(amount),
        import_date=import_date,
        is_green=bool(green_columns),
        green_columns=green_columns,
        workbook_created_date=workbook_created_date,
        plan_contact_date=plan_contact_date,
        plan_contact_raw=plan_contact_raw,
        comment=comment,
        issues=issues,
        ready_for_import=ready_for_import,
        advance_count_from_create=advance_count,
        final_status_after_create=final_status_after_create,
        skip_reason=None,
    )


def parse_workbook(
    path: Path,
    sheets: list[str],
    green_date: date,
    regular_date: date,
    default_source: str | None,
    status_only_stage: str | None,
) -> dict[str, Any]:
    reader = WorkbookReader(path)
    try:
        rows: list[ImportPreviewRow] = []
        for sheet in sheets:
            if sheet not in reader.sheets:
                raise SystemExit(f"Sheet '{sheet}' not found in {path}")

            for row in reader.iter_rows(sheet):
                row_number = int(row.get("r"))
                if row_number <= 4:
                    continue

                values: dict[str, str | None] = {}
                green_columns: list[str] = []
                for cell in row:
                    cell_ref = cell.get("r")
                    column = "".join(ch for ch in cell_ref if ch.isalpha())
                    values[column] = reader.cell_value(cell)
                    if reader.is_green_cell(cell):
                        green_columns.append(column)

                if not any(normalize_text(values.get(column)) is not None for column in (chr(code) for code in range(ord("A"), ord("V") + 1))):
                    continue

                rows.append(preview_row(sheet, row_number, values, green_columns, green_date, regular_date, default_source, status_only_stage))

        ready_rows = [row for row in rows if row.ready_for_import]
        skipped_rows = [row for row in rows if row.skip_reason is not None]
        issue_counter = Counter(issue for row in rows for issue in row.issues)
        sheet_counter = Counter(row.sheet for row in rows if row.ready_for_import)
        green_counter = Counter(row.sheet for row in rows if row.is_green and row.skip_reason is None)

        return {
            "file": str(path),
            "total_rows_seen": len(rows),
            "ready_rows": len(ready_rows),
            "skipped_rows": len(skipped_rows),
            "default_source": default_source,
            "status_only_stage": status_only_stage,
            "ready_by_sheet": dict(sheet_counter),
            "green_rows_by_sheet": dict(green_counter),
            "issues": dict(issue_counter),
            "rows": [asdict(row) for row in rows],
        }
    finally:
        reader.close()


def parse_date(value: str) -> date:
    return date.fromisoformat(value)


def print_summary(report: dict[str, Any], show_ready: int, show_skipped: int) -> None:
    print(f"File: {report['file']}")
    print(f"Default source for empty type: {report['default_source']}")
    print(f"Default stage for status-only rows: {report['status_only_stage'] or 'not set'}")
    print(f"Rows parsed: {report['total_rows_seen']}")
    print(f"Ready rows: {report['ready_rows']}")
    print(f"Skipped rows: {report['skipped_rows']}")
    print(f"Ready by sheet: {report['ready_by_sheet']}")
    print(f"Green rows by sheet: {report['green_rows_by_sheet']}")
    print(f"Issues: {report['issues']}")

    ready_rows = [row for row in report["rows"] if row["ready_for_import"]][:show_ready]
    skipped_rows = [row for row in report["rows"] if row["skip_reason"] is not None][:show_skipped]

    if ready_rows:
        print("\nReady rows:")
        for row in ready_rows:
            print(
                f"  {row['sheet']}#{row['row_number']}: {row['title']} | "
                f"stage={row['current_stage']} status={row['current_status']} "
                f"amount={row['current_amount']} source={row['source']} "
                f"date={row['import_date']} green={row['is_green']}"
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
                "createdAt": f"{row['import_date']}T00:00:00Z",
                "responsibleUserId": None,
            }
        )

    return {
        "dryRun": dry_run,
        "rows": rows,
    }


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
                "createdAt": f"{row['import_date']}T00:00:00Z",
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


def main() -> None:
    parser = argparse.ArgumentParser(description="Preview one-time import plan for aaa.xlsx")
    parser.add_argument("xlsx", type=Path, help="Path to aaa.xlsx")
    parser.add_argument("--sheet", dest="sheets", action="append", help="Sheet to include. Can be passed multiple times.")
    parser.add_argument("--green-date", type=parse_date, default=DEFAULT_GREEN_DATE, help="Import date for green rows in YYYY-MM-DD")
    parser.add_argument("--regular-date", type=parse_date, default=DEFAULT_REGULAR_DATE, help="Import date for non-green rows in YYYY-MM-DD")
    parser.add_argument("--default-source", choices=sorted(SOURCE_MAP.values()), default=DEFAULT_SOURCE, help="Fallback source when column B is empty")
    parser.add_argument("--status-only-stage", choices=STAGE_ROUTE, default=DEFAULT_STATUS_ONLY_STAGE, help="Fallback stage for LOST/ON_HOLD rows that have no active stage column")
    parser.add_argument("--json", action="store_true", help="Print full JSON report")
    parser.add_argument("--payload-json", action="store_true", help="Print backend payload with only ready rows")
    parser.add_argument("--apply", action="store_true", help="Set dryRun=false in payload output")
    parser.add_argument("--accounts-file", type=Path, help="JSON file with per-sheet email/password credentials")
    parser.add_argument("--api-base-url", default="http://localhost:8080", help="Backend base URL for sign-in and import")
    parser.add_argument("--show-ready", type=int, default=12, help="How many ready rows to print in text mode")
    parser.add_argument("--show-skipped", type=int, default=12, help="How many skipped rows to print in text mode")
    args = parser.parse_args()

    sheets = list(dict.fromkeys(args.sheets or list(DEFAULT_SHEETS)))
    report = parse_workbook(args.xlsx, sheets, args.green_date, args.regular_date, args.default_source, args.status_only_stage)

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
