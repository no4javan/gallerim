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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.todo.TodoItem
import com.google.ai.edge.gallery.todo.TodoStore
import java.util.Calendar
import java.util.TimeZone

/** Writes Latin digits as Persian ones, for display only. */
fun String.toPersianDigits(): String =
  map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")

private fun startOfDay(offsetDays: Int): Long =
  Calendar.getInstance()
    .apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      add(Calendar.DATE, offsetDays)
    }
    .timeInMillis

private fun atTime(offsetDays: Int, hour: Int, minute: Int = 0): Long =
  startOfDay(offsetDays) + (hour * 60L + minute) * 60_000L

/** "دیروز", "امروز ۱۸:۰۰", "فردا ۰۹:۰۰" or "۲۰۲۶/۱۰/۲۵ ۰۹:۰۰". */
fun formatDue(millis: Long): String {
  val cal = Calendar.getInstance().apply { timeInMillis = millis }
  val time = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
  val day =
    when {
      millis >= startOfDay(-1) && millis < startOfDay(0) -> "دیروز"
      millis >= startOfDay(0) && millis < startOfDay(1) -> "امروز"
      millis >= startOfDay(1) && millis < startOfDay(2) -> "فردا"
      else ->
        "%d/%02d/%02d"
          .format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }
  return "$day $time".toPersianDigits()
}

private enum class TaskGroup(val label: String) {
  TODAY("امروز"),
  TOMORROW("فردا"),
  LATER("بعداً"),
  ANYTIME("بدون زمان"),
  DONE("انجام شده"),
}

private fun groupOf(item: TodoItem): TaskGroup {
  val due = item.dueMillis
  return when {
    item.done -> TaskGroup.DONE
    due == null -> TaskGroup.ANYTIME
    // Overdue items stay on today so they are not missed.
    due < startOfDay(1) -> TaskGroup.TODAY
    due < startOfDay(2) -> TaskGroup.TOMORROW
    else -> TaskGroup.LATER
  }
}

/** The tasks and reminders tool: a local to-do list, also used by the assistant's task skill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(navigateUp: () -> Unit) {
  val context = LocalContext.current
  LaunchedEffect(Unit) { TodoStore.ensureLoaded(context) }
  val items by TodoStore.items.collectAsState()
  var input by rememberSaveable { mutableStateOf("") }
  var due by rememberSaveable { mutableStateOf<Long?>(null) }
  var showDueMenu by remember { mutableStateOf(false) }

  val add = {
    val title = input.trim()
    if (title.isNotEmpty()) {
      TodoStore.add(context, title, due, remind = due != null)
      input = ""
      due = null
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("کارها و یادآوری‌ها", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "برگشت")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).fillMaxSize().imePadding()) {
      val open = items.count { !it.done }
      val done = items.size - open
      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item(key = "summary") {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              if (items.isEmpty()) "هنوز کاری نداری. اولین کار را پایین بنویس."
              else "${open} کار مانده از ${items.size}".toPersianDigits(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
            if (items.isNotEmpty()) {
              Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                  progress = { done.toFloat() / items.size },
                  modifier = Modifier.size(48.dp),
                  trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text(
                  "٪${done * 100 / items.size}".toPersianDigits(),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
        val grouped =
          items.sortedBy { it.dueMillis ?: Long.MAX_VALUE }.groupBy(::groupOf)
        for (group in TaskGroup.entries) {
          val groupItems = grouped[group].orEmpty()
          if (groupItems.isEmpty()) continue
          item(key = "group_${group.name}") {
            Text(
              group.label,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 8.dp),
            )
          }
          items(groupItems, key = { it.id }) { item -> TaskRow(item) }
        }
      }

      Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
      ) {
        Column(modifier = Modifier.padding(6.dp)) {
          due?.let { dueMillis ->
            InputChip(
              selected = true,
              onClick = { due = null },
              label = { Text("یادآوری: ${formatDue(dueMillis)} ✕") },
              leadingIcon = { Icon(Icons.Outlined.Alarm, null, Modifier.size(18.dp)) },
              modifier = Modifier.padding(start = 8.dp),
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
              value = input,
              onValueChange = { input = it },
              placeholder = { Text("مثلاً: قبض گاز رو بدم") },
              modifier = Modifier.weight(1f),
              maxLines = 3,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { add() }),
              colors =
                TextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  focusedIndicatorColor = Color.Transparent,
                  unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            IconButton(onClick = { showDueMenu = true }) {
              Icon(Icons.Outlined.Alarm, contentDescription = "زمان یادآوری")
            }
            IconButton(
              onClick = add,
              enabled = input.isNotBlank(),
              modifier = Modifier.size(48.dp),
              colors =
                IconButtonDefaults.iconButtonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                  disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                  disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = "افزودن")
            }
          }
        }
      }
    }
  }

  if (showDueMenu) {
    DuePickerDialog(
      onPicked = {
        due = it
        showDueMenu = false
      },
      onDismiss = { showDueMenu = false },
    )
  }
}

@Composable
private fun TaskRow(item: TodoItem) {
  val context = LocalContext.current
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier.clickable { TodoStore.setDone(context, item.id, !item.done) }
          .padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = item.done,
        onCheckedChange = { TodoStore.setDone(context, item.id, it) },
      )
      Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
        Text(
          item.title,
          style = MaterialTheme.typography.bodyLarge,
          color =
            if (item.done) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
          textDecoration = if (item.done) TextDecoration.LineThrough else null,
        )
        item.dueMillis?.let {
          Text(
            (if (item.remind && !item.done) "⏰ " else "") + formatDue(it),
            style = MaterialTheme.typography.bodySmall,
            color =
              if (!item.done && it < System.currentTimeMillis()) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      IconButton(onClick = { TodoStore.delete(context, item.id) }) {
        Icon(
          Icons.Outlined.DeleteOutline,
          contentDescription = "حذف",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/** Quick reminder times, plus a full date and time picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuePickerDialog(onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
  var step by remember { mutableStateOf(0) } // 0 quick list, 1 date, 2 time
  val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
  val timeState = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = true)
  val now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

  when (step) {
    0 ->
      AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کی یادت بیندازم؟") },
        text = {
          Column {
            val options = buildList {
              if (now < 18) add("امروز عصر، ساعت ۱۸" to atTime(0, 18))
              if (now < 21) add("امروز شب، ساعت ۲۱" to atTime(0, 21))
              add("فردا صبح، ساعت ۹" to atTime(1, 9))
              add("فردا عصر، ساعت ۱۸" to atTime(1, 18))
              add("هفته‌ی بعد، ساعت ۹" to atTime(7, 9))
            }
            for ((label, millis) in options) {
              TextButton(onClick = { onPicked(millis) }, modifier = Modifier.fillMaxWidth()) {
                Text(label, modifier = Modifier.fillMaxWidth())
              }
            }
            TextButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) {
              Text("انتخاب تاریخ و ساعت…", modifier = Modifier.fillMaxWidth())
            }
          }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } },
      )
    1 ->
      DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { step = 2 }) { Text("بعدی") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } },
      ) {
        DatePicker(state = dateState)
      }
    else ->
      AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ساعت") },
        text = { TimePicker(state = timeState) },
        confirmButton = {
          TextButton(
            onClick = {
              // The date picker returns UTC midnight of the chosen day.
              val utc =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                  timeInMillis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                }
              val local =
                Calendar.getInstance().apply {
                  set(
                    utc.get(Calendar.YEAR),
                    utc.get(Calendar.MONTH),
                    utc.get(Calendar.DAY_OF_MONTH),
                    timeState.hour,
                    timeState.minute,
                    0,
                  )
                  set(Calendar.MILLISECOND, 0)
                }
              onPicked(local.timeInMillis)
            }
          ) {
            Text("تأیید")
          }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } },
      )
  }
}
