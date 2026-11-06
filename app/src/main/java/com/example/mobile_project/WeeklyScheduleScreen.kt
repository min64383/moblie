package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeeklyScheduleScreen(
    subjects: List<Subject>,
    schedule: List<TimeSlot>,
    game: GameState,
    onPaint: (Int, TimeSlot) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onStartWeek: () -> Unit,
    onRoutines: () -> Unit
) {
    val days = listOf("월", "화", "수", "목", "금", "토", "일")
    val blocks = listOf("오전", "오후", "저녁", "밤")
    val activities = buildList {
        add(ActivityType.WORK)
        if (AcademicEngine.canUseActivity(game, ActivityType.TUTOR)) add(ActivityType.TUTOR)
        addAll(listOf(ActivityType.FUN, ActivityType.REST, ActivityType.CLUB,
            ActivityType.RUN, ActivityType.GYM, ActivityType.PT))
        if (AcademicEngine.canUseActivity(game, ActivityType.CAREER)) add(ActivityType.CAREER)
        add(ActivityType.SLEEP)
        add(ActivityType.EMPTY)
    }
    val choices = subjects.map { TimeSlot(ActivityType.STUDY, it.id) } + activities.map { TimeSlot(it) }

    var selectedChoice by rememberSaveable { mutableStateOf(0) }
    val sleepCount = schedule.count { it.activity == ActivityType.SLEEP }
    val filledCount = schedule.count { it.activity != ActivityType.EMPTY }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "주간 시간표",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A3A1E)
        )
        Text(
            "아래에서 활동을 고른 뒤 시간표 칸을 누르세요.\n" +
                "수업·조모임·데이트는 고정입니다. 표는 좌우로 밀어 볼 수 있어요.",
            fontSize = 13.sp
        )

        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("시간", Modifier.width(40.dp), textAlign = TextAlign.Center)
                days.forEach { day ->
                    Text(
                        day,
                        Modifier.width(60.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            blocks.forEachIndexed { blockIndex, blockName ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier.width(40.dp).height(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(blockName, fontSize = 12.sp)
                    }

                    days.forEachIndexed { dayIndex, _ ->
                        val index = dayIndex * 4 + blockIndex
                        val slot = schedule[index]
                        val subjectName = subjects.find { it.id == slot.subjectId }?.name
                        val label = listOfNotNull(
                            slot.activity.label,
                            subjectName,
                            if (slot.locked) "고정" else null
                        ).joinToString("\n")

                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(72.dp)
                                .background(Color(slot.activity.color), RoundedCornerShape(8.dp))
                                .clickable(enabled = !slot.locked) {
                                    onPaint(index, choices[selectedChoice])
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (slot.activity == ActivityType.EMPTY) {
                                    Color(0xFF5A3A1E)
                                } else {
                                    Color.White
                                },
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Text(
            "배치 $filledCount/28칸 · 수면 $sleepCount 칸 (최소 5칸)",
            color = if (sleepCount < 5) Color(0xFFC0504D) else Color(0xFF2E7D52),
            fontWeight = FontWeight.Bold
        )
        Text("배치할 활동", fontWeight = FontWeight.Bold)

        choices.chunked(3).forEachIndexed { rowIndex, rowChoices ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowChoices.forEachIndexed { columnIndex, choice ->
                    val choiceIndex = rowIndex * 3 + columnIndex
                    val selected = choiceIndex == selectedChoice
                    val label = if (choice.activity == ActivityType.STUDY) {
                        "${subjects.first { it.id == choice.subjectId }.name}\n공부"
                    } else {
                        choice.activity.label
                    }

                    Button(
                        onClick = { selectedChoice = choiceIndex },
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFFFF9431) else Color(0xFFE9D8C0),
                            contentColor = if (selected) Color.White else Color(0xFF5A3A1E)
                        )
                    ) {
                        Text(label, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
                repeat(3 - rowChoices.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Text(
            "빈 칸은 아무 활동 없이 지나가요.\n" +
                "시작하면 한 주가 끝날 때까지 시간표를 바꿀 수 없어요.",
            fontSize = 13.sp
        )
        Button(onClick = onStartWeek, modifier = Modifier.fillMaxWidth()) {
            Text("시간 보내기")
        }

        OutlinedButton(onClick = onRoutines, modifier = Modifier.fillMaxWidth()) {
            Text("루틴 저장·불러오기")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text("시간표 비우기")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("홈으로")
            }
        }
    }
}
