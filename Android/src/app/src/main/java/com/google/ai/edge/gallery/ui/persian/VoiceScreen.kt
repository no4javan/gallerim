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
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.ai.edge.gallery.common.calculatePeakAmplitude
import com.google.ai.edge.gallery.data.MAX_AUDIO_CLIP_DURATION_SEC
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.SAMPLE_RATE
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The prompt sent with a voice recording. */
const val VOICE_PROMPT =
  "به این پیام صوتی گوش بده. اگر کاری از من خواسته شده، انجامش بده؛ اگر سؤال است، کوتاه و روشن جواب بده. جوابت به همان زبانی باشد که صحبت شده."

/**
 * Voice mode: tap to talk, tap again (or wait for the 30-second limit Gemma 4 accepts) and the
 * recording is sent to the chat, which listens to it directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(chatModel: Model?, navigateUp: () -> Unit, onSend: (ByteArray) -> Unit) {
  val context = LocalContext.current
  var recording by remember { mutableStateOf(false) }
  var amplitude by remember { mutableFloatStateOf(0f) }
  var elapsedMs by remember { mutableLongStateOf(0L) }
  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val canListen = chatModel?.supportAudio == true
  val audioStream = remember { ByteArrayOutputStream() }
  val scope = rememberCoroutineScope()
  // Set by the cancel button, so the recording is thrown away instead of sent.
  var cancelled by remember { mutableStateOf(false) }

  // Recording runs in the screen's scope, so leaving the screen stops it and frees the mic.
  val start = {
    cancelled = false
    recording = true
    scope.launch {
      audioStream.reset()
      elapsedMs = 0L
      val bytes =
        recordUntilStopped(
          isRecording = { recording },
          onAmplitude = { amplitude = (it / 32767f).coerceIn(0f, 1f) },
          onElapsed = { elapsedMs = it },
          out = audioStream,
        )
      recording = false
      amplitude = 0f
      // Stopping by hand and reaching the length limit both send the clip.
      if (!cancelled && bytes.isNotEmpty()) onSend(bytes)
    }
    Unit
  }
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      hasPermission = it
      if (it) start()
    }

  val scale by animateFloatAsState(1f + amplitude * 0.6f, label = "voice_scale")

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("حالت صوتی", fontWeight = FontWeight.Bold) },
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
    Column(
      modifier = Modifier.padding(innerPadding).fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        when {
          chatModel == null -> "اول یک مدل گفتگو دانلود کن."
          !canListen -> "مدل فعلی صدا نمی‌فهمد. برای حالت صوتی از Gemma 4 (E2B یا E4B) استفاده کن."
          recording -> "دارم گوش می‌دهم…"
          else -> "بزن و حرف بزن"
        },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        if (recording) {
          val sec = elapsedMs / 1000
          "${sec} از ${MAX_AUDIO_CLIP_DURATION_SEC} ثانیه · دوباره بزن تا بفرستم".toPersianDigits()
        } else {
          "تا ${MAX_AUDIO_CLIP_DURATION_SEC} ثانیه. صدا روی همین گوشی پردازش می‌شود.".toPersianDigits()
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(48.dp))
      Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        Box(
          modifier =
            Modifier.size(150.dp)
              .scale(if (recording) scale else 1f)
              .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        )
        FilledIconButton(
          onClick = {
            when {
              recording -> recording = false
              !hasPermission -> launcher.launch(Manifest.permission.RECORD_AUDIO)
              else -> start()
            }
          },
          enabled = canListen,
          modifier = Modifier.size(112.dp),
          colors =
            IconButtonDefaults.filledIconButtonColors(
              containerColor = if (recording) ACTION_AMBER else MaterialTheme.colorScheme.primary,
              contentColor = if (recording) ON_ACTION_AMBER else MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
          Icon(
            if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
            contentDescription = if (recording) "بفرست" else "شروع ضبط",
            modifier = Modifier.size(48.dp),
          )
        }
      }
      Spacer(Modifier.height(32.dp))
      if (recording) {
        TextButton(
          onClick = {
            cancelled = true
            recording = false
          }
        ) {
          Text("لغو")
        }
      } else {
        Text(
          "مثلاً: «فردا ساعت پنج قرار دکتر دارم» یا «امروز چه کارهایی دارم؟»",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

/** Records 16 kHz mono PCM until [isRecording] turns false or the length limit is reached. */
@SuppressLint("MissingPermission") // Checked before recording starts.
private suspend fun recordUntilStopped(
  isRecording: () -> Boolean,
  onAmplitude: (Int) -> Unit,
  onElapsed: (Long) -> Unit,
  out: ByteArrayOutputStream,
): ByteArray =
  withContext(Dispatchers.IO) {
    val channel = AudioFormat.CHANNEL_IN_MONO
    val format = AudioFormat.ENCODING_PCM_16BIT
    val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, channel, format)
    val recorder =
      try {
        AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, channel, format, minBuffer)
      } catch (e: Exception) {
        return@withContext ByteArray(0)
      }
    val buffer = ByteArray(minBuffer)
    try {
      recorder.startRecording()
      val start = System.currentTimeMillis()
      while (isActive && isRecording()) {
        val read = recorder.read(buffer, 0, buffer.size)
        if (read > 0) {
          out.write(buffer, 0, read)
          onAmplitude(calculatePeakAmplitude(buffer = buffer, bytesRead = read))
        }
        val elapsed = System.currentTimeMillis() - start
        onElapsed(elapsed)
        if (elapsed >= MAX_AUDIO_CLIP_DURATION_SEC * 1000L) break
      }
    } finally {
      if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
      recorder.release()
    }
    out.toByteArray()
  }
