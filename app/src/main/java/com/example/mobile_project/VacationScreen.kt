package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.IconTile
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangChip
import com.example.mobile_project.ui.components.SummaryTile
import com.example.mobile_project.ui.components.hardShadow
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import java.text.NumberFormat
import java.util.Locale

/** 방학 활동별 아이콘·색 */
private fun vacationIcon(id: String): Int = when (id) {
    "work" -> R.drawable.pooang_ic_work
    "rest" -> R.drawable.pooang_ic_rest
    "cert" -> R.drawable.pooang_ic_cert
    "trip" -> R.drawable.pooang_ic_plane
    "lang" -> R.drawable.pooang_ic_globe
    "intern" -> R.drawable.pooang_ic_intern
    else -> R.drawable.pooang_ic_tent
}

private fun vacationColor(id: String): Color = when (id) {
    "work" -> Color(0xFFD98C2B)
    "rest" -> Color(0xFF5A9E4B)
    "cert" -> Color(0xFF8A6BB8)
    "trip" -> Color(0xFFD4537E)
    "lang" -> Color(0xFF3B82C4)
    "intern" -> Color(0xFF4A5A72)
    else -> PooangColors.StatIcon
}

private fun vacationMood(id: String?): PooangMood = when (id) {
    "rest", "trip" -> PooangMood.HAPPY
    "work", "intern" -> PooangMood.TIRED
    "cert", "lang" -> PooangMood.PROUD
    else -> PooangMood.HEHE
}

private fun won(value: Int) = NumberFormat.getIntegerInstance(Locale.KOREA).format(value) + "원"

/**
 * 방학 화면 (`#sc-vac`). 활동 하나를 고르면 바로 정산되고, 결과와 바뀐 스탯을 보여 준다.
 */
@Composable
fun VacationScreen(
    game: GameState,
    subjects: List<Subject>,
    onChoose: (String) -> Unit,
    onAdvance: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val record = AcademicEngine.currentVacation(game)
    val chosen = record?.let { AcademicEngine.vacationOption(it.activityId) }

    ChallengeScaffold(
        mood = vacationMood(record?.activityId),
        kicker = "방학",
        title = vacationLabel(game.semester),
        subtitle = if (record == null) "방학을 어떻게 보낼지 하나만 골라 주세요." else null,
        badgeIcon = if (record != null) vacationIcon(record.activityId) else R.drawable.pooang_ic_tent,
        badgeColor = if (record != null) vacationColor(record.activityId) else Color(0xFF2E9183),
        heroColors = listOf(Color(0xFFA9D8FF), Color(0xFFE4F3FF)),
        modifier = modifier
    ) {
        if (record == null) {
            PooangCard(contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("현재 잔액", color = PooangColors.TextSub, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f))
                    Text(won(game.money), color = PooangColors.Text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("활동 비용과 별도로 방학 생활비 ${won(AcademicEngine.VACATION_EXPENSE)}이 나가요.",
                    color = Color(0xFF7A8290), fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp))
            }
            AcademicEngine.vacationOptions(game.semester).forEach { option ->
                VacationOptionCard(option, AcademicEngine.canAffordVacation(game, option)) { onChoose(option.id) }
            }
        } else {
            PooangCard(
                title = "${chosen?.title.orEmpty()}(으)로 방학을 보냈어요",
                titleIcon = vacationIcon(record.activityId),
                titleIconTint = vacationColor(record.activityId)
            ) {
                EffectChips(chosen?.description.orEmpty())
                Text("방학 생활비 ${won(AcademicEngine.VACATION_EXPENSE)}을 포함해 정산했어요.",
                    color = Color(0xFF7A8290), fontSize = 11.5.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryTile("체력", "${game.hp}/${game.maxHp}", Modifier.weight(1f))
                SummaryTile("행복도", "${game.happiness}", Modifier.weight(1f))
                SummaryTile("돈", manWon(game.money), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryTile("스펙", "${game.spec}", Modifier.weight(1f))
                SummaryTile("인맥", "${game.network}", Modifier.weight(1f))
                SummaryTile("매력", "${game.charm}", Modifier.weight(1f))
            }
            val scholarship = game.semesterResult?.scholarship ?: 0
            if (scholarship > 0) {
                PooangCard(background = Color(0xFFEAF7E0), borderColor = Color(0xFFB8E0A0), contentPadding = 12.dp) {
                    Text("다음 학기 수강신청 때 장학금 ${won(scholarship)}을 받아요.",
                        color = PooangColors.UpText, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            PooangButton("다음 학기로", onClick = onAdvance, modifier = Modifier.fillMaxWidth(),
                style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_play)
        }
        PooangButton("홈으로", onClick = onHome, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
    }
}

/** 방학 선택지 (`.vac`): 색 아이콘 + 이름 + 효과 칩. 돈이 모자라면 흐리게. */
@Composable
private fun VacationOptionCard(option: VacationOption, affordable: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (affordable) 1f else 0.42f)
            .padding(bottom = 2.dp)
            .hardShadow(PooangColors.Line, 2.dp, 14.dp)
            .clip(shape)
            .background(Color.White)
            .border(2.dp, PooangColors.Line, shape)
            .clickable(enabled = affordable, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.Top
    ) {
        IconTile(vacationIcon(option.id), vacationColor(option.id), size = 38.dp, iconSize = 21.dp, corner = 11.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(option.title, color = Color(0xFF15294A), fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
            EffectChips(option.description)
            if (!affordable) Text("잔액 부족", color = PooangColors.Warn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** "돈 -80,000 · 체력 완전 회복 · 행복 +35" → 색 칩들 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectChips(description: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        description.split(" · ").filter { it.isNotBlank() }.forEach { part ->
            val tone = if ("회복" in part) com.example.mobile_project.ui.components.ChipTone.Up else EventTone.of(" $part")
            PooangChip(part, tone, fontSize = 10.5f)
        }
    }
}

@Preview(name = "방학 - 고르기", showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun VacationChoosePreview() {
    PooangTheme { VacationScreen(GameState(semester = 3, money = 420_000), emptyList(), {}, {}, {}) }
}
