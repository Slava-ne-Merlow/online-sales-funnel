import sys
import json
import os
from exchangelib import Account, Credentials, Message, Mailbox

LOG_PATH = os.environ.get("MAIL_LOG_PATH", "./mail/send_mail.log")

def log_line(message):
    try:
        with open(LOG_PATH, "a") as f:
            f.write(message + "\n")
    except Exception:
        # Avoid crashing mail send on log failure
        pass

def run():
    email = os.environ["MAIL_USERNAME"]
    password = os.environ["MAIL_PASSWORD"]

    log_line("MAIL_USERNAME=" + str(email))
    log_line("MAIL_PASSWORD set=" + str(password is not None) + ", length=" + str(len(password)))

    payload = json.loads(sys.stdin.read() or "{}")
    to_email = payload["to"]
    subject = payload["subject"]
    body = payload["body"]
    bcc = payload.get("bcc")

    account = Account(
        primary_smtp_address=email,
        credentials=Credentials(email, password),
        autodiscover=True,
        access_type="delegate"
    )

    msg = Message(
        account=account,
        folder=account.sent,
        subject=subject,
        body=body,
        to_recipients=[Mailbox(email_address=to_email)]
    )

    if bcc:
        msg.bcc_recipients = [Mailbox(email_address=bcc)]

    msg.send_and_save()

if __name__ == "__main__":
    try:
        run()
    except Exception as e:
        print(str(e), file=sys.stdout)
        sys.exit(1)
