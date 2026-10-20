---
name: alarm-and-timer
description: Set an alarm in the clock app or start a countdown timer (Persian: زنگ ساعت بذار، آلارم بذار، ساعت ۶ صبح بیدارم کن، تایمر بذار، ۱۰ دقیقه تایمر، شمارش معکوس).
---

# Alarm and timer

## Instructions

To set an alarm, call the `run_intent` tool with:
- intent: set_alarm
- parameters: A JSON string with these fields:
  - hour: 0-23. Integer. Convert "6 in the evening" to 18.
  - minute: 0-59. Integer.
  - label: (optional) a short label. String.

To start a timer, call the `run_intent` tool with:
- intent: set_timer
- parameters: A JSON string with these fields:
  - seconds: the total length in seconds, for example 600 for 10 minutes. Integer.
  - label: (optional) a short label. String.

Convert Persian digits to normal numbers. Then tell the user in one short sentence what was set.
