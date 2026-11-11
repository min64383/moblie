package com.example.mobile_project.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CSS 의 `box-shadow: 0 3px 0 색` 처럼 흐림 없는 그림자를 아래쪽에 그린다.
 * 카드·버튼이 살짝 튀어나와 보이게 하는 프로토타입의 핵심 효과.
 * 그림자는 요소 밖(아래)으로 그려지므로, 바로 아래 요소와 간격을 [offsetY] 이상 두면 좋다.
 */
fun Modifier.hardShadow(color: Color, offsetY: Dp = 3.dp, corner: Dp = 18.dp): Modifier =
    if (offsetY <= 0.dp) this else drawBehind {
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, offsetY.toPx()),
            size = size,
            cornerRadius = CornerRadius(corner.toPx())
        )
    }

/**
 * 아래쪽 안쪽 그림자 (`inset 0 -2px 0 rgba(0,0,0,.16)`). 아이콘 타일·시간표 칸에 쓴다.
 * clip 다음에 붙여야 모서리 밖으로 삐져나가지 않는다.
 */
fun Modifier.insetBottomShade(height: Dp = 2.dp, alpha: Float = 0.16f): Modifier = drawBehind {
    val h = height.toPx()
    drawRect(Color.Black.copy(alpha = alpha), topLeft = Offset(0f, size.height - h))
}

/** 위아래로 둥실둥실 움직이는 효과 (`@keyframes bob`). */
fun Modifier.bob(amplitude: Dp = 9.dp, periodMs: Int = 2600): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "bob")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bobOffset"
    )
    graphicsLayer { translationY = -amplitude.toPx() * t }
}

/** 크기가 살짝 커졌다 작아지는 반복 효과 (`@keyframes pulse`). 알림 점·진행 버튼에 쓴다. */
fun Modifier.pulse(scale: Float = 1.07f, periodMs: Int = 1600): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val s by transition.animateFloat(
        initialValue = 1f,
        targetValue = scale,
        animationSpec = infiniteRepeatable(tween(periodMs / 2), RepeatMode.Reverse),
        label = "pulseScale"
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

/** 반짝임 효과 (`@keyframes tw`). 별·"눌러서 계속" 안내에 쓴다. */
fun Modifier.twinkle(min: Float = 0.18f, max: Float = 0.75f, periodMs: Int = 3000, delayMs: Int = 0): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "twinkle")
    val a by transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = infiniteRepeatable(
            tween(periodMs / 2),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs)
        ),
        label = "twinkleAlpha"
    )
    graphicsLayer { alpha = a }
}
