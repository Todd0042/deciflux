/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.deciflux.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DecifluxElectricCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1B3854),
    onPrimaryContainer = Color(0xFFD0ECFF),
    secondary = DecifluxLuminousViolet,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF35244E),
    onSecondaryContainer = Color(0xFFEADBFF),
    tertiary = DecifluxEmeraldPeak,
    onTertiary = Color.Black,
    background = DecifluxSurfaceDark,
    onBackground = Color(0xFFE2E8F0),
    surface = DecifluxSurfaceDark,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = DecifluxSurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DecifluxCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    primaryContainer = primaryContainerLight,
    secondary = secondaryLight,
    tertiary = tertiaryLight
)

@Composable
fun DecifluxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

