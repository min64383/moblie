package com.example.mobile_project

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 홈 화면에 보여 줄 글자·숫자를 계산한다. 화면(Compose)과 분리해 두어 단위 테스트할 수 있다.
 * 규칙은 프로토타입의 renderHud / renderStage / bubbleText 를 앱의 실제 데이터에 맞춰 옮긴 것.
 */
object HomeModel {
    const val MIDTERM_WEEK = 8
    const val FINAL_WEEK = 16
    const val MIN_SLEEP = 5
    const val SLOT_COUNT = 28
    const val DEBT_LIMIT = -50_000

    /** 매주 자동으로 빠지는 생활비. GameEngine.settleWeek 와 같은 식. */
    fun weeklyCost(semester: Int): Int = 25_000 + ((semester - 1) / 2) * 5_000

    /** 이번 주 시간표를 세울 수 있는 상태인지. (기존 "주간 시간표" 버튼의 활성 조건과 같다) */
    fun canPlan(game: GameState): Boolean =
        game.day == 0 && game.pendingEvent == null && game.pendingConfession == null &&
            game.pendingMiniGame == null && game.miniBonus == null &&
            game.life.pendingMorning == null && game.life.morningNotice == null

    /** 큰 진행 버튼의 글자. (기존 "시간 보내기" 버튼과 같은 우선순위) */
    fun progressLabel(game: GameState): String = when {
        game.pendingConfession != null -> "고백 이벤트 이어하기"
        game.life.pendingMorning?.phase == MorningPhase.RESULT -> "늦잠 결과 보기"
        game.life.pendingMorning != null -> "늦잠 달리기 이어하기"
        game.pendingMiniGame?.phase == MiniPhase.RESULT -> "미니게임 결과 보기"
        game.pendingMiniGame != null -> "미니게임 이어하기"
        game.pendingEvent?.phase == EventPhase.RESULT -> "이벤트 결과 보기"
        game.pendingEvent != null -> "이벤트 이어하기"
        game.semesterResult != null -> "학기 성적 보기"
        game.exam?.phase == ExamPhase.RESULT -> "시험 결과 보기"
        game.exam != null -> "시험 이어하기"
        game.day == 0 -> "시간 보내기"
        else -> "진행 결과 보기"
    }

    /** HUD 오른쪽에 깜빡이는 배지. 없으면 null. */
    fun hudBadge(game: GameState): String? = when {
        game.week == MIDTERM_WEEK -> "중간고사"
        game.week == FINAL_WEEK -> "기말고사"
        LifeEngine.isSick(game) -> "감기몸살"
        else -> null
    }

    /** HUD 두 번째 줄. 예) "그럭저럭 · 누적 평점 3.85 · 민지" */
    fun hudMoodLine(game: GameState): String {
        val parts = mutableListOf(PooangMood.of(game).label)
        if (game.completedSemesters.isNotEmpty()) {
            parts += "누적 평점 " + String.format(Locale.US, "%.2f", game.cumulativeGpa)
        }
        game.love?.let { parts += it.partner }
        return parts.joinToString(" · ")
    }

    data class Mission(val tag: String, val text: String, val progress: String?, val warn: Boolean)

    /**
     * 미션 배너. 진행 중인 주에는 진행 상황을, 계획 단계에서는 시간표 목표를 보여 준다.
     * 시간표가 아직 없으면(빈 리스트) 빈칸·수면 계산은 건너뛴다.
     */
    fun mission(game: GameState, schedule: List<TimeSlot>): Mission {
        when {
            game.semesterResult != null ->
                return Mission("알림", "학기가 끝났어요. 성적을 확인하자", null, false)
            game.day == 7 ->
                return Mission("알림", "이번 주가 끝났어요. 결과를 확인하자", null, false)
            game.day in 1..6 ->
                return Mission("진행 중", "${GameEngine.dayNames[game.day - 1]}까지 진행했어요", "${game.day}/7일", false)
        }
        val planned = schedule.size == SLOT_COUNT
        val free = if (planned) schedule.count { it.activity == ActivityType.EMPTY } else SLOT_COUNT
        val sleep = if (planned) schedule.count { it.activity == ActivityType.SLEEP } else 0
        val progress = "${SLOT_COUNT - free}/$SLOT_COUNT"
        val left = examWeeksLeft(game.week)
        return when {
            planned && sleep < MIN_SLEEP ->
                Mission("주의", "수면을 ${MIN_SLEEP}칸 이상 확보하자 ($sleep/$MIN_SLEEP)", progress, true)
            !planned -> Mission("이번 주 목표", "이번 주 시간표를 세워 보자", null, false)
            free > 0 -> Mission("이번 주 목표", "시간표 빈칸 ${free}개를 채우자", progress, false)
            left == 0 -> Mission("이번 주 목표", "${examName(game.week)} 주간! 공부 칸을 점검하자", progress, false)
            else -> Mission("이번 주 목표", "시간표 완성! 시간을 보내자", progress, false)
        }
    }

    fun examName(week: Int): String = if (week <= MIDTERM_WEEK) "중간고사" else "기말고사"

    fun examWeeksLeft(week: Int): Int = (if (week <= MIDTERM_WEEK) MIDTERM_WEEK else FINAL_WEEK) - week

    /** 과목 예상 점수의 평균 (0~100). 과목이 없으면 0. */
    fun expectedAverage(game: GameState): Int {
        if (game.study.isEmpty()) return 0
        return game.study.values.map { ExamEngine.expectedScore(it, game.semester) }.average().roundToInt()
    }

    /** 시간표에 빈칸이 있거나 수면이 부족하면 시간표 버튼에 알림 점을 띄운다. */
    fun scheduleNeedsAttention(game: GameState, schedule: List<TimeSlot>): Boolean {
        if (!canPlan(game)) return false
        if (schedule.size != SLOT_COUNT) return true
        return schedule.any { it.activity == ActivityType.EMPTY } ||
            schedule.count { it.activity == ActivityType.SLEEP } < MIN_SLEEP
    }

    /** 푸앙이 말풍선. 위에서부터 먼저 맞는 한 줄을 고른다. */
    fun bubble(game: GameState): String {
        val hpRate = if (game.maxHp <= 0) 0.0 else game.hp * 100.0 / game.maxHp
        val team = game.teamProject
        val love = game.love
        return when {
            LifeEngine.isSick(game) -> "감기에 걸렸어요… 이번 주는 뭘 해도 잘 안 될 것 같아요."
            game.money <= DEBT_LIMIT -> "통장이 한도까지 찼어요. 알바를 좀 해야겠어요."
            game.money < 0 -> "잔액이 마이너스예요… 이러다 큰일 나겠는데요."
            hpRate < 25 -> "너무 피곤해요. 이러다 늦잠 잘 것 같아요."
            game.happiness < 25 -> "요즘 재미있는 게 하나도 없어요."
            game.week == MIDTERM_WEEK -> "중간고사 주간이에요! 이번 주가 끝나면 시험이에요."
            game.week == FINAL_WEEK -> "기말고사 주간이에요. 이번 학기 마지막이에요."
            game.semester == 8 -> "마지막 학기예요. 졸업이 코앞이에요."
            team != null -> "조별과제가 진행 중이에요. ${team.weeksLeft}주 남았어요."
            love != null -> "${love.partner}랑 만난 지 ${love.weeksTogether}주째예요."
            game.semester >= 5 && game.spec < 20 -> "슬슬 취업 준비를 해야 할 것 같은데요…"
            game.happiness >= 80 && hpRate >= 60 -> "컨디션 최고예요! 뭐든 할 수 있을 것 같아요."
            else -> "이번 주는 어떻게 보낼까요?"
        }
    }

    /** 푸앙이를 콕 찔렀을 때 나오는 말 */
    val pokeLines = listOf(
        "간지러워요!",
        "오늘도 화이팅이에요.",
        "시간표는 다 짜셨나요?",
        "의혈! 푸앙!",
        "잠은 꼭 챙겨 주세요.",
        "배고파요… 학식 먹으러 가요."
    )
}

/** 돈을 짧게: 350000 → "₩35만", 12500 → "₩1.3만", 9000 → "₩9,000", -20000 → "−₩2만" */
fun manWon(value: Int): String {
    val a = abs(value)
    val sign = if (value < 0) "−" else ""
    if (a < 10_000) return sign + "₩" + String.format(Locale.KOREA, "%,d", a)
    val man = a / 10_000.0
    val text = if (a >= 100_000) man.roundToInt().toString()
    else String.format(Locale.US, "%.1f", man).removeSuffix(".0")
    return "$sign₩${text}만"
}
