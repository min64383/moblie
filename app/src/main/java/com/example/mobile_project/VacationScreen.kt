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
fun VacationScreen(
    game: GameState,
    subjects: List<Subject>,
    onChoose: (String) -> Unit,
    onAdvance: () -> Unit,
    onHome: () -> Unit
) {
    val record = AcademicEngine.currentVacation(game)
    val money = NumberFormat.getIntegerInstance(Locale.KOREA).format(game.money)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(vacationLabel(game.semester), fontSize = 26.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFF5A3A1E))
        if (record == null) {
            Text("방학을 어떻게 보낼지 하나만 골라 주세요.")
            Text("현재 잔액 ${money}원\n활동 비용 외에 방학 생활비 200,000원이 나가요.")
            AcademicEngine.vacationOptions(game.semester).forEach { option ->
                val affordable = AcademicEngine.canAffordVacation(game, option)
                OutlinedButton(
                    onClick = { onChoose(option.id) },
                    enabled = affordable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(option.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(option.description)
                        if (!affordable) Text("잔액 부족")
                    }
                }
            }
        } else {
            val option = AcademicEngine.vacationOption(record.activityId)
            Text("${option?.title.orEmpty()}(으)로 방학을 보냈어요.", fontSize = 20.sp,
                fontWeight = FontWeight.Bold)
            Text(option?.description.orEmpty())
            Text("방학 생활비 200,000원을 포함해 정산했어요.")
            GameStatusPanel(game, subjects)
            val scholarship = game.semesterResult?.scholarship ?: 0
            if (scholarship > 0) {
                val amount = NumberFormat.getIntegerInstance(Locale.KOREA).format(scholarship)
                Text("다음 학기 수강신청에 들어가면 장학금 ${amount}원을 받아요.")
            }
            Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) { Text("다음 학기로") }
        }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
    }
}
