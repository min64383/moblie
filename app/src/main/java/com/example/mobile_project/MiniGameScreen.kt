package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.BigTapButton
import com.example.mobile_project.ui.components.CountdownBar
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.ProgressTrack
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import kotlinx.coroutines.delay

/**
 * 미니게임 화면. 활동 칸(공부·알바·운동 등)에 붙은 짧은 게임이다.
 * 준비 → (외우기) → 플레이 → 결과 순서이고, 판정·시간 계산은 MiniGameEngine 이 한다.
 * 이 화면은 남은 시간을 새로 고쳐 그리고, 누른 칸 번호를 [onInput] 으로 넘긴다.
 */
@Composable
fun MiniGameScreen(
    game: GameState,
    onBegin: (String) -> Unit,
    onRefresh: (String, Long) -> Unit,
    onInput: (String, Int, Int) -> Unit,
    onSubmit: (String) -> Unit,
    onSkip: (String) -> Unit,
    onContinue: (String) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val run = game.pendingMiniGame ?: return
    var now by remember(run.key, run.startedAtMs) { mutableStateOf(System.currentTimeMillis()) }
    val refresh by rememberUpdatedState(onRefresh)

    LaunchedEffect(run.key, run.phase, run.startedAtMs, run.previewUntilMs) {
        if (run.phase !in setOf(MiniPhase.MEMORIZE, MiniPhase.PLAY)) return@LaunchedEffect
        if (run.phase == MiniPhase.PLAY && run.durationMs == 0L && run.kind != MiniKind.TIMING) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            refresh(run.key, now)
            delay(if (run.kind == MiniKind.TIMING) 16L else 80L)
        }
    }

    val mood = when (run.phase) {
        MiniPhase.READY -> PooangMood.HEHE
        MiniPhase.MEMORIZE -> PooangMood.STUDY
        MiniPhase.PLAY -> if (run.kind in setOf(MiniKind.TIMING, MiniKind.HOLD)) PooangMood.FIRE else PooangMood.STUDY
        MiniPhase.RESULT -> if (run.reward?.let { it == MiniReward() } != false) PooangMood.SAD else PooangMood.PROUD
    }
    val input = { index: Int -> onInput(run.key, index, run.revision) }

    ChallengeScaffold(
        mood = mood,
        kicker = "${run.activity.label} 미니게임 · ${GameEngine.dayNames[run.day.coerceIn(0, 6)]} ${DayLog.blockNames[run.block.coerceIn(0, 3)]}",
        title = run.kind.title,
        subtitle = null,
        badgeIcon = ActivityStyle.icon(run.activity),
        badgeColor = ActivityStyle.color(run.activity),
        modifier = modifier
    ) {
        when (run.phase) {
            MiniPhase.READY -> {
                PooangCard(contentPadding = 13.dp) {
                    Text(instruction(run.kind), color = PooangColors.Text, fontSize = 14.5.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                }
                PooangButton("시작", onClick = { onBegin(run.key) }, modifier = Modifier.fillMaxWidth(),
                    style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_play)
            }

            MiniPhase.MEMORIZE -> {
                MemoBox(run.sequence.map { run.choices[it] })
                Text("잠시 뒤 선택 화면으로 넘어가요.", color = PooangColors.TextSub, fontSize = 12.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            MiniPhase.PLAY -> {
                if (run.durationMs > 0L) {
                    val leftMs = (run.deadlineMs - now).coerceAtLeast(0L)
                    val leftSec = (leftMs + 999) / 1_000
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CountdownBar(leftMs.toFloat() / run.durationMs, Modifier.weight(1f))
                        Text("${leftSec}초", color = if (leftSec <= 3) PooangColors.Warn else PooangColors.TextMid,
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                PlayArea(run, now, input, onSubmit = { onSubmit(run.key) })
            }

            MiniPhase.RESULT -> {
                Text(run.resultTitle, color = PooangColors.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                val reward = run.reward ?: MiniReward()
                val good = reward != MiniReward()
                PooangCard(
                    background = if (good) Color(0xFFEAF7E0) else PooangColors.Card,
                    borderColor = if (good) Color(0xFFB8E0A0) else PooangColors.Line,
                    contentPadding = 12.dp
                ) {
                    Text(reward.description, color = if (good) PooangColors.UpText else PooangColors.TextSub,
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "이 결과는 오늘 ${DayLog.blockNames[run.block.coerceIn(0, 3)]} ${run.activity.label} 칸에 반영돼요.",
                        color = Color(0xFF7A8290), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                    )
                }
                PooangButton("이어서 하루 진행", onClick = { onContinue(run.key) }, modifier = Modifier.fillMaxWidth(),
                    style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_play)
            }
        }

        if (run.phase != MiniPhase.RESULT) {
            PooangButton(
                if (run.phase == MiniPhase.READY) "이번 미니게임 건너뛰기" else "포기하고 결과 보기",
                onClick = { onSkip(run.key) },
                modifier = Modifier.fillMaxWidth(),
                style = PooangButtonStyle.Ghost,
                small = true
            )
        }
        Text("시작한 제한 시간은 화면을 나가도 계속 흘러요.", color = PooangColors.TextFaint, fontSize = 11.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        PooangButton("홈으로", onClick = onHome, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
    }
}

private fun instruction(kind: MiniKind): String = when (kind) {
    MiniKind.FOCUS -> "9초 안에 1부터 9까지 순서대로 누르세요. 빠르게 끝낼수록 보너스가 커져요."
    MiniKind.MEMORY -> "용어 4개의 순서를 외운 뒤 같은 순서로 눌러 주세요."
    MiniKind.CALC -> "11초 안에 계산 문제 3개를 풀어 주세요."
    MiniKind.ORDER -> "3초 동안 주문 3개를 외운 뒤 골라 주세요."
    MiniKind.CHANGE -> "9초 안에 정확한 거스름돈을 골라 주세요."
    MiniKind.TUTOR -> "11초 안에 학생의 질문에 답해 주세요."
    MiniKind.INTERVIEW -> "12초 안에 면접 답변을 골라 주세요."
    MiniKind.TIMING -> "표시가 초록 구간에 있을 때 멈추세요. 기회는 3번이에요."
    MiniKind.HOLD -> "6초 동안 40번 누르세요. 누른 횟수에 따라 최대 체력이 추가로 올라요."
}

@Composable
private fun PlayArea(run: MiniGameRun, now: Long, input: (Int) -> Unit, onSubmit: () -> Unit) {
    when (run.kind) {
        MiniKind.FOCUS, MiniKind.MEMORY, MiniKind.ORDER -> {
            Text(
                when (run.kind) {
                    MiniKind.FOCUS -> "다음 숫자: ${run.progress + 1}"
                    MiniKind.MEMORY -> "${run.progress + 1}번째 용어를 고르세요"
                    else -> "기억한 주문 3개를 고르세요 (${run.selected.size}/3)"
                },
                color = PooangColors.TextMid, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
            )
            val columns = if (run.kind == MiniKind.FOCUS) 3 else 2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                run.choices.chunked(columns).forEachIndexed { row, labels ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        labels.forEachIndexed { column, label ->
                            val index = row * columns + column
                            val picked = index in run.selected
                            val done = when (run.kind) {
                                MiniKind.FOCUS -> label.toIntOrNull()?.let { it <= run.progress } == true
                                MiniKind.MEMORY -> index in run.sequence.take(run.progress)
                                else -> false
                            }
                            val enabled = !done && (run.kind != MiniKind.ORDER || picked || run.selected.size < 3)
                            AnswerButton(
                                text = if (picked) "✓ $label" else label,
                                onClick = { input(index) },
                                state = when {
                                    done -> AnswerState.Done
                                    picked -> AnswerState.Picked
                                    else -> AnswerState.Normal
                                },
                                enabled = enabled,
                                center = true,
                                fontSize = if (run.kind == MiniKind.FOCUS) 22f else 14f,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(if (run.kind == MiniKind.FOCUS) Modifier.aspectRatio(1f) else Modifier.height(56.dp))
                            )
                        }
                        repeat(columns - labels.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (run.kind == MiniKind.ORDER) {
                PooangButton("주문 제출", onClick = onSubmit, enabled = run.selected.size == 3,
                    modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Gold)
            }
        }

        MiniKind.CALC, MiniKind.CHANGE, MiniKind.TUTOR, MiniKind.INTERVIEW -> {
            val question = run.questions.getOrNull(run.progress) ?: return
            Text("${run.progress + 1}/${run.questions.size}문항", color = PooangColors.TextSub, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            QuestionCard(question.prompt)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEachIndexed { index, option ->
                    AnswerButton(option, onClick = { input(index) })
                }
            }
        }

        MiniKind.TIMING -> {
            Text("${run.progress + 1}/3번째 · 성공 ${run.correct}회", color = PooangColors.TextMid, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            TimingBar(MiniGameEngine.marker(run, now) / 100f)
            BigTapButton("멈추기", onClick = { input(0) })
        }

        MiniKind.HOLD -> {
            Text("${run.progress}/40회", color = PooangColors.Text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            ProgressTrack(
                run.progress / 40f,
                fill = Brush.horizontalGradient(listOf(Color(0xFF4FB3A6), Color(0xFF2E9183))),
                height = 22.dp
            )
            BigTapButton("들어올리기", onClick = { input(0) }, color = Color(0xFF2E9183), shadow = Color(0xFF1F6B60))
        }
    }
}

/** 외우기 상자 (`.memo`): 남색 바탕에 번호와 함께 흰 글씨. */
@Composable
private fun MemoBox(items: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF15294A), Color(0xFF23406B))))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("순서를 기억하세요", color = Color(0xFF9FB2CE), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        items.forEachIndexed { i, item ->
            Text("${i + 1}. $item", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** 타이밍 막대 (`.tmbar`): 가운데 초록 구간(39~61%)과 움직이는 파란 표시. */
@Composable
private fun TimingBar(position: Float) {
    val shape = RoundedCornerShape(12.dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(Color(0xFFEFEBE0))
            .border(2.dp, Color(0xFFDDD6C5), shape)
    ) {
        val w = maxWidth
        Box(
            Modifier
                .offset(x = w * 0.39f)
                .width(w * 0.22f)
                .fillMaxHeight()
                .background(Color(0xFFCDE6D7))
                .border(width = 3.dp, color = PooangColors.UpText)
        )
        Box(
            Modifier
                .offset(x = w * position.coerceIn(0f, 1f) - 2.5.dp)
                .width(5.dp)
                .fillMaxHeight()
                .shadow(6.dp, RoundedCornerShape(3.dp), ambientColor = PooangColors.CauBlue, spotColor = PooangColors.CauBlue)
                .clip(RoundedCornerShape(3.dp))
                .background(PooangColors.CauBlue)
        )
    }
}

@Preview(name = "미니게임 - 숫자 집중", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun MiniFocusPreview() {
    val run = MiniGameRun(1, 2, 1, 1, ActivityType.STUDY, MiniKind.FOCUS, phase = MiniPhase.PLAY,
        choices = listOf("3", "7", "1", "9", "5", "2", "8", "4", "6"), progress = 3,
        durationMs = 9_000L, deadlineMs = System.currentTimeMillis() + 5_000L)
    PooangTheme {
        MiniGameScreen(GameState(pendingMiniGame = run), {}, { _, _ -> }, { _, _, _ -> }, {}, {}, {}, {})
    }
}

@Preview(name = "미니게임 - 문제", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun MiniQuizPreview() {
    val run = MiniGameRun(1, 2, 1, 2, ActivityType.TUTOR, MiniKind.TUTOR, phase = MiniPhase.PLAY,
        questions = MiniGameCatalog.tutorQuestions.take(3), durationMs = 11_000L, deadlineMs = System.currentTimeMillis() + 7_000L)
    PooangTheme {
        MiniGameScreen(GameState(pendingMiniGame = run), {}, { _, _ -> }, { _, _, _ -> }, {}, {}, {}, {})
    }
}

@Preview(name = "미니게임 - 운동 타이밍", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun MiniTimingPreview() {
    val run = MiniGameRun(1, 2, 1, 2, ActivityType.GYM, MiniKind.TIMING, phase = MiniPhase.PLAY, progress = 1, correct = 1)
    PooangTheme {
        MiniGameScreen(GameState(pendingMiniGame = run), {}, { _, _ -> }, { _, _, _ -> }, {}, {}, {}, {})
    }
}

@Preview(name = "미니게임 - 결과", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun MiniResultPreview() {
    val run = MiniGameRun(1, 2, 1, 1, ActivityType.STUDY, MiniKind.MEMORY, phase = MiniPhase.RESULT,
        resultTitle = "완벽하게 외웠어요!", reward = MiniReward(studyMultiplier = 1.3, description = "이번 공부 효율 ×1.3"))
    PooangTheme {
        MiniGameScreen(GameState(pendingMiniGame = run), {}, { _, _ -> }, { _, _, _ -> }, {}, {}, {}, {})
    }
}
