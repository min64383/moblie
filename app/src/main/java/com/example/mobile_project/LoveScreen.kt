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

@Composable
fun LoveScreen(
    game: GameState,
    schedule: List<TimeSlot>,
    onChoose: (String, Boolean) -> Unit,
    onContinue: (String) -> Unit,
    onBack: () -> Unit
) {
    val confession = game.pendingConfession
    val love = game.love
    val blocks = listOf("오전", "오후", "저녁", "밤")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("연애", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4537E))
        if (confession != null) {
            if (confession.choice == LoveChoice.UNDECIDED) {
                Text("${confession.partner}가 고백했어요", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("운동으로 다져진 모습과 넓은 인맥이 눈에 띄었나 봐요. 어떻게 답할까요?", lineHeight = 25.sp)
                Text("사귀면 주간 시간표에 데이트 1칸이 고정돼요. " +
                    "데이트마다 돈 20,000원과 행복도 +25가 적용돼요.")
                Text("잔액 부족 등으로 데이트를 누적 3회 놓치면 주말에 헤어져요.")
                Button(
                    onClick = { onChoose(confession.key, true) },
                    enabled = LoveEngine.canAccept(game, schedule),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("사귀기") }
                if (!LoveEngine.canAccept(game, schedule)) Text("데이트를 배정할 자유 시간이 없어요.")
                OutlinedButton(onClick = { onChoose(confession.key, false) }, modifier = Modifier.fillMaxWidth()) {
                    Text("지금은 다른 일에 집중할게요")
                }
            } else {
                Text(if (confession.choice == LoveChoice.ACCEPT) "연애 시작" else "정중히 거절했어요",
                    fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(if (confession.choice == LoveChoice.ACCEPT) {
                    "${confession.partner}와 사귀기로 했어요. 시간표에 고정된 데이트를 확인해 주세요."
                } else "이번 학기에는 다른 일에 집중하기로 했어요.")
                Button(onClick = { onContinue(confession.key) }, modifier = Modifier.fillMaxWidth()) {
                    Text("계속하기")
                }
            }
        } else if (love == null) {
            Text("현재 연애 중이 아니에요.")
            Text("2학년부터 학기 시작 시 매력 40·인맥 30 이상이면 고백 이벤트가 발생해요.")
            Text("현재 매력 ${game.charm}/40 · 인맥 ${game.network}/30")
        }

        if (love != null) {
            Text("${love.partner}와 연애 중", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("함께 보낸 기간 ${love.weeksTogether}주 · 애정도 ${love.affection}/100")
            Text("데이트 누적 취소 ${love.missedDates}/${LoveEngine.MISSED_LIMIT}")
            Text(if (love.dateIndex >= 0) {
                "고정 데이트: ${GameEngine.dayNames[love.dateIndex / 4]} ${blocks[love.dateIndex % 4]}"
            } else "이번 주는 빈 자유 시간이 없어 데이트가 배정되지 않았어요.")
            Text("데이트 취소 횟수는 성공한 주가 있어도 누적돼요. 애정도는 선택 이벤트에 따라 바뀌어요.", fontSize = 13.sp)
        }
        if (game.loveHistory.isNotEmpty()) {
            Text("지난 연애", fontWeight = FontWeight.Bold)
            game.loveHistory.reversed().forEach { record ->
                Text("${record.partner} · ${record.weeksTogether}주\n" +
                    "${semesterLabel(record.endSemester)} ${record.endWeek}주차 · ${record.reason.label}")
            }
        }
        if (confession == null) {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
        }
    }
}
