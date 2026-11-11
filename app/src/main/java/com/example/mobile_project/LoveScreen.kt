package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.example.mobile_project.ui.components.ProgressTrack
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme

private val LovePink = Color(0xFFD4537E)
private val PinkFill = Brush.horizontalGradient(listOf(Color(0xFFFFB8CA), Color(0xFFE86A92)))

/**
 * 연애 패널 (`#p-love`).
 * - 고백 받음: 설렌 푸앙이 + 사귀기/거절 (고백이 끝날 때까지 닫을 수 없다)
 * - 연애 중: 함께한 주·애정도 막대·데이트 취소 횟수·고정 데이트 칸
 * - 연애 전: 고백 조건(학년·매력·인맥)까지 남은 정도
 * - 지난 연애 기록
 */
@Composable
fun LoveScreen(
    game: GameState,
    schedule: List<TimeSlot>,
    onChoose: (String, Boolean) -> Unit,
    onContinue: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confession = game.pendingConfession
    val love = game.love

    PanelScaffold(
        title = "연애",
        onClose = onBack,
        modifier = modifier,
        icon = R.drawable.pooang_ic_heart,
        iconTint = LovePink,
        caption = love?.let { "${it.weeksTogether}주째" },
        closable = confession == null
    ) {
        if (confession != null) {
            ConfessionCard(game, schedule, confession, onChoose, onContinue)
        } else if (love == null) {
            NotInLoveCard(game)
        }

        if (love != null) RelationshipCard(love)

        if (game.loveHistory.isNotEmpty()) {
            PooangCard(title = "지난 연애", titleIcon = R.drawable.pooang_ic_heart, titleIconTint = PooangColors.TextFaint) {
                game.loveHistory.reversed().forEach { r ->
                    InfoLine(
                        "${r.partner} · ${r.weeksTogether}주\n${semesterLabel(r.endSemester)} ${r.endWeek}주차",
                        r.reason.label, PooangColors.TextSub
                    )
                }
            }
        }

        if (confession == null) {
            PooangButton("홈으로", onClick = onBack, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
        }
    }
}

@Composable
private fun ConfessionCard(
    game: GameState,
    schedule: List<TimeSlot>,
    confession: LoveConfession,
    onChoose: (String, Boolean) -> Unit,
    onContinue: (String) -> Unit
) {
    val deciding = confession.choice == LoveChoice.UNDECIDED
    val mood = when (confession.choice) {
        LoveChoice.UNDECIDED -> PooangMood.SHOCK
        LoveChoice.ACCEPT -> PooangMood.LOVE
        LoveChoice.DECLINE -> PooangMood.OK
    }
    PooangCard(background = Color(0xFFFFF1F5), borderColor = Color(0xFFF5C2D3)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PooangImage(mood, size = 110.dp, bob = !LocalInspectionMode.current)
            Text(
                when (confession.choice) {
                    LoveChoice.UNDECIDED -> "${confession.partner}가 고백했어요!"
                    LoveChoice.ACCEPT -> "연애 시작!"
                    LoveChoice.DECLINE -> "정중히 거절했어요"
                },
                color = LovePink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
            )
            Text(
                when (confession.choice) {
                    LoveChoice.UNDECIDED -> "운동으로 다져진 모습과 넓은 인맥이 눈에 띄었나 봐요. 어떻게 답할까요?"
                    LoveChoice.ACCEPT -> "${confession.partner}와 사귀기로 했어요. 시간표에 고정된 데이트를 확인해 주세요."
                    LoveChoice.DECLINE -> "이번 학기에는 다른 일에 집중하기로 했어요."
                },
                color = PooangColors.TextBody, fontSize = 13.5.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
    if (deciding) {
        PooangCard(contentPadding = 12.dp) {
            Text("사귀면 주간 시간표에 데이트 1칸이 고정돼요.", color = PooangColors.TextBody, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                PooangChip("데이트마다 행복 +25", ChipTone.Up)
                PooangChip("돈 −20,000원", ChipTone.Down)
            }
            Text("잔액 부족 등으로 데이트를 누적 ${LoveEngine.MISSED_LIMIT}회 놓치면 주말에 헤어져요.",
                color = PooangColors.Warn, fontSize = 11.5.sp, modifier = Modifier.padding(top = 6.dp))
        }
        val canAccept = LoveEngine.canAccept(game, schedule)
        PooangButton("사귀기", onClick = { onChoose(confession.key, true) }, enabled = canAccept,
            modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_heart)
        if (!canAccept) {
            Text("데이트를 배정할 자유 시간이 없어요.", color = PooangColors.Warn, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        PooangButton("지금은 다른 일에 집중할게요", onClick = { onChoose(confession.key, false) },
            modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost)
    } else {
        PooangButton("계속하기", onClick = { onContinue(confession.key) }, modifier = Modifier.fillMaxWidth(),
            style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_play)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelationshipCard(love: LoveRelationship) {
    PooangCard(title = "${love.partner}와 연애 중", titleIcon = R.drawable.pooang_ic_heart, titleIconTint = LovePink) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PooangImage(PooangMood.LOVE, size = 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("애정도", color = Color(0xFFA8395C), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    ProgressTrack(love.affection / 100f, Modifier.weight(1f), fill = PinkFill)
                    Text("${love.affection}", color = Color(0xFFA8395C), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("데이트 취소", color = PooangColors.TextSub, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(end = 4.dp))
                    repeat(LoveEngine.MISSED_LIMIT) { i ->
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (i < love.missedDates) PooangColors.Warn else PooangColors.Track)
                        )
                    }
                    Text("${love.missedDates}/${LoveEngine.MISSED_LIMIT}", color = PooangColors.TextSub, fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            PooangChip("함께한 ${love.weeksTogether}주", ChipTone.Up)
            PooangChip(
                if (love.dateIndex >= 0) "고정 데이트 · ${GameEngine.dayNames[love.dateIndex / 4]} ${DayLog.blockNames[love.dateIndex % 4]}"
                else "이번 주 데이트 칸 없음",
                if (love.dateIndex >= 0) ChipTone.Even else ChipTone.Down
            )
        }
        Text("데이트 취소 횟수는 성공한 주가 있어도 쌓여요. 애정도는 선택 이벤트에 따라 바뀌어요.",
            color = Color(0xFF7A8290), fontSize = 11.5.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

/** 연애 전: 고백 조건까지 얼마나 남았는지 */
@Composable
private fun NotInLoveCard(game: GameState) {
    val yearOk = game.semester >= LoveEngine.FIRST_SEMESTER
    PooangCard(title = "아직 연애 중이 아니에요", titleIcon = R.drawable.pooang_ic_heart, titleIconTint = PooangColors.TextFaint) {
        Text("2학년부터, 학기가 시작할 때 매력 ${LoveEngine.REQUIRED_CHARM}·인맥 ${LoveEngine.REQUIRED_NETWORK} 이상이면 고백 이벤트가 생겨요.",
            color = Color(0xFF7A8290), fontSize = 12.sp, lineHeight = 18.sp)
        ConditionRow("학년", if (yearOk) "2학년 이상 ✓" else "2학년부터", if (yearOk) 1f else 0f)
        ConditionRow("매력", "${game.charm}/${LoveEngine.REQUIRED_CHARM}", game.charm / LoveEngine.REQUIRED_CHARM.toFloat())
        ConditionRow("인맥", "${game.network}/${LoveEngine.REQUIRED_NETWORK}", game.network / LoveEngine.REQUIRED_NETWORK.toFloat())
    }
}

@Composable
private fun ConditionRow(label: String, value: String, fraction: Float) {
    val done = fraction >= 1f
    Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = PooangColors.TextMid, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.size(width = 32.dp, height = 18.dp))
        ProgressTrack(
            fraction, Modifier.weight(1f),
            fill = if (done) Brush.horizontalGradient(listOf(Color(0xFF8FD6A0), Color(0xFF4BB36A))) else PinkFill
        )
        Text(value, color = if (done) PooangColors.UpText else PooangColors.TextSub, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Preview(name = "연애 - 고백", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun LoveConfessionPreview() {
    PooangTheme {
        LoveScreen(GameState(semester = 3, pendingConfession = LoveConfession(3, "민지")), emptyList(), { _, _ -> }, {}, {})
    }
}
