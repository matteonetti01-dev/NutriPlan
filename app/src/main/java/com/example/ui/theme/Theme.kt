package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Consistent clean light theme as requested in user specifications
private val LightColorScheme = lightColorScheme(
  primary = NutriDark,
  onPrimary = Color.White,
  secondary = NutriDarkSurface,
  onSecondary = Color.White,
  surface = NutriCardBg,
  onSurface = NutriTextPrimary,
  background = NutriBgLight,
  onBackground = NutriTextPrimary,
  outline = NutriCardBorder,
  surfaceVariant = NutriCardInner,
  onSurfaceVariant = NutriTextSecondary,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Always clean light theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun nutriTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
  focusedTextColor = NutriTextPrimary,
  unfocusedTextColor = NutriTextPrimary,
  focusedContainerColor = Color.White,
  unfocusedContainerColor = Color.White,
  focusedBorderColor = NutriDark,
  unfocusedBorderColor = Color(0xFFE5E7EB),
  cursorColor = NutriDark
)



