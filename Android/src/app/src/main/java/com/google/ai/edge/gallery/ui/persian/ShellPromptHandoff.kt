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

/**
 * Carries a prompt the user typed on the start screen into the chat screen.
 *
 * Prompts that arrive through a deep link are untrusted and the chat asks before sending them.
 * A prompt typed inside the app is the user's own, so it travels here instead of in the route,
 * where it would look like a deep link, and is sent without asking again.
 */
object ShellPromptHandoff {
  @Volatile private var pending: String? = null

  fun put(prompt: String) {
    pending = prompt
  }

  /** Returns the waiting prompt once, then forgets it. */
  fun take(): String? {
    val prompt = pending
    pending = null
    return prompt
  }
}
