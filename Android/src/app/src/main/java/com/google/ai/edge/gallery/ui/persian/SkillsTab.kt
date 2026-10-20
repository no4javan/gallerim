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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.ai.edge.gallery.agent.sessions.LlmSessionManager
import com.google.ai.edge.gallery.customtasks.agentchat.AgentTools
import com.google.ai.edge.gallery.customtasks.agentchat.McpManagerBottomSheet
import com.google.ai.edge.gallery.customtasks.agentchat.McpManagerViewModel
import com.google.ai.edge.gallery.customtasks.agentchat.SkillManagerBottomSheet
import com.google.ai.edge.gallery.customtasks.agentchat.SkillManagerViewModel
import com.google.ai.edge.gallery.data.Model
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Lets screens outside the agent chat reach the shared [AgentTools] singleton. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AgentToolsEntryPoint {
  fun agentTools(): AgentTools
}

/** Lets the start screen list past chats without the chat screen's view model. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SessionsEntryPoint {
  fun llmSessionManager(): LlmSessionManager
}

/** Persian names and one-line explanations for the built-in skills. */
private val SKILL_TEXTS: Map<String, Pair<String, String>> =
  mapOf(
    "create-calendar-event" to ("ساخت رویداد" to "«فردا ساعت ۵ برم دکتر» را در تقویم ثبت می‌کند"),
    "read-calendar-events" to ("خواندن تقویم" to "«امروز چه برنامه‌ای دارم؟»"),
    "update-calendar-event" to ("ویرایش رویداد" to "جابه‌جا کردن یا تغییر نام، با تأیید تو"),
    "delete-calendar-event" to ("حذف رویداد" to "پاک کردن یک قرار، با تأیید تو"),
    "manage-tasks" to ("کارها" to "«پس‌فردا قبض گاز رو بدم» را به کارهایت اضافه می‌کند"),
    "send-sms" to ("پیامک" to "پیش‌نویس پیامک، ارسال با خودت"),
    "phone-controls" to ("کنترل گوشی" to "چراغ‌قوه و باز کردن تنظیمات"),
    "alarm-and-timer" to ("زنگ ساعت و تایمر" to "«ساعت ۶:۳۰ صبح بیدارم کن»"),
    "calls-and-contacts" to ("تماس و مخاطب" to "شماره‌گیری، مخاطب جدید، اشتراک‌گذاری متن"),
    "maps-and-web" to ("نقشه و وب" to "نشان دادن روی نقشه و جستجو در مرورگر"),
    "schedule-notification" to ("یادآوری" to "«ساعت ۹ شب یادم بنداز قرصم رو بخورم»"),
    "send-email" to ("ایمیل" to "پیش‌نویس ایمیل، ارسال با خودت"),
    "query-wikipedia" to ("ویکی‌پدیا" to "جستجو در ویکی‌پدیا (به اینترنت نیاز دارد)"),
    "interactive-map" to ("نقشه" to "نشان دادن یک مکان روی نقشه (به اینترنت نیاز دارد)"),
    "qr-code" to ("کد QR" to "ساخت کد QR از متن یا لینک"),
    "calculate-hash" to ("محاسبه‌ی hash" to "SHA و MD5 یک متن"),
    "mood-tracker" to ("ثبت حال" to "ثبت و مرور حال روزانه"),
    "learn-something-new" to ("یادگیری" to "هر روز یک چیز تازه"),
    "kitchen-adventure" to ("آشپزی" to "یک بازی آشپزی کوچک"),
    "text-spinner" to ("بازنویسی متن" to "چرخاندن و بازنویسی یک متن"),
  )

private enum class SkillsSection(val label: String) {
  SKILLS("مهارت‌ها"),
  MCP("MCP"),
  PERMISSIONS("دسترسی‌ها"),
}

/** The skills tab: built-in and added skills, MCP servers, and the Android permissions they use. */
@Composable
fun SkillsTab(
  chatModel: Model?,
  modifier: Modifier = Modifier,
  skillManagerViewModel: SkillManagerViewModel = hiltViewModel(),
  mcpManagerViewModel: McpManagerViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  var section by rememberSaveable { mutableStateOf(SkillsSection.SKILLS) }

  Column(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
      Text("مهارت‌ها", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
      Text(
        "کارهایی که دستیار می‌تواند خودش انجام دهد",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    PrimaryTabRow(selectedTabIndex = section.ordinal) {
      for (entry in SkillsSection.entries) {
        Tab(
          selected = section == entry,
          onClick = { section = entry },
          text = { Text(entry.label) },
        )
      }
    }
    when (section) {
      SkillsSection.SKILLS -> SkillsList(chatModel, skillManagerViewModel, context)
      SkillsSection.MCP -> McpSection(mcpManagerViewModel)
      SkillsSection.PERMISSIONS -> PermissionsSection()
    }
  }
}

@Composable
private fun SkillsList(
  chatModel: Model?,
  skillManagerViewModel: SkillManagerViewModel,
  context: Context,
) {
  val uiState by skillManagerViewModel.uiState.collectAsState()
  var showManager by remember { mutableStateOf(false) }
  val agentTools = remember {
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        AgentToolsEntryPoint::class.java,
      )
      .agentTools()
  }

  LaunchedEffect(chatModel?.name) {
    if (!skillManagerViewModel.skillLoaded) skillManagerViewModel.loadSkills(chatModel)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(key = "intro") {
      Text(
        "دستیار خودش تشخیص می‌دهد کدام مهارت به کار می‌آید. مهارتی را که نمی‌خواهی خاموش کن. تغییرها از گفتگوی بعدی اعمال می‌شوند.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    items(uiState.skills, key = { it.skill.name }) { state ->
      val skill = state.skill
      val (title, subtitle) =
        SKILL_TEXTS[skill.name] ?: (skill.name to skill.description.take(90))
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
              subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
            )
          }
          Switch(
            checked = skill.selected,
            onCheckedChange = { skillManagerViewModel.setSkillSelected(state, it) },
          )
        }
      }
    }
    item(key = "manage") {
      FilledTonalButton(
        onClick = { showManager = true },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      ) {
        Text("افزودن مهارت تازه یا مدیریت پیشرفته")
      }
    }
  }

  if (showManager) {
    SkillManagerBottomSheet(
      agentTools = agentTools,
      skillManagerViewModel = skillManagerViewModel,
      onDismiss = { showManager = false },
    )
  }
}

@Composable
private fun McpSection(mcpManagerViewModel: McpManagerViewModel) {
  val uiState by mcpManagerViewModel.uiState.collectAsState()
  var showManager by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { mcpManagerViewModel.loadMcpServers() }

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      "MCP راهی است برای وصل کردن دستیار به ابزارهای بیرونی، مثلاً یک سرور روی کامپیوتر خانه. فعلاً فقط سرورهای HTTP پشتیبانی می‌شوند و این قابلیت آزمایشی است. با MCP، درخواست تو از گوشی بیرون می‌رود.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (uiState.mcpServers.isEmpty()) {
      Text("هنوز سروری اضافه نکرده‌ای.", style = MaterialTheme.typography.bodyMedium)
    }
    for (server in uiState.mcpServers) {
      val enabledTools = server.mcpServer.toolsList.count { it.enabled }
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            server.mcpServer.url,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
          Text(
            (if (server.mcpServer.enabled) "روشن" else "خاموش") + " · $enabledTools ابزار فعال",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    Button(onClick = { showManager = true }, modifier = Modifier.fillMaxWidth()) {
      Text("افزودن یا مدیریت سرورهای MCP")
    }
  }

  if (showManager) {
    McpManagerBottomSheet(
      mcpManagerViewModel = mcpManagerViewModel,
      onDismiss = { showManager = false },
    )
  }
}

private class PermissionItem(val title: String, val why: String, val permissions: List<String>)

private val PERMISSION_ITEMS =
  listOf(
    PermissionItem(
      "تقویم",
      "برای ثبت، خواندن، جابه‌جا کردن و حذف قرارها",
      listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
    ),
    PermissionItem(
      "اعلان‌ها",
      "برای یادآوری‌ها و کارها",
      listOf(Manifest.permission.POST_NOTIFICATIONS),
    ),
    PermissionItem(
      "میکروفون",
      "برای حالت صوتی و صدا به متن",
      listOf(Manifest.permission.RECORD_AUDIO),
    ),
    PermissionItem("دوربین", "برای پرسیدن از عکس", listOf(Manifest.permission.CAMERA)),
    PermissionItem(
      "عکس‌ها و ویدیوها",
      "برای جستجوی گالری و پیدا کردن لحظه در ویدیو",
      listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO),
    ),
  )

@Composable
private fun PermissionsSection() {
  val context = LocalContext.current
  // Bumped whenever permissions may have changed, so the list re-reads them.
  var refresh by remember { mutableIntStateOf(0) }
  LifecycleResumeEffect(Unit) {
    refresh++
    onPauseOrDispose {}
  }
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      refresh++
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(key = "intro") {
      Text(
        "همه‌چیز روی گوشی می‌ماند. هر دسترسی فقط وقتی استفاده می‌شود که کارش را بخواهی.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    items(PERMISSION_ITEMS, key = { it.title }) { item ->
      val granted =
        remember(refresh) {
          item.permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
          }
        }
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
              item.why,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (granted) {
            Text(
              "داده شده",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          } else {
            TextButton(onClick = { launcher.launch(item.permissions.toTypedArray()) }) {
              Text("اجازه بده")
            }
          }
        }
      }
    }
    item(key = "settings") {
      TextButton(
        onClick = {
          context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
              .setData(Uri.fromParts("package", context.packageName, null))
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          )
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("گرفتن دسترسی در تنظیمات گوشی")
      }
    }
  }
}
