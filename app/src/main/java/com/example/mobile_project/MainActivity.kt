package com.example.mobile_project

import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModelProvider
import com.example.mobile_project.ui.theme.PooangTheme

class MainActivity : ComponentActivity() {
    private val gameSession: GameSession by lazy {
        ViewModelProvider(
            this,
            GameSession.Factory(applicationContext)
        )[GameSession::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PooangTheme {
                PooangApp(gameSession)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        gameSession.saveNow()
    }
}

@Composable
private fun PooangApp(session: GameSession) {
    var screen by session.screenState
    var selectedIds by session.draftState
    var weekSchedule by session.scheduleState
    var game by session.gameState

    var askNewGame by rememberSaveable { mutableStateOf(false) }

    // 결과 화면 진행 방식 (하루씩 / 한 주 자동)과 이번 주 기록
    var progressModeName by rememberSaveable { mutableStateOf(ProgressMode.DAY.name) }
    val progressMode = ProgressMode.fromName(progressModeName)
    var weekLog by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(session.message) {
        if (session.message.isNotBlank()) {
            Toast.makeText(
                context.applicationContext,
                session.message,
                Toast.LENGTH_SHORT
            ).show()

            session.clearMessage()
        }
    }

    if (!session.ready) {
        Text(
            text = "저장 데이터를 불러오는 중이에요...",
            modifier = Modifier.padding(24.dp)
        )
        return
    }

    val availableSubjects = subjectsForSemester(game.semester)
    val subjects = game.study.keys.mapNotNull { subjectForId(it) }
    val scrollState = rememberScrollState()

    LaunchedEffect(
        screen,
        game.semester,
        game.week,
        game.day,
        game.exam?.index,
        game.exam?.phase,
        game.pendingEvent?.key,
        game.pendingEvent?.phase,
        game.life.pendingMorning?.key,
        game.life.pendingMorning?.phase,
        game.pendingMiniGame?.key,
        game.pendingMiniGame?.phase,
        game.pendingConfession?.key,
        game.pendingConfession?.choice,
        game.teamProject?.key,
        game.teamProject?.announced
    ) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(game.semester, game.needsRegistration) {
        if (game.needsRegistration) {
            val allowed = subjectsForSemester(game.semester)
                .map { it.id }
                .toSet()

            selectedIds = selectedIds
                .filter { it in allowed }
                .take(3)
        }
    }

    fun prepareWeekSystems(): Boolean {
        val team = TeamEngine.prepareWeek(game, weekSchedule)
        val romance = LoveEngine.prepareWeek(team.game, team.schedule)
        game = romance.game
        weekSchedule = romance.schedule
        if (game.pendingConfession != null) {
            screen = "love"
            return true
        }
        if (game.teamProject?.announced == false) {
            screen = "team"
            return true
        }
        return false
    }

    fun leaveTeamScreen() {
        game.teamProject?.let { game = TeamEngine.acknowledge(game, it.key) }
        screen = "main"
    }

    fun advanceOneDay() {
        if (prepareWeekSystems()) return
        game = DayFlowEngine.advanceDay(game, weekSchedule, subjects)
        weekSchedule = LoveEngine.syncSchedule(game, TeamEngine.syncSchedule(game, weekSchedule))
        screen = when {
            game.life.pendingMorning != null -> "morning"
            game.pendingMiniGame != null -> "mini"
            game.pendingEvent != null -> "event"
            else -> "report"
        }
    }

    // 최초 진입 시 월요일을 처리하고,
    // 진행 중에는 기존 결과를 다시 연다.
    fun showReport() {
        if (game.needsRegistration) {
            screen = "register"
            return
        }

        if (subjects.size != 3) return

        if (game.pendingConfession != null) {
            screen = "love"
            return
        }
        if (game.life.pendingMorning != null) {
            screen = "morning"
            return
        }
        if (game.pendingMiniGame != null) {
            screen = "mini"
            return
        }
        if (game.pendingEvent != null) {
            screen = "event"
            return
        }

        if (game.semesterResult != null) {
            screen = "semester"
            return
        }

        if (game.exam != null) {
            screen = "exam"
            return
        }

        if (weekSchedule.size != 28) {
            weekSchedule = createWeekSchedule(
                game.study.keys.toList()
            )
        }

        if (game.day == 0) {
            advanceOneDay()
            return
        }

        screen = "report"
    }

    fun continueAfterWeek() {
        if (game.day != 7) return
        game = LifeEngine.settleWeek(game)

        if (game.pendingConfession != null) {
            screen = "love"
            return
        }
        if (game.life.pendingMorning != null) {
            screen = "morning"
            return
        }
        if (game.pendingMiniGame != null) {
            screen = "mini"
            return
        }
        if (game.pendingEvent != null) {
            screen = "event"
            return
        }

        if (game.exam != null) {
            screen = "exam"
            return
        }

        val withEvent = EventEngine.startWeeklyEvent(game)
        if (withEvent.pendingEvent != null) {
            game = withEvent
            screen = "event"
            return
        }

        val pending = ExamEngine.pendingExam(game)

        if (pending != null) {
            game = ExamEngine.startPendingExam(game)
            screen = "exam"
        } else if (game.week >= 16) {
            game = ExamEngine.finishSemester(game)
            screen = "semester"
        } else {
            game = GameEngine.nextWeek(game)
            screen = "main"
        }
    }

    fun continueAfterExam() {
        val run = game.exam ?: return

        if (run.phase != ExamPhase.RESULT) return
        game = LifeEngine.settleWeek(game)

        if (run.kind == ExamKind.FINAL) {
            game = ExamEngine.finishSemester(game)
            screen = "semester"
        } else if (game.week >= 16) {
            // 이전 단계에서 시험이 누락된 임시 상태도 보완한다.
            game = ExamEngine.startPendingExam(
                game.copy(exam = null)
            )
            screen = "exam"
        } else {
            game = GameEngine.nextWeek(game)
            screen = "main"
        }
    }

    fun continueAfterSemester() {
        if (game.semesterResult == null) return

        screen = if (game.semester >= 8) {
            "graduation"
        } else {
            "vacation"
        }
    }

    fun advanceSemester() {
        val next = AcademicEngine.advanceSemester(game)

        if (next !== game) {
            game = next
            selectedIds = emptyList()
            weekSchedule = emptyList()
            screen = "register"
        }
    }

    // 하루가 끝날 때마다 그날 기록을 주간 기록에 모은다.
    LaunchedEffect(game.semester, game.week, game.day, game.log) {
        weekLog = WeekLogBook.record(weekLog, game)
    }

    // 한 주 자동: 결과 화면에서 잠시 보여 준 뒤 다음 날을 진행한다.
    // 사건·미니게임·늦잠 등 입력이 필요한 상태가 되면 해당 화면으로 넘어가고,
    // 그 화면을 마치고 결과 화면으로 돌아오면 다시 이어서 진행한다.
    LaunchedEffect(screen, progressMode, game.semester, game.week, game.day) {
        if (screen == "report" && progressMode == ProgressMode.WEEK && WeekLogBook.canAutoAdvance(game)) {
            delay(ProgressMode.AUTO_DELAY_MS)
            if (screen == "report" && WeekLogBook.canAutoAdvance(game)) {
                advanceOneDay()
            }
        }
    }

    // 주간 진행 전 화면에 들어왔을 때 배정과 사건 추첨을 한 번만 준비한다.
    LaunchedEffect(screen, game.semester, game.week, game.day) {
        if (screen in setOf("main", "schedule", "routines", "love", "life") && game.day == 0) {
            prepareWeekSystems()
        }
    }

    BackHandler(enabled = screen != "title") {
        if (screen == "account") {
            if (!session.onlineBusy) {
                session.cancelCloudPreview()
                screen = if (session.hasGame) "main" else "title"
            }
            return@BackHandler
        }
        if (screen == "love" && game.pendingConfession != null) return@BackHandler
        if (screen == "team") {
            leaveTeamScreen()
            return@BackHandler
        }
        screen = when (screen) {
            "schedule",
            "report",
            "exam",
            "semester",
            "grades",
            "vacation",
            "graduation",
            "event",
            "love",
            "mini",
            "morning",
            "life",
            "routines" -> "main"

            else -> "title"
        }
    }

    if (askNewGame) {
        AlertDialog(
            onDismissRequest = {
                askNewGame = false
            },
            title = {
                Text("새로 입학할까요?")
            },
            text = {
                Text(
                    "기존 게임 진행 내용이 새 게임으로 바뀝니다. " +
                            "저장한 루틴은 유지됩니다."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        askNewGame = false
                        session.startNewGame()
                    }
                ) {
                    Text("새로 시작")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        askNewGame = false
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFFFF4E4)
    ) { innerPadding ->
        // 타이틀은 상태 표시줄 아래까지 꽉 채우는 화면이라 공통 여백 없이 따로 그린다.
        if (screen == "title") {
            TitleScreen(
                hasGame = session.hasGame,
                game = game,
                saveStatus = session.saveStatus,
                loadError = session.loadError,
                onContinue = { session.resumeGame() },
                onNewGame = {
                    if (session.hasGame || session.loadError != null) {
                        askNewGame = true
                    } else {
                        session.startNewGame()
                    }
                },
                onAccount = { screen = "account" },
                onRetryLoad = { session.retryLoad() },
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            )
            return@Scaffold
        }

        // 하루 진행 화면들(결과·늦잠·딜레마)도 상태 표시줄까지 꽉 채우는 전체 화면이다.
        if (screen == "report" || screen == "morning" || screen == "event") {
            when (screen) {
                "report" -> {
                    DayReportScreen(
                        game = game,
                        subjects = subjects,
                        onNextDay = {
                            if (game.day < 7) {
                                advanceOneDay()
                            }
                        },
                        onNextWeek = {
                            continueAfterWeek()
                        },
                        onHome = {
                            screen = "main"
                        },
                        continueLabel = ExamEngine.pendingExam(game)?.let {
                            "${it.title} 보러가기"
                        } ?: if (game.week >= 16) {
                            "학기 결산 보기"
                        } else {
                            "다음 주로"
                        },
                        mode = progressMode,
                        onModeChange = { progressModeName = it.name },
                        weekEntries = WeekLogBook.decode(weekLog, game),
                        schedule = weekSchedule
                    )
                }

                "morning" -> {
                    MorningScreen(
                        game = game,
                        onBegin = { key -> game = LifeEngine.beginMorning(game, key, System.currentTimeMillis()) },
                        onTap = { key, taps -> game = LifeEngine.tapMorning(game, key, taps, System.currentTimeMillis()) },
                        onRefresh = { key, now ->
                            if (game.life.pendingMorning?.key == key) game = LifeEngine.refreshMorning(game, now)
                        },
                        onSkip = { key -> game = LifeEngine.skipMorning(game, key, System.currentTimeMillis()) },
                        onContinue = { key ->
                            val next = LifeEngine.continueMorning(game, weekSchedule, subjects, key)
                            if (next !== game) {
                                game = next
                                weekSchedule = LoveEngine.syncSchedule(game, TeamEngine.syncSchedule(game, weekSchedule))
                                screen = when {
                                    game.life.pendingMorning != null -> "morning"
                                    game.pendingMiniGame != null -> "mini"
                                    game.pendingEvent != null -> "event"
                                    else -> "report"
                                }
                            }
                        },
                        onHome = { screen = "main" }
                    )
                }

                "event" -> {
                    EventScreen(
                        game = game,
                        subjects = subjects,
                        onChoose = { key, index ->
                            game = EventEngine.choose(game, key, index)
                            weekSchedule = LoveEngine.syncSchedule(game, TeamEngine.syncSchedule(game, weekSchedule))
                        },
                        onContinue = { key ->
                            val run = game.pendingEvent
                            val next = EventEngine.continueEvent(
                                game, weekSchedule, subjects, key
                            )
                            if (next !== game) {
                                game = next
                                weekSchedule = LoveEngine.syncSchedule(game, TeamEngine.syncSchedule(game, weekSchedule))
                                if (run?.day == 7) {
                                    continueAfterWeek()
                                } else {
                                    screen = "report"
                                }
                            }
                        },
                        onHome = { screen = "main" }
                    )
                }
            }
            return@Scaffold
        }

        // 수강신청·시간표·루틴은 머리 줄과 아래 고정 버튼이 있는 전체 화면 패널이다.
        if (screen == "register" || screen == "schedule" || screen == "routines") {
            when (screen) {
                "register" -> {
                    CourseRegistrationScreen(
                        selectedIds = selectedIds,
                        subjects = availableSubjects,
                        semester = game.semester,
                        scholarship = game.registrationScholarship,
                        onToggle = { id ->
                            selectedIds = when {
                                id in selectedIds -> {
                                    selectedIds - id
                                }

                                selectedIds.size < 3 -> {
                                    selectedIds + id
                                }

                                else -> selectedIds
                            }
                        },
                        onConfirm = {
                            val next = AcademicEngine.registerSubjects(
                                game,
                                selectedIds
                            )

                            if (next !== game) {
                                game = next

                                weekSchedule = createWeekSchedule(
                                    next.study.keys.toList()
                                )

                                if (!prepareWeekSystems()) screen = "main"
                            }
                        },
                        onBack = {
                            screen = "title"
                        }
                    )
                }

                "schedule" -> {
                    if (
                        game.day == 0 &&
                        game.pendingEvent == null &&
                        game.pendingConfession == null &&
                        game.pendingMiniGame == null && game.miniBonus == null &&
                        game.life.pendingMorning == null && game.life.morningNotice == null &&
                        weekSchedule.size == 28
                    ) {
                        WeeklyScheduleScreen(
                            subjects = subjects,
                            schedule = weekSchedule,
                            game = game,
                            onPaint = { index, choice ->
                                if (
                                    game.day == 0 &&
                                    game.pendingEvent == null &&
                                    game.pendingConfession == null &&
                                    game.pendingMiniGame == null && game.miniBonus == null &&
                                    game.life.pendingMorning == null && game.life.morningNotice == null &&
                                    !weekSchedule[index].locked &&
                                    AcademicEngine.canUseActivity(
                                        game,
                                        choice.activity
                                    )
                                ) {
                                    weekSchedule = weekSchedule.mapIndexed {
                                            i, slot ->
                                        if (i == index) {
                                            choice
                                        } else {
                                            slot
                                        }
                                    }
                                }
                            },
                            onClear = {
                                if (game.day == 0 && game.pendingEvent == null && game.pendingConfession == null && game.pendingMiniGame == null && game.miniBonus == null &&
                                    game.life.pendingMorning == null && game.life.morningNotice == null) {
                                    weekSchedule = weekSchedule.map { slot ->
                                        if (slot.locked) {
                                            slot
                                        } else {
                                            TimeSlot()
                                        }
                                    }
                                }
                            },
                            onBack = {
                                screen = "main"
                            },
                            onStartWeek = {
                                showReport()
                            },
                            onRoutines = {
                                screen = "routines"
                            }
                        )
                    } else {
                        // 한 주 진행 중 등 시간표를 바꿀 수 없는 상태면 홈으로 돌려보낸다.
                        LaunchedEffect(Unit) { screen = "main" }
                    }
                }

                "routines" -> {
                    RoutineScreen(
                        session = session,
                        onBack = {
                            screen = "main"
                        }
                    )
                }
            }
            return@Scaffold
        }

        // 홈은 HUD·무대·아래 내비게이션이 화면 전체를 쓰므로 공통 여백 없이 따로 그린다.
        if (screen == "main") {
            HomeScreen(
                game = game,
                schedule = weekSchedule,
                saveStatus = session.saveStatus,
                actions = HomeActions(
                    onSchedule = {
                        if (HomeModel.canPlan(game)) {
                            if (weekSchedule.size != 28) {
                                weekSchedule = createWeekSchedule(game.study.keys.toList())
                            }
                            if (!prepareWeekSystems()) screen = "schedule"
                        }
                    },
                    onProgress = { showReport() },
                    onGrades = { screen = "grades" },
                    onLife = { screen = "life" },
                    onTeam = { screen = "team" },
                    onLove = { screen = "love" },
                    onRoutines = { screen = "routines" },
                    onSave = { session.saveNow() },
                    onAccount = { screen = "account" },
                    onTitle = { screen = "title" },
                    onToggleMiniGames = {
                        if (game.pendingMiniGame == null && game.life.pendingMorning == null) {
                            game = game.copy(skipMiniGames = !game.skipMiniGames)
                        }
                    }
                )
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = session.saveStatus,
                fontSize = 12.sp,
                color = Color(0xFF8A6A48)
            )

            when (screen) {
                "account" -> {
                    AccountScreen(session = session, onBack = {
                        session.cancelCloudPreview()
                        screen = if (session.hasGame) "main" else "title"
                    })
                }



                "life" -> {
                    LifeScreen(
                        game = game,
                        onBuy = { semester, week -> game = LifeEngine.buyLottery(game, semester, week) },
                        onBack = { screen = "main" }
                    )
                }

                "mini" -> {
                    MiniGameScreen(
                        game = game,
                        onBegin = { key -> game = MiniGameEngine.begin(game, key, System.currentTimeMillis()) },
                        onRefresh = { key, now ->
                            if (game.pendingMiniGame?.key == key) game = MiniGameEngine.refresh(game, now)
                        },
                        onInput = { key, value, revision ->
                            game = MiniGameEngine.input(game, key, value, revision, System.currentTimeMillis())
                        },
                        onSubmit = { key -> game = MiniGameEngine.submitOrders(game, key, System.currentTimeMillis()) },
                        onSkip = { key -> game = MiniGameEngine.skipOrForfeit(game, key, System.currentTimeMillis()) },
                        onContinue = { key ->
                            val next = DayFlowEngine.continueMini(game, weekSchedule, subjects, key)
                            if (next !== game) {
                                game = next
                                weekSchedule = LoveEngine.syncSchedule(game, TeamEngine.syncSchedule(game, weekSchedule))
                                screen = when {
                                    game.life.pendingMorning != null -> "morning"
                                    game.pendingMiniGame != null -> "mini"
                                    game.pendingEvent != null -> "event"
                                    else -> "report"
                                }
                            }
                        },
                        onHome = { screen = "main" }
                    )
                }

                "love" -> {
                    LoveScreen(
                        game = game,
                        schedule = weekSchedule,
                        onChoose = { key, accept ->
                            val chosen = LoveEngine.chooseConfession(game, weekSchedule, key, accept)
                            game = chosen.game
                            weekSchedule = chosen.schedule
                        },
                        onContinue = { key ->
                            val next = LoveEngine.completeConfession(game, key)
                            if (next !== game) {
                                game = next
                                screen = "main"
                            }
                        },
                        onBack = { screen = "main" }
                    )
                }

                "team" -> {
                    TeamScreen(
                        game = game,
                        onConfirm = { key ->
                            game = TeamEngine.acknowledge(game, key)
                            screen = "schedule"
                        },
                        onBack = { leaveTeamScreen() }
                    )
                }


                "exam" -> {
                    ExamScreen(
                        game = game,
                        subjects = subjects,
                        onBegin = {
                            game = ExamEngine.beginExam(
                                game,
                                SystemClock.elapsedRealtime()
                            )
                        },
                        onAnswer = { answer, index ->
                            game = ExamEngine.answerQuestion(
                                game,
                                answer,
                                index,
                                SystemClock.elapsedRealtime()
                            )
                        },
                        onTimeout = { index ->
                            game = ExamEngine.answerQuestion(
                                game,
                                -1,
                                index,
                                SystemClock.elapsedRealtime()
                            )
                        },
                        onNext = { index ->
                            game = ExamEngine.nextQuestion(
                                game,
                                index,
                                SystemClock.elapsedRealtime()
                            )
                        },
                        onContinue = {
                            continueAfterExam()
                        },
                        onHome = {
                            screen = "main"
                        }
                    )
                }

                "semester" -> {
                    game.semesterResult?.let {
                        SemesterSummaryScreen(
                            game,
                            subjects,
                            onContinue = {
                                continueAfterSemester()
                            },
                            onHome = {
                                screen = "main"
                            }
                        )
                    }
                }

                "grades" -> {
                    GradeRecordScreen(
                        game,
                        subjects,
                        onBack = {
                            screen = "main"
                        }
                    )
                }


                "vacation" -> {
                    VacationScreen(
                        game,
                        subjects,
                        onChoose = { id ->
                            game = AcademicEngine.applyVacation(
                                game,
                                id
                            )
                        },
                        onAdvance = {
                            advanceSemester()
                        },
                        onHome = {
                            screen = "main"
                        }
                    )
                }

                "graduation" -> {
                    GraduationScreen(
                        game,
                        onTitle = {
                            screen = "title"
                        },
                        onHome = {
                            screen = "main"
                        }
                    )
                }
            }
        }
    }
}
