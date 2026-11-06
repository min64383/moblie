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
fun LifeScreen(game: GameState, onBuy: (Int, Int) -> Unit, onBack: () -> Unit) {
    val format = NumberFormat.getIntegerInstance(Locale.KOREA)
    fun won(value: Int) = "${format.format(value)}원"
    val ticket = LifeEngine.currentTicket(game)
    val canBuy = LifeEngine.canBuyLottery(game)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("생활 · 건강과 주간 복권", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A3A1E))
        Text("건강", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(when {
            LifeEngine.isSick(game) -> "감기몸살 · 이번 주 공부 효율이 평소의 60%예요."
            game.life.sickWeek == LifeEngine.absoluteWeek(game) + 1 -> "다음 주 감기몸살 · 공부 효율이 60%가 될 예정이에요."
            else -> "현재 감기 효과가 없어요."
        })
        Text("아침 체력이 최대 체력의 40% 미만이면 늦잠을 잘 수 있어요. 주말 정산 후 30% 미만이면 45% 확률로 다음 주 감기에 걸려요. 수면과 휴식으로 체력을 관리하세요.")
        Text("수업 지각은 해당 과목의 다음 시험 계산에서 1회당 3점 감점돼요. 중간·기말고사가 끝나면 해당 범위의 횟수를 비워요.")
        val counts = game.life.lateCounts.filterValues { it > 0 }
        if (counts.isEmpty()) Text("이번 시험 범위의 수업 지각: 없음")
        else counts.forEach { (id, count) -> Text("${subjectForId(id)?.name ?: id}: ${count}회 (-${count * LifeEngine.LATE_PENALTY}점)") }

        Text("주간 복권", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("모든 금액은 게임 속 재화예요. 한 장 ${won(LifeEngine.LOTTERY_PRICE)}, 주당 한 장만 살 수 있어요.")
        Text("현재 잔액: ${won(game.money)}")
        LifeEngine.prizes.forEach { prize ->
            Text("${prize.title} · ${prize.ticketsPerThousand / 10.0}% · ${won(prize.amount)}")
        }
        Text("평균 당첨금은 7,700원으로 구매가보다 낮아요.", fontSize = 13.sp)
        if (ticket != null) {
            val prize = LifeEngine.prizes[ticket.prizeIndex]
            Text("이번 주 결과: ${prize.title}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("구매 -${won(LifeEngine.LOTTERY_PRICE)} · 당첨 +${won(prize.amount)} · 순변화 ${won(prize.amount - LifeEngine.LOTTERY_PRICE)}")
            Text("비용과 당첨금은 이미 잔액에 반영됐어요. 다음 주에 다시 살 수 있어요.")
        } else {
            Button(onClick = { onBuy(game.semester, game.week) }, enabled = canBuy, modifier = Modifier.fillMaxWidth()) {
                Text("복권 사기 (${won(LifeEngine.LOTTERY_PRICE)})")
            }
            if (!canBuy) Text(when {
                game.money - LifeEngine.LOTTERY_PRICE < -50_000 -> "잔액 한도 때문에 지금은 살 수 없어요."
                game.day == 7 -> "이번 주 진행이 끝났어요. 다음 주가 시작되면 살 수 있어요."
                game.semesterResult != null || game.needsRegistration -> "학기 진행 중에 살 수 있어요."
                else -> "진행 중인 시험이나 이벤트를 먼저 마쳐 주세요."
            })
        }
        if (game.life.lotteryHistory.isNotEmpty()) {
            Text("최근 구매 기록", fontWeight = FontWeight.Bold)
            game.life.lotteryHistory.takeLast(8).reversed().forEach { saved ->
                val prize = LifeEngine.prizes[saved.prizeIndex]
                Text("${semesterLabel(saved.semester)} ${saved.week}주차 · ${prize.title} · ${won(prize.amount)}")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
    }
}
