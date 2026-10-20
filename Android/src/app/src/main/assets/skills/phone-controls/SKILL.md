---
name: phone-controls
description: Turn the flashlight on or off, or open a settings page such as Wi-Fi, Bluetooth, display, sound, battery or location (Persian: چراغ‌قوه رو روشن کن، چراغ‌قوه رو خاموش کن، تنظیمات وای‌فای، بلوتوث، صفحه‌نمایش، صدا، باتری، مکان).
---

# Phone controls

## Instructions

For the flashlight, call the `run_intent` tool with:
- intent: flashlight
- parameters: A JSON string with the field `on`: true to turn it on, false to turn it off. Boolean.

To open a settings page, call the `run_intent` tool with:
- intent: open_settings
- parameters: A JSON string with the field `page`, one of: "wifi", "bluetooth", "display", "sound", "battery", "location", "apps", "main". String.

Then tell the user in one short sentence what was done. Android does not let apps switch Wi-Fi or Bluetooth on or off directly, so for those open the settings page and say so.
