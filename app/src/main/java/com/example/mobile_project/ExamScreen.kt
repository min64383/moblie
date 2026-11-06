package com.example.mobile_project

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ExamScreen(
    game: GameState,
    subjects: List<Subject>,
    onBegin: () -> Unit,
    onAnswer: (Int, Int) -> Unit,
    onTimeout: (Int) -> Unit,
    onNext: (Int) -> Unit,
    onContinue: () -> Unit,
    onHome: () -> Unit
) {
    val run = game.exam ?: return
    var now by remember(run.index, run.deadlineMs) {
        mutableStateOf(SystemClock.elapsedRealtime())
    }
    val currentTimeout by rememberUpdatedState(onTimeout)

    // 저장한 마감 시각을 사용해 화면을 회전해도 제한 시간이 늘어나지 않게 한다.
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "${semesterLabel(game.semester)} ${run.kind.title}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A3A1E)
        )

        when (run.phase) {
            ExamPhase.READY -> {
                Text("시험장 입실", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(when (run.optionCount) {
                    2 -> "충분히 공부했습니다."
                    3 -> "어느 정도는 봤습니다."
                    else -> "거의 안 봤습니다."
                })
                if (run.jokbo) {
                    Text("족보 입수! 과목별 공부량이 15씩 증가했어요.", color = Color(0xFF2E7D52))
                }
                Text("보기 ${run.optionCount}개 · 문항당 ${run.secondsPerQuestion}초 · 총 ${run.questionIds.size}문항")
                Text("공부량을 바탕으로 정답률에 따라 점수가 0.85~1.15배 조정돼요.")
                game.life.lateCounts.filterValues { it > 0 }.forEach { (id, count) ->
                    Text("${subjectForId(id)?.name ?: id}: 지각 ${count}회 · 점수 계산에서 -${count * LifeEngine.LATE_PENALTY}점",
                        color = Color(0xFFC0504D))
                }
                Button(onClick = onBegin, modifier = Modifier.fillMaxWidth()) {
                    Text("시험 시작")
                }
            }

            ExamPhase.QUESTION, ExamPhase.FEEDBACK -> {
                val question = ExamEngine.questionBank[run.questionIds[run.index]]
                val remaining = ((run.deadlineMs - now).coerceAtLeast(0L) + 999L) / 1_000L
                Text("${run.index + 1} / ${run.questionIds.size}문항")
                if (run.phase == ExamPhase.QUESTION) {
                    Text(
                        "남은 시간 ${remaining}초",
                        color = if (remaining <= 3) Color(0xFFC0504D) else Color(0xFF5A3A1E),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(question.prompt, fontSize = 19.sp, fontWeight = FontWeight.Bold)

                run.optionOrders[run.index].forEachIndexed { displayedIndex, answer ->
                    val color = when {
                        run.phase == ExamPhase.FEEDBACK && answer == 0 -> Color(0xFF2E7D52)
                        run.phase == ExamPhase.FEEDBACK && answer == run.selectedAnswer -> Color(0xFFC0504D)
                        else -> Color(0xFF5A3A1E)
                    }
                    OutlinedButton(
                        onClick = { onAnswer(answer, run.index) },
                        enabled = run.phase == ExamPhase.QUESTION,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, color)
                    ) {
                        Text("${displayedIndex + 1}. ${question.options[answer]}", color = color)
                    }
                }

                if (run.phase == ExamPhase.FEEDBACK) {
                    Text(when (run.selectedAnswer) {
                        0 -> "정답이에요!"
                        -1 -> "시간이 초과됐어요."
                        else -> "오답이에요."
                    })
                    Text("정답: ${question.options[0]}")
                    Button(onClick = { onNext(run.index) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (run.index == run.questionIds.lastIndex) "성적표 보기" else "다음 문제")
                    }
                }
            }

            ExamPhase.RESULT -> {
                val scores = if (run.kind == ExamKind.MIDTERM) game.midtermScores else game.finalScores
                Text("성적표", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("정답 ${run.correct}/${run.questionIds.size}문항")
                Text(String.format(Locale.KOREA, "정답률 %.0f%% · 보정 %.2f배", run.rate * 100, run.modifier))
                subjects.forEach { subject ->
                    Text("${subject.name}: ${scores[subject.id] ?: 0}점", fontSize = 18.sp)
                }
                val average = if (scores.isEmpty()) 0 else scores.values.average().roundToInt()
                Text("평균 ${average}점", fontWeight = FontWeight.Bold)
                Text("과목별 지각 감점을 반영한 점수예요. 이번 시험 범위의 지각 횟수는 초기화됐어요.", fontSize = 13.sp)
                if (run.kind == ExamKind.MIDTERM) {
                    Text("중간고사 범위의 공부량은 초기화됐어요. 기말고사를 위해 다시 쌓아 주세요.")
                }
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(if (run.kind == ExamKind.MIDTERM) "계속하기" else "학기 결산 보기")
                }
            }
        }

        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("홈으로")
        }
    }
}
