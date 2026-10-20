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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.data.ConfigKeys
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.SegmentedButtonConfig
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.humanReadableSize
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

/** Plain-language hints for each accelerator, shown under the picker. */
private val ACCELERATOR_HINTS =
  mapOf(
    "GPU" to "GPU برای Gemma 4 سریع‌تر است و پیشنهاد می‌شود.",
    "CPU" to "CPU کندتر است ولی روی همه‌ی گوشی‌ها پایدار کار می‌کند. اگر با GPU کرش کرد، این را امتحان کن.",
    "NPU" to "NPU فقط روی بعضی گوشی‌ها و مدل‌ها کار می‌کند.",
    "TPU" to "TPU فقط روی گوشی‌های Pixel کار می‌کند.",
  )

/**
 * The card at the top of the models tab: which model the chat uses, whether it is loaded, and
 * which accelerator it runs on.
 */
@Composable
fun ActiveModelCard(
  chatTask: Task?,
  chatModel: Model?,
  downloadedChatModels: List<Model>,
  modelManagerViewModel: ModelManagerViewModel,
  onStartChat: () -> Unit,
) {
  val context = LocalContext.current
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier.size(44.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Outlined.Memory,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "مدل گفتگو",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            chatModel?.let { it.displayName.ifEmpty { it.name } } ?: "هنوز مدلی دانلود نشده",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        if (chatModel != null) {
          ModelStatusDot(chatModel)
        }
      }

      if (chatModel == null) {
        Text(
          "برای گفتگو، Gemma 4 E4B (حدود ۳٫۷ گیگابایت) یا E2B (حدود ۲٫۶ گیگابایت، سبک‌تر) را از فهرست پایین دانلود کن.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return@Column
      }

      if (downloadedChatModels.size > 1) {
        Text("کدام مدل در گفتگو استفاده شود؟", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          for (model in downloadedChatModels) {
            FilterChip(
              selected = model.name == chatModel.name,
              onClick = { modelManagerViewModel.selectModel(model) },
              label = { Text(model.displayName.ifEmpty { model.name }, maxLines = 1) },
            )
          }
        }
      }

      val acceleratorOptions =
        (chatModel.configs.find { it.key == ConfigKeys.ACCELERATOR } as? SegmentedButtonConfig)
          ?.options
          .orEmpty()
      val current = chatModel.currentAccelerator?.label
      if (acceleratorOptions.isNotEmpty()) {
        Text("پردازنده", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          for (option in acceleratorOptions) {
            FilterChip(
              selected = option.equals(current, ignoreCase = true),
              onClick = {
                if (!option.equals(current, ignoreCase = true)) {
                  chatModel.prevConfigValues = chatModel.configValues
                  chatModel.configValues =
                    chatModel.configValues.toMutableMap().apply {
                      put(ConfigKeys.ACCELERATOR.label, option)
                    }
                  modelManagerViewModel.updateConfigValuesUpdateTrigger()
                  // A loaded model keeps its old accelerator, so unload it. The chat loads it
                  // again with the new one when it opens.
                  if (chatTask != null && chatModel.instance != null) {
                    modelManagerViewModel.cleanupModel(
                      context = context,
                      task = chatTask,
                      model = chatModel,
                    )
                  }
                }
              },
              label = { Text(option) },
            )
          }
        }
        ACCELERATOR_HINTS[current?.uppercase()]?.let {
          Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      val size = chatModel.downloadInfo.totalBytes
      if (size > 0) {
        Text(
          "حجم روی گوشی: ${size.humanReadableSize()}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Button(onClick = onStartChat, modifier = Modifier.fillMaxWidth()) { Text("شروع گفتگو") }
    }
  }
}

@Composable
private fun ModelStatusDot(model: Model) {
  val status by model.initStatusFlow.collectAsState()
  val (label, color) =
    when (status) {
      is Model.InitializationStatus.Initialized -> "آماده" to Color(0xFF1F9D55)
      is Model.InitializationStatus.Initializing -> "در حال بارگذاری" to Color(0xFFF2A93B)
      is Model.InitializationStatus.Failed -> "خطا در بارگذاری" to MaterialTheme.colorScheme.error
      else -> "بارگذاری نشده" to MaterialTheme.colorScheme.outline
    }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
    Text(label, style = MaterialTheme.typography.labelMedium)
  }
}
