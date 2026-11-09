package com.example.mobile_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
        if (LifeEngine.isSick(game)) {
            Text("감기몸살 · 이번 주 공부 효율 60%", color = Color(0xFFC0504D), fontSize = 13.sp)
        } else if (game.life.sickWeek == LifeEngine.absoluteWeek(game) + 1) {
            Text("건강 경고 · 다음 주 감기몸살 예정 · 공부 효율 60%", color = Color(0xFFC0504D), fontSize = 13.sp)
        }
        game.life.lateCounts.filterValues { it > 0 }.forEach { (id, count) ->
            Text("${subjectForId(id)?.name ?: id} · 지각 ${count}회 · 이번 시험 계산에서 -${count * LifeEngine.LATE_PENALTY}점",
                color = Color(0xFFC0504D), fontSize = 13.sp)
        }
        game.love?.let { love ->
            Text("연애 · ${love.partner} · 애정도 ${love.affection}/100 · " +
                "데이트 취소 ${love.missedDates}/${LoveEngine.MISSED_LIMIT}",
                fontSize = 13.sp, color = Color(0xFFD4537E))
        }
        game.teamProject?.let { project ->
            Text("조별과제 · ${subjectForId(project.subjectId)?.name ?: project.subjectId} · " +
                "${project.weeksLeft}주 남음", fontSize = 13.sp, color = Color(0xFFC0504D))
        }
        EventEngine.visibleBuffs(game).forEach { buff ->
            val planned = buff.startDay > EventEngine.absoluteDay(game)
            val label = if (planned) "예정 · ${buff.label}" else
                "적용 중 · ${buff.label.replace("다음 주", "이번 주")}" 
            Text(label, color = Color(0xFF2E7D52), fontSize = 13.sp)
        }
        if (game.completedSemesters.isNotEmpty()) {
            Text(String.format(Locale.KOREA, "누적 평점 %.2f / 4.50", game.cumulativeGpa))
        }
        // 원본 HTML처럼 누적 공부량 대신 시험 예상 점수(0~100)로 보여 준다.
        Text("과목별 예상 점수", fontWeight = FontWeight.Bold)
        subjects.forEach { subject ->
            val study = game.study[subject.id] ?: 0
            Text("${subject.name}: ${ExamEngine.expectedScore(study, game.semester)}점 (공부량 $study)")
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
    continueLabel: String = "다음 주로",
    mode: ProgressMode = ProgressMode.DAY,
    onModeChange: (ProgressMode) -> Unit = {},
    weekEntries: List<DayLogEntry> = emptyList()
) {
    val completedDay = GameEngine.dayNames[(game.day - 1).coerceIn(0, 6)]
    val autoRunning = mode == ProgressMode.WEEK && WeekLogBook.canAutoAdvance(game)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (mode == ProgressMode.WEEK) "${game.week}주차 · 주간 기록" else "${game.week}주차 · $completedDay 결과",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A3A1E)
        )

        // 진행 방식 선택: 하루씩 직접 넘기기 / 한 주 자동 진행
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressMode.values().forEach { option ->
                val selected = option == mode
                Button(
                    onClick = { onModeChange(option) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFFFF9431) else Color(0xFFE9D8C0),
                        contentColor = if (selected) Color.White else Color(0xFF5A3A1E)
                    )
                ) {
                    Text(option.label)
                }
            }
        }

        GameStatusPanel(game, subjects)

        if (mode == ProgressMode.WEEK) {
            Text("이번 주 기록", fontWeight = FontWeight.Bold)
            weekEntries.forEach { entry ->
                Text(entry.dayName, fontWeight = FontWeight.Bold, color = Color(0xFF5A3A1E))
                entry.lines.forEach { line -> Text(line, lineHeight = 22.sp) }
            }
        } else {
            Text("오늘의 기록", fontWeight = FontWeight.Bold)
            game.log.forEach { line -> Text(line, lineHeight = 22.sp) }
        }

        if (game.day < 7) {
            if (autoRunning) {
                Text(
                    "${GameEngine.dayNames[game.day.coerceIn(0, 6)]} 자동 진행 중…",
                    color = Color(0xFF8A9099)
                )
            } else {
                Button(onClick = onNextDay, modifier = Modifier.fillMaxWidth()) {
                    Text("${GameEngine.dayNames[game.day.coerceIn(0, 6)]} 진행")
                }
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
