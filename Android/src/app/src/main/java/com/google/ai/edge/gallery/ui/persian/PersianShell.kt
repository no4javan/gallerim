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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.google.ai.edge.gallery.ui.modelmanager.GlobalModelManager
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.google.ai.edge.gallery.proto.ChatSessionProto
import dagger.hilt.android.EntryPointAccessors
import java.util.Calendar
import kotlinx.coroutines.launch

/** The amber of the main action buttons (send, voice, add), on light and dark themes alike. */
val ACTION_AMBER = Color(0xFFF2A93B)
val ON_ACTION_AMBER = Color(0xFF0E1726)

/** The four bottom tabs of the Persian assistant. */
enum class ShellTab(val label: String, val icon: ImageVector) {
  CHAT("گفتگو", Icons.Outlined.ChatBubbleOutline),
  TOOLS("ابزارها", Icons.Outlined.GridView),
  SKILLS("مهارت‌ها", Icons.Outlined.Bolt),
  MODELS("مدل‌ها", Icons.Outlined.Memory),
}

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
  // Text in the chat box. Kept here so the tools tab can fill in an example.
  var chatDraft by rememberSaveable { mutableStateOf("") }
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
  val openVoice: () -> Unit = {
    if (chatModel != null) {
      onOpenVoice()
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
          onOpenVoice = openVoice,
          draft = chatDraft,
          onDraftChange = { chatDraft = it },
          modifier = Modifier.padding(innerPadding),
        )
      ShellTab.TOOLS ->
        ToolsTab(
          availableTaskIds = uiState.tasks.map { it.id }.toSet(),
          onTarget = { target ->
            when (target) {
              is ToolTarget.Example -> {
                chatDraft = target.prompt
                currentTab = ShellTab.CHAT
              }
              is ToolTarget.TaskScreen -> openTaskById(target.taskId)
              ToolTarget.TasksList -> onOpenTasks()
              ToolTarget.Voice -> openVoice()
            }
          },
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
  draft: String,
  onDraftChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAllSessions by rememberSaveable { mutableStateOf(false) }
  val send = {
    val text = draft.trim()
    if (text.isNotEmpty()) {
      onStartChat(text)
      onDraftChange("")
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
          subtitle = "تقویم، زنگ ساعت، تماس و…",
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
      value = draft,
      onValueChange = onDraftChange,
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
            containerColor = ACTION_AMBER,
            contentColor = ON_ACTION_AMBER,
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
