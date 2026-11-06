package com.example.mobile_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
fun GameStatusPanel(game: GameState, subjects: List<Subject>) {
    val moneyText = NumberFormat.getIntegerInstance(Locale.KOREA).format(game.money)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("체력 ${game.hp}/${game.maxHp} · 행복도 ${game.happiness}/100")
        Text("돈 ${moneyText}원 · 인맥 ${game.network} · 매력 ${game.charm}")
        Text("스펙 ${game.spec}/100")
        if (game.completedSemesters.isNotEmpty()) {
            Text(String.format(Locale.KOREA, "누적 평점 %.2f / 4.50", game.cumulativeGpa))
        }
        Text("과목별 공부량", fontWeight = FontWeight.Bold)
        subjects.forEach { subject ->
            Text("${subject.name}: ${game.study[subject.id] ?: 0}")
        }
    }
}

@Composable
fun DayReportScreen(
    game: GameState,
    subjects: List<Subject>,
    onNextDay: () -> Unit,
    onNextWeek: () -> Unit,
    onHome: () -> Unit,
    continueLabel: String = "다음 주로"
) {
    val completedDay = GameEngine.dayNames[(game.day - 1).coerceIn(0, 6)]

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "${game.week}주차 · $completedDay 결과",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A3A1E)
        )

        GameStatusPanel(game, subjects)

        Text("오늘의 기록", fontWeight = FontWeight.Bold)
        game.log.forEach { line -> Text(line, lineHeight = 22.sp) }

        if (game.day < 7) {
            Button(onClick = onNextDay, modifier = Modifier.fillMaxWidth()) {
                Text("${GameEngine.dayNames[game.day.coerceIn(0, 6)]} 진행")
            }
        } else {
            Button(onClick = onNextWeek, modifier = Modifier.fillMaxWidth()) {
                Text(continueLabel)
            }
        }

        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("홈으로")
        }
    }
}
