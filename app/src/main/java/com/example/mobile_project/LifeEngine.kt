package com.example.mobile_project

import kotlin.random.Random

object LifeEngine {
    const val MORNING_DURATION_MS = 4_000L
    const val MORNING_TAPS = 22
    const val LATE_PENALTY = 3
    const val LOTTERY_PRICE = 10_000
    val morningActivities = setOf(ActivityType.CLASS, ActivityType.STUDY, ActivityType.WORK,
        ActivityType.TEAM, ActivityType.CAREER)
    val prizes = listOf(
        LotteryPrize("꽝", 0, 840), LotteryPrize("5등 당첨", 20_000, 120),
        LotteryPrize("4등 당첨", 80_000, 35), LotteryPrize("3등 당첨", 500_000, 5)
    )

    fun absoluteWeek(state: GameState): Int = (state.semester - 1) * 16 + state.week - 1
    fun isSick(state: GameState): Boolean = state.life.sickWeek == absoluteWeek(state)
    fun studyMultiplier(state: GameState): Double = if (isSick(state)) 0.6 else 1.0
    fun morningChance(state: GameState): Double =
        ((40.0 - state.hp * 100.0 / state.maxHp) * 0.015).coerceIn(0.0, 0.6)

    fun prepareMorning(state: GameState, schedule: List<TimeSlot>, random: Random = Random.Default): GameState {
        if (state.day !in 0..6 || state.study.isEmpty() || state.needsRegistration || state.semesterResult != null ||
            state.exam != null || state.pendingEvent != null || state.pendingConfession != null ||
            state.pendingMiniGame != null || state.miniBonus != null || state.life.pendingMorning != null ||
            state.teamProject?.announced == false || schedule.size != 28) return state
        val today = EventEngine.absoluteDay(state)
        if (state.life.morningCheckedDay == today) return state
        val checked = state.copy(life = state.life.copy(morningCheckedDay = today))
        val slot = schedule[state.day * 4]
        val chance = morningChance(state)
        if (slot.activity !in morningActivities || chance <= 0.0 || random.nextDouble() >= chance) return checked
        // 지각 감점은 실제 오전 수업 과목에만 적용한다. 알바 지각이 다른 과목 성적을 깎지 않는다.
        val subject = slot.subjectId?.takeIf { slot.activity == ActivityType.CLASS && it in state.study }
        val run = MorningRun(state.semester, state.week, state.day, slot.activity, subject, random.nextBoolean())
        val offered = checked.copy(life = checked.life.copy(pendingMorning = run))
        if (!state.skipMiniGames) return offered
        val resolved = finishMorning(offered, run.autoSuccess)
        return resolved.copy(life = resolved.life.copy(pendingMorning = null))
    }

    private fun matches(state: GameState, run: MorningRun, key: String): Boolean =
        run.key == key && run.semester == state.semester && run.week == state.week && run.day == state.day

    fun beginMorning(state: GameState, key: String, nowMs: Long): GameState {
        val run = state.life.pendingMorning ?: return state
        if (!matches(state, run, key) || run.phase != MorningPhase.READY) return state
        return state.copy(life = state.life.copy(pendingMorning = run.copy(phase = MorningPhase.PLAY,
            startedAtMs = nowMs, deadlineMs = nowMs + MORNING_DURATION_MS)))
    }

    private fun finishMorning(state: GameState, success: Boolean): GameState {
        val run = state.life.pendingMorning ?: return state
        if (run.phase == MorningPhase.RESULT) return state
        val record = AttendanceRecord(run.semester, run.week, run.day, run.activity, run.subjectId, success)
        val counts = if (!success && run.subjectId != null) state.life.lateCounts +
            (run.subjectId to (state.life.lateCounts.getOrDefault(run.subjectId, 0) + 1)) else state.life.lateCounts
        return state.copy(life = state.life.copy(
            pendingMorning = run.copy(phase = MorningPhase.RESULT, success = success),
            lateCounts = counts, morningNotice = record,
            attendanceHistory = state.life.attendanceHistory + record))
    }

    fun refreshMorning(state: GameState, nowMs: Long): GameState {
        val run = state.life.pendingMorning ?: return state
        if (run.phase == MorningPhase.PLAY && (nowMs >= run.deadlineMs || nowMs < run.startedAtMs))
            return finishMorning(state, false)
        return state
    }

    fun tapMorning(state: GameState, key: String, expectedTaps: Int, nowMs: Long): GameState {
        val game = refreshMorning(state, nowMs)
        val run = game.life.pendingMorning ?: return game
        if (!matches(game, run, key) || run.phase != MorningPhase.PLAY || run.taps != expectedTaps) return game
        val next = game.copy(life = game.life.copy(pendingMorning = run.copy(taps = run.taps + 1)))
        return if (run.taps + 1 >= MORNING_TAPS) finishMorning(next, true) else next
    }

    fun skipMorning(state: GameState, key: String, nowMs: Long): GameState {
        val game = refreshMorning(state, nowMs)
        val run = game.life.pendingMorning ?: return game
        if (!matches(game, run, key) || run.phase == MorningPhase.RESULT) return game
        return finishMorning(game, run.phase == MorningPhase.READY && run.autoSuccess)
    }

    fun continueMorning(state: GameState, schedule: List<TimeSlot>, subjects: List<Subject>, key: String,
        random: Random = Random.Default): GameState {
        val run = state.life.pendingMorning ?: return state
        if (!matches(state, run, key) || run.phase != MorningPhase.RESULT) return state
        return DayFlowEngine.advanceDay(state.copy(life = state.life.copy(pendingMorning = null)), schedule, subjects, random)
    }

    fun attendanceMessage(record: AttendanceRecord): String = when {
        record.success -> "늦잠 · 전력질주로 오전 일정에 제시간에 도착했어요."
        record.subjectId != null -> "늦잠 · ${subjectForId(record.subjectId)?.name ?: record.subjectId} 수업에 지각했어요. 이번 시험 계산에서 ${LATE_PENALTY}점 감점돼요."
        else -> "늦잠 · 오전 ${record.activity.label}에 늦었어요. 수업 지각이 아니므로 성적 감점은 없어요."
    }

    // 하루 실행과 선택 이벤트의 실행 경로가 모두 이 함수를 거친다. 같은 일요일은 한 번만 추첨한다.
    fun settleWeek(state: GameState, random: Random = Random.Default): GameState {
        if (state.day != 7 || state.needsRegistration || state.study.isEmpty() || state.semesterResult != null ||
            state.life.pendingMorning != null || state.pendingMiniGame != null || state.miniBonus != null) return state
        val week = absoluteWeek(state)
        if (state.life.healthCheckedWeek == week) return state
        val notes = mutableListOf<String>()
        if (isSick(state)) notes += "건강 · 이번 주 감기 효과가 끝났어요."
        val sickNextWeek = state.week < 16 && state.hp * 100.0 / state.maxHp < 30.0 && random.nextDouble() < 0.45
        if (sickNextWeek) notes += "건강 경고 · 감기몸살! 다음 한 주 공부 효율이 60%가 돼요. 수면과 휴식을 챙겨 주세요."
        return state.copy(life = state.life.copy(healthCheckedWeek = week,
            sickWeek = if (sickNextWeek) week + 1 else -1), log = state.log + notes)
    }

    fun currentTicket(state: GameState): LotteryTicket? = state.life.lotteryHistory.find {
        it.semester == state.semester && it.week == state.week
    }

    fun canBuyLottery(state: GameState): Boolean = state.day in 0..6 && state.study.size == 3 &&
        !state.needsRegistration && state.semesterResult == null && state.exam == null &&
        state.pendingEvent == null && state.pendingConfession == null && state.pendingMiniGame == null &&
        state.miniBonus == null && state.life.pendingMorning == null && state.life.morningNotice == null &&
        state.teamProject?.announced != false && currentTicket(state) == null && state.money - LOTTERY_PRICE >= -50_000

    fun buyLottery(state: GameState, semester: Int, week: Int, random: Random = Random.Default): GameState {
        if (semester != state.semester || week != state.week || !canBuyLottery(state)) return state
        val roll = random.nextInt(1_000)
        var end = 0
        val index = prizes.indexOfFirst { prize -> end += prize.ticketsPerThousand; roll < end }
        val prize = prizes[index]
        return state.copy(money = state.money - LOTTERY_PRICE + prize.amount,
            life = state.life.copy(lotteryHistory = state.life.lotteryHistory + LotteryTicket(semester, week, index)))
    }

    fun validateState(state: GameState, schedule: List<TimeSlot>) {
        val life = state.life
        require(life.lateCounts.all { it.key in state.study && it.value in 0..112 })
        require(life.sickWeek in -1..128 && life.healthCheckedWeek in -1..127)
        require(life.morningCheckedDay in -1..895)
        require(life.lotteryHistory.all { it.semester in 1..8 && it.week in 1..16 && it.prizeIndex in prizes.indices })
        require(life.lotteryHistory.map { it.semester to it.week }.distinct().size == life.lotteryHistory.size)
        require(life.attendanceHistory.all { it.semester in 1..8 && it.week in 1..16 && it.day in 0..6 &&
            it.activity in morningActivities && (it.subjectId == null || (it.activity == ActivityType.CLASS && subjectForId(it.subjectId) != null)) })
        require(life.attendanceHistory.map { Triple(it.semester, it.week, it.day) }.distinct().size == life.attendanceHistory.size)
        life.morningNotice?.let { notice ->
            require(notice.semester == state.semester && notice.week == state.week && notice.day == state.day)
            require(notice in life.attendanceHistory)
        }
        life.pendingMorning?.let { run ->
            require(matches(state, run, run.key) && state.day in 0..6 && run.activity in morningActivities)
            require(life.morningCheckedDay == EventEngine.absoluteDay(state))
            require(schedule.size == 28 && schedule[state.day * 4].activity == run.activity)
            require(run.subjectId == schedule[state.day * 4].subjectId.takeIf { run.activity == ActivityType.CLASS })
            require(run.taps in 0..MORNING_TAPS && (run.phase == MorningPhase.RESULT) == (run.success != null))
            require(!state.needsRegistration && state.semesterResult == null && state.exam == null &&
                state.pendingEvent == null && state.pendingConfession == null && state.pendingMiniGame == null && state.miniBonus == null)
            if (run.phase == MorningPhase.RESULT) {
                require(life.morningNotice == AttendanceRecord(run.semester, run.week, run.day, run.activity, run.subjectId, requireNotNull(run.success)))
            } else require(life.morningNotice == null)
            if (run.phase == MorningPhase.PLAY) require(run.deadlineMs - run.startedAtMs == MORNING_DURATION_MS)
        }
    }
}
