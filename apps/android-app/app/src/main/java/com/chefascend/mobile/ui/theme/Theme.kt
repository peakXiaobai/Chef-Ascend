package com.chefascend.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
  primary = Ember,
  onPrimary = Cream,
  secondary = Olive,
  onSecondary = Cream,
  background = Cream,
  onBackground = Ink,
  surface = WarmPaper,
  onSurface = Ink,
  tertiary = Clay
)

private val DarkColors = darkColorScheme(
  primary = Ember,
  onPrimary = Cream,
  secondary = Olive,
  onSecondary = Cream,
  background = Ink,
  onBackground = Cream,
  surface = ColorFallback.surfaceDark,
  onSurface = Cream,
  tertiary = Clay
)

private object ColorFallback {
  val surfaceDark = androidx.compose.ui.graphics.Color(0xFF35231C)
}

@Composable
fun ChefAscendTheme(content: @Composable () -> Unit) {
  val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
  MaterialTheme(
    colorScheme = colors,
    typography = AppTypography,
    content = content
  )
}
