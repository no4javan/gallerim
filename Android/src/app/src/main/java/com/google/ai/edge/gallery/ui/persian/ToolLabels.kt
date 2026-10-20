/*
 * Copyright 2026 no4javan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.persian

/** Persian names for the built-in skills and intents, shown in the chat's tool trace. */
private val TOOL_NAMES =
  mapOf(
    "create-calendar-event" to "ساخت رویداد در تقویم",
    "read-calendar-events" to "خواندن تقویم",
    "update-calendar-event" to "ویرایش رویداد",
    "delete-calendar-event" to "حذف رویداد",
    "schedule-notification" to "یادآوری",
    "send-email" to "ایمیل",
    "query-wikipedia" to "ویکی‌پدیا",
    "interactive-map" to "نقشه",
    "qr-code" to "کد QR",
    "calculate-hash" to "محاسبه‌ی hash",
    "mood-tracker" to "ثبت حال",
    "learn-something-new" to "یادگیری چیز تازه",
    "kitchen-adventure" to "آشپزی",
    "text-spinner" to "بازنویسی متن",
    "create_calendar_event" to "ثبت در تقویم",
    "read_calendar_events" to "خواندن تقویم",
    "update_calendar_event" to "ویرایش تقویم",
    "delete_calendar_event" to "حذف از تقویم",
    "get_current_date_and_time" to "تاریخ و ساعت",
    "schedule_notification" to "زمان‌بندی یادآوری",
    "send_email" to "ایمیل",
    "send_sms" to "پیامک",
    "manage-tasks" to "کارها",
    "add_task" to "افزودن کار",
    "list_tasks" to "خواندن کارها",
    "complete_task" to "انجام کار",
    "send-sms" to "پیامک",
    "phone-controls" to "کنترل گوشی",
    "alarm-and-timer" to "زنگ ساعت و تایمر",
    "calls-and-contacts" to "تماس و مخاطب",
    "maps-and-web" to "نقشه و وب",
    "flashlight" to "چراغ‌قوه",
    "open_settings" to "تنظیمات",
    "set_alarm" to "زنگ ساعت",
    "set_timer" to "تایمر",
    "dial_number" to "شماره‌گیری",
    "create_contact" to "مخاطب جدید",
    "share_text" to "اشتراک‌گذاری",
    "show_on_map" to "نقشه",
    "web_search" to "جستجوی وب",
  )

private val PATTERNS =
  listOf(
    Regex("^Loading skill \"(.+)\"$") to "در حال آماده‌کردن مهارت «%s»",
    Regex("^Loaded skill \"(.+)\"$") to "مهارت «%s»",
    Regex("^Failed to load skill \"(.+)\"$") to "مهارت «%s» پیدا نشد",
    Regex("^Calling JS script \"(.+)\"$") to "در حال اجرای «%s»",
    Regex("^Called JS script \"(.+)\"$") to "اجرا شد: «%s»",
    Regex("^Failed to call skill \"(.+)\"$") to "اجرای «%s» ناموفق بود",
    Regex("^Executing intent \"(.+)\"$") to "در حال انجام: %s",
    Regex("^Executed intent \"(.+)\"$") to "انجام شد: %s",
    Regex("^Calling MCP tool \"(.+)\"$") to "در حال صدا زدن ابزار MCP «%s»",
    Regex("^Called MCP tool \"(.+)\"$") to "ابزار MCP «%s»",
    Regex("^Succeeded calling MCP tool \"(.+)\"$") to "ابزار MCP «%s»",
    Regex("^Failed to call MCP tool \"(.+)\"$") to "ابزار MCP «%s» ناموفق بود",
    Regex("^Error calling MCP tool \"(.+)\"$") to "خطا در ابزار MCP «%s»",
    Regex("^Permission denied for MCP tool \"(.+)\"$") to "اجازه‌ی ابزار MCP «%s» داده نشد",
  )

/**
 * Turns the English progress titles the tools emit (which the chat logic also matches on, so they
 * stay English internally) into Persian text for display.
 */
fun persianToolLabel(title: String): String {
  for ((regex, template) in PATTERNS) {
    val match = regex.find(title) ?: continue
    val raw = match.groupValues[1]
    val name = TOOL_NAMES[raw] ?: TOOL_NAMES[raw.substringBefore("/")] ?: raw
    return template.format(name)
  }
  return title
}
