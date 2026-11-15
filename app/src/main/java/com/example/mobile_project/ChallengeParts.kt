package com.example.mobile_project

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.theme.PooangColors

/**
 * 미니게임·시험처럼 "도전" 화면의 공통 틀.
 * 위: 그라데이션 머리 그림(표정 푸앙이 + 아이콘 배지), 그 아래 작은 머리말·제목·부제
 * 본문은 [content] 로 받고, 화면 전체를 세로로 스크롤한다.
 */
@Composable
fun ChallengeScaffold(
    mood: PooangMood,
    kicker: String,
    title: String,
    subtitle: String?,
    @DrawableRes badgeIcon: Int,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    heroColors: List<Color> = listOf(Color(0xFFFFF0DA), Color(0xFFFFE0B8)),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .background(PooangColors.Panel)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(heroColors))
                .statusBarsPadding()
                .height(132.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            PooangImage(mood, size = 110.dp, bob = !LocalInspectionMode.current, modifier = Modifier.padding(bottom = 6.dp))
            Box(
                Modifier
                    .offset(x = 56.dp, y = (-12).dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(badgeIcon), null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
        }
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(kicker, color = PooangColors.Warn, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
            Text(title, color = PooangColors.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) {
                Text(subtitle, color = PooangColors.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

/** 문제 카드 (`.quiz-q`): 흰 바탕에 굵은 질문. */
@Composable
fun QuestionCard(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, PooangColors.Line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, color = Color(0xFF15294A), fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** 보기 상태 (`.qopt`, `.right`, `.wrong`) */
enum class AnswerState { Normal, Right, Wrong, Picked, Done }

/** 보기 버튼. 맞으면 초록, 틀리면 빨강, 고른 것은 주황, 끝난 것은 흐린 초록. */
@Composable
fun AnswerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: AnswerState = AnswerState.Normal,
    enabled: Boolean = true,
    center: Boolean = false,
    fontSize: Float = 15f,
    height: androidx.compose.ui.unit.Dp? = null
) {
    val (bg, border, fg) = when (state) {
        AnswerState.Normal -> Triple(Color.White, Color(0xFFDDD6C5), Color(0xFF22262E))
        AnswerState.Right, AnswerState.Done -> Triple(Color(0xFFDCF0E3), PooangColors.UpText, PooangColors.UpText)
        AnswerState.Wrong -> Triple(Color(0xFFFADEDC), PooangColors.Warn, PooangColors.Warn)
        AnswerState.Picked -> Triple(Color(0xFFFDF2E2), PooangColors.Gold, Color(0xFF8A5A1C))
    }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier)
            .alpha(if (enabled || state != AnswerState.Normal) 1f else 0.45f)
            .clip(shape)
            .background(bg)
            .border(2.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        contentAlignment = if (center) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text,
            color = fg,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.35f).sp,
            fontWeight = if (center) FontWeight.ExtraBold else FontWeight.SemiBold,
            textAlign = if (center) TextAlign.Center else TextAlign.Start
        )
    }
}
