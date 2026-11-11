package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.ChipTone
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangChip
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.components.hardShadow
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme

/**
 * 딜레마(캠퍼스 사건) 화면. 프로토타입의 딜레마 모달(`.dl-*`)을 전체 화면으로 옮겼다.
 * 고르기 단계: 놀란 푸앙이 + 사건 설명 + 선택지 카드(효과 칩)
 * 결과 단계: 결과에 맞는 표정 + 결과 글 + 실제 변화 칩 + 이어서 진행
 */
@Composable
fun EventScreen(
    game: GameState,
    subjects: List<Subject>,
    onChoose: (String, Int) -> Unit,
    onContinue: (String) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val run = game.pendingEvent ?: return
    val event = EventEngine.eventForId(run.eventId) ?: return
    val choosing = run.phase == EventPhase.CHOICE
    val option = event.options.getOrNull(run.selectedIndex)
    val resultTone = if (choosing) ChipTone.Even else EventTone.overall(run.changes)
    val mood = when {
        choosing -> PooangMood.SHOCK
        resultTone == ChipTone.Up -> PooangMood.HAPPY
        resultTone == ChipTone.Down -> PooangMood.SAD
        else -> PooangMood.OK
    }

    Column(
        modifier
            .fillMaxSize()
            .background(PooangColors.Panel)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        EventHero(mood, event.activity)

        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (event.weeklyEvent) "주간 행사 · ${event.category}"
                else "${GameEngine.dayNames[run.day.coerceIn(0, 6)]} 진행 전 · ${event.category}",
                color = PooangColors.Warn,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp
            )
            Text(event.title, color = PooangColors.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "${semesterLabel(game.semester)} · ${game.week}주차",
                color = PooangColors.TextFaint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            if (choosing) {
                PooangCard {
                    Text(
                        event.description.replace("{name}", run.contextName),
                        color = Color(0xFF3C434F),
                        fontSize = 14.sp,
                        lineHeight = 23.sp
                    )
                }
                Text("어떻게 할까요?", color = PooangColors.TextMid, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                event.options.forEachIndexed { index, opt ->
                    OptionCard(opt, affordable = EventEngine.canChoose(game, opt)) { onChoose(run.key, index) }
                }
            } else if (option != null) {
                ResultBox(option.label, option.result.replace("{name}", run.contextName), resultTone)
                if (run.changes.isNotEmpty()) {
                    Text("실제 변화", color = PooangColors.TextMid, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Chips(run.changes)
                }
                PooangButton(
                    if (event.weeklyEvent) "다음 주로" else "이어서 하루 진행",
                    onClick = { onContinue(run.key) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    style = PooangButtonStyle.Go,
                    icon = R.drawable.pooang_ic_play
                )
            }

            PooangButton("홈으로", onClick = onHome, modifier = Modifier.fillMaxWidth(),
                style = PooangButtonStyle.Ghost, small = true)
        }
    }
}

/** 효과 글자의 색(좋음/나쁨) 판정. "체력 +5"는 좋음, "돈 -10000원"은 나쁨. */
object EventTone {
    fun of(text: String): ChipTone = when {
        "연애 종료" in text || "▼" in text -> ChipTone.Down
        Regex("""\s-\d|\s−\d""").containsMatchIn(text) -> ChipTone.Down
        "▲" in text || Regex("""\+\d|×""").containsMatchIn(text) -> ChipTone.Up
        else -> ChipTone.Even
    }

    /** 변화 목록 전체가 좋았는지 나빴는지 (좋은 칩 수 - 나쁜 칩 수). */
    fun overall(changes: List<String>): ChipTone {
        val score = changes.sumOf { when (of(it)) { ChipTone.Up -> 1; ChipTone.Down -> -1; else -> 0 }.toInt() }
        return ChipTone.of(score)
    }
}

@Composable
private fun EventHero(mood: PooangMood, activity: ActivityType?) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF0DA), Color(0xFFFFE0B8))))
            .statusBarsPadding()
            .height(150.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        PooangImage(mood, size = 124.dp, bob = !LocalInspectionMode.current, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            Modifier
                .offset(x = 62.dp, y = (-14).dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(3.dp)
                .clip(CircleShape)
                .background(if (activity != null) ActivityStyle.color(activity) else PooangColors.Warn),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(if (activity != null) ActivityStyle.icon(activity) else R.drawable.pooang_ic_flag),
                null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/** 선택지 카드 (`.dl-opt`): 이름 + 효과 칩. 돈이 모자라면 흐리게. */
@Composable
private fun OptionCard(option: EventOption, affordable: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .alpha(if (affordable) 1f else 0.45f)
            .padding(bottom = 3.dp)
            .hardShadow(PooangColors.Line, 3.dp, 14.dp)
            .clip(shape)
            .background(PooangColors.Card)
            .border(2.5.dp, PooangColors.Line, shape)
            .clickable(enabled = affordable, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(option.label, color = Color(0xFF4A2E14), fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
        Chips(EventEngine.effectDescriptions(option))
        if (!affordable) {
            Text("잔액 부족 · 빚 한도 -50,000원을 넘을 수 없어요.", color = PooangColors.Warn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(texts: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        texts.forEach { PooangChip(it, EventTone.of(it), fontSize = 11f) }
    }
}

/** 결과 상자 (`.dl-mood`): 좋으면 초록, 나쁘면 빨강. */
@Composable
private fun ResultBox(choice: String, result: String, tone: ChipTone) {
    val (bg, border) = when (tone) {
        ChipTone.Up -> Color(0xFFEAF7E0) to Color(0xFFB8E0A0)
        ChipTone.Down -> Color(0xFFFDECEA) to Color(0xFFF0B8B2)
        ChipTone.Even -> PooangColors.Card to PooangColors.Line
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("선택 · $choice", color = PooangColors.TextSub, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
        Text(result, color = PooangColors.TextBody, fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(name = "딜레마 - 고르기", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun EventChoicePreview() {
    val event = EventCatalog.events.first()
    PooangTheme {
        EventScreen(
            game = GameState(pendingEvent = CampusEventRun(event.id, 1, 1, 0)),
            subjects = emptyList(), onChoose = { _, _ -> }, onContinue = {}, onHome = {}
        )
    }
}
