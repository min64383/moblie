package com.example.mobile_project

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.mobile_project.ui.components.PooangArt
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.components.twinkle
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import java.util.Locale
import kotlin.random.Random

/**
 * 시작(타이틀) 화면. 프로토타입 `#sc-title`.
 * 위쪽은 밤하늘 그라데이션 + 반짝이는 별 + 둥실거리는 푸앙이 + 캠퍼스 실루엣,
 * 아래쪽은 게임 소개 카드와 시작 버튼.
 *
 * 저장된 게임이 있으면 "이어하기"가 주 버튼이 되고, 새 게임은 "처음부터 시작" 보조 버튼이 된다.
 */
@Composable
fun TitleScreen(
    hasGame: Boolean,
    game: GameState,
    saveStatus: String,
    loadError: String?,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onAccount: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier
) {
    DarkStatusBarIcons()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PooangColors.Panel)
            .verticalScroll(rememberScrollState())
    ) {
        TitleHero()

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            AboutCard()
            ProgressTipCard()

            if (hasGame) {
                PooangButton("이어하기", onClick = onContinue, modifier = Modifier.fillMaxWidth())
                Text(
                    text = continueSummary(game),
                    color = PooangColors.TextSub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (loadError != null) {
                Text(
                    text = loadError,
                    color = PooangColors.Warn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                PooangButton(
                    "저장 데이터 다시 불러오기",
                    onClick = onRetryLoad,
                    modifier = Modifier.fillMaxWidth(),
                    style = PooangButtonStyle.Ghost,
                    small = true
                )
            }

            PooangButton(
                text = if (hasGame) "처음부터 시작" else "입학하기",
                onClick = onNewGame,
                modifier = Modifier.fillMaxWidth(),
                style = if (hasGame) PooangButtonStyle.Ghost else PooangButtonStyle.Primary
            )

            PooangButton(
                "계정 · 클라우드 저장",
                onClick = onAccount,
                modifier = Modifier.fillMaxWidth(),
                style = PooangButtonStyle.Ghost,
                small = true
            )

            if (saveStatus.isNotBlank()) {
                Text(
                    text = saveStatus,
                    color = PooangColors.TextFaint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

/** "1학년 1학기 3주차 · 누적 평점 3.85" */
internal fun continueSummary(game: GameState): String {
    val head = "${semesterLabel(game.semester)} ${game.week}주차"
    return if (game.completedSemesters.isEmpty()) head
    else head + " · 누적 평점 " + String.format(Locale.US, "%.2f", game.cumulativeGpa)
}

/** 밤하늘 영역. 상태 표시줄 아래까지 그라데이션을 깐다. */
@Composable
private fun TitleHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to PooangColors.TitleTop,
                    0.55f to PooangColors.TitleMid,
                    1f to PooangColors.TitleBottom
                )
            )
    ) {
        Stars(Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val shadowY = with(LocalDensity.current) { 3.dp.toPx() }
            Text(
                text = "푸앙이 대학생활",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    shadow = Shadow(Color(0x2E000000), Offset(0f, shadowY), 0f)
                )
            )
            Text(
                text = "입학부터 졸업까지, 4년을 설계하세요",
                color = PooangColors.TitleSub,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 5.dp)
            )
            Text(
                text = "4학년 8학기 · 128주",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 11.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.17f))
                    .padding(horizontal = 13.dp, vertical = 4.dp)
            )
            PooangImage(
                drawable = PooangArt.Title,
                contentDescription = "푸앙이",
                size = 150.dp,
                bob = !LocalInspectionMode.current,
                modifier = Modifier.padding(top = 14.dp)
            )
            // 캠퍼스 실루엣: 아래 크림색 영역과 이어지도록 살짝 겹친다.
            Image(
                painter = painterResource(R.drawable.pooang_campus_title),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(460f / 92f)
                    .offset(y = 1.dp)
            )
        }
    }
}

/** 하늘 위쪽 56% 에 흩뿌린 작은 별 22개. 자리는 매번 같다(고정 시드). */
@Composable
private fun Stars(modifier: Modifier = Modifier) {
    val stars = remember {
        val r = Random(2024)
        List(22) {
            StarSpec(
                x = r.nextFloat(),
                y = r.nextFloat() * 0.56f,
                size = 1.5f + r.nextFloat() * 2f,
                delayMs = r.nextInt(3000)
            )
        }
    }
    val animate = !LocalInspectionMode.current
    BoxWithConstraints(modifier) {
        stars.forEach { s ->
            Box(
                Modifier
                    .offset(x = maxWidth * s.x, y = maxHeight * s.y)
                    .then(if (animate) Modifier.twinkle(delayMs = s.delayMs) else Modifier)
                    .size(s.size.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (animate) 1f else 0.5f))
            )
        }
    }
}

private data class StarSpec(val x: Float, val y: Float, val size: Float, val delayMs: Int)

@Composable
private fun AboutCard() {
    PooangCard(title = "이런 게임입니다", titleIcon = R.drawable.pooang_ic_cap, titleIconTint = PooangColors.CauBlue) {
        val bold = SpanStyle(fontWeight = FontWeight.ExtraBold, color = PooangColors.TextMid)
        Text(
            text = buildAnnotatedString {
                withStyle(bold) { append("8학기, 128주") }
                append("를 직접 설계합니다. 학기마다 3과목을 수강신청하고, 매주 28칸 시간표에 할 일을 칠하세요. 손가락으로 끌면 여러 칸이 한 번에 칠해져요.\n\n")
                append("체력이 떨어지면 공부 효율이 떨어지고 ")
                withStyle(bold) { append("늦잠") }
                append("을 잡습니다. 학기마다 중간·기말고사가 있고, ")
                withStyle(bold) { append("조별과제") }
                append("가 터집니다.\n\n")
                append("학기 사이 ")
                withStyle(bold) { append("방학") }
                append("에는 알바·인턴·어학연수 중 하나를 고릅니다. 조건을 채우면 ")
                withStyle(bold) { append("연애") }
                append("도 시작할 수 있어요. 3학년부터 ")
                withStyle(bold) { append("취업 준비") }
                append(", 누적 평점 3.0을 넘으면 ")
                withStyle(bold) { append("과외") }
                append("가 열리고, 주 1회 ")
                withStyle(bold) { append("복권") }
                append("도 살 수 있습니다.\n\n")
                append("졸업 시 누적 평점·스펙·인맥·자산으로 ")
                withStyle(bold) { append("진로가 갈립니다.") }
            },
            color = Color(0xFF7A8290),
            fontSize = 12.sp,
            lineHeight = 18.6.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProgressTipCard() {
    val border = Color(0xFFC3D5EC)
    PooangCard(background = Color(0xFFEDF3FB), borderColor = border) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("하루씩 직접 넘깁니다") }
                append(" — 결과 화면에서 \"하루씩\" 또는 \"한 주 자동\"으로 진행 방식을 고를 수 있어요. 미니게임이 번거로우면 건너뛰기를 켜세요.")
            },
            color = Color(0xFF2B4C7E),
            fontSize = 12.sp,
            lineHeight = 18.6.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 타이틀은 배경이 짙은 남색이라 상태 표시줄 아이콘(시계·배터리)을 흰색으로 바꾼다.
 * 다른 화면으로 나가면 원래대로(어두운 아이콘) 되돌린다.
 */
@Composable
private fun DarkStatusBarIcons() {
    if (LocalInspectionMode.current) return
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (controller != null && before != null) controller.isAppearanceLightStatusBars = before
        }
    }
}

@Preview(name = "타이틀 - 첫 실행", showBackground = true, widthDp = 380, heightDp = 1100)
@Composable
private fun TitleScreenNewPreview() {
    PooangTheme {
        TitleScreen(
            hasGame = false, game = GameState(), saveStatus = "기기에 자동 저장돼요", loadError = null,
            onContinue = {}, onNewGame = {}, onAccount = {}, onRetryLoad = {}
        )
    }
}

@Preview(name = "타이틀 - 이어하기", showBackground = true, widthDp = 380, heightDp = 1100)
@Composable
private fun TitleScreenContinuePreview() {
    PooangTheme {
        TitleScreen(
            hasGame = true, game = GameState(semester = 3, week = 5), saveStatus = "방금 저장됨", loadError = null,
            onContinue = {}, onNewGame = {}, onAccount = {}, onRetryLoad = {}
        )
    }
}
