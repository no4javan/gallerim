---
name: delete-calendar-event
description: Delete or cancel an existing calendar event.
---

# Delete calendar event

## Instructions
To delete an event, you must follow these exact steps:
1. Call the `run_intent` tool with `intent` as `get_current_date_and_time` and `parameters` as `{}` to get the user's local date, time, and the current day of the week.
2. Explicitly calculate the date of the event the user wants to delete (for example "tomorrow" or "this Friday") in YYYY-MM-DD format. If the user did not say which day, use today's date.
3. Call the `run_intent` tool with `intent` as `read_calendar_events` and `parameters` as a JSON string with the field `date` set to that date.
4. From the returned events, pick the one whose title matches what the user described. If none matches, tell the user and stop. If more than one matches, ask the user which one they mean and stop.
5. Call the `run_intent` tool with the following exact parameters:
    - `intent`: delete_calendar_event
    - `parameters`: A JSON string with the following field:
        - `event_id`: the event_id of the chosen event. Integer.
6. Tell the user which event was deleted, with its title and time.
