---
name: maps-and-web
description: Show a place or address on the map app, or search something on the web in the browser (Persian: روی نقشه نشون بده، مسیر، کجاست، آدرس، توی گوگل سرچ کن، در اینترنت جستجو کن).
---

# Maps and web search

## Instructions

To show a place on the map, call the `run_intent` tool with:
- intent: show_on_map
- parameters: A JSON string with the field `location`: the place name, business or address, as the user said it. String.

To search the web, call the `run_intent` tool with:
- intent: web_search
- parameters: A JSON string with the field `query`: the search text. String.

Then tell the user in one short sentence what was opened.
