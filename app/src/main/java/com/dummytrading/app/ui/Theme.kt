package com.dummytrading.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B7A3D)
private val Amber = Color(0xFFE8A317)

@Composable
fun DummyTradingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            secondary = Amber,
            background = Color(0xFFEFF3F9),
            surface = Color.White
        ),
        content = content
    )
}
