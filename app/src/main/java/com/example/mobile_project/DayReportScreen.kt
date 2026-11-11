package com.example.mobile_project

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.ChipTone
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangChip
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.components.WoodPanel
import com.example.mobile_project.ui.components.hardShadow
import com.example.mobile_project.ui.components.insetBottomShade
import com.example.mobile_project.ui.components.twinkle
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * 예전 결과 화면에서 쓰던 상태 요약. 지금은 새 화면들이 직접 그리므로 쓰지 않지만,
 * 다른 팀원 코드가 부를 수 있어 남겨 둔다.
 */
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

/**
 * 하루·한 주 결과 화면 (`#sc-report`).
 * - 위: 요일 타임라인, 진행 방식(하루씩 / 한 주 자동) 전환
 * - 하루씩: 날짜판·돈판, 표정 푸앙이와 오늘 4칸, 스탯 막대(오늘 증감), 시간대별 기록, 알림
 * - 한 주 자동: 주간 요약(7일 미니 시간표·주간 증감), 지난 날들의 카드
 * - 아래 고정: 다음 날 / 자동 진행 중 / 다음 주로(시험 보러 가기) + 홈으로
 *
 * @param schedule 이번 주 시간표. 칸 아이콘을 그릴 때만 쓴다(없어도 동작).
 */
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
    weekEntries: List<DayLogEntry> = emptyList(),
    schedule: List<TimeSlot> = emptyList(),
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    LaunchedEffect(game.semester, game.week, game.day, mode) { scroll.scrollTo(0) }
    val autoRunning = mode == ProgressMode.WEEK && WeekLogBook.canAutoAdvance(game)
    val nextDayName = GameEngine.dayNames[game.day.coerceIn(0, 6)]

    Column(modifier.fillMaxSize().background(PooangColors.Panel)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .statusBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DayTimeline(game.day)
            ModeToggle(mode, onModeChange)
            if (mode == ProgressMode.WEEK) {
                WeekView(game, weekEntries, schedule)
            } else {
                DayView(game, schedule)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(PooangColors.LineSoft, size = Size(size.width, 2.dp.toPx())) }
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                game.day >= 7 -> PooangButton(continueLabel, onClick = onNextWeek, modifier = Modifier.fillMaxWidth(),
                    style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_play)
                autoRunning -> Text(
                    "$nextDayName 자동 진행 중…",
                    color = PooangColors.TextSub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (LocalInspectionMode.current) Modifier else Modifier.twinkle(0.45f, 1f, 1100))
                        .padding(vertical = 14.dp)
                )
                else -> PooangButton("$nextDayName 진행", onClick = onNextDay, modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.pooang_ic_play)
            }
            PooangButton("홈으로", onClick = onHome, modifier = Modifier.fillMaxWidth(),
                style = PooangButtonStyle.Ghost, small = true)
        }
    }
}

// ------------------------------------------------------------ 공통 위쪽

/** 월~일 진행 점 (`.tl`). 끝난 날은 하늘색, 방금 끝난 날은 진한 파랑. */
@Composable
private fun DayTimeline(day: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("월", "화", "수", "목", "금", "토", "일").forEachIndexed { i, label ->
            val now = i == day - 1
            val done = i < day - 1
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .then(if (now) Modifier.border(3.dp, Color(0x2E0B4DA2), RoundedCornerShape(4.dp)) else Modifier)
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                now -> Color(0xFF0B4DA2)
                                done -> Color(0xFF9FC0E6)
                                else -> Color(0xFFDED7C6)
                            }
                        )
                )
                Text(
                    label,
                    color = when {
                        now -> Color(0xFF0B4DA2)
                        done -> Color(0xFF73798A)
                        else -> Color(0xFFAEA79A)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** 진행 방식 전환 (`.vt`). 고른 쪽이 나무색으로 눌린다. */
@Composable
private fun ModeToggle(mode: ProgressMode, onModeChange: (ProgressMode) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PooangColors.Track)
            .border(2.dp, PooangColors.StatBorder, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ProgressMode.values().forEach { option ->
            val on = option == mode
            Row(
                Modifier
                    .weight(1f)
                    .then(if (on) Modifier.padding(bottom = 2.dp).hardShadow(PooangColors.WoodShadow, 2.dp, 10.dp) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (on) Brush.verticalGradient(listOf(PooangColors.WoodTop, PooangColors.WoodBottom))
                        else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onModeChange(option) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val color = if (on) PooangColors.WoodText else PooangColors.TextSub
                Icon(
                    painterResource(if (option == ProgressMode.DAY) R.drawable.pooang_ic_cal else R.drawable.pooang_ic_play),
                    null, tint = color, modifier = Modifier.size(16.dp)
                )
                Text(option.label, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ------------------------------------------------------------ 하루 보기

@Composable
private fun DayView(game: GameState, schedule: List<TimeSlot>) {
    val finished = (game.day - 1).coerceIn(0, 6)
    val entries = DayLog.parse(game.log)
    val totals = DayLog.totals(entries)
    val slots = daySlots(schedule, finished)

    // 날짜판 + 돈판
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WoodPanel(Modifier.weight(1f), contentPadding = 9.dp) {
            Text("${semesterLabel(game.semester)} · ${game.week}주차", color = PooangColors.WoodSub, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(GameEngine.dayNames[finished], color = Color(0xFFFFF7E6), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("${game.day}/7일", color = PooangColors.WoodSub, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        WoodPanel(Modifier.width(128.dp), contentPadding = 9.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text("돈", color = PooangColors.WoodSub, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                Text(manWon(game.money), color = Color(0xFFFFE08A), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                totals["돈"]?.let { PooangChip(DayLog.Delta("", it, "원").text.trim(), ChipTone.of(it), fontSize = 10.5f) }
            }
        }
    }

    // 표정 카드 + 스탯 막대
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MoodCard(game, slots, Modifier.weight(0.44f))
        Column(Modifier.weight(0.56f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            StatRow("체력", game.hp, game.maxHp, PooangColors.Hp, totals["체력"])
            StatRow("행복도", game.happiness, 100, PooangColors.Happy, totals["행복도"])
            StatRow("스펙", game.spec, 100, PooangColors.Spec, totals["스펙"])
            StatRow("인맥", game.network, 100, PooangColors.Network, totals["인맥"])
            StatRow("매력", game.charm, 100, PooangColors.Charm, totals["매력"])
        }
    }

    // 시간대별 기록
    LogCard(entries.filterIsInstance<DayLog.Block>(), slots)
    entries.filterIsInstance<DayLog.Note>().forEach { NoteBox(it) }
}

private fun daySlots(schedule: List<TimeSlot>, day: Int): List<TimeSlot?> =
    List(4) { b -> schedule.getOrNull(day * 4 + b) }

@Composable
private fun MoodCard(game: GameState, slots: List<TimeSlot?>, modifier: Modifier) {
    val mood = PooangMood.of(game)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .padding(bottom = 3.dp)
            .hardShadow(Color(0xFF86B566), 3.dp, 16.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFEAF6DF), Color(0xFFD3EBC0))))
            .border(3.dp, Color(0xFF9FCB80), shape)
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            mood.label,
            color = Color(0xFF3E6B2A),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White)
                .border(2.dp, Color(0xFF9FCB80), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 1.dp)
        )
        PooangImage(mood, size = 104.dp, bob = !LocalInspectionMode.current, modifier = Modifier.padding(vertical = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            slots.forEach { slot -> SlotChip(slot, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SlotChip(slot: TimeSlot?, modifier: Modifier) {
    val activity = slot?.activity ?: ActivityType.EMPTY
    Box(
        modifier
            .height(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (activity == ActivityType.EMPTY) Color(0xFFD9CFBE) else ActivityStyle.color(activity)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(ActivityStyle.icon(activity)),
            activity.label,
            tint = Color.White.copy(alpha = if (activity == ActivityType.EMPTY) 0.5f else 1f),
            modifier = Modifier.size(14.dp)
        )
    }
}

/** 나무 스탯 줄 (`.pm-row`): 이름 · 값 · 막대 · 오늘 증감 */
@Composable
private fun StatRow(name: String, value: Int, max: Int, color: Color, delta: Int?) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .hardShadow(PooangColors.WoodShadow, 2.dp, 10.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PooangColors.WoodTop, PooangColors.WoodBottom)))
            .border(2.dp, PooangColors.WoodBorder, shape)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(name, color = Color(0xFFFFE3B5), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
            modifier = Modifier.width(36.dp))
        Text("$value", color = Color(0xFFFFF7E6), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
        Box(
            Modifier
                .weight(1f)
                .height(9.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF4A2A10))
        ) {
            Box(
                Modifier
                    .fillMaxWidth((value.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f))
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
        val d = delta ?: 0
        Text(
            if (d == 0) "−" else (if (d > 0) "+$d" else "−${-d}"),
            color = when {
                d > 0 -> PooangColors.UpText
                d < 0 -> PooangColors.Warn
                else -> Color(0xFFC9A27A)
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        d > 0 -> PooangColors.UpBg
                        d < 0 -> PooangColors.DownBg
                        else -> Color.Transparent
                    }
                )
                .padding(vertical = 1.dp)
        )
    }
}

/** 시간대별 기록 카드 (`.pm-log`). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogCard(blocks: List<DayLog.Block>, slots: List<TimeSlot?>) {
    if (blocks.isEmpty()) return
    PooangCard(contentPadding = 10.dp) {
        blocks.forEachIndexed { i, block ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(PooangColors.LineSoft))
            val activity = slots.getOrNull(block.block)?.activity ?: ActivityType.EMPTY
            Row(
                Modifier.padding(vertical = 7.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    DayLog.blockNames[block.block],
                    color = PooangColors.TextSub,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.width(26.dp).padding(top = 5.dp)
                )
                ActivityIcon(activity, block.note != null)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(block.title, color = PooangColors.TextBody, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    block.note?.let { Text(it, color = PooangColors.Warn, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    if (block.deltas.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            block.deltas.forEach { PooangChip(it.text, ChipTone.of(it.value), fontSize = 10f) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityIcon(activity: ActivityType, cancelled: Boolean) {
    Box(
        Modifier
            .size(24.dp)
            .alpha(if (cancelled) 0.45f else 1f)
            .clip(RoundedCornerShape(7.dp))
            .background(if (activity == ActivityType.EMPTY) Color(0xFFD9CFBE) else ActivityStyle.color(activity))
            .insetBottomShade(),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(ActivityStyle.icon(activity)), null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

/** 알림 상자 (`.incident`). 나쁜 소식은 빨강, 그 밖은 초록. */
@Composable
private fun NoteBox(note: DayLog.Note) {
    val bg = if (note.bad) Color(0xFFFDF0EF) else Color(0xFFE9F5EE)
    val border = if (note.bad) Color(0xFFF0C8C5) else Color(0xFFBDDECB)
    val head = if (note.bad) PooangColors.Warn else PooangColors.UpText
    val body = if (note.bad) Color(0xFF7A3835) else Color(0xFF2A5A3E)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(painterResource(if (note.bad) R.drawable.pooang_ic_warn else R.drawable.pooang_ic_check), null,
                tint = head, modifier = Modifier.size(12.dp))
            Text(note.tag ?: "알림", color = head, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(note.text, color = body, fontSize = 12.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 3.dp))
    }
}

// ------------------------------------------------------------ 한 주 보기

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekView(game: GameState, entries: List<DayLogEntry>, schedule: List<TimeSlot>) {
    val parsed = entries.associate { it.day to DayLog.parse(it.lines) }
    val weekTotals = DayLog.totals(parsed.values.flatten())

    // 주간 요약 카드 (`.wk-sum`)
    PooangCard(borderColor = PooangColors.Accent, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF0DA))
                    .border(2.dp, PooangColors.Line, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { PooangImage(PooangMood.of(game), size = 48.dp) }
            Column {
                Text(semesterLabel(game.semester), color = PooangColors.TextSub, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("${game.week}주차 주간 기록", color = PooangColors.Text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text("${entries.size}/7일 진행", color = PooangColors.TextFaint, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.padding(top = 10.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(7) { day ->
                val done = day in parsed
                Column(Modifier.weight(1f).alpha(if (done) 1f else 0.38f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(listOf("월", "화", "수", "목", "금", "토", "일")[day], color = PooangColors.TextMid,
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                    daySlots(schedule, day).forEach { SlotChip(it, Modifier.fillMaxWidth()) }
                }
            }
        }
        if (weekTotals.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("이번 주 변화", color = PooangColors.TextSub, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.CenterVertically))
                weekTotals.forEach { (name, v) ->
                    PooangChip(DayLog.Delta(name, v, if (name == "돈") "원" else "").text, ChipTone.of(v), fontSize = 11f)
                }
            }
        }
    }

    if (entries.isEmpty()) {
        Text("아직 이번 주에 지난 날이 없어요.", color = PooangColors.TextSub, fontSize = 12.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }

    // 날마다 카드 (`.wd-card`)
    entries.forEach { entry ->
        val dayEntries = parsed[entry.day].orEmpty()
        val slots = daySlots(schedule, entry.day)
        val shape = RoundedCornerShape(18.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp)
                .hardShadow(PooangColors.Line, 3.dp, 18.dp)
                .clip(shape)
                .background(PooangColors.Card)
                .border(2.5.dp, PooangColors.Line, shape)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(PooangColors.WoodTop, PooangColors.WoodBottom)))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(entry.dayName, color = Color(0xFFFFF7E6), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    slots.forEach { SlotChip(it, Modifier.width(22.dp)) }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                dayEntries.forEach { e ->
                    when (e) {
                        is DayLog.Block -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(DayLog.blockNames[e.block], color = PooangColors.TextSub, fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(26.dp))
                            Text(
                                e.title + (e.note?.let { " · 취소" } ?: ""),
                                color = if (e.note != null) PooangColors.Warn else PooangColors.TextBody,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        is DayLog.Note -> Text(
                            (e.tag?.let { "$it · " } ?: "") + e.text,
                            color = if (e.bad) Color(0xFF7A3835) else Color(0xFF2A5A3E),
                            fontSize = 11.5.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (e.bad) Color(0xFFFDECEA) else Color(0xFFEAF7E0))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                val t = DayLog.totals(dayEntries)
                if (t.isNotEmpty()) {
                    FlowRow(Modifier.padding(top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        t.forEach { (name, v) ->
                            PooangChip(DayLog.Delta(name, v, if (name == "돈") "원" else "").text, ChipTone.of(v), fontSize = 10.5f)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ 미리보기

private val previewLog = listOf(
    "늦잠 · 전력질주로 오전 일정에 제시간에 도착했어요.",
    "오전 · 자료구조 수업\n체력 -6 · 행복도 -2 · 공부량 +5",
    "오후 · 자료구조 공부\n체력 -9 · 행복도 -3 · 공부량 +12",
    "저녁 · 알바\n체력 -13 · 행복도 -6 · 돈 +25000원",
    "밤 · 수면\n체력 +17"
)

@Preview(name = "결과 - 하루씩", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun DayReportDayPreview() {
    val subjects = subjectsForSemester(3).take(3)
    PooangTheme {
        DayReportScreen(
            game = GameState(semester = 3, week = 5, day = 2, hp = 58, happiness = 61, log = previewLog),
            subjects = subjects, onNextDay = {}, onNextWeek = {}, onHome = {},
            schedule = createWeekSchedule(subjects.map { it.id })
        )
    }
}
