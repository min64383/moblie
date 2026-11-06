package com.example.mobile_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun GraduationScreen(game: GameState, onTitle: () -> Unit, onHome: () -> Unit) {
    val ending = AcademicEngine.graduationResult(game)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("졸업 심사 결과", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(ending.title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF5A3A1E))
        Text(ending.description, lineHeight = 24.sp)
        Text("이수 학기 ${game.completedSemesters.size}/8")
        Text(String.format(Locale.KOREA, "누적 평점 %.2f / 4.50", game.cumulativeGpa))
        Text("스펙 ${game.spec} · 인맥 ${game.network} · 최종 잔액 ${game.money}원")
        Text("학기별 평점", fontWeight = FontWeight.Bold)
        game.completedSemesters.forEach { record ->
            Text(String.format(Locale.KOREA, "%s: %.2f", semesterLabel(record.semester), record.gpa))
        }
        Text("방학 기록", fontWeight = FontWeight.Bold)
        game.vacationHistory.forEach { record ->
            val title = AcademicEngine.vacationOption(record.activityId)?.title ?: record.activityId
            Text("${vacationLabel(record.semester)}: $title")
        }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
        OutlinedButton(onClick = onTitle, modifier = Modifier.fillMaxWidth()) { Text("타이틀로") }
    }
}
