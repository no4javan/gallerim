---
name: manage-tasks
description: Add, list or complete items in the user's to-do list, optionally with a reminder (Persian: کار، کارها، لیست کارها، تودو، باید … رو انجام بدم، اضافه کن به کارهام، چه کارهایی دارم، انجامش دادم، تیک بزن، «پس‌فردا قبض گاز رو بدم»).
---

# Manage tasks

## Instructions
The to-do list lives on the phone. Use these steps:

To ADD a task:
1. If the user mentions a day or time, call the `run_intent` tool with `intent` as `get_current_date_and_time` and `parameters` as `{}`, then calculate the exact date and time in YYYY-MM-DDTHH:MM:SS format. If only a day is given, use 09:00:00 on that day.
2. Call the `run_intent` tool with:
    - `intent`: add_task
    - `parameters`: A JSON string with these fields:
        - `title`: a short title for the task, in the user's language. String.
        - `due_time`: (optional) when it is due, in YYYY-MM-DDTHH:MM:SS format. String.
        - `remind`: (optional) true to send a reminder notification at due_time. Defaults to true when due_time is given. Boolean.
3. Tell the user in one short sentence that the task was added, and when the reminder is.

To LIST tasks:
1. Call the `run_intent` tool with `intent` as `list_tasks` and `parameters` as `{}`.
2. Show the open tasks as a short list, with their due times if any.

To COMPLETE a task:
1. Call the `run_intent` tool with `intent` as `list_tasks` and `parameters` as `{}`.
2. Pick the task whose title matches what the user described. If none matches, say so and stop.
3. Call the `run_intent` tool with `intent` as `complete_task` and `parameters` as a JSON string with the field `task_id` set to that task's task_id.
4. Tell the user in one short sentence that it is done.
