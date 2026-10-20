/*
 * Copyright 2026 no4javan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.google.ai.edge.gallery.ui.modelmanager.GlobalModelManager
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.google.ai.edge.gallery.proto.ChatSessionProto
import dagger.hilt.android.EntryPointAccessors
import java.util.Calendar
import kotlinx.coroutines.launch

/** The four bottom tabs of the Persian assistant. */
enum class ShellTab(val label: String, val icon: ImageVector) {
  CHAT("گفتگو", Icons.Outlined.ChatBubbleOutline),
  TOOLS("ابزارها", Icons.Outlined.GridView),
  SKILLS("مهارت‌ها", Icons.Outlined.Bolt),
  MODELS("مدل‌ها", Icons.Outlined.Memory),
}

/** Persian names for the built-in tasks shown on the tools tab. */
private val TOOL_TEXTS: Map<String, Pair<String, String>> =
  mapOf(
    BuiltInTaskId.LLM_ASK_IMAGE to ("پرسش از عکس" to "قبض، منو، نوشته یا هر چیزی در عکس"),
    BuiltInTaskId.LLM_ASK_AUDIO to ("صدا به متن" to "رونویسی و ترجمه‌ی صدا"),
    BuiltInTaskId.SMART_ALBUM to ("جستجوی گالری" to "پیدا کردن عکس با یک جمله"),
    BuiltInTaskId.VIDEO_MOMENT_FINDER to ("لحظه در ویدیو" to "پیدا کردن یک صحنه"),
    BuiltInTaskId.LLM_PROMPT_LAB to ("آزمایشگاه متن" to "خلاصه، بازنویسی، کد"),
    BuiltInTaskId.LLM_CHAT to ("گفتگوی ساده" to "بدون مهارت و ابزار"),
    BuiltInTaskId.LLM_MOBILE_ACTIONS to ("کارهای گوشی" to "چراغ‌قوه، مخاطب، Wi-Fi"),
    BuiltInTaskId.MP_SCRAPBOOK to ("اسکرپ‌بوک" to "جدا کردن سوژه از عکس"),
    BuiltInTaskId.LLM_TINY_GARDEN to ("باغچه‌ی کوچک" to "بازی با جمله‌های ساده"),
  )

/** Tasks that are not tools: the chat tab is the agent chat, and LLM_TEST is for side-loading. */
private val HIDDEN_TOOL_IDS = setOf(BuiltInTaskId.LLM_AGENT_CHAT, BuiltInTaskId.LLM_TEST)

/**
 * Root screen of the Persian assistant: a bottom bar with chat, tools, skills and models.
 *
 * Existing Gallery screens (agent chat, task screens, model manager) are reused; this shell only
 * decides where they are reached from.
 */
@Composable
fun PersianShell(
  modelManagerViewModel: ModelManagerViewModel,
  tosViewModel: TosViewModel,
  onStartChat: (model: Model, prompt: String?) -> Unit,
  onOpenTask: (Task) -> Unit,
  onModelSelected: (Task, Model) -> Unit,
  onBenchmarkClicked: (Model) -> Unit,
  onOpenTasks: () -> Unit,
  onOpenVoice: () -> Unit,
  onOpenSession: (ChatSessionProto) -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val sessionsFlow = remember {
    EntryPointAccessors.fromApplication(context.applicationContext, SessionsEntryPoint::class.java)
      .llmSessionManager()
      .chatSessions
  }
  val allSessions by sessionsFlow.collectAsState(initial = emptyList())
  val chatSessions =
    remember(allSessions) {
      allSessions
        .filter { it.taskId == BuiltInTaskId.LLM_AGENT_CHAT && it.messagesCount > 0 }
        .sortedByDescending { it.timestampMs }
    }
  var currentTab by rememberSaveable { mutableStateOf(ShellTab.CHAT) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val agentTask = uiState.tasks.find { it.id == BuiltInTaskId.LLM_AGENT_CHAT }
  val downloadedChatModels =
    agentTask?.models.orEmpty().filter {
      uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
    }
  // The model picked on the models tab, or the first downloaded one.
  val chatModel =
    downloadedChatModels.find { it.name == uiState.selectedModel.name }
      ?: downloadedChatModels.firstOrNull()

  val startChat: (String?) -> Unit = { prompt ->
    if (chatModel != null) {
      onStartChat(chatModel, prompt)
    } else {
      currentTab = ShellTab.MODELS
      scope.launch { snackbarHostState.showSnackbar("اول یک مدل گفتگو مثل Gemma 4 دانلود کن.") }
    }
  }
  val openTaskById: (String) -> Unit = { id -> uiState.tasks.find { it.id == id }?.let(onOpenTask) }

  Scaffold(
    modifier = modifier,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        for (tab in ShellTab.entries) {
          NavigationBarItem(
            selected = currentTab == tab,
            onClick = { currentTab = tab },
            icon = { Icon(tab.icon, contentDescription = null) },
            label = { Text(tab.label) },
            colors =
              NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
              ),
          )
        }
      }
    },
  ) { innerPadding ->
    val bottomOnly = PaddingValues(bottom = innerPadding.calculateBottomPadding())
    when (currentTab) {
      ShellTab.CHAT ->
        ChatStartTab(
          modelName = chatModel?.displayName?.ifEmpty { chatModel.name },
          loadingModels = uiState.loadingModelAllowlist,
          onStartChat = startChat,
          onOpenModels = { currentTab = ShellTab.MODELS },
          onOpenTools = { currentTab = ShellTab.TOOLS },
          onOpenTaskById = openTaskById,
          onOpenTasks = onOpenTasks,
          sessions = chatSessions,
          onOpenSession = { session ->
            if (chatModel != null) {
              onOpenSession(session)
            } else {
              currentTab = ShellTab.MODELS
              scope.launch {
                snackbarHostState.showSnackbar("اول یک مدل گفتگو مثل Gemma 4 دانلود کن.")
              }
            }
          },
          onOpenVoice = {
            if (chatModel != null) {
              onOpenVoice()
            } else {
              currentTab = ShellTab.MODELS
              scope.launch {
                snackbarHostState.showSnackbar("اول یک مدل گفتگو مثل Gemma 4 دانلود کن.")
              }
            }
          },
          modifier = Modifier.padding(innerPadding),
        )
      ShellTab.TOOLS ->
        ToolsTab(
          tasks = uiState.tasks.filter { it.id !in HIDDEN_TOOL_IDS },
          onOpenTask = onOpenTask,
          onOpenTasks = onOpenTasks,
          onOpenVoice = onOpenVoice,
          modifier = Modifier.padding(innerPadding),
        )
      ShellTab.SKILLS ->
        SkillsTab(chatModel = chatModel, modifier = Modifier.padding(innerPadding))
      ShellTab.MODELS ->
        // GlobalModelManager applies its modifier to its top bar too, so pad a wrapper instead.
        Box(modifier = Modifier.padding(bottomOnly).consumeWindowInsets(bottomOnly)) {
          GlobalModelManager(
            viewModel = modelManagerViewModel,
            tosViewModel = tosViewModel,
            navigateUp = {},
            onModelSelected = onModelSelected,
            onBenchmarkClicked = onBenchmarkClicked,
            showCloseButton = false,
            groupByDownloadState = true,
            header = {
              ActiveModelCard(
                chatTask = agentTask,
                chatModel = chatModel,
                downloadedChatModels = downloadedChatModels,
                modelManagerViewModel = modelManagerViewModel,
                onStartChat = { startChat(null) },
              )
            },
          )
        }
    }
  }
}

private fun greeting(): String {
  val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
  return when (hour) {
    in 5..11 -> "صبح بخیر"
    in 12..16 -> "ظهر بخیر"
    in 17..20 -> "عصر بخیر"
    else -> "شب بخیر"
  }
}

@Composable
private fun ChatStartTab(
  modelName: String?,
  loadingModels: Boolean,
  onStartChat: (String?) -> Unit,
  onOpenModels: () -> Unit,
  onOpenTools: () -> Unit,
  onOpenTaskById: (String) -> Unit,
  onOpenTasks: () -> Unit,
  sessions: List<ChatSessionProto>,
  onOpenSession: (ChatSessionProto) -> Unit,
  onOpenVoice: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var input by rememberSaveable
  var showAllSessions by rememberSaveable { mutableStateOf(false) } { mutableStateOf("") }
  val send = {
    val text = input.trim()
    if (text.isNotEmpty()) {
      onStartChat(text)
      input = ""
    }
  }

  Column(modifier = modifier.fillMaxSize().imePadding()) {
    Column(
      modifier =
        Modifier.weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      ModelChip(modelName = modelName, loading = loadingModels, onClick = onOpenModels)
      Column {
        Text(
          greeting(),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          "چه کاری برایت انجام بدهم؟",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.ExtraBold,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SuggestionCard(
          icon = Icons.Outlined.Event,
          title = "«فردا ساعت ۵ برم دکتر»",
          subtitle = "ثبت در تقویم گوشی",
          tint = MaterialTheme.colorScheme.primary,
          container = MaterialTheme.colorScheme.primaryContainer,
          onClick = { onStartChat("فردا ساعت ۵ برم دکتر") },
          modifier = Modifier.weight(1f),
        )
        SuggestionCard(
          icon = Icons.Outlined.Image,
          title = "از یک عکس بپرس",
          subtitle = "قبض، منو، نوشته، اشیا",
          tint = MaterialTheme.colorScheme.secondary,
          container = MaterialTheme.colorScheme.secondaryContainer,
          onClick = { onOpenTaskById(BuiltInTaskId.LLM_ASK_IMAGE) },
          modifier = Modifier.weight(1f),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SuggestionCard(
          icon = Icons.Outlined.Checklist,
          title = "کارها و یادآوری‌ها",
          subtitle = "لیست کارهای امروز و فردا",
          tint = MaterialTheme.colorScheme.tertiary,
          container = MaterialTheme.colorScheme.tertiaryContainer,
          onClick = onOpenTasks,
          modifier = Modifier.weight(1f),
        )
        SuggestionCard(
          icon = Icons.Outlined.Widgets,
          title = "همه‌ی ابزارها",
          subtitle = "گالری، ویدیو، گوشی…",
          tint = MaterialTheme.colorScheme.onSurface,
          container = MaterialTheme.colorScheme.surfaceContainerHigh,
          onClick = onOpenTools,
          modifier = Modifier.weight(1f),
        )
      }
      if (sessions.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "گفتگوهای قبلی",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          if (sessions.size > RECENT_SESSIONS) {
            TextButton(onClick = { showAllSessions = !showAllSessions }) {
              Text(if (showAllSessions) "کمتر" else "همه (${sessions.size})".toPersianDigits())
            }
          }
        }
        Column(
          modifier =
            Modifier.clip(RoundedCornerShape(18.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
          val shown = if (showAllSessions) sessions else sessions.take(RECENT_SESSIONS)
          shown.forEachIndexed { index, session ->
            SessionRow(session = session, onClick = { onOpenSession(session) })
            if (index < shown.lastIndex) {
              HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
              )
            }
          }
        }
      }
    }

    Composer(
      value = input,
      onValueChange = { input = it },
      onSend = send,
      onVoice = onOpenVoice,
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
    )
  }
}

private const val RECENT_SESSIONS = 4

@Composable
private fun SessionRow(session: ChatSessionProto, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      Icons.Outlined.History,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
    Text(
      session.title.ifBlank { "گفتگوی بی‌نام" },
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    Text(
      formatDue(session.timestampMs),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ModelChip(modelName: String?, loading: Boolean, onClick: () -> Unit) {
  val ready = modelName != null
  Row(
    modifier =
      Modifier.clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier =
        Modifier.size(8.dp)
          .clip(CircleShape)
          .background(if (ready) Color(0xFF1F9D55) else MaterialTheme.colorScheme.outline)
    )
    Text(
      text =
        when {
          ready -> modelName!!
          loading -> "در حال بارگذاری فهرست مدل‌ها…"
          else -> "مدلی دانلود نشده · دانلود کن"
        },
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun SuggestionCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  tint: Color,
  container: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .clickable(onClick = onClick)
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(container),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Text(
      subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun Composer(
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  onVoice: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier.padding(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("بنویس چه کاری انجام بدهم…") },
        modifier = Modifier.weight(1f),
        maxLines = 4,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
          ),
      )
      // With nothing typed the button opens voice mode, as in most messengers.
      val empty = value.isBlank()
      IconButton(
        onClick = if (empty) onVoice else onSend,
        modifier = Modifier.size(48.dp),
        colors =
          IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      ) {
        if (empty) {
          Icon(Icons.Rounded.Mic, contentDescription = "حالت صوتی")
        } else {
          Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "بفرست")
        }
      }
    }
  }
}

@Composable
private fun ToolsTab(
  tasks: List<Task>,
  onOpenTask: (Task) -> Unit,
  onOpenTasks: () -> Unit,
  onOpenVoice: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text("ابزارها", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
          "همه روی گوشی اجرا می‌شوند",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    item(key = "persian_tasks") {
      ToolCard(
        title = "کارها و یادآوری‌ها",
        subtitle = "لیست کار با یادآوری",
        onClick = onOpenTasks,
        icon = { mod, tint -> Icon(Icons.Outlined.Checklist, null, tint = tint, modifier = mod) },
      )
    }
    item(key = "persian_voice") {
      ToolCard(
        title = "حالت صوتی",
        subtitle = "حرف بزن، دستیار گوش می‌دهد",
        onClick = onOpenVoice,
        icon = { mod, tint -> Icon(Icons.Rounded.Mic, null, tint = tint, modifier = mod) },
      )
    }
    items(tasks, key = { it.id }) { task ->
      val (title, subtitle) = TOOL_TEXTS[task.id] ?: (task.label to task.description)
      ToolCard(
        title = title,
        subtitle = subtitle,
        onClick = { onOpenTask(task) },
        icon = { mod, tint ->
          when {
            task.icon != null -> Icon(task.icon, contentDescription = null, tint = tint, modifier = mod)
            task.iconVectorResourceId != null ->
              Icon(
                painterResource(task.iconVectorResourceId),
                contentDescription = null,
                tint = tint,
                modifier = mod,
              )
            else -> Icon(Icons.Outlined.Widgets, contentDescription = null, tint = tint, modifier = mod)
          }
        },
      )
    }
  }
}

@Composable
private fun ToolCard(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  icon: @Composable (Modifier, Color) -> Unit,
) {
  Row(
    modifier =
      Modifier.clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .clickable(onClick = onClick)
        .padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier =
        Modifier.size(40.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      icon(Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        fontSize = 11.sp,
      )
    }
  }
}
