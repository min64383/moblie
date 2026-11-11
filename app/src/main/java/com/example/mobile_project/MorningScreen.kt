package com.example.mobile_project

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.components.hardShadow
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import kotlinx.coroutines.delay

/**
 * 늦잠 화면. 4초 안에 "달리기!"를 22번 눌러야 제시간에 도착한다.
 * 준비 → 달리기(남은 시간 막대 + 달리는 막대 + 큰 버튼) → 결과 순서로 바뀐다.
 * 시간 계산·판정은 LifeEngine 이 하고, 이 화면은 50ms 마다 시간을 새로 고쳐 보여 주기만 한다.
 */
@Composable
fun MorningScreen(
    game: GameState,
    onBegin: (String) -> Unit,
    onTap: (String, Int) -> Unit,
    onRefresh: (String, Long) -> Unit,
    onSkip: (String) -> Unit,
    onContinue: (String) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val run = game.life.pendingMorning ?: return
    var now by remember(run.key, run.startedAtMs) { mutableStateOf(System.currentTimeMillis()) }
    val refresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(run.key, run.phase, run.startedAtMs) {
        if (run.phase != MorningPhase.PLAY) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            refresh(run.key, now)
            delay(50L)
        }
    }

    val mood = when (run.phase) {
        MorningPhase.READY -> PooangMood.YAWN
        MorningPhase.PLAY -> PooangMood.SHOCK
        MorningPhase.RESULT -> if (run.success == true) PooangMood.PROUD else PooangMood.SAD
    }
    val subjectName = run.subjectId?.let { subjectForId(it)?.name ?: it }

    Column(
        modifier
            .fillMaxSize()
            .background(PooangColors.Panel)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        // 아침 하늘 머리 그림
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFD9B8), Color(0xFFFFF0DA))))
                .statusBarsPadding()
                .height(150.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            PooangImage(mood, size = 124.dp, bob = !LocalInspectionMode.current, modifier = Modifier.padding(bottom = 6.dp))
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("늦잠!", color = PooangColors.Warn, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
            Text("${GameEngine.dayNames[run.day.coerceIn(0, 6)]} 아침", color = PooangColors.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "체력이 떨어져 알람을 놓쳤어요. 오전 ${run.activity.label}에 늦을 것 같아요.",
                color = Color(0xFF3C434F),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            when (run.phase) {
                MorningPhase.READY -> {
                    PooangCard(contentPadding = 12.dp) {
                        Text(
                            "${LifeEngine.MORNING_DURATION_MS / 1000}초 안에 달리기를 ${LifeEngine.MORNING_TAPS}번 누르면 제시간에 도착해요.",
                            color = PooangColors.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            if (subjectName != null) "실패하면 $subjectName 이번 시험 계산에서 ${LifeEngine.LATE_PENALTY}점이 깎여요."
                            else "오전 수업이 아니라서 실패해도 성적 감점은 없어요.",
                            color = if (subjectName != null) PooangColors.Warn else PooangColors.TextSub,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    PooangButton("달리기 시작", onClick = { onBegin(run.key) }, modifier = Modifier.fillMaxWidth(),
                        style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_run)
                    PooangButton("자동 판정으로 건너뛰기 (성공 50%)", onClick = { onSkip(run.key) },
                        modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
                }

                MorningPhase.PLAY -> {
                    val total = LifeEngine.MORNING_DURATION_MS.toFloat()
                    val leftMs = (run.deadlineMs - now).coerceAtLeast(0L)
                    val progress = run.taps.toFloat() / LifeEngine.MORNING_TAPS
                    Text(
                        "남은 시간 ${(leftMs + 999L) / 1000L}초 · ${run.taps}/${LifeEngine.MORNING_TAPS}회",
                        color = PooangColors.Warn, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                    TimerBar((leftMs / total).coerceIn(0f, 1f))
                    RunBar(progress.coerceIn(0f, 1f))
                    TapButton { onTap(run.key, run.taps) }
                    PooangButton("포기하고 결과 보기", onClick = { onSkip(run.key) },
                        modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
                }

                MorningPhase.RESULT -> {
                    val ok = run.success == true
                    Text(
                        if (ok) "간신히 도착했어요!" else "결국 지각했어요",
                        color = if (ok) PooangColors.UpText else PooangColors.Warn,
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                    game.life.morningNotice?.let {
                        PooangCard(contentPadding = 12.dp) {
                            Text(LifeEngine.attendanceMessage(it), color = PooangColors.TextBody, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                    PooangButton("이어서 하루 진행", onClick = { onContinue(run.key) }, modifier = Modifier.fillMaxWidth(),
                        style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_play)
                }
            }

            Text(
                "시작한 ${LifeEngine.MORNING_DURATION_MS / 1000}초는 홈으로 나가거나 앱을 꺼도 계속 흘러요.",
                color = PooangColors.TextFaint, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            PooangButton("홈으로", onClick = onHome, modifier = Modifier.fillMaxWidth(),
                style = PooangButtonStyle.Ghost, small = true)
        }
    }
}

/** 남은 시간 막대 (`.timer`): 주황→빨강, 시간이 지나면 줄어든다. */
@Composable
private fun TimerBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(PooangColors.Line)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Brush.horizontalGradient(listOf(PooangColors.HpMid, PooangColors.Warn)))
        )
    }
}

/** 달리기 막대 (`.runbar`): 누를수록 파란 막대가 차고 달리는 아이콘이 앞으로 간다. */
@Composable
private fun RunBar(fraction: Float) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(PooangColors.Line)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Brush.horizontalGradient(listOf(Color(0xFF378ADD), PooangColors.CauBlue)))
        )
        Icon(
            painterResource(R.drawable.pooang_ic_run),
            null,
            tint = if (fraction > 0.08f) Color.White else PooangColors.CauBlue,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = ((maxWidth - 30.dp) * fraction) + 3.dp)
                .size(24.dp)
        )
    }
}

/** 큰 파란 탭 버튼 (`.tapbtn`). 연타해야 하므로 눌림 효과는 짧게. */
@Composable
private fun TapButton(onTap: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shift by animateDpAsState(if (pressed) 4.dp else 0.dp, tween(40), label = "tap")
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onTap)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .offset(y = shift)
                .hardShadow(Color(0xFF073A7C), 4.dp - shift, 17.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(PooangColors.CauBlue)
                .padding(vertical = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("달리기!", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Preview(name = "늦잠 - 달리기", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun MorningPlayPreview() {
    val run = MorningRun(1, 3, 1, ActivityType.CLASS, "ds", phase = MorningPhase.PLAY, taps = 9,
        startedAtMs = 0L, deadlineMs = System.currentTimeMillis() + 2_500L)
    PooangTheme {
        MorningScreen(
            game = GameState(life = LifeState(pendingMorning = run)),
            onBegin = {}, onTap = { _, _ -> }, onRefresh = { _, _ -> }, onSkip = {}, onContinue = {}, onHome = {}
        )
    }
}
