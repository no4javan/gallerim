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

package com.google.ai.edge.gallery.todo

import android.content.Context
import android.util.Log
import com.google.ai.edge.gallery.notifications.NotificationScheduleManagerEntryPoint
import com.google.ai.edge.gallery.proto.ScheduledNotification
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "AGTodoStore"
private const val FILE_NAME = "gallerim_tasks.json"

/** Tapping a task reminder opens the tasks screen. */
const val TASKS_DEEPLINK = "com.google.ai.edge.gallery://persian_tasks"

/** One to-do item. [dueMillis] is optional; with [remind] a notification fires at that time. */
@JsonClass(generateAdapter = true)
data class TodoItem(
  val id: String,
  val title: String,
  val createdMillis: Long,
  val dueMillis: Long? = null,
  val remind: Boolean = false,
  val done: Boolean = false,
)

/**
 * The user's local to-do list, kept in a small JSON file in the app's private storage. Shared by
 * the tasks screen and the assistant's task intents.
 */
object TodoStore {
  private val _items = MutableStateFlow<List<TodoItem>>(emptyList())
  val items: StateFlow<List<TodoItem>> = _items.asStateFlow()
  @Volatile private var loaded = false

  private val adapter by lazy {
    Moshi.Builder()
      .build()
      .adapter<List<TodoItem>>(Types.newParameterizedType(List::class.java, TodoItem::class.java))
  }

  @Synchronized
  fun ensureLoaded(context: Context) {
    if (loaded) return
    val file = File(context.filesDir, FILE_NAME)
    if (file.exists()) {
      try {
        _items.value = adapter.fromJson(file.readText()) ?: emptyList()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to read tasks", e)
      }
    }
    loaded = true
  }

  fun add(context: Context, title: String, dueMillis: Long?, remind: Boolean): TodoItem {
    ensureLoaded(context)
    val item =
      TodoItem(
        id = UUID.randomUUID().toString(),
        title = title.trim(),
        createdMillis = System.currentTimeMillis(),
        dueMillis = dueMillis,
        remind = remind && dueMillis != null,
      )
    _items.update { it + item }
    save(context)
    if (item.remind) scheduleReminder(context, item)
    return item
  }

  fun setDone(context: Context, id: String, done: Boolean) {
    ensureLoaded(context)
    val item = _items.value.find { it.id == id } ?: return
    _items.update { list -> list.map { if (it.id == id) it.copy(done = done) else it } }
    save(context)
    if (item.remind) {
      cancelReminder(context, id)
      if (!done) scheduleReminder(context, item)
    }
  }

  fun delete(context: Context, id: String) {
    ensureLoaded(context)
    val item = _items.value.find { it.id == id } ?: return
    _items.update { list -> list.filter { it.id != id } }
    save(context)
    if (item.remind) cancelReminder(context, id)
  }

  private fun save(context: Context) {
    try {
      File(context.filesDir, FILE_NAME).writeText(adapter.toJson(_items.value))
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save tasks", e)
    }
  }

  private fun scheduleReminder(context: Context, item: TodoItem) {
    val due = item.dueMillis ?: return
    // An alarm in the past would fire at once.
    if (due <= System.currentTimeMillis()) return
    val cal = Calendar.getInstance().apply { timeInMillis = due }
    val notification =
      ScheduledNotification.newBuilder()
        .setId(item.id)
        .setTitle("یادآوری")
        .setMessage(item.title)
        .setYear(cal.get(Calendar.YEAR))
        .setMonth(cal.get(Calendar.MONTH) + 1)
        .setDay(cal.get(Calendar.DAY_OF_MONTH))
        .setHour(cal.get(Calendar.HOUR_OF_DAY))
        .setMinute(cal.get(Calendar.MINUTE))
        .setDeeplink(TASKS_DEEPLINK)
        .setChannelId("gallerim_tasks_channel")
        .setChannelName("کارها و یادآوری‌ها")
        .build()
    manager(context).scheduleNotification(notification)
  }

  private fun cancelReminder(context: Context, id: String) {
    manager(context).removeNotification(id)
  }

  private fun manager(context: Context) =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        NotificationScheduleManagerEntryPoint::class.java,
      )
      .notificationScheduleManager()
}
