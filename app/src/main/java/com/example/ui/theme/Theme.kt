package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// APEX // AI Cyber Stealth Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
  primary = ApexNeonLime,
  onPrimary = Color.Black,
  secondary = ApexNeonLime,
  onSecondary = Color.Black,
  surface = ApexDarkSurface,
  onSurface = ApexTextPrimary,
  background = ApexBlack,
  onBackground = ApexTextPrimary,
  outline = ApexBorder,
  surfaceVariant = ApexDarkSurfaceHighlight,
  onSurfaceVariant = ApexTextSecondary,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // APEX // AI Stealth Dark theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun nutriTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
  focusedTextColor = ApexTextPrimary,
  unfocusedTextColor = ApexTextPrimary,
  focusedContainerColor = ApexDarkSurfaceHighlight,
  unfocusedContainerColor = ApexDarkSurfaceHighlight,
  focusedBorderColor = ApexNeonLime,
  unfocusedBorderColor = ApexBorder,
  cursorColor = ApexNeonLime
)




