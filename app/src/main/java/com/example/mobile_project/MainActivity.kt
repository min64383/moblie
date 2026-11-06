package com.example.mobile_project

import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private val gameSession by lazy {
        ViewModelProvider(
            this,
            GameSession.Factory(applicationContext)
        )[GameSession::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
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

    var askNewGame by rememberSaveable {
        mutableStateOf(false)
    }

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
            text = "저장 데이터를 불러오는 중이에요.",
            modifier = Modifier.padding(24.dp)
        )
        return
    }

    val availableSubjects = subjectsForSemester(game.semester)
    val subjects = game.study.keys.mapNotNull { subjectForId(it) }
    val scrollState = rememberScrollState()

    LaunchedEffect(screen, game.semester, game.week, game.day, game.exam?.index, game.exam?.phase) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(game.semester, game.needsRegistration) {
        if (game.needsRegistration) {
            val allowed = subjectsForSemester(game.semester).map { it.id }.toSet()
            selectedIds = selectedIds.filter { it in allowed }.take(3)
        }
    }

    // 최초 진입 시 월요일을 처리하고, 진행 중에는 기존 결과만 다시 연다.
    fun showReport() {
        if (game.needsRegistration) {
            screen = "register"
            return
        }
        if (subjects.size != 3) return
        if (game.semesterResult != null) {
            screen = "semester"
            return
        }
        if (game.exam != null) {
            screen = "exam"
            return
        }
        if (weekSchedule.size != 28) {
            weekSchedule = createWeekSchedule(game.study.keys.toList())
        }
        if (game.day == 0) {
            game = GameEngine.advanceDay(game, weekSchedule, subjects)
        }
        screen = "report"
    }

    fun continueAfterWeek() {
        if (game.day != 7) return
        if (game.exam != null) {
            screen = "exam"
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
        if (run.kind == ExamKind.FINAL) {
            game = ExamEngine.finishSemester(game)
            screen = "semester"
        } else if (game.week >= 16) {
            // 4단계에서 이미 16주차를 지난 임시 상태의 시험 누락도 보완한다.
            game = ExamEngine.startPendingExam(game.copy(exam = null))
            screen = "exam"
        } else {
            game = GameEngine.nextWeek(game)
            screen = "main"
        }
    }

    fun continueAfterSemester() {
        if (game.semesterResult == null) return
        screen = if (game.semester >= 8) "graduation" else "vacation"
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

    BackHandler(enabled = screen != "title") {
        screen = when (screen) {
            "schedule", "report", "exam", "semester", "grades", "vacation", "graduation" -> "main"
            else -> "title"
        }
    }

    if (askNewGame) {
        AlertDialog(
            onDismissRequest = { askNewGame = false },
            title = { Text("새 게임을 시작할까요?") },
            text = {
                Text("기존 게임 진행을 새 게임으로 덮어써요. 저장한 루틴은 유지돼요.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        askNewGame = false
                        session.startNewGame()
                    }
                ) {
                    Text("새 게임 시작")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { askNewGame = false }
                ) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(containerColor = Color(0xFFFFF4E4)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            when (screen) {
                "title" -> {
                    Text(
                        text = "푸앙이\n대학생활",
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF5A3A1E)
                    )
                    Text(
                        text = "입학부터 졸업까지, 4년을 설계하세요",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF8A6A48)
                    )
                    Text(
                        text = "4년 · 8학기 · 128주",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB5651D)
                    )
                    Text(
                        text = "수업과 공부, 알바와 휴식을 조절하며\n" +
                            "푸앙이의 대학생활을 설계해 보세요.",
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )

                    if (game.needsRegistration || game.study.isNotEmpty() || game.completedSemesters.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { screen = if (game.needsRegistration) "register" else "main" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("계속하기")
                        }
                    }

                    Button(
                        onClick = {
                            session.resumeGame()
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9431),
                            contentColor = Color.White
                        )
                    ) {
                        Text("입학하기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                "register" -> {
                    CourseRegistrationScreen(
                        selectedIds = selectedIds,
                        subjects = availableSubjects,
                        semester = game.semester,
                        scholarship = game.registrationScholarship,
                        onToggle = { id ->
                            selectedIds = when {
                                id in selectedIds -> selectedIds - id
                                selectedIds.size < 3 -> selectedIds + id
                                else -> selectedIds
                            }
                        },
                        onConfirm = {
                            val next = AcademicEngine.registerSubjects(game, selectedIds)
                            if (next !== game) {
                                game = next
                                weekSchedule = createWeekSchedule(next.study.keys.toList())
                                screen = "main"
                            }
                        },
                        onBack = { screen = "title" }
                    )
                }

                "main" -> {
                    Text(
                        text = "${semesterLabel(game.semester)} · ${game.week}주차",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5A3A1E)
                    )

                    GameStatusPanel(game, subjects)

                    if (game.semesterResult != null) {
                        Text("이번 학기가 끝났어요. 학기 성적을 확인해 보세요.")
                    } else if (game.day in 1..6) {
                        Text("${GameEngine.dayNames[game.day - 1]}까지 진행했어요.")
                    } else if (game.day == 7) {
                        Text("이번 주가 끝났어요. 진행 결과를 확인해 주세요.")
                    }

                    Button(
                        onClick = {
                            if (game.day == 0) {
                                if (weekSchedule.size != 28) {
                                    weekSchedule = createWeekSchedule(game.study.keys.toList())
                                }
                                screen = "schedule"
                            }
                        },
                        enabled = game.day == 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("주간 시간표")
                    }

                    Button(
                        onClick = { showReport() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(when {
                            game.semesterResult != null -> "학기 성적 보기"
                            game.exam?.phase == ExamPhase.RESULT -> "시험 결과 보기"
                            game.exam != null -> "시험 이어하기"
                            game.day == 0 -> "시간 보내기"
                            else -> "진행 결과 보기"
                        })
                    }

                    OutlinedButton(
                        onClick = { screen = "grades" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("성적 기록") }
                }

                "schedule" -> {
                    if (game.day == 0 && weekSchedule.size == 28) {
                        WeeklyScheduleScreen(
                            subjects = subjects,
                            schedule = weekSchedule,
                            game = game,
                            onPaint = { index, choice ->
                                if (game.day == 0 && !weekSchedule[index].locked &&
                                    AcademicEngine.canUseActivity(game, choice.activity)) {
                                    weekSchedule = weekSchedule.mapIndexed { i, slot ->
                                        if (i == index) choice else slot
                                    }
                                }
                            },
                            onClear = {
                                if (game.day == 0) {
                                    weekSchedule = weekSchedule.map { slot ->
                                        if (slot.locked) slot else TimeSlot()
                                    }
                                }
                            },
                            onBack = { screen = "main" },
                            onStartWeek = { showReport() }
                        )
                    } else {
                        Button(onClick = { screen = "main" }) { Text("홈으로") }
                    }
                }

                "report" -> {
                    DayReportScreen(
                        game = game,
                        subjects = subjects,
                        onNextDay = {
                            if (game.day < 7) {
                                game = GameEngine.advanceDay(game, weekSchedule, subjects)
                            }
                        },
                        onNextWeek = { continueAfterWeek() },
                        onHome = { screen = "main" },
                        continueLabel = ExamEngine.pendingExam(game)?.let {
                            "${it.title} 보러가기"
                        } ?: if (game.week >= 16) "학기 결산 보기" else "다음 주로"
                    )
                }

                "exam" -> {
                    ExamScreen(
                        game = game,
                        subjects = subjects,
                        onBegin = {
                            game = ExamEngine.beginExam(game, SystemClock.elapsedRealtime())
                        },
                        onAnswer = { answer, index ->
                            game = ExamEngine.answerQuestion(
                                game, answer, index, SystemClock.elapsedRealtime()
                            )
                        },
                        onTimeout = { index ->
                            game = ExamEngine.answerQuestion(
                                game, -1, index, SystemClock.elapsedRealtime()
                            )
                        },
                        onNext = { index ->
                            game = ExamEngine.nextQuestion(game, index, SystemClock.elapsedRealtime())
                        },
                        onContinue = { continueAfterExam() },
                        onHome = { screen = "main" }
                    )
                }

                "semester" -> {
                    game.semesterResult?.let {
                        SemesterSummaryScreen(
                            game, subjects,
                            onContinue = { continueAfterSemester() },
                            onHome = { screen = "main" }
                        )
                    }
                }

                "grades" -> {
                    GradeRecordScreen(game, subjects, onBack = { screen = "main" })
                }

                "vacation" -> {
                    VacationScreen(
                        game, subjects,
                        onChoose = { id -> game = AcademicEngine.applyVacation(game, id) },
                        onAdvance = { advanceSemester() },
                        onHome = { screen = "main" }
                    )
                }

                "graduation" -> {
                    GraduationScreen(game, onTitle = { screen = "title" }, onHome = { screen = "main" })
                }
            }
        }
    }
}
