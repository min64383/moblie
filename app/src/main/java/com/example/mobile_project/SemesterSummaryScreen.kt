package com.example.mobile_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SemesterSummaryScreen(
    game: GameState,
    subjects: List<Subject>,
    onContinue: () -> Unit,
    onHome: () -> Unit
) {
    val result = game.semesterResult ?: return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "${semesterLabel(result.semester)} 종료",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A3A1E)
        )
        Text("수고했어요! 이번 학기 성적을 확인해 보세요.")
        Text(
            String.format(Locale.KOREA, "학기 평점 %.2f / 4.50", result.gpa),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(String.format(Locale.KOREA, "누적 평점 %.2f / 4.50", game.cumulativeGpa))
        result.grades.forEach { grade ->
            val name = subjects.find { it.id == grade.subjectId }?.name ?: grade.subjectId
            Text("$name: ${grade.score}점 · ${grade.letter}", fontSize = 18.sp)
        }
        if (result.scholarship > 0 && game.semester < 8) {
            val amount = NumberFormat.getIntegerInstance(Locale.KOREA).format(result.scholarship)
            Text("다음 학기 장학금 ${amount}원 예정", color = Color(0xFF2E7D52))
        }
        Text("학기별 평점", fontWeight = FontWeight.Bold)
        game.completedSemesters.forEach { record ->
            Text(String.format(Locale.KOREA, "%s: %.2f", semesterLabel(record.semester), record.gpa))
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(when {
                game.semester >= 8 -> "졸업 심사"
                AcademicEngine.currentVacation(game) != null -> "방학 결과 보기"
                else -> "방학으로"
            })
        }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
    }
}

@Composable
fun GradeRecordScreen(game: GameState, subjects: List<Subject>, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("${semesterLabel(game.semester)} 성적 기록", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        subjects.forEach { subject ->
            val midterm = game.midtermScores[subject.id]?.let { "${it}점" } ?: "미응시"
            val finalScore = game.finalScores[subject.id]?.let { "${it}점" } ?: "미응시"
            Text("${subject.name}\n중간: $midterm · 기말: $finalScore", lineHeight = 24.sp)
        }
        if (game.completedSemesters.isNotEmpty()) {
            Text(String.format(Locale.KOREA, "누적 평점 %.2f", game.cumulativeGpa), fontWeight = FontWeight.Bold)
        }
        game.completedSemesters.forEach { record ->
            Text(String.format(Locale.KOREA, "%s 평점 %.2f", semesterLabel(record.semester), record.gpa),
                fontWeight = FontWeight.Bold)
            record.grades.forEach { grade ->
                val name = subjectForId(grade.subjectId)?.name ?: grade.subjectId
                Text("$name: ${grade.score}점 · ${grade.letter}")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
    }
}
