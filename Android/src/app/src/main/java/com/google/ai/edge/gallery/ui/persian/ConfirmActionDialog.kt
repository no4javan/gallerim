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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * The card shown before the assistant changes the user's data. Nothing happens until the user
 * presses the approve button; dismissing the card counts as cancel.
 */
@Composable
fun ConfirmActionDialog(
  title: String,
  detail: String,
  confirmLabel: String,
  destructive: Boolean,
  onResult: (Boolean) -> Unit,
) {
  Dialog(
    onDismissRequest = { onResult(false) },
    properties = DialogProperties(dismissOnClickOutside = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceContainerLowest,
      tonalElevation = 2.dp,
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val tint =
            if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
          Icon(
            if (destructive) Icons.Outlined.DeleteOutline else Icons.Outlined.EditCalendar,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
          )
          Text(
            "تأیید لازم است",
            style = MaterialTheme.typography.labelLarge,
            color = tint,
          )
        }
        Text(
          title,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            detail,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(14.dp),
          )
        }
        Text(
          "تا تأیید نکنی، چیزی تغییر نمی‌کند.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onResult(true) },
            modifier = Modifier.weight(1f),
            colors =
              if (destructive)
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError,
                )
              else ButtonDefaults.buttonColors(),
          ) {
            Text(confirmLabel)
          }
          OutlinedButton(onClick = { onResult(false) }, modifier = Modifier.weight(1f)) {
            Text("لغو")
          }
        }
      }
    }
  }
}
