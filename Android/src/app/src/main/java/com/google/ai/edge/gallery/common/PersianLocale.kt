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

package com.google.ai.edge.gallery.common

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Makes resources resolve to Persian (values-fa) regardless of the phone's language.
 *
 * Only the resource configuration is changed. Locale.getDefault() is left alone on purpose, so
 * String.format and number parsing keep using Latin digits and nothing that round-trips numbers
 * through strings breaks.
 */
object PersianLocale {
  private val PERSIAN = Locale.forLanguageTag("fa")

  fun wrap(base: Context): Context {
    val config = Configuration(base.resources.configuration)
    config.setLocale(PERSIAN)
    config.setLayoutDirection(PERSIAN)
    return base.createConfigurationContext(config)
  }
}
