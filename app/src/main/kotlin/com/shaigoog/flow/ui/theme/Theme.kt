package com.shaigoog.flow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FlowColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Teal,
    onSecondary = Color.White,
    tertiary = Gold,
    background = Mist,
    surface = Color.White,
    surfaceVariant = Sand,
    onSurface = Ink
)

@Composable
fun ShaigoogFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlowColors,
        typography = Typography(),
        content = content
    )
}
