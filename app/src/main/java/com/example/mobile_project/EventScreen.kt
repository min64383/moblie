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
fun EventScreen(
    game: GameState,
    subjects: List<Subject>,
    onChoose: (String, Int) -> Unit,
    onContinue: (String) -> Unit,
    onHome: () -> Unit
) {
    val run = game.pendingEvent ?: return
    val event = EventEngine.eventForId(run.eventId) ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "${semesterLabel(game.semester)} · ${game.week}주차",
            fontSize = 18.sp,
            color = Color(0xFF8A6A48)
        )
        Text(
            if (event.weeklyEvent) "주간 행사 · ${event.category}" else
                "${GameEngine.dayNames[run.day]} 진행 전 · ${event.category}",
            color = Color(0xFFB5651D),
            fontWeight = FontWeight.Bold
        )
        Text(
            event.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A3A1E)
        )

        if (run.phase == EventPhase.CHOICE) {
            Text(event.description.replace("{name}", run.contextName), fontSize = 17.sp, lineHeight = 27.sp)
            Text("어떻게 할까요?", fontWeight = FontWeight.Bold)

            event.options.forEachIndexed { index, option ->
                val affordable = EventEngine.canChoose(game, option)
                OutlinedButton(
                    onClick = { onChoose(run.key, index) },
                    enabled = affordable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(option.label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(EventEngine.effectDescriptions(option).joinToString(" · "))
                        if (!affordable) Text("잔액 부족 · 빚 한도 -50,000원을 넘을 수 없어요.")
                    }
                }
            }
            Text("선택을 마치면 이어서 진행할 수 있어요.", fontSize = 13.sp)
        } else {
            val option = event.options[run.selectedIndex]
            Text("선택: ${option.label}", fontWeight = FontWeight.Bold)
            Text(option.result.replace("{name}", run.contextName), fontSize = 17.sp, lineHeight = 27.sp)
            Text("실제 변화", fontWeight = FontWeight.Bold)
            run.changes.forEach { Text(it) }
            GameStatusPanel(game, subjects)

            Button(
                onClick = { onContinue(run.key) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (event.weeklyEvent) "다음 주로" else "이어서 하루 진행")
            }
        }

        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("홈으로")
        }
    }
}
