package com.example.mobile_project

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.BubbleTail
import com.example.mobile_project.ui.components.ChipTone
import com.example.mobile_project.ui.components.CircleIconButton
import com.example.mobile_project.ui.components.NoticeDot
import com.example.mobile_project.ui.components.PooangArt
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangChip
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.components.PooangSheet
import com.example.mobile_project.ui.components.ProgressTrack
import com.example.mobile_project.ui.components.SheetOption
import com.example.mobile_project.ui.components.SpeechBubble
import com.example.mobile_project.ui.components.StatTile
import com.example.mobile_project.ui.components.SummaryTile
import com.example.mobile_project.ui.components.hardShadow
import com.example.mobile_project.ui.components.insetBottomShade
import com.example.mobile_project.ui.components.pulse
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 홈 화면에서 다른 화면으로 가는 동작들. MainActivity 가 채워 준다. */
data class HomeActions(
    val onSchedule: () -> Unit = {},
    val onProgress: () -> Unit = {},
    val onGrades: () -> Unit = {},
    val onLife: () -> Unit = {},
    val onTeam: () -> Unit = {},
    val onLove: () -> Unit = {},
    val onRoutines: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onAccount: () -> Unit = {},
    val onTitle: () -> Unit = {},
    val onToggleMiniGames: () -> Unit = {}
)

private enum class HomeSheet { None, Menu, Study }

/**
 * 메인(홈) 화면. 프로토타입 `#sc-main` + `#hud` + `#footer`.
 * 맨 위 HUD, 하늘·캠퍼스 무대(양옆 버튼 8개 + 가운데 푸앙이), 미션 배너, 요약 칸, 공부 현황, 아래 내비게이션.
 */
@Composable
fun HomeScreen(
    game: GameState,
    schedule: List<TimeSlot>,
    saveStatus: String,
    actions: HomeActions,
    modifier: Modifier = Modifier
) {
    var sheet by remember { mutableStateOf(HomeSheet.None) }
    val canPlan = HomeModel.canPlan(game)

    Box(modifier.fillMaxSize().background(PooangColors.Panel)) {
        Column(Modifier.fillMaxSize()) {
            PooangHud(game = game, onMenu = { sheet = HomeSheet.Menu })

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                HomeStage(
                    game = game,
                    schedule = schedule,
                    canPlan = canPlan,
                    actions = actions,
                    onStudy = { sheet = HomeSheet.Study }
                )
                Column(
                    Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MissionBanner(
                        mission = HomeModel.mission(game, schedule),
                        onClick = if (canPlan) actions.onSchedule else actions.onProgress
                    )
                    EffectsRow(game)
                    QuickRow(game)
                    PooangButton(
                        text = HomeModel.progressLabel(game),
                        onClick = actions.onProgress,
                        modifier = Modifier.fillMaxWidth(),
                        style = if (canPlan) PooangButtonStyle.Ghost else PooangButtonStyle.Go,
                        icon = R.drawable.pooang_ic_play
                    )
                    StudyCard(game, onGrades = actions.onGrades)
                }
            }

            HomeBottomNav(
                game = game,
                schedule = schedule,
                canPlan = canPlan,
                onStudy = { sheet = HomeSheet.Study },
                onPlan = if (canPlan) actions.onSchedule else actions.onProgress,
                onGrades = actions.onGrades,
                onMenu = { sheet = HomeSheet.Menu }
            )
        }

        PooangSheet(visible = sheet == HomeSheet.Study, onDismiss = { sheet = HomeSheet.None }, title = "공부 현황") {
            StudyList(game)
        }
        PooangSheet(visible = sheet == HomeSheet.Menu, onDismiss = { sheet = HomeSheet.None }, title = "메뉴") {
            MenuOptions(game, saveStatus, actions, close = { sheet = HomeSheet.None })
        }
    }
}

// ---------------------------------------------------------------- HUD

/** 맨 위 상태 표시줄 (`#hud`). 프로필, 학기·주차, 기분, 시험 배지, 메뉴, 스탯 6칸. */
@Composable
fun PooangHud(game: GameState, onMenu: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(PooangColors.HudTop, PooangColors.HudBottom)))
            .drawBehind {
                val h = 3.dp.toPx()
                drawRect(PooangColors.Accent, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
            }
            .statusBarsPadding()
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                Modifier
                    .padding(bottom = 2.dp)
                    .hardShadow(PooangColors.Accent, 2.dp, 12.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.5.dp, PooangColors.Accent, RoundedCornerShape(12.dp))
            ) {
                Image(
                    painterResource(PooangArt.Face),
                    contentDescription = "푸앙이",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${semesterLabel(game.semester)} ${game.week}주차",
                    color = PooangColors.Text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 16.sp
                )
                Text(
                    HomeModel.hudMoodLine(game),
                    color = PooangColors.TextSub,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HomeModel.hudBadge(game)?.let { badge ->
                Text(
                    badge,
                    color = Color(0xFF1B1405),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .then(if (LocalInspectionMode.current) Modifier else Modifier.pulse())
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFFFD079))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            if (onMenu != null) {
                CircleIconButton(onClick = onMenu, contentDescription = "메뉴", icon = R.drawable.pooang_ic_menu)
            }
        }
        Spacer(Modifier.height(9.dp))
        HudStats(game)
    }
}

@Composable
private fun HudStats(game: GameState) {
    val hpRate = if (game.maxHp <= 0) 0f else game.hp.toFloat() / game.maxHp
    val hpColor = when {
        hpRate < 0.25f -> PooangColors.Danger
        hpRate < 0.5f -> PooangColors.HpMid
        else -> PooangColors.Hp
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        val m = Modifier.weight(1f)
        StatTile(R.drawable.pooang_ic_hp, "체력", "${game.hp}/${game.maxHp}", m, hpRate, hpColor)
        StatTile(R.drawable.pooang_ic_hap, "행복도", "${game.happiness}", m, game.happiness / 100f,
            if (game.happiness < 25) PooangColors.Danger else PooangColors.Happy)
        StatTile(R.drawable.pooang_ic_spec, "스펙", "${game.spec}", m, game.spec / 100f, PooangColors.Spec)
        StatTile(R.drawable.pooang_ic_net, "인맥", "${game.network}", m, game.network / 100f, PooangColors.Network)
        StatTile(R.drawable.pooang_ic_charm, "매력", "${game.charm}", m, game.charm / 100f, PooangColors.Charm)
        StatTile(R.drawable.pooang_ic_money, "돈", manWon(game.money), m,
            valueColor = if (game.money < 0) PooangColors.Danger else PooangColors.MoneyFlat)
    }
}

// ---------------------------------------------------------------- 무대

@Composable
private fun HomeStage(
    game: GameState,
    schedule: List<TimeSlot>,
    canPlan: Boolean,
    actions: HomeActions,
    onStudy: () -> Unit
) {
    val animate = !LocalInspectionMode.current
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(
                Brush.verticalGradient(
                    0f to PooangColors.SkyTop,
                    0.58f to PooangColors.SkyMid,
                    1f to PooangColors.Panel
                )
            )
    ) {
        val stageWidth = maxWidth
        if (animate) {
            Cloud(stageWidth, top = 40.dp, width = 70.dp, durationMs = 18_000, startFraction = 0f)
            Cloud(stageWidth, top = 92.dp, width = 54.dp, durationMs = 24_000, startFraction = 0.4f)
            Cloud(stageWidth, top = 150.dp, width = 80.dp, durationMs = 30_000, startFraction = 0.7f, alpha = 0.7f)
        }

        // 캠퍼스 실루엣 + 잔디
        Image(
            painterResource(R.drawable.pooang_campus_main),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .fillMaxWidth()
                .height(84.dp)
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(68.dp)
                .background(Brush.verticalGradient(listOf(PooangColors.GrassTop, PooangColors.GrassBottom)))
                .drawBehind {
                    drawRect(PooangColors.GrassLine, size = Size(size.width, 3.dp.toPx()))
                }
        )

        // 양옆 버튼
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            SideButton(R.drawable.pooang_ic_cal, Color(0xFF3B82C4), "시간표",
                notice = HomeModel.scheduleNeedsAttention(game, schedule), off = !canPlan, onClick = actions.onSchedule)
            SideButton(R.drawable.pooang_ic_study, Color(0xFF5A9E4B), "공부 현황", onClick = onStudy)
            SideButton(R.drawable.pooang_ic_exam, Color(0xFF8A6BB8), "성적", onClick = actions.onGrades)
            SideButton(R.drawable.pooang_ic_lotto, Color(0xFFD98C2B), "복권",
                notice = LifeEngine.canBuyLottery(game), onClick = actions.onLife)
        }
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            SideButton(R.drawable.pooang_ic_team, Color(0xFFC0504D), "조별과제",
                notice = game.teamProject != null, off = game.teamProject == null, onClick = actions.onTeam)
            SideButton(R.drawable.pooang_ic_heart, Color(0xFFD4537E), "연애",
                notice = (game.love?.missedDates ?: 0) > 0, off = game.love == null, onClick = actions.onLove)
            SideButton(R.drawable.pooang_ic_max, Color(0xFFE0A33E), "루틴", onClick = actions.onRoutines)
            SideButton(R.drawable.pooang_ic_save, Color(0xFF4A5A72), "저장", onClick = actions.onSave)
        }

        // 가운데 말풍선 + 푸앙이
        StageCenter(game, maxBubbleWidth = minOf(210.dp, stageWidth - 150.dp), animate = animate)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.StageCenter(game: GameState, maxBubbleWidth: Dp, animate: Boolean) {
    val scope = rememberCoroutineScope()
    var pokeText by remember { mutableStateOf<String?>(null) }
    var pokeJob by remember { mutableStateOf<Job?>(null) }
    val squashX = remember { Animatable(1f) }
    val squashY = remember { Animatable(1f) }

    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 26.dp)
            .width(210.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpeechBubble(
            text = pokeText ?: HomeModel.bubble(game),
            tail = BubbleTail.Bottom,
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .padding(bottom = 8.dp)
        )
        Box(
            Modifier
                .graphicsLayer {
                    scaleX = squashX.value
                    scaleY = squashY.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
                .clickable(remember { MutableInteractionSource() }, indication = null) {
                    pokeText = HomeModel.pokeLines.random()
                    scope.launch {
                        // 콕 찌르면 납작해졌다가 튀어 오른다 (`@keyframes poke`)
                        launch { squashX.animateTo(1.14f, tween(150)); squashX.animateTo(0.95f, tween(160)); squashX.animateTo(1f, tween(140)) }
                        squashY.animateTo(0.86f, tween(150)); squashY.animateTo(1.06f, tween(160)); squashY.animateTo(1f, tween(140))
                    }
                    pokeJob?.cancel()
                    pokeJob = scope.launch { delay(2200); pokeText = null }
                }
        ) {
            PooangImage(PooangArt.Main, "푸앙이 (눌러 보세요)", size = 132.dp, bob = animate)
        }
    }
}

/** 하늘을 천천히 가로지르는 구름 (`.cloud`). */
@Composable
private fun Cloud(stageWidth: Dp, top: Dp, width: Dp, durationMs: Int, startFraction: Float, alpha: Float = 0.9f) {
    val transition = rememberInfiniteTransition(label = "cloud")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = LinearEasing),
            RepeatMode.Restart,
            initialStartOffset = StartOffset((durationMs * startFraction).toInt())
        ),
        label = "cloudX"
    )
    val height = width * 0.29f
    Box(
        Modifier
            .offset(x = -width - 10.dp + (stageWidth + width + 20.dp) * t, y = top)
            .alpha(alpha)
            .size(width, height + width * 0.2f)
    ) {
        // 납작한 몸통 + 위로 볼록한 두 덩어리
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(height).clip(RoundedCornerShape(999.dp)).background(Color.White))
        Box(Modifier.offset(x = width * 0.17f).size(width * 0.43f).clip(CircleShape).background(Color.White))
        Box(Modifier.offset(x = width * 0.51f, y = width * 0.08f).size(width * 0.31f).clip(CircleShape).background(Color.White))
    }
}

/** 무대 양옆 버튼 (`.sbtn`). [off] 면 흑백으로 흐리게, [notice] 면 주황 알림 점. */
@Composable
private fun SideButton(
    @DrawableRes icon: Int,
    tint: Color,
    label: String,
    notice: Boolean = false,
    off: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shift = if (pressed) 3.dp else 0.dp
    Box(Modifier.width(60.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(interaction, indication = null, onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                Modifier
                    .padding(bottom = 3.dp)
                    .offset(y = shift)
                    .hardShadow(PooangColors.Accent, 3.dp - shift, 15.dp)
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(PooangColors.Card)
                    .border(2.5.dp, PooangColors.Accent, RoundedCornerShape(15.dp))
                    .alpha(if (off) 0.5f else 1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = if (off) Color(0xFF9A9A9A) else tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                label,
                maxLines = 1,
                style = TextStyle(
                    color = PooangColors.TextMid.copy(alpha = if (off) 0.6f else 1f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    // 하늘 위에서도 읽히도록 흰 테두리 대신 흰 그림자
                    shadow = Shadow(Color.White, Offset.Zero, 6f)
                )
            )
        }
        if (notice) {
            NoticeDot(Modifier.align(Alignment.TopEnd).offset(x = (-3).dp, y = (-3).dp))
        }
    }
}

// ---------------------------------------------------------------- 무대 아래

@Composable
private fun MissionBanner(mission: HomeModel.Mission, onClick: () -> Unit) {
    val border = if (mission.warn) Color(0xFFE8A09D) else PooangColors.Accent
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp)
            .hardShadow(border, 3.dp, 18.dp)
            .clip(shape)
            .background(PooangColors.Card)
            .border(2.5.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (mission.warn) Color(0xFFFDE5E3) else Color(0xFFFFF0C2))
                .border(2.dp, if (mission.warn) Color(0xFFE8A09D) else Color(0xFFF2C14E), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(if (mission.warn) R.drawable.pooang_ic_warn else R.drawable.pooang_ic_flag),
                null,
                tint = if (mission.warn) PooangColors.Danger else PooangColors.HpMid,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                mission.tag,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (mission.warn) PooangColors.Danger else Color(0xFF8B5A2B))
                    .padding(horizontal = 9.dp, vertical = 2.dp)
            )
            Text(
                mission.text,
                color = PooangColors.Text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        mission.progress?.let {
            Text(it, color = PooangColors.TextSub, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** 지금 걸려 있는 효과 (`.moods-row`): 이벤트 버프, 감기, 지각. 없으면 아무것도 그리지 않는다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectsRow(game: GameState) {
    val chips = buildList {
        if (LifeEngine.isSick(game)) add("감기몸살 · 공부 효율 60%" to ChipTone.Down)
        else if (game.life.sickWeek == LifeEngine.absoluteWeek(game) + 1) add("다음 주 감기몸살 예정" to ChipTone.Down)
        EventEngine.visibleBuffs(game).forEach { buff ->
            val planned = buff.startDay > EventEngine.absoluteDay(game)
            add((if (planned) "예정 · " + buff.label else buff.label.replace("다음 주", "이번 주")) to ChipTone.Up)
        }
        game.life.lateCounts.filterValues { it > 0 }.forEach { (id, count) ->
            add("${subjectForId(id)?.name ?: id} 지각 ${count}회 · -${count * LifeEngine.LATE_PENALTY}점" to ChipTone.Down)
        }
    }
    if (chips.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            "효과",
            color = PooangColors.TextSub,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        chips.forEach { (text, tone) -> PooangChip(text, tone, fontSize = 10.5f) }
    }
}

@Composable
private fun QuickRow(game: GameState) {
    val left = HomeModel.examWeeksLeft(game.week)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryTile(
            HomeModel.examName(game.week),
            if (left <= 0) "이번 주" else "D-${left}주",
            Modifier.weight(1f),
            sub = "${game.week}/16주차"
        )
        SummaryTile("예상 평균", "${HomeModel.expectedAverage(game)}점", Modifier.weight(1f), sub = "지금 공부량 기준")
        SummaryTile("생활비", manWon(HomeModel.weeklyCost(game.semester)), Modifier.weight(1f), sub = "매주 자동 지출")
    }
}

@Composable
private fun StudyCard(game: GameState, onGrades: () -> Unit) {
    PooangCard(
        title = "공부 현황",
        titleIcon = R.drawable.pooang_ic_study,
        titleIconTint = Color(0xFF5A9E4B),
        trailing = {
            PooangButton("성적 기록", onClick = onGrades, style = PooangButtonStyle.Ghost, small = true)
        }
    ) {
        StudyList(game)
    }
}

/** 과목별 예상 점수 막대. 홈 카드와 "공부 현황" 시트가 함께 쓴다. */
@Composable
private fun StudyList(game: GameState) {
    val subjects = game.study.keys.mapNotNull { subjectForId(it) }
    if (subjects.isEmpty()) {
        Text("수강신청을 마치면 과목별 공부 현황이 보여요.", color = PooangColors.TextSub, fontSize = 12.sp)
        return
    }
    Text(
        "${HomeModel.examName(game.week)} 대비 누적치입니다.",
        color = Color(0xFF7A8290),
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
    subjects.forEach { subject ->
        val score = ExamEngine.expectedScore(game.study[subject.id] ?: 0, game.semester)
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(subject.name, color = PooangColors.TextBody, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("예상 ${score}점", color = Color(0xFF8A9099), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        ProgressTrack(
            fraction = score / 100f,
            fill = Brush.horizontalGradient(listOf(Color(0xFF5FA3DC), Color(0xFF3B82C4))),
            track = Color(0xFFEFEBE0),
            height = 8.dp,
            borderColor = null,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
    if (game.completedSemesters.isNotEmpty()) {
        Text(
            "누적 평점 " + String.format(java.util.Locale.US, "%.2f", game.cumulativeGpa) + " / 4.50",
            color = PooangColors.TextSub,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun MenuOptions(game: GameState, saveStatus: String, actions: HomeActions, close: () -> Unit) {
    SheetOption("진행 저장", onClick = { actions.onSave(); close() },
        description = saveStatus.ifBlank { "지금까지의 진행을 저장합니다" },
        icon = R.drawable.pooang_ic_save, iconTint = Color(0xFF4A5A72))
    SheetOption("루틴 관리", onClick = { close(); actions.onRoutines() },
        description = "저장해 둔 시간표를 불러오거나 관리합니다",
        icon = R.drawable.pooang_ic_max, iconTint = Color(0xFFE0A33E))
    SheetOption("성적 기록", onClick = { close(); actions.onGrades() },
        description = "학기별 평점과 시험 기록",
        icon = R.drawable.pooang_ic_exam, iconTint = Color(0xFF8A6BB8))
    SheetOption("생활 · 건강과 주간 복권", onClick = { close(); actions.onLife() },
        description = "건강 상태를 보고 복권을 삽니다",
        icon = R.drawable.pooang_ic_lotto, iconTint = Color(0xFFD98C2B))
    SheetOption(
        if (game.skipMiniGames) "미니게임: 꺼짐" else "미니게임: 켜짐",
        onClick = actions.onToggleMiniGames,
        description = if (game.skipMiniGames) "늦잠은 성공 확률 50%로 자동 판정해요." else "누르면 미니게임을 건너뜁니다",
        icon = R.drawable.pooang_ic_play, iconTint = Color(0xFF2E9183),
        enabled = game.pendingMiniGame == null && game.life.pendingMorning == null
    )
    SheetOption("계정 · 클라우드 저장", onClick = { close(); actions.onAccount() },
        description = "다른 기기에서 이어서 할 수 있게 저장합니다",
        icon = R.drawable.pooang_ic_globe, iconTint = Color(0xFF3B82C4))
    SheetOption("타이틀로", onClick = { close(); actions.onTitle() },
        description = "진행은 자동 저장되며 이어하기로 돌아올 수 있습니다",
        icon = R.drawable.pooang_ic_home, iconTint = PooangColors.Warn)
}

// ---------------------------------------------------------------- 아래 내비게이션

@Composable
private fun HomeBottomNav(
    game: GameState,
    schedule: List<TimeSlot>,
    canPlan: Boolean,
    onStudy: () -> Unit,
    onPlan: () -> Unit,
    onGrades: () -> Unit,
    onMenu: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    // clip 을 쓰면 위로 튀어나온 큰 버튼이 잘리므로, 모양은 배경에만 준다.
    // 바깥 상자(연두 선 색) 안에 3dp 아래로 내린 그라데이션 상자를 겹쳐 위쪽 테두리를 만든다.
    Box(
        Modifier
            .fillMaxWidth()
            .background(PooangColors.GrassLine, shape)
            .padding(top = 3.dp)
            .background(Brush.verticalGradient(listOf(PooangColors.GrassTop, PooangColors.GrassBottom)), shape)
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 9.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            NavItem(R.drawable.pooang_ic_home, "홈", selected = true, onClick = {})
            NavItem(R.drawable.pooang_ic_study, "공부", onClick = onStudy)
            BigNavItem(
                icon = if (canPlan) R.drawable.pooang_ic_cal else R.drawable.pooang_ic_play,
                label = if (canPlan) "시간표 세우기" else "이어서 진행",
                pending = if (canPlan) HomeModel.scheduleNeedsAttention(game, schedule) else true,
                onClick = onPlan
            )
            NavItem(R.drawable.pooang_ic_exam, "기록", onClick = onGrades)
            NavItem(R.drawable.pooang_ic_menu, "메뉴", onClick = onMenu)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    @DrawableRes icon: Int,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .weight(1f)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            Modifier
                .then(if (selected) Modifier.padding(bottom = 2.dp).hardShadow(PooangColors.GrassLine, 2.dp, 12.dp) else Modifier)
                .size(42.dp, 34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) PooangColors.Card else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = if (selected) Color(0xFFE86A92) else Color(0xFF8A6A48),
                modifier = Modifier.size(23.dp)
            )
        }
        Text(label, color = PooangColors.TextMid, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

/** 가운데 볼록 튀어나온 큰 분홍 버튼 (`.nv.big`). */
@Composable
private fun androidx.compose.foundation.layout.RowScope.BigNavItem(
    @DrawableRes icon: Int,
    label: String,
    pending: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .weight(1.3f)
            .offset(y = (-4).dp)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(Modifier.size(70.dp, 40.dp)) {
            // 원은 위로 26dp 튀어나온다. 레이아웃 높이는 40dp 만 차지한다.
            // requiredSize: 부모(40dp)보다 커도 줄어들지 않고 가운데 기준으로 넘쳐 그려진다.
            // 원 중심을 위로 13dp 올려 원 윗부분이 바 위로 약 26dp 솟게 한다.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = (-13).dp)
                    .hardShadow(PooangColors.PinkShadow, 4.dp, 33.dp)
                    .requiredSize(66.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(PooangColors.PinkTop, PooangColors.PinkBottom)))
                    .border(4.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            if (pending) {
                NoticeDot(Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = (-26).dp), size = 15.dp)
            }
        }
        Text(label, color = PooangColors.PinkText, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

// ---------------------------------------------------------------- 미리보기

private val previewGame = GameState(
    semester = 3, week = 5, hp = 62, happiness = 74, money = 182_000, network = 18, charm = 33, spec = 12,
    study = mapOf("ds" to 120, "dm" to 80, "ca" to 40)
)

@Preview(name = "홈", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun HomeScreenPreview() {
    PooangTheme {
        HomeScreen(previewGame, emptyList(), "방금 저장됨", HomeActions())
    }
}

@Preview(name = "HUD - 중간고사 주간", showBackground = true, widthDp = 390)
@Composable
private fun HudPreview() {
    PooangTheme { PooangHud(previewGame.copy(week = 8, money = -20_000), onMenu = {}) }
}
