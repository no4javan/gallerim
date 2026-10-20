---
name: calls-and-contacts
description: Open the dialer with a phone number, add a new contact, or share a text to another app such as Telegram or WhatsApp (Persian: زنگ بزن به این شماره، شماره بگیر، مخاطب جدید، این شماره رو ذخیره کن، بفرست برای، به اشتراک بذار).
---

# Calls, contacts and sharing

## Instructions

To call a number, call the `run_intent` tool with:
- intent: dial_number
- parameters: A JSON string with the field `phone_number`: digits only (convert Persian digits to 0-9). String.
The dialer opens with the number; the user presses call.

To add a contact, call the `run_intent` tool with:
- intent: create_contact
- parameters: A JSON string with these fields:
  - name: the full name. String.
  - phone_number: (optional) digits only. String.
  - email: (optional). String.
The contact form opens filled in; the user presses save.

To share a text with another app, call the `run_intent` tool with:
- intent: share_text
- parameters: A JSON string with the field `text`: the text to share. String.

Then tell the user in one short sentence what to do next.
