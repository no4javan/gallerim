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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.data.BuiltInTaskId

/** What tapping a capability does. */
sealed interface ToolTarget {
  /** Puts an example sentence in the chat box, so the user can edit it before sending. */
  data class Example(val prompt: String) : ToolTarget

  /** Opens one of the built-in task screens. */
  data class TaskScreen(val taskId: String) : ToolTarget

  data object TasksList : ToolTarget

  data object Voice : ToolTarget
}

private class Capability(
  val icon: ImageVector,
  val title: String,
  val detail: String,
  val target: ToolTarget,
)

private class Section(val title: String, val items: List<Capability>)

private fun ex(prompt: String) = ToolTarget.Example(prompt)

/**
 * Every action the assistant can really perform on this phone. Chat actions map to the agent's
 * skills and intents (IntentHandler); the last section lists tools that have their own screen.
 */
private val SECTIONS =
  listOf(
    Section(
      "تقویم",
      listOf(
        Capability(Icons.Outlined.Event, "ثبت قرار", "«فردا ساعت ۵ برم دکتر»", ex("فردا ساعت ۵ برم دکتر")),
        Capability(Icons.Outlined.CalendarMonth, "دیدن برنامه‌ی یک روز", "«فردا چه برنامه‌ای دارم؟»", ex("فردا چه برنامه‌ای دارم؟")),
        Capability(Icons.Outlined.EditCalendar, "جابه‌جا کردن قرار", "«قرار دکتر فردا رو بنداز ساعت ۶» · با تأیید تو", ex("قرار دکتر فردا رو بنداز ساعت ۶")),
        Capability(Icons.Outlined.DeleteOutline, "حذف قرار", "«قرار دکتر فردا رو حذف کن» · با تأیید تو", ex("قرار دکتر فردا رو حذف کن")),
      ),
    ),
    Section(
      "کارها و یادآوری",
      listOf(
        Capability(Icons.Outlined.Checklist, "افزودن کار", "«پس‌فردا قبض گاز رو بدم»", ex("پس‌فردا ساعت ۹ صبح قبض گاز رو بدم")),
        Capability(Icons.Outlined.TaskAlt, "دیدن و تیک زدن کارها", "«چه کارهایی دارم؟»", ex("چه کارهایی دارم؟")),
        Capability(Icons.Outlined.NotificationsActive, "یادآوری", "«ساعت ۹ شب یادم بنداز قرصم رو بخورم»", ex("ساعت ۹ شب یادم بنداز قرصم رو بخورم")),
        Capability(Icons.AutoMirrored.Outlined.OpenInNew, "لیست کارها", "صفحه‌ی کارها را باز می‌کند", ToolTarget.TasksList),
      ),
    ),
    Section(
      "ساعت",
      listOf(
        Capability(Icons.Outlined.Alarm, "زنگ ساعت", "«ساعت ۶:۳۰ صبح بیدارم کن»", ex("ساعت ۶:۳۰ صبح بیدارم کن")),
        Capability(Icons.Outlined.Timer, "تایمر", "«۱۰ دقیقه تایمر بذار»", ex("۱۰ دقیقه تایمر بذار")),
      ),
    ),
    Section(
      "گوشی",
      listOf(
        Capability(Icons.Outlined.FlashlightOn, "چراغ‌قوه", "«چراغ‌قوه رو روشن کن»", ex("چراغ‌قوه رو روشن کن")),
        Capability(Icons.Outlined.Settings, "باز کردن تنظیمات", "Wi-Fi، بلوتوث، صدا، باتری، مکان", ex("تنظیمات بلوتوث رو باز کن")),
      ),
    ),
    Section(
      "تماس و پیام",
      listOf(
        Capability(Icons.Outlined.Phone, "شماره‌گیری", "«به ۰۹۱۲۰۰۰۰۰۰۰ زنگ بزن» · دکمه‌ی تماس با خودت", ex("به ۰۹۱۲۰۰۰۰۰۰۰ زنگ بزن")),
        Capability(Icons.Outlined.Contacts, "مخاطب جدید", "«علی رضایی با شماره‌ی ۰۹۱۲۰۰۰۰۰۰۰ رو ذخیره کن»", ex("علی رضایی با شماره‌ی ۰۹۱۲۰۰۰۰۰۰۰ رو ذخیره کن")),
        Capability(Icons.AutoMirrored.Outlined.Message, "پیش‌نویس پیامک", "ارسال با خودت", ex("به ۰۹۱۲۰۰۰۰۰۰۰ پیامک بده که دیر می‌رسم")),
        Capability(Icons.Outlined.Email, "پیش‌نویس ایمیل", "ارسال با خودت", ex("به ali@example.com ایمیل بزن که جلسه فردا ساعت ۱۰ است")),
        Capability(Icons.Outlined.Share, "فرستادن متن به برنامه‌ی دیگر", "مثلاً تلگرام یا واتس‌اپ", ex("یک پیام تبریک تولد کوتاه بنویس و به اشتراک بذار")),
      ),
    ),
    Section(
      "نقشه و اینترنت",
      listOf(
        Capability(Icons.Outlined.Map, "نشان دادن روی نقشه", "«میدان آزادی رو روی نقشه نشون بده»", ex("میدان آزادی رو روی نقشه نشون بده")),
        Capability(Icons.Outlined.Search, "جستجو در وب", "در مرورگر باز می‌شود", ex("در اینترنت دنبال ساعت کاری کتابخانه ملی بگرد")),
        Capability(Icons.AutoMirrored.Outlined.MenuBook, "ویکی‌پدیا", "«در ویکی‌پدیا درباره‌ی حافظ بگرد»", ex("در ویکی‌پدیا درباره‌ی حافظ بگرد")),
      ),
    ),
    Section(
      "دیدن و شنیدن",
      listOf(
        Capability(Icons.Outlined.Mic, "حالت صوتی", "حرف بزن، دستیار گوش می‌دهد", ToolTarget.Voice),
        Capability(Icons.Outlined.Image, "پرسش از عکس", "قبض، منو، نوشته، اشیا", ToolTarget.TaskScreen(BuiltInTaskId.LLM_ASK_IMAGE)),
        Capability(Icons.Outlined.GraphicEq, "صدا به متن", "رونویسی و ترجمه‌ی صدا", ToolTarget.TaskScreen(BuiltInTaskId.LLM_ASK_AUDIO)),
        Capability(Icons.Outlined.PhotoLibrary, "جستجو در گالری", "پیدا کردن عکس با یک جمله", ToolTarget.TaskScreen(BuiltInTaskId.SMART_ALBUM)),
        Capability(Icons.Outlined.VideoLibrary, "لحظه در ویدیو", "پیدا کردن یک صحنه", ToolTarget.TaskScreen(BuiltInTaskId.VIDEO_MOMENT_FINDER)),
      ),
    ),
  )

/** The tools tab: every action the assistant can really do, grouped, each with an example. */
@Composable
fun ToolsTab(
  availableTaskIds: Set<String>,
  onTarget: (ToolTarget) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(key = "header") {
      Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text("ابزارها", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
          "همه‌ی کارهایی که دستیار روی این گوشی بلد است. هر کدام را بزنی، نمونه‌اش در گفتگو آماده می‌شود تا ویرایش کنی و بفرستی.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    for (section in SECTIONS) {
      val items =
        section.items.filter {
          val target = it.target
          target !is ToolTarget.TaskScreen || target.taskId in availableTaskIds
        }
      if (items.isEmpty()) continue
      item(key = "title_${section.title}") {
        Text(
          section.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      item(key = "card_${section.title}") {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
          items.forEachIndexed { index, capability ->
            CapabilityRow(capability, onClick = { onTarget(capability.target) })
            if (index < items.lastIndex) {
              HorizontalDivider(
                modifier = Modifier.padding(start = 64.dp, end = 14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
              )
            }
          }
        }
      }
    }
    item(key = "limits") {
      Text(
        "کارهایی که اندروید به هیچ برنامه‌ای اجازه نمی‌دهد: خواندن پیامک‌ها و اعلان‌ها، کنترل برنامه‌های دیگر، فرستادن پیام یا تماس بدون زدن دکمه توسط خودت، و روشن و خاموش کردن مستقیم Wi-Fi یا بلوتوث.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
      )
    }
  }
}

@Composable
private fun CapabilityRow(capability: Capability, onClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier =
        Modifier.size(40.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        capability.icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(capability.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Text(
        capability.detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
