package com.example.mobile_project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.theme.PooangColors

/**
 * 진행 막대 (`.track`, `.pp-track`). [fraction] 은 0~1, 바뀌면 부드럽게 늘어나고 줄어든다.
 */
@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    fill: Brush = Brush.horizontalGradient(listOf(PooangColors.OrangeTop, PooangColors.OrangeBottom)),
    track: Color = PooangColors.Track,
    height: Dp = 10.dp,
    borderColor: Color? = PooangColors.StatBorder
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "track")
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(track)
            .then(if (borderColor != null) Modifier.border(1.5.dp, borderColor, shape) else Modifier)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(shape)
                .background(fill)
        )
    }
}

/** 단색 진행 막대 */
@Composable
fun ProgressTrack(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 4.dp) =
    ProgressTrack(
        fraction = fraction,
        modifier = modifier,
        fill = Brush.horizontalGradient(listOf(color, color)),
        height = height,
        borderColor = null
    )

/**
 * HUD 스탯 칸 (`.st`). 아이콘 + 이름, 값, 막대.
 * 값이 바뀌면 한 번 통 튀는 효과(`bump`)가 난다.
 *
 * @param fraction 막대 비율 0~1. null 이면 막대 없이 값만 보인다 (돈 칸처럼 `.flat`).
 * @param valueColor 값 글자색. 돈 칸은 [PooangColors.MoneyFlat], 음수면 [PooangColors.Danger].
 */
@Composable
fun StatTile(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    fraction: Float? = null,
    barColor: Color = PooangColors.Hp,
    valueColor: Color = PooangColors.Text,
    onClick: (() -> Unit)? = null
) {
    val scale = remember { Animatable(1f) }
    val first = remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first.value) {
            first.value = false
        } else {
            scale.animateTo(1.12f, tween(160))
            scale.animateTo(1f, tween(240))
        }
    }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .padding(bottom = 2.dp)
            .hardShadow(PooangColors.StatBorder, 2.dp, 12.dp)
            .clip(shape)
            .background(PooangColors.StatBg)
            .border(2.dp, PooangColors.StatBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 3.dp, end = 3.dp, top = 5.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(painterResource(icon), null, tint = PooangColors.StatIcon, modifier = Modifier.size(11.dp))
            Text(label, color = PooangColors.TextSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(
            value,
            color = valueColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 1.dp, bottom = 3.dp)
        )
        if (fraction != null) {
            ProgressTrack(fraction, barColor, Modifier.padding(horizontal = 3.dp))
        }
    }
}

/** 증감 칩의 색 */
enum class ChipTone(val background: Color, val content: Color) {
    Up(PooangColors.UpBg, PooangColors.UpText),
    Down(PooangColors.DownBg, PooangColors.DownText),
    Even(PooangColors.EqBg, PooangColors.EqText);

    companion object {
        fun of(delta: Int): ChipTone = when {
            delta > 0 -> Up
            delta < 0 -> Down
            else -> Even
        }
    }
}

/** 둥근 알약 모양 칩 (`.dl-chip`, `.mood-chip`, `.deltas span`). */
@Composable
fun PooangChip(text: String, tone: ChipTone, modifier: Modifier = Modifier, fontSize: Float = 11f) {
    Text(
        text = text,
        color = tone.content,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.background)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

/**
 * 스탯 변화 칩. 예: `DeltaChip("체력", -5)` → "체력 -5" (빨강), `DeltaChip("돈", 30000, "원")` → "돈 +30000원" (초록)
 */
@Composable
fun DeltaChip(label: String, delta: Int, modifier: Modifier = Modifier, unit: String = "") {
    PooangChip(text = deltaText(label, delta, unit), tone = ChipTone.of(delta), modifier = modifier)
}

/** "체력 +5" 같은 증감 글자. 0이면 부호 없이 "±0". */
fun deltaText(label: String, delta: Int, unit: String = ""): String {
    val sign = when {
        delta > 0 -> "+"
        delta < 0 -> "-"
        else -> "±"
    }
    return "$label $sign${kotlin.math.abs(delta)}$unit".trim()
}

/** 새 소식 알림 점 (`.dot`, `.nv-dot`). */
@Composable
fun NoticeDot(modifier: Modifier = Modifier, size: Dp = 13.dp) {
    Box(
        modifier
            .pulse(1.07f, 1400)
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .padding(2.5.dp)
            .clip(CircleShape)
            .background(PooangColors.Notice)
    )
}

/** 작은 라벨 + 값 묶음 (`.qk`, `.bigstat .bs`). 홈 요약·학기 결산에서 쓴다. */
@Composable
fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    PooangCard(modifier = modifier, contentPadding = 9.dp) {
        Text(
            label,
            color = PooangColors.TextSub,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            value,
            color = PooangColors.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
        if (sub != null) {
            Text(
                sub,
                color = PooangColors.TextFaint,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
