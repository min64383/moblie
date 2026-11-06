package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MiniGameScreen(
    game: GameState,
    onBegin: (String) -> Unit,
    onRefresh: (String, Long) -> Unit,
    onInput: (String, Int, Int) -> Unit,
    onSubmit: (String) -> Unit,
    onSkip: (String) -> Unit,
    onContinue: (String) -> Unit,
    onHome: () -> Unit
) {
    val run = game.pendingMiniGame ?: return
    val blocks = listOf("오전", "오후", "저녁", "밤")
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

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("${run.activity.label} 미니게임", fontSize = 18.sp, color = Color(run.activity.color))
        Text(run.kind.title, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("${GameEngine.dayNames[run.day]} ${blocks[run.block]} 활동")

        when (run.phase) {
            MiniPhase.READY -> {
                Text(when (run.kind) {
                    MiniKind.FOCUS -> "9초 안에 1부터 9까지 순서대로 누르세요. 빠르게 끝낼수록 보너스가 커져요."
                    MiniKind.MEMORY -> "용어 4개의 순서를 외운 뒤 같은 순서로 눌러 주세요."
                    MiniKind.CALC -> "11초 안에 계산 문제 3개를 풀어 주세요."
                    MiniKind.ORDER -> "3초 동안 주문 3개를 외운 뒤 골라 주세요."
                    MiniKind.CHANGE -> "9초 안에 정확한 거스름돈을 골라 주세요."
                    MiniKind.TUTOR -> "11초 안에 학생의 질문에 답해 주세요."
                    MiniKind.INTERVIEW -> "12초 안에 면접 답변을 골라 주세요."
                    MiniKind.TIMING -> "표시가 초록 구간에 있을 때 멈추세요. 기회는 3번이에요."
                    MiniKind.HOLD -> "6초 동안 40번 누르세요. 누른 횟수에 따라 최대 체력이 추가로 올라요."
                }, fontSize = 17.sp)
                Button(onClick = { onBegin(run.key) }, modifier = Modifier.fillMaxWidth()) { Text("시작") }
            }
            MiniPhase.MEMORIZE -> {
                Text("순서를 기억하세요", fontWeight = FontWeight.Bold)
                run.sequence.forEachIndexed { index, choice ->
                    Text("${index + 1}. ${run.choices[choice]}", fontSize = 21.sp)
                }
                Text("잠시 뒤 선택 화면으로 넘어가요.")
            }
            MiniPhase.PLAY -> {
                if (run.durationMs > 0L) {
                    val left = ((run.deadlineMs - now).coerceAtLeast(0L) + 999) / 1_000
                    Text("남은 시간 ${left}초", fontWeight = FontWeight.Bold,
                        color = if (left <= 3) Color(0xFFC0504D) else Color(0xFF5A3A1E))
                }
                when (run.kind) {
                    MiniKind.FOCUS, MiniKind.MEMORY, MiniKind.ORDER -> {
                        Text(when (run.kind) {
                            MiniKind.FOCUS -> "다음 숫자: ${run.progress + 1}"
                            MiniKind.MEMORY -> "${run.progress + 1}번째 용어를 고르세요"
                            else -> "기억한 주문 3개를 고르세요 (${run.selected.size}/3)"
                        }, fontWeight = FontWeight.Bold)
                        run.choices.chunked(3).forEachIndexed { row, choices ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                choices.forEachIndexed { column, label ->
                                    val index = row * 3 + column
                                    val selected = index in run.selected
                                    val completed = when (run.kind) {
                                        MiniKind.FOCUS -> label.toInt() <= run.progress
                                        MiniKind.MEMORY -> index in run.sequence.take(run.progress)
                                        else -> false
                                    }
                                    OutlinedButton(
                                        onClick = { onInput(run.key, index, run.revision) },
                                        enabled = !completed && (run.kind != MiniKind.ORDER || selected || run.selected.size < 3),
                                        modifier = Modifier.weight(1f).height(66.dp)
                                    ) { Text(if (selected) "✓ $label" else label) }
                                }
                                repeat(3 - choices.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if (run.kind == MiniKind.ORDER) {
                            Button(onClick = { onSubmit(run.key) }, enabled = run.selected.size == 3,
                                modifier = Modifier.fillMaxWidth()) { Text("주문 제출") }
                        }
                    }
                    MiniKind.CALC, MiniKind.CHANGE, MiniKind.TUTOR, MiniKind.INTERVIEW -> {
                        val question = run.questions[run.progress]
                        Text("${run.progress + 1}/${run.questions.size}문항")
                        Text(question.prompt, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        question.options.forEachIndexed { index, option ->
                            OutlinedButton(onClick = { onInput(run.key, index, run.revision) }, modifier = Modifier.fillMaxWidth()) {
                                Text(option)
                            }
                        }
                    }
                    MiniKind.TIMING -> {
                        Text("${run.progress + 1}/3번째 · 성공 ${run.correct}회")
                        val position = MiniGameEngine.marker(run, now) / 100f
                        BoxWithConstraints(Modifier.fillMaxWidth().height(34.dp).background(Color(0xFFE9D8C0))) {
                            val width = maxWidth
                            Box(Modifier.offset(x = width * 0.39f).width(width * 0.22f)
                                .fillMaxHeight().background(Color(0xFF5A9E4B)))
                            Box(Modifier.offset(x = width * position - 2.dp).width(4.dp)
                                .fillMaxHeight().background(Color(0xFF5A3A1E)))
                        }
                        Button(onClick = { onInput(run.key, 0, run.revision) }, modifier = Modifier.fillMaxWidth()) { Text("멈추기") }
                    }
                    MiniKind.HOLD -> {
                        Text("${run.progress}/40회", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.fillMaxWidth().height(22.dp).background(Color(0xFFE9D8C0))) {
                            Box(Modifier.fillMaxWidth((run.progress / 40f).coerceIn(0f, 1f)).fillMaxHeight()
                                .background(Color(0xFF2E9183)))
                        }
                        Button(onClick = { onInput(run.key, 0, run.revision) },
                            modifier = Modifier.fillMaxWidth().height(90.dp)) { Text("들어올리기", fontSize = 22.sp) }
                    }
                }
            }
            MiniPhase.RESULT -> {
                Text(run.resultTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(requireNotNull(run.reward).description, fontSize = 18.sp)
                Text("이 결과는 오늘 ${blocks[run.block]} ${run.activity.label} 칸에 반영돼요.")
                Button(onClick = { onContinue(run.key) }, modifier = Modifier.fillMaxWidth()) { Text("이어서 하루 진행") }
            }
        }

        if (run.phase != MiniPhase.RESULT) {
            OutlinedButton(onClick = { onSkip(run.key) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (run.phase == MiniPhase.READY) "이번 미니게임 건너뛰기" else "포기하고 결과 보기")
            }
        }
        Text("시작한 제한 시간은 화면을 나가도 계속 흘러요.", fontSize = 13.sp)
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
    }
}
