package com.example.mobile_project.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 프로토타입은 굵은 글씨(800)를 많이 쓴다. 기본 글꼴(시스템 한글 글꼴)은 그대로 둔다.
// 색은 넣지 않는다: 글자색은 배경에 따라 LocalContentColor 가 정하도록 둔다.
val Typography = Typography(
    headlineMedium = TextStyle(fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 14.5.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold),
    titleSmall = TextStyle(fontSize = 13.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold),
    labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold),
    labelSmall = TextStyle(fontSize = 9.5.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
)
