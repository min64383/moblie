package com.example.mobile_project

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.ChipTone
import com.example.mobile_project.ui.components.CountdownBar
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangChip
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 중간·기말고사 화면. 입실(준비) → 문제/정답 확인 반복 → 성적표 순서다.
 * 제한 시간은 저장된 마감 시각(deadlineMs)으로 계산하므로 화면을 돌려도 늘어나지 않는다.
 */
@Composable
fun ExamScreen(
    game: GameState,
    subjects: List<Subject>,
    onBegin: () -> Unit,
    onAnswer: (Int, Int) -> Unit,
    onTimeout: (Int) -> Unit,
    onNext: (Int) -> Unit,
    onContinue: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val run = game.exam ?: return
    var now by remember(run.index, run.deadlineMs) { mutableStateOf(SystemClock.elapsedRealtime()) }
    val currentTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(run.phase, run.index, run.deadlineMs) {
        if (run.phase != ExamPhase.QUESTION) return@LaunchedEffect
        while (true) {
            now = SystemClock.elapsedRealtime()
            if (now >= run.deadlineMs) {
                currentTimeout(run.index)
                break
            }
            delay(100)
        }
    }

    val scores = if (run.kind == ExamKind.MIDTERM) game.midtermScores else game.finalScores
    val average = if (scores.isEmpty()) 0 else scores.values.average().roundToInt()
    val mood = when (run.phase) {
        ExamPhase.READY -> if (run.optionCount == 2) PooangMood.PROUD else PooangMood.STUDY
        ExamPhase.QUESTION -> PooangMood.STUDY
        ExamPhase.FEEDBACK -> if (run.selectedAnswer == 0) PooangMood.HAPPY else PooangMood.SAD
        ExamPhase.RESULT -> when {
            average >= 80 -> PooangMood.PROUD
            average >= 60 -> PooangMood.OK
            else -> PooangMood.SAD
        }
    }

    ChallengeScaffold(
        mood = mood,
        kicker = "${semesterLabel(game.semester)} 시험",
        title = if (run.phase == ExamPhase.RESULT) "${run.kind.title} 성적표" else run.kind.title,
        subtitle = if (run.phase in setOf(ExamPhase.QUESTION, ExamPhase.FEEDBACK))
            "${run.index + 1} / ${run.questionIds.size}문항 · 맞힌 문제 ${run.correct}개" else null,
        badgeIcon = R.drawable.pooang_ic_exam,
        badgeColor = Color(0xFF8A6BB8),
        heroColors = listOf(Color(0xFFE6E0F5), Color(0xFFF6EEFF)),
        modifier = modifier
    ) {
        when (run.phase) {
            ExamPhase.READY -> ReadyPart(game, run, onBegin)
            ExamPhase.QUESTION, ExamPhase.FEEDBACK -> QuestionPart(run, now, onAnswer, onNext)
            ExamPhase.RESULT -> ResultPart(run, subjects, scores, average, onContinue)
        }
        PooangButton("홈으로", onClick = onHome, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadyPart(game: GameState, run: ExamRun, onBegin: () -> Unit) {
    PooangCard(title = "시험장 입실", titleIcon = R.drawable.pooang_ic_exam, titleIconTint = Color(0xFF8A6BB8)) {
        Text(
            when (run.optionCount) {
                2 -> "충분히 공부했습니다. 보기가 2개로 줄었어요!"
                3 -> "어느 정도는 봤습니다. 보기 3개 중에 고르세요."
                else -> "거의 안 봤습니다… 보기 4개 중에 골라야 해요."
            },
            color = PooangColors.TextBody, fontSize = 14.sp, fontWeight = FontWeight.Bold
        )
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            PooangChip("보기 ${run.optionCount}개", if (run.optionCount == 2) ChipTone.Up else ChipTone.Even)
            PooangChip("문항당 ${run.secondsPerQuestion}초", ChipTone.Even)
            PooangChip("총 ${run.questionIds.size}문항", ChipTone.Even)
        }
        Text(
            "공부량을 바탕으로, 정답률에 따라 점수가 0.85~1.15배로 조정돼요.",
            color = Color(0xFF7A8290), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
        )
    }
    if (run.jokbo) {
        Notice("족보 입수! 과목별 공부량이 15씩 늘었어요.", good = true)
    }
    game.life.lateCounts.filterValues { it > 0 }.forEach { (id, count) ->
        Notice("${subjectForId(id)?.name ?: id} 지각 ${count}회 · 점수 계산에서 −${count * LifeEngine.LATE_PENALTY}점", good = false)
    }
    PooangButton("시험 시작", onClick = onBegin, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Gold,
        icon = R.drawable.pooang_ic_play)
}

@Composable
private fun QuestionPart(run: ExamRun, now: Long, onAnswer: (Int, Int) -> Unit, onNext: (Int) -> Unit) {
    val question = ExamEngine.questionBank[run.questionIds[run.index]]
    val feedback = run.phase == ExamPhase.FEEDBACK

    // 문항 진행 점: 지난 문제는 보라, 지금 문제는 진한 보라
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        run.questionIds.indices.forEach { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            i < run.index -> Color(0xFFC9B8E8)
                            i == run.index -> Color(0xFF8A6BB8)
                            else -> Color(0xFFDED7C6)
                        }
                    )
            )
        }
    }

    if (!feedback) {
        val totalMs = run.secondsPerQuestion * 1_000L
        val leftMs = (run.deadlineMs - now).coerceAtLeast(0L)
        val leftSec = (leftMs + 999L) / 1_000L
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CountdownBar(leftMs.toFloat() / totalMs, Modifier.weight(1f))
            Text("${leftSec}초", color = if (leftSec <= 3) PooangColors.Warn else PooangColors.TextMid,
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }

    QuestionCard(question.prompt)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        run.optionOrders[run.index].forEachIndexed { shown, answer ->
            AnswerButton(
                text = "${shown + 1}. ${question.options[answer]}",
                onClick = { onAnswer(answer, run.index) },
                enabled = !feedback,
                state = when {
                    feedback && answer == 0 -> AnswerState.Right
                    feedback && answer == run.selectedAnswer -> AnswerState.Wrong
                    else -> AnswerState.Normal
                }
            )
        }
    }

    if (feedback) {
        val (text, good) = when (run.selectedAnswer) {
            0 -> "정답이에요!" to true
            -1 -> "시간이 초과됐어요." to false
            else -> "오답이에요." to false
        }
        Text(text, color = if (good) PooangColors.UpText else PooangColors.Warn, fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (!good) {
            Text("정답: ${question.options[0]}", color = PooangColors.TextSub, fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        val last = run.index == run.questionIds.lastIndex
        PooangButton(if (last) "성적표 보기" else "다음 문제", onClick = { onNext(run.index) },
            modifier = Modifier.fillMaxWidth(), style = if (last) PooangButtonStyle.Gold else PooangButtonStyle.Primary)
    }
}

@Composable
private fun ResultPart(run: ExamRun, subjects: List<Subject>, scores: Map<String, Int>, average: Int, onContinue: () -> Unit) {
    PooangCard(title = "성적표", titleIcon = R.drawable.pooang_ic_exam, titleIconTint = Color(0xFF8A6BB8)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            PooangChip("정답 ${run.correct}/${run.questionIds.size}", ChipTone.Even)
            PooangChip(String.format(Locale.KOREA, "정답률 %.0f%%", run.rate * 100), ChipTone.of(((run.rate - 0.5) * 100).toInt()))
            PooangChip(String.format(Locale.KOREA, "보정 ×%.2f", run.modifier), ChipTone.of(((run.modifier - 1.0) * 100).toInt()))
        }
        Column(Modifier.padding(top = 6.dp)) {
            subjects.forEachIndexed { i, subject ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEFEBE0)))
                Row(Modifier.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(subject.name, color = PooangColors.TextBody, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    val score = scores[subject.id] ?: 0
                    Text("${score}점", color = scoreColor(score), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(PooangColors.Line))
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("평균", color = PooangColors.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("${average}점", color = scoreColor(average), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
    Text(
        "과목별 지각 감점을 반영한 점수예요. 이번 시험 범위의 지각 횟수는 초기화됐어요." +
            if (run.kind == ExamKind.MIDTERM) "\n중간고사 범위의 공부량은 초기화됐어요. 기말고사를 위해 다시 쌓아 주세요." else "",
        color = Color(0xFF7A8290), fontSize = 12.sp, lineHeight = 18.sp
    )
    PooangButton(if (run.kind == ExamKind.MIDTERM) "계속하기" else "학기 결산 보기", onClick = onContinue,
        modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_play)
}

private fun scoreColor(score: Int): Color = when {
    score >= 90 -> PooangColors.UpText
    score >= 70 -> PooangColors.GhostText
    else -> PooangColors.Warn
}

@Composable
private fun Notice(text: String, good: Boolean) {
    Text(
        text,
        color = if (good) Color(0xFF2A5A3E) else Color(0xFF7A3835),
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (good) Color(0xFFE9F5EE) else Color(0xFFFDF0EF))
            .padding(horizontal = 11.dp, vertical = 10.dp)
    )
}

private fun previewExam(phase: ExamPhase, selected: Int = -1) = ExamRun(
    kind = ExamKind.MIDTERM, phase = phase, questionIds = listOf(0, 1, 2, 3, 4),
    optionOrders = List(5) { listOf(2, 0, 1) }, optionCount = 3, secondsPerQuestion = 8, jokbo = true,
    index = 1, correct = 1, selectedAnswer = selected, deadlineMs = Long.MAX_VALUE
)

@Preview(name = "시험 - 입실", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ExamReadyPreview() {
    PooangTheme { ExamScreen(GameState(exam = previewExam(ExamPhase.READY)), emptyList(), {}, { _, _ -> }, {}, {}, {}, {}) }
}

@Preview(name = "시험 - 정답 확인(오답)", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ExamFeedbackPreview() {
    PooangTheme { ExamScreen(GameState(exam = previewExam(ExamPhase.FEEDBACK, selected = 2)), emptyList(), {}, { _, _ -> }, {}, {}, {}, {}) }
}

@Preview(name = "시험 - 성적표", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ExamResultPreview() {
    val subjects = subjectsForSemester(1).take(3)
    PooangTheme {
        ExamScreen(
            GameState(exam = previewExam(ExamPhase.RESULT).copy(correct = 4),
                midtermScores = mapOf(subjects[0].id to 92, subjects[1].id to 74, subjects[2].id to 58)),
            subjects, {}, { _, _ -> }, {}, {}, {}, {}
        )
    }
}
