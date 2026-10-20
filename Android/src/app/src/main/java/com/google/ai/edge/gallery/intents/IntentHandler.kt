/*
 * Copyright 2026 Google LLC
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
package com.google.ai.edge.gallery.intents

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import android.provider.CalendarContract.Reminders
import android.util.Log
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.net.toUri
import com.google.ai.edge.gallery.notifications.NotificationScheduleManagerEntryPoint
import com.google.ai.edge.gallery.proto.ScheduledNotification
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.EntryPointAccessors
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@JsonClass(generateAdapter = true)
data class SendEmailParams(
  val extra_email: String,
  val extra_subject: String,
  val extra_text: String,
)

@JsonClass(generateAdapter = true)
data class SendSmsParams(val phone_number: String, val sms_body: String)

@JsonClass(generateAdapter = true)
data class CreateCalendarEventParams(
  val title: String,
  val description: String,
  val begin_time: String,
  val end_time: String,
)

@JsonClass(generateAdapter = true) data class ReadCalendarEventsParams(val date: String)

@JsonClass(generateAdapter = true)
data class UpdateCalendarEventParams(
  val event_id: Long,
  val title: String? = null,
  val description: String? = null,
  val begin_time: String? = null,
  val end_time: String? = null,
)

@JsonClass(generateAdapter = true) data class DeleteCalendarEventParams(val event_id: Long)

@JsonClass(generateAdapter = true)
data class CalendarEventDto(
  val title: String,
  val description: String,
  val begin_time: String,
  val end_time: String,
  val event_id: Long = 0,
)

@JsonClass(generateAdapter = true)
data class ReadCalendarEventsResponse(val events: List<CalendarEventDto>)

enum class IntentAction(val action: String) {
  SEND_EMAIL("send_email"),
  SEND_SMS("send_sms"),
  CREATE_CALENDAR_EVENT("create_calendar_event"),
  READ_CALENDAR_EVENTS("read_calendar_events"),
  UPDATE_CALENDAR_EVENT("update_calendar_event"),
  DELETE_CALENDAR_EVENT("delete_calendar_event"),
  GET_CURRENT_DATE_AND_TIME("get_current_date_and_time"),
  SCHEDULE_NOTIFICATION("schedule_notification");

  companion object {
    fun from(action: String): IntentAction? = entries.find { it.action == action }
  }
}

@JsonClass(generateAdapter = true)
data class ScheduleNotificationParams(
  val title: String,
  val message: String,
  val hour: Int,
  val minute: Int,
  val deeplink: String? = null,
  val task_id: String? = null,
  val model_name: String? = null,
  val year: Int? = null,
  val month: Int? = null,
  val day: Int? = null,
  val repeat_daily: Boolean? = null,
)

/** What the user is asked to approve before an intent changes their data. */
data class ConfirmRequest(
  val title: String,
  val detail: String,
  val confirmLabel: String,
  val destructive: Boolean,
)

object IntentHandler {
  private const val TAG = "IntentHandler"
  private const val DEFAULT_EVENT_DURATION_MS = 60 * 60 * 1000L
  private const val DEFAULT_REMINDER_MINUTES = 15

  suspend fun handleAction(
    context: Context,
    action: String,
    parameters: String,
    // requestPermission is a suspend function that takes a permission string and returns true if
    // the permission is granted, false otherwise.
    requestPermission: suspend (String) -> Boolean,
    // confirm shows the user what is about to change and returns true only if they approve it.
    confirm: suspend (ConfirmRequest) -> Boolean = { false },
  ): String {
    return when (IntentAction.from(action)) {
      IntentAction.SEND_EMAIL -> {
        try {
          val moshi = Moshi.Builder().build()
          val jsonAdapter = moshi.adapter(SendEmailParams::class.java)
          val params = jsonAdapter.fromJson(parameters)
          if (params != null) {
            val intent =
              Intent(Intent.ACTION_SEND).apply {
                data = "mailto:".toUri()
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(params.extra_email))
                putExtra(Intent.EXTRA_SUBJECT, params.extra_subject)
                putExtra(Intent.EXTRA_TEXT, params.extra_text)
              }
            context.startActivity(intent)
            "succeeded"
          } else {
            Log.e(TAG, "Failed to parse send_email parameters: $parameters")
            "failed"
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to parse send_email parameters: $parameters", e)
          "failed"
        }
      }
      IntentAction.SEND_SMS -> {
        try {
          val moshi = Moshi.Builder().build()
          val jsonAdapter = moshi.adapter(SendSmsParams::class.java)
          val params = jsonAdapter.fromJson(parameters)
          if (params != null) {
            val uri = "smsto:${params.phone_number}".toUri()
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            intent.putExtra("sms_body", params.sms_body)
            context.startActivity(intent)
            "succeeded"
          } else {
            Log.e(TAG, "Failed to parse send_sms parameters: $parameters")
            "failed"
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to parse send_sms parameters: $parameters", e)
          "failed"
        }
      }
      IntentAction.CREATE_CALENDAR_EVENT -> {
        createCalendarEvent(context, parameters, requestPermission)
      }
      IntentAction.READ_CALENDAR_EVENTS -> {
        readCalendarEvents(context, parameters, requestPermission)
      }
      IntentAction.UPDATE_CALENDAR_EVENT -> {
        updateCalendarEvent(context, parameters, requestPermission, confirm)
      }
      IntentAction.DELETE_CALENDAR_EVENT -> {
        deleteCalendarEvent(context, parameters, requestPermission, confirm)
      }
      IntentAction.GET_CURRENT_DATE_AND_TIME -> {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss EEEE", Locale.US)
        val currentDateAndTime = sdf.format(Date())
        Log.d(
          TAG,
          "get_current_date_and_time via handleAction. Current date and time: $currentDateAndTime",
        )
        currentDateAndTime
      }
      IntentAction.SCHEDULE_NOTIFICATION -> {
        scheduleNotification(context, parameters)
      }
      null -> "failed"
    }
  }

  suspend fun readCalendarEvents(
    context: Context,
    parameters: String,
    requestPermission: suspend (String) -> Boolean,
  ): String {
    if (
      checkSelfPermission(context, Manifest.permission.READ_CALENDAR) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      val granted = requestPermission(Manifest.permission.READ_CALENDAR)
      if (!granted) {
        Log.e(TAG, "READ_CALENDAR permission denied by user")
        return "failed: READ_CALENDAR permission denied by user"
      }
    }

    try {
      val moshi = Moshi.Builder().build()
      val jsonAdapter = moshi.adapter(ReadCalendarEventsParams::class.java)
      val params = jsonAdapter.fromJson(parameters)
      if (params != null) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateObj = format.parse(params.date)
        if (dateObj != null) {
          val cal =
            Calendar.getInstance().apply {
              timeInMillis = dateObj.time
              set(Calendar.HOUR_OF_DAY, 0)
              set(Calendar.MINUTE, 0)
              set(Calendar.SECOND, 0)
              set(Calendar.MILLISECOND, 0)
            }
          val startOfDayMillis = cal.timeInMillis

          cal.apply {
            add(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MILLISECOND, -1)
          }
          val endOfDayMillis = cal.timeInMillis

          val projection =
            arrayOf(
              Instances.TITLE,
              Instances.DESCRIPTION,
              Instances.BEGIN,
              Instances.END,
              Instances.EVENT_ID,
            )

          val builder = Instances.CONTENT_URI.buildUpon()
          ContentUris.appendId(builder, startOfDayMillis)
          ContentUris.appendId(builder, endOfDayMillis)

          val cursor =
            context.contentResolver.query(
              builder.build(),
              projection,
              null,
              null,
              "${Instances.BEGIN} ASC",
            )

          val eventsList = mutableListOf<CalendarEventDto>()
          cursor?.use { c ->
            val titleIdx = c.getColumnIndex(Instances.TITLE)
            val descIdx = c.getColumnIndex(Instances.DESCRIPTION)
            val startIdx = c.getColumnIndex(Instances.BEGIN)
            val endIdx = c.getColumnIndex(Instances.END)
            val eventIdIdx = c.getColumnIndex(Instances.EVENT_ID)
            val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            while (c.moveToNext()) {
              val title = if (titleIdx >= 0) c.getString(titleIdx) ?: "" else ""
              val desc = if (descIdx >= 0) c.getString(descIdx) ?: "" else ""
              val start = if (startIdx >= 0) c.getLong(startIdx) else 0L
              val end = if (endIdx >= 0) c.getLong(endIdx) else 0L
              val eventId = if (eventIdIdx >= 0) c.getLong(eventIdIdx) else 0L
              eventsList.add(
                CalendarEventDto(
                  title = title,
                  description = desc,
                  begin_time = if (start > 0) timeFormat.format(Date(start)) else "",
                  end_time = if (end > 0) timeFormat.format(Date(end)) else "",
                  event_id = eventId,
                )
              )
            }
          }
          val responseAdapter = moshi.adapter(ReadCalendarEventsResponse::class.java)
          return responseAdapter.toJson(ReadCalendarEventsResponse(eventsList))
        } else {
          Log.e(TAG, "Failed to parse read_calendar_events date: ${params.date}")
          return "failed"
        }
      } else {
        Log.e(TAG, "Failed to parse read_calendar_events parameters: $parameters")
        return "failed"
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to read calendar events: $parameters", e)
      return "failed: ${e.message}"
    }
  }

  /**
   * Saves an event straight into the user's calendar. Falls back to opening the calendar app's
   * pre-filled form when calendar permission is denied or no writable calendar exists.
   */
  suspend fun createCalendarEvent(
    context: Context,
    parameters: String,
    requestPermission: suspend (String) -> Boolean,
  ): String {
    val params =
      try {
        Moshi.Builder().build().adapter(CreateCalendarEventParams::class.java).fromJson(parameters)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse create_calendar_event parameters: $parameters", e)
        null
      } ?: return "failed: could not parse parameters"
    val beginMillis =
      parseDateTimeMillis(params.begin_time)
        ?: return "failed: begin_time must be in YYYY-MM-DDTHH:MM:SS format"
    val endMillis =
      parseDateTimeMillis(params.end_time)?.takeIf { it > beginMillis }
        ?: (beginMillis + DEFAULT_EVENT_DURATION_MS)

    if (ensureCalendarPermissions(context, requestPermission)) {
      try {
        val calendarId = findWritableCalendarId(context)
        if (calendarId != null) {
          val values =
            ContentValues().apply {
              put(Events.CALENDAR_ID, calendarId)
              put(Events.TITLE, params.title)
              put(Events.DESCRIPTION, params.description)
              put(Events.DTSTART, beginMillis)
              put(Events.DTEND, endMillis)
              put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
              put(Events.HAS_ALARM, 1)
            }
          val eventId =
            context.contentResolver.insert(Events.CONTENT_URI, values)?.lastPathSegment?.toLongOrNull()
          if (eventId != null) {
            val reminder =
              ContentValues().apply {
                put(Reminders.EVENT_ID, eventId)
                put(Reminders.MINUTES, DEFAULT_REMINDER_MINUTES)
                put(Reminders.METHOD, Reminders.METHOD_ALERT)
              }
            context.contentResolver.insert(Reminders.CONTENT_URI, reminder)
            return "succeeded: event saved to calendar with event_id=$eventId"
          }
        }
        Log.w(TAG, "No writable calendar found, falling back to the calendar app")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to insert calendar event, falling back to the calendar app", e)
      }
    }

    return try {
      val intent =
        Intent(Intent.ACTION_INSERT).apply {
          data = Events.CONTENT_URI
          putExtra(Events.TITLE, params.title)
          putExtra(Events.DESCRIPTION, params.description)
          putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
          putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        }
      context.startActivity(intent)
      "succeeded: opened the calendar app, the user must tap save"
    } catch (e: Exception) {
      Log.e(TAG, "Failed to open the calendar app", e)
      "failed: ${e.message}"
    }
  }

  /** Changes the title, description or time of an existing event, keeping its duration. */
  suspend fun updateCalendarEvent(
    context: Context,
    parameters: String,
    requestPermission: suspend (String) -> Boolean,
    confirm: suspend (ConfirmRequest) -> Boolean,
  ): String {
    if (!ensureCalendarPermissions(context, requestPermission)) {
      return "failed: calendar permission denied by user"
    }
    return try {
      val params =
        Moshi.Builder().build().adapter(UpdateCalendarEventParams::class.java).fromJson(parameters)
          ?: return "failed: could not parse parameters"
      val eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, params.event_id)
      var oldBegin = 0L
      var oldEnd = 0L
      var oldTitle = ""
      val found =
        context.contentResolver
          .query(eventUri, arrayOf(Events.DTSTART, Events.DTEND, Events.TITLE), null, null, null)
          ?.use { c ->
            if (c.moveToFirst()) {
              oldBegin = c.getLong(0)
              oldEnd = c.getLong(1)
              oldTitle = c.getString(2) ?: ""
              true
            } else false
          } ?: false
      if (!found) return "failed: no event with event_id=${params.event_id}"

      val values = ContentValues()
      params.title?.let { values.put(Events.TITLE, it) }
      params.description?.let { values.put(Events.DESCRIPTION, it) }
      val newBegin =
        params.begin_time?.let {
          parseDateTimeMillis(it) ?: return "failed: begin_time must be YYYY-MM-DDTHH:MM:SS"
        }
      val newEnd =
        params.end_time?.let {
          parseDateTimeMillis(it) ?: return "failed: end_time must be YYYY-MM-DDTHH:MM:SS"
        }
      if (newBegin != null) values.put(Events.DTSTART, newBegin)
      when {
        newEnd != null -> values.put(Events.DTEND, newEnd)
        // Keep the original duration when only the start moves. Recurring events have no DTEND.
        newBegin != null && oldEnd > 0 -> values.put(Events.DTEND, newBegin + (oldEnd - oldBegin))
      }
      if (values.size() == 0) return "failed: nothing to update"

      val changes = buildList {
        params.title?.let { add("عنوان: «$oldTitle» ← «$it»") }
        newBegin?.let { add("زمان: ${formatForUser(oldBegin)} ← ${formatForUser(it)}") }
        if (newBegin == null && newEnd != null) add("پایان: ${formatForUser(newEnd)}")
        if (params.description != null) add("توضیحات تغییر می‌کند")
      }
      val approved =
        confirm(
          ConfirmRequest(
            title = "ویرایش «$oldTitle»",
            detail = changes.joinToString("\n"),
            confirmLabel = "اعمال تغییر",
            destructive = false,
          )
        )
      if (!approved) return "cancelled: the user declined, nothing was changed"
      val rows = context.contentResolver.update(eventUri, values, null, null)
      if (rows > 0) "succeeded: updated event_id=${params.event_id}"
      else "failed: no event with event_id=${params.event_id}"
    } catch (e: Exception) {
      Log.e(TAG, "Failed to update calendar event: $parameters", e)
      "failed: ${e.message}"
    }
  }

  suspend fun deleteCalendarEvent(
    context: Context,
    parameters: String,
    requestPermission: suspend (String) -> Boolean,
    confirm: suspend (ConfirmRequest) -> Boolean,
  ): String {
    if (!ensureCalendarPermissions(context, requestPermission)) {
      return "failed: calendar permission denied by user"
    }
    return try {
      val params =
        Moshi.Builder().build().adapter(DeleteCalendarEventParams::class.java).fromJson(parameters)
          ?: return "failed: could not parse parameters"
      val eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, params.event_id)
      val summary =
        context.contentResolver
          .query(eventUri, arrayOf(Events.TITLE, Events.DTSTART), null, null, null)
          ?.use { c ->
            if (c.moveToFirst()) "«${c.getString(0) ?: ""}»\n${formatForUser(c.getLong(1))}"
            else null
          } ?: return "failed: no event with event_id=${params.event_id}"
      val approved =
        confirm(
          ConfirmRequest(
            title = "حذف رویداد از تقویم",
            detail = summary,
            confirmLabel = "حذف کن",
            destructive = true,
          )
        )
      if (!approved) return "cancelled: the user declined, nothing was deleted"
      val rows =
        context.contentResolver.delete(
          ContentUris.withAppendedId(Events.CONTENT_URI, params.event_id),
          null,
          null,
        )
      if (rows > 0) "succeeded: deleted event_id=${params.event_id}"
      else "failed: no event with event_id=${params.event_id}"
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete calendar event: $parameters", e)
      "failed: ${e.message}"
    }
  }

  /** Formats a time for the confirmation card, e.g. "2026-10-21 18:00". */
  private fun formatForUser(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))

  private suspend fun ensureCalendarPermissions(
    context: Context,
    requestPermission: suspend (String) -> Boolean,
  ): Boolean {
    for (permission in listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) {
      if (
        checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
          !requestPermission(permission)
      ) {
        Log.e(TAG, "$permission denied by user")
        return false
      }
    }
    return true
  }

  /** Returns the primary writable calendar, or the first writable one. */
  private fun findWritableCalendarId(context: Context): Long? {
    val projection = arrayOf(Calendars._ID, Calendars.IS_PRIMARY)
    val selection = "${Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${Calendars.VISIBLE} = 1"
    val args = arrayOf(Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
    context.contentResolver.query(Calendars.CONTENT_URI, projection, selection, args, null)?.use { c
      ->
      var first: Long? = null
      while (c.moveToNext()) {
        val id = c.getLong(0)
        if (c.getInt(1) == 1) return id
        if (first == null) first = id
      }
      return first
    }
    return null
  }

  /** Parses the model's date-times, with or without seconds. */
  private fun parseDateTimeMillis(value: String): Long? {
    val patterns =
      listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm")
    for (pattern in patterns) {
      try {
        val format = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
        format.parse(value.trim())?.let {
          return it.time
        }
      } catch (_: Exception) {}
    }
    return null
  }

  fun scheduleNotification(context: Context, parameters: String): String {
    try {
      val moshi = Moshi.Builder().build()
      val jsonAdapter = moshi.adapter(ScheduleNotificationParams::class.java)
      val params = jsonAdapter.fromJson(parameters)
      if (params != null) {
        val notificationProtoBuilder =
          ScheduledNotification.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTitle(params.title)
            .setMessage(params.message)
            .setHour(params.hour)
            .setMinute(params.minute)
            .setChannelId("agent_skill_tasks_channel")
            .setChannelName("Agent Skill Task")
        if (params.deeplink != null) {
          notificationProtoBuilder.setDeeplink(params.deeplink)
        } else if (params.task_id != null && params.model_name != null) {
          val uri =
            "com.google.ai.edge.gallery://model/${params.task_id}/${params.model_name}"
              .toUri()
              .buildUpon()
              .appendQueryParameter("query", params.message)
              .build()
              .toString()
          Log.d(TAG, "Setting constructed deeplink to: $uri")
          notificationProtoBuilder.setDeeplink(uri)
        } else if (params.task_id != null) {
          val uri =
            "com.google.ai.edge.gallery://${params.task_id}/"
              .toUri()
              .buildUpon()
              .appendQueryParameter("query", params.message)
              .build()
              .toString()
          Log.d(TAG, "Setting constructed deeplink to: $uri")
          notificationProtoBuilder.setDeeplink(uri)
        } else {
          val fallbackUri =
            "com.google.ai.edge.gallery://llm_agent_chat/"
              .toUri()
              .buildUpon()
              .appendQueryParameter("query", params.message)
              .build()
              .toString()
          Log.d(TAG, "Setting fallback deeplink to: $fallbackUri")
          notificationProtoBuilder.setDeeplink(fallbackUri)
        }
        if (params.year != null) {
          notificationProtoBuilder.setYear(params.year)
        }
        if (params.month != null) {
          notificationProtoBuilder.setMonth(params.month)
        }
        if (params.day != null) {
          notificationProtoBuilder.setDay(params.day)
        }
        if (params.repeat_daily != null) {
          notificationProtoBuilder.setRepeatDaily(params.repeat_daily)
        }

        val entryPoint =
          EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationScheduleManagerEntryPoint::class.java,
          )
        val success =
          entryPoint
            .notificationScheduleManager()
            .scheduleNotification(notificationProtoBuilder.build())
        if (!success) {
          return "failed"
        }
        return "succeeded"
      } else {
        Log.e(TAG, "Failed to parse schedule_notification parameters: $parameters")
        return "failed"
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse schedule_notification parameters: $parameters", e)
      return "failed"
    }
  }
}
