---
name: send-email
description: Write an email draft and open it in the email app; the user presses send (Persian: ایمیل بزن، ایمیل بفرست، یک ایمیل بنویس به …).
---

# Send email

## Instructions

Call the `run_intent` tool with the following exact parameters:

- intent: send_email
- parameters: A JSON string with the following fields:
  - extra_email: the email address to send the email to. String.
  - extra_subject: the subject of the email. String.
  - extra_text: the body of the email, in the language the user asked for. String.

Then tell the user in one short sentence that the draft is open and they only need to press send.
