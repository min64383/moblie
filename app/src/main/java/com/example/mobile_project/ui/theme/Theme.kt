package com.example.mobile_project.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 게임 화면은 항상 밝은 크림 테마를 쓴다. (기기 다크 모드·배경화면 색 무시)
private val PooangColorScheme = lightColorScheme(
    primary = PooangColors.OrangeBottom,
    onPrimary = Color.White,
    primaryContainer = PooangColors.HudBottom,
    onPrimaryContainer = PooangColors.Text,
    secondary = PooangColors.GhostText,
    onSecondary = Color.White,
    tertiary = PooangColors.PinkBottom,
    onTertiary = Color.White,
    background = PooangColors.Panel,
    onBackground = PooangColors.Text,
    surface = PooangColors.Card,
    onSurface = PooangColors.Text,
    surfaceVariant = PooangColors.Track,
    onSurfaceVariant = PooangColors.TextSub,
    outline = PooangColors.Line,
    error = PooangColors.Warn,
    onError = Color.White
)

@Composable
fun PooangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PooangColorScheme,
        typography = Typography,
        content = content
    )
}
