package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepSpaceBackground,
    secondary = GlowPrimary,
    onSecondary = OffWhite,
    tertiary = VioletGlowing,
    onTertiary = OffWhite,
    background = DeepSpaceBackground,
    onBackground = OffWhite,
    surface = SpaceSurface,
    onSurface = OffWhite,
    surfaceVariant = SpaceCardBg,
    onSurfaceVariant = SlateGrey
  )

private val LightColorScheme = DarkColorScheme // Standardize on eye-safe dark theme for premium alarm clock look

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force eye-safe dark theme by default
  dynamicColor: Boolean = false, // Use our gorgeous custom slate palette instead of system colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
