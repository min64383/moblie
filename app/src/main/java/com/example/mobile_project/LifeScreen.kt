package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.ChipTone
import com.example.mobile_project.ui.components.PanelScaffold
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangChip
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import java.text.NumberFormat
import java.util.Locale

private val LottoGold = Color(0xFFD98C2B)

/**
 * 생활 패널: 건강(감기·지각)과 주간 복권 (`#p-lotto`).
 * 복권은 주당 한 장, 사면 바로 결과가 나온다. 결과·구매 기록을 함께 보여 준다.
 */
@Composable
fun LifeScreen(game: GameState, onBuy: (Int, Int) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    PanelScaffold(
        title = "생활 · 복권",
        onClose = onBack,
        modifier = modifier,
        icon = R.drawable.pooang_ic_lotto,
        iconTint = LottoGold,
        caption = "잔액 ${won(game.money)}"
    ) {
        HealthCard(game)
        LotteryCard(game, onBuy)
        if (game.life.lotteryHistory.isNotEmpty()) {
            PooangCard(title = "최근 구매 기록", titleIcon = R.drawable.pooang_ic_cal, titleIconTint = PooangColors.TextFaint) {
                game.life.lotteryHistory.takeLast(8).reversed().forEach { saved ->
                    val prize = LifeEngine.prizes[saved.prizeIndex]
                    InfoLine("${semesterLabel(saved.semester)} ${saved.week}주차 · ${prize.title}", won(prize.amount),
                        if (prize.amount > 0) PooangColors.UpText else PooangColors.TextFaint)
                }
            }
        }
        PooangButton("홈으로", onClick = onBack, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
    }
}

private fun won(value: Int): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(value) + "원"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthCard(game: GameState) {
    val sick = LifeEngine.isSick(game)
    val sickNext = game.life.sickWeek == LifeEngine.absoluteWeek(game) + 1
    PooangCard(title = "건강", titleIcon = R.drawable.pooang_ic_hp, titleIconTint = PooangColors.Hp) {
        val (text, bad) = when {
            sick -> "감기몸살 · 이번 주 공부 효율이 평소의 60%예요." to true
            sickNext -> "다음 주 감기몸살 예정 · 공부 효율이 60%가 돼요." to true
            else -> "건강해요! 감기 효과가 없어요." to false
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (bad) Color(0xFFFDF0EF) else Color(0xFFEFF7F1))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PooangImage(if (bad) PooangMood.SICK else PooangMood.HAPPY, size = 40.dp)
            Text(text, color = if (bad) PooangColors.Warn else PooangColors.UpText, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            "아침 체력이 최대의 40% 미만이면 늦잠을 잘 수 있어요. 주말 정산 뒤 30% 미만이면 45% 확률로 다음 주에 감기에 걸려요. 수면과 휴식으로 관리하세요.",
            color = Color(0xFF7A8290), fontSize = 11.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp)
        )
        Text("이번 시험 범위의 수업 지각", color = PooangColors.TextMid, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp))
        val counts = game.life.lateCounts.filterValues { it > 0 }
        if (counts.isEmpty()) {
            PooangChip("없음", ChipTone.Up)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                counts.forEach { (id, count) ->
                    PooangChip("${subjectForId(id)?.name ?: id} ${count}회 · −${count * LifeEngine.LATE_PENALTY}점", ChipTone.Down)
                }
            }
        }
        Text("지각 1회마다 그 과목의 다음 시험 계산에서 ${LifeEngine.LATE_PENALTY}점 감점돼요. 시험이 끝나면 횟수가 비워져요.",
            color = Color(0xFF7A8290), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun LotteryCard(game: GameState, onBuy: (Int, Int) -> Unit) {
    val ticket = LifeEngine.currentTicket(game)
    val canBuy = LifeEngine.canBuyLottery(game)
    PooangCard(title = "주간 복권", titleIcon = R.drawable.pooang_ic_lotto, titleIconTint = LottoGold, trailing = {
        Text("한 장 ${won(LifeEngine.LOTTERY_PRICE)}", color = PooangColors.TextSub, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }) {
        // 당첨표
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, PooangColors.Line, RoundedCornerShape(10.dp))
        ) {
            LifeEngine.prizes.reversed().forEachIndexed { i, prize ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(PooangColors.LineSoft))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (ticket != null && LifeEngine.prizes[ticket.prizeIndex] == prize) Color(0xFFFFF0C2) else Color.White)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(prize.title, color = PooangColors.TextBody, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${prize.ticketsPerThousand / 10.0}%", color = PooangColors.TextFaint, fontSize = 11.sp,
                        modifier = Modifier.padding(end = 10.dp))
                    Text(won(prize.amount), color = if (prize.amount > 0) LottoGold else PooangColors.TextFaint,
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Text("모든 금액은 게임 속 돈이에요. 평균 당첨금은 7,700원으로 구매가보다 낮아요.",
            color = Color(0xFF7A8290), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))

        if (ticket != null) {
            val prize = LifeEngine.prizes[ticket.prizeIndex]
            val net = prize.amount - LifeEngine.LOTTERY_PRICE
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (prize.amount > 0) Color(0xFFFFF0C2) else Color(0xFFF3F0E7))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PooangImage(if (prize.amount > 0) PooangMood.PROUD else PooangMood.SAD, size = 72.dp,
                    bob = prize.amount > 0 && !LocalInspectionMode.current)
                Text("이번 주 결과", color = PooangColors.TextSub, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(prize.title, color = if (prize.amount > 0) LottoGold else PooangColors.TextSub,
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    PooangChip("당첨 +${won(prize.amount)}", if (prize.amount > 0) ChipTone.Up else ChipTone.Even)
                    PooangChip("순변화 ${if (net >= 0) "+" else "−"}${won(kotlin.math.abs(net))}", ChipTone.of(net))
                }
                Text("다음 주에 다시 살 수 있어요.", color = PooangColors.TextFaint, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            PooangButton("복권 사기 (${won(LifeEngine.LOTTERY_PRICE)})", onClick = { onBuy(game.semester, game.week) },
                enabled = canBuy, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Gold)
            if (!canBuy) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.pooang_ic_warn), null, tint = PooangColors.Warn, modifier = Modifier.size(13.dp))
                    Text(
                        when {
                            game.money - LifeEngine.LOTTERY_PRICE < -50_000 -> " 잔액 한도 때문에 지금은 살 수 없어요."
                            game.day == 7 -> " 이번 주 진행이 끝났어요. 다음 주에 살 수 있어요."
                            game.semesterResult != null || game.needsRegistration -> " 학기 진행 중에 살 수 있어요."
                            else -> " 진행 중인 시험이나 이벤트를 먼저 마쳐 주세요."
                        },
                        color = PooangColors.Warn, fontSize = 11.5.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(name = "생활 · 복권", showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun LifePreview() {
    PooangTheme {
        LifeScreen(
            GameState(study = mapOf("cal" to 0, "phy" to 0, "intro" to 0),
                life = LifeState(lateCounts = mapOf("cal" to 1), lotteryHistory = listOf(LotteryTicket(1, 1, 1), LotteryTicket(1, 2, 0)))),
            { _, _ -> }, {}
        )
    }
}
