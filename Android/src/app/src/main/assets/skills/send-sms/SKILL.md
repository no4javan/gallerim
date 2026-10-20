---
name: send-sms
description: Write a text message (SMS) draft and open it in the messages app; the user presses send (Persian: پیامک بده، اس‌ام‌اس بفرست، پیام بده به …، به این شماره پیام بده).
---

# Send SMS

## Instructions

Call the `run_intent` tool with the following exact parameters:

- intent: send_sms
- parameters: A JSON string with the following fields:
  - phone_number: the recipient's phone number, digits only (convert Persian digits to 0-9). String.
  - sms_body: the text of the message, in the language the user asked for. String.

Then tell the user in one short sentence that the draft is open and they only need to press send.
