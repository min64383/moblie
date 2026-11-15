package com.example.mobile_project

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.PanelScaffold
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.ProgressTrack
import com.example.mobile_project.ui.components.hardShadow
import com.example.mobile_project.ui.components.insetBottomShade
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme

private val DayLabels = listOf("월", "화", "수", "목", "금", "토", "일")
private val BlockLabels = listOf("오전", "오후", "저녁", "밤")
private val LabelWidth = 26.dp
private val CellGap = 3.dp
private val CellHeight = 44.dp
private val HeaderHeight = 18.dp

/**
 * 주간 시간표 패널 (`#p-sched`).
 * 위: 7×4 시간표 (칸을 누르거나 손가락으로 끌어서 여러 칸을 한 번에 칠한다)
 * 가운데: 수면 안내, 할 일 팔레트와 효과 설명
 * 아래 고정: 채운 칸 수 막대 + "시간 보내기" 버튼
 *
 * 칸 하나가 칠해질 때마다 [onPaint] 가 불리고, 실제로 칠할 수 있는지는 MainActivity 가 판단한다.
 */
@Composable
fun WeeklyScheduleScreen(
    subjects: List<Subject>,
    schedule: List<TimeSlot>,
    game: GameState,
    onPaint: (Int, TimeSlot) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onStartWeek: () -> Unit,
    onRoutines: () -> Unit,
    modifier: Modifier = Modifier
) {
    val choices = paletteChoices(subjects, game)
    var selectedChoice by rememberSaveable { mutableStateOf(0) }
    val choice = choices[selectedChoice.coerceIn(choices.indices)]

    val free = schedule.count { it.activity == ActivityType.EMPTY }
    val filled = HomeModel.SLOT_COUNT - free
    val sleep = schedule.count { it.activity == ActivityType.SLEEP }
    val done = free == 0

    PanelScaffold(
        title = "주간 시간표",
        onClose = onBack,
        modifier = modifier,
        icon = R.drawable.pooang_ic_cal,
        iconTint = Color(0xFF3B82C4),
        caption = if (free > 0) "빈 칸 ${free}개" else "빈 칸 없음",
        footer = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (done) "시간표 완성! 28/28칸" else "시간표 $filled/28칸 · 빈칸 ${free}개",
                    color = PooangColors.TextMid,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                ProgressTrack(
                    filled / 28f,
                    Modifier.weight(1f),
                    fill = if (done) Brush.horizontalGradient(listOf(Color(0xFF8FD6A0), Color(0xFF4BB36A)))
                    else Brush.horizontalGradient(listOf(Color(0xFFFFC37F), PooangColors.OrangeBottom))
                )
            }
            PooangButton(
                text = if (done) "시간 보내기" else "빈칸 ${free}개 남음 · 그래도 시간 보내기",
                onClick = onStartWeek,
                modifier = Modifier.fillMaxWidth(),
                style = if (done) PooangButtonStyle.Go else PooangButtonStyle.Ghost,
                icon = R.drawable.pooang_ic_play
            )
        }
    ) {
        Text(
            "할 일을 고른 뒤 칸을 누르거나 손가락으로 끌어 칠하세요. 점이 찍힌 칸(수업·조모임·데이트)은 바꿀 수 없어요.",
            color = Color(0xFF7A8290),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        PooangCard(contentPadding = 10.dp) {
            ScheduleGrid(schedule, subjects, onPaintCell = { index -> onPaint(index, choice) })
            SleepBar(sleep)
        }

        PooangCard(
            title = "무엇을 할까요?",
            trailing = { PooangButton("비우기", onClick = onClear, style = PooangButtonStyle.Ghost, small = true) }
        ) {
            Palette(choices, subjects, selectedChoice, onSelect = { selectedChoice = it })
            EffectLine(choice, subjects)
        }

        PooangButton(
            "루틴 저장 · 불러오기",
            onClick = onRoutines,
            modifier = Modifier.fillMaxWidth(),
            style = PooangButtonStyle.Ghost,
            small = true,
            icon = R.drawable.pooang_ic_max
        )
        Text(
            "시작하면 한 주가 끝날 때까지 시간표를 바꿀 수 없어요.",
            color = PooangColors.TextFaint,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 팔레트 순서: 과목별 공부 → 알바 → (과외) → 여가·휴식·동아리·운동 → (취업준비) → 수면 → 비우기 */
private fun paletteChoices(subjects: List<Subject>, game: GameState): List<TimeSlot> {
    val activities = buildList {
        add(ActivityType.WORK)
        if (AcademicEngine.canUseActivity(game, ActivityType.TUTOR)) add(ActivityType.TUTOR)
        addAll(listOf(ActivityType.FUN, ActivityType.REST, ActivityType.CLUB, ActivityType.RUN, ActivityType.GYM, ActivityType.PT))
        if (AcademicEngine.canUseActivity(game, ActivityType.CAREER)) add(ActivityType.CAREER)
        add(ActivityType.SLEEP)
        add(ActivityType.EMPTY)
    }
    return subjects.map { TimeSlot(ActivityType.STUDY, it.id) } + activities.map { TimeSlot(it) }
}

/**
 * 7×4 시간표. 칸 위치는 고정 크기(라벨 폭·칸 간격·칸 높이)로 계산해서
 * 손가락이 지나가는 칸을 찾아 칠한다. 같은 칸은 한 번만 칠한다.
 */
@Composable
private fun ScheduleGrid(schedule: List<TimeSlot>, subjects: List<Subject>, onPaintCell: (Int) -> Unit) {
    val paint by rememberUpdatedState(onPaintCell)
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val labelPx = with(density) { LabelWidth.toPx() }
        val gapPx = with(density) { CellGap.toPx() }
        val cellHPx = with(density) { CellHeight.toPx() }
        val headerPx = with(density) { HeaderHeight.toPx() } + gapPx
        val cellWPx = (widthPx - labelPx - gapPx * 7) / 7f

        fun indexAt(p: Offset): Int? {
            val col = ((p.x - labelPx - gapPx) / (cellWPx + gapPx)).toInt()
            val row = ((p.y - headerPx) / (cellHPx + gapPx)).toInt()
            if (p.x < labelPx || p.y < headerPx || col !in 0..6 || row !in 0..3) return null
            return col * 4 + row
        }

        Column(
            Modifier.pointerInput(widthPx) {
                var last: Int? = null
                detectDragGestures(
                    onDragStart = { start -> last = indexAt(start)?.also { paint(it) } },
                    onDragEnd = { last = null },
                    onDragCancel = { last = null },
                    onDrag = { change, _ ->
                        val i = indexAt(change.position)
                        if (i != null && i != last) {
                            last = i
                            paint(i)
                        }
                        change.consume()
                    }
                )
            }.pointerInput(widthPx) {
                detectTapGestures { p -> indexAt(p)?.let { paint(it) } }
            },
            verticalArrangement = Arrangement.spacedBy(CellGap)
        ) {
            Row(Modifier.height(HeaderHeight), horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                Spacer(Modifier.width(LabelWidth))
                DayLabels.forEachIndexed { i, d ->
                    Text(
                        d,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (i) {
                            5 -> Color(0xFF3B82C4)
                            6 -> PooangColors.Warn
                            else -> Color(0xFF8A9099)
                        }
                    )
                }
            }
            BlockLabels.forEachIndexed { block, label ->
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        Modifier.width(LabelWidth),
                        textAlign = TextAlign.Center,
                        fontSize = 8.5.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF8A9099)
                    )
                    repeat(7) { day ->
                        val index = day * 4 + block
                        ScheduleCell(schedule.getOrNull(index) ?: TimeSlot(), subjects, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** 시간표 한 칸 (`.cell`). 칠해질 때 한 번 톡 튄다(`ping`). */
@Composable
private fun ScheduleCell(slot: TimeSlot, subjects: List<Subject>, modifier: Modifier) {
    val empty = slot.activity == ActivityType.EMPTY
    val shape = RoundedCornerShape(9.dp)
    val tag = subjects.find { it.id == slot.subjectId }?.name?.take(2)

    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(slot) {
        if (first) { first = false; return@LaunchedEffect }
        scale.snapTo(0.78f)
        scale.animateTo(1.1f, tween(160))
        scale.animateTo(1f, tween(140))
    }

    Box(
        modifier
            .height(CellHeight)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(shape)
            .then(
                if (empty) Modifier
                    .background(Color(0xFFFBF9F3))
                    .drawBehind {
                        // 점선 테두리 (`border:2px dashed`)
                        val w = 2.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFFD3CCBA),
                            topLeft = Offset(w / 2, w / 2),
                            size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                            cornerRadius = CornerRadius(9.dp.toPx()),
                            style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
                        )
                    }
                else Modifier
                    .background(ActivityStyle.color(slot.activity))
                    .insetBottomShade(3.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painterResource(ActivityStyle.icon(slot.activity)),
                contentDescription = slot.activity.label,
                tint = if (empty) Color(0x4DB0AA9A) else Color.White,
                modifier = Modifier.size(17.dp)
            )
            if (tag != null) {
                Text(tag, color = Color.White.copy(alpha = 0.92f), fontSize = 7.5.sp, lineHeight = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (slot.locked) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.65f))
            )
        }
    }
}

@Composable
private fun SleepBar(sleep: Int) {
    val bad = sleep < HomeModel.MIN_SLEEP
    val color = if (bad) PooangColors.Warn else PooangColors.UpText
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (bad) Color(0xFFFDF0EF) else Color(0xFFEFF7F1))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painterResource(if (bad) R.drawable.pooang_ic_warn else R.drawable.pooang_ic_sleep),
            null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            "수면 $sleep/${HomeModel.MIN_SLEEP}칸",
            color = color,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (bad) "최대 체력 −${(HomeModel.MIN_SLEEP - sleep) * 2} 예정" else "최소 수면 충족",
            color = Color(0xFF7A8290),
            fontSize = 11.sp
        )
    }
}

/** 할 일 팔레트 (`.pal`): 4열 칩. 고른 칩은 파란 테두리로 살짝 떠 있다. */
@Composable
private fun Palette(choices: List<TimeSlot>, subjects: List<Subject>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.chunked(4).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { c, item ->
                    val i = r * 4 + c
                    PaletteChip(item, subjects, i == selected, Modifier.weight(1f)) { onSelect(i) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PaletteChip(item: TimeSlot, subjects: List<Subject>, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val label = subjects.find { it.id == item.subjectId }?.name ?: item.activity.label
    val shape = RoundedCornerShape(12.dp)
    val lift = if (selected) 2.dp else 0.dp
    Column(
        modifier
            .padding(top = 2.dp, bottom = 3.dp)
            .graphicsLayer { translationY = -lift.toPx() }
            .then(if (selected) Modifier.hardShadow(Color(0xFFBCD0EA), 3.dp, 12.dp) else Modifier)
            .clip(shape)
            .background(if (selected) Color(0xFFEDF3FB) else Color.White)
            .border(2.dp, if (selected) Color(0xFF0B4DA2) else PooangColors.Line, shape)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(top = 7.dp, bottom = 6.dp, start = 2.dp, end = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(ActivityStyle.color(item.activity))
                .insetBottomShade(),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(ActivityStyle.icon(item.activity)), null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(
            label,
            color = Color(0xFF5A6270),
            fontSize = 9.5.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun EffectLine(choice: TimeSlot, subjects: List<Subject>) {
    val name = subjects.find { it.id == choice.subjectId }?.let { "${it.name} 공부" } ?: choice.activity.label
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = PooangColors.Text, fontWeight = FontWeight.ExtraBold)) { append(name) }
            append(" — ")
            append(ActivityStyle.effect(choice.activity))
        },
        color = Color(0xFF7A8290),
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFFFAF8F2))
            .padding(horizontal = 9.dp, vertical = 7.dp)
    )
}

@Preview(name = "주간 시간표", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun WeeklySchedulePreview() {
    val subjects = subjectsForSemester(3).take(3)
    val schedule = createWeekSchedule(subjects.map { it.id }).mapIndexed { i, s ->
        if (s.activity == ActivityType.EMPTY && i % 3 == 0) TimeSlot(ActivityType.WORK) else s
    }
    PooangTheme {
        WeeklyScheduleScreen(subjects, schedule, GameState(semester = 3), { _, _ -> }, {}, {}, {}, {})
    }
}
