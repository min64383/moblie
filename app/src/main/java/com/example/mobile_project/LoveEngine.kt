package com.example.mobile_project

import kotlin.random.Random

object LoveEngine {
    const val FIRST_SEMESTER = 3
    const val REQUIRED_CHARM = 40
    const val REQUIRED_NETWORK = 30
    const val DATE_COST = 20_000
    const val MISSED_LIMIT = 3
    const val INCIDENT_CHANCE = 0.35

    fun absoluteWeek(state: GameState): Int = (state.semester - 1) * 16 + state.week - 1

    fun eligible(state: GameState): Boolean =
        state.semester >= FIRST_SEMESTER && state.charm >= REQUIRED_CHARM &&
            state.network >= REQUIRED_NETWORK && state.love == null

    private fun dateCandidates(schedule: List<TimeSlot>): List<Int> {
        if (schedule.size != 28) return emptyList()
        val ordered = (5..6).flatMap { day -> (0..2).map { day * 4 + it } } +
            (0..4).flatMap { day -> (1..2).map { day * 4 + it } }
        return ordered.filter { !schedule[it].locked }.take(4)
    }

    fun canAccept(state: GameState, schedule: List<TimeSlot>): Boolean =
        state.pendingConfession?.choice == LoveChoice.UNDECIDED && state.love == null &&
            dateCandidates(schedule).isNotEmpty()

    fun syncSchedule(state: GameState, schedule: List<TimeSlot>): List<TimeSlot> {
        if (schedule.size != 28) return schedule
        val index = state.love?.dateIndex
        return schedule.mapIndexed { i, slot ->
            when {
                i == index && (slot.activity == ActivityType.DATE || !slot.locked) -> TimeSlot(ActivityType.DATE)
                slot.activity == ActivityType.DATE -> TimeSlot(ActivityType.REST)
                else -> slot
            }
        }
    }

    private fun planIncident(love: LoveRelationship, week: Int, random: Random): LoveRelationship {
        if (love.plannedWeek == week) return love
        val incident = if (random.nextDouble() < INCIDENT_CHANCE) {
            LoveIncident(random.nextInt(7), random.nextInt(LoveCatalog.incidents.size))
        } else null
        return love.copy(plannedWeek = week, incident = incident)
    }

    // 학기 첫 주의 월요일 진행 전에 조건을 한 번 확인한다. 거절한 고백은 다시 뽑지 않는다.
    fun prepareWeek(
        state: GameState,
        schedule: List<TimeSlot>,
        random: Random = Random.Default
    ): LoveWeekPreparation {
        if (state.day != 0 || state.needsRegistration || state.study.size != 3 || schedule.size != 28 ||
            state.semesterResult != null || state.exam != null || state.pendingEvent != null ||
            state.pendingConfession != null || state.pendingMiniGame != null || state.life.pendingMorning != null ||
            state.life.morningNotice != null) return LoveWeekPreparation(state, schedule)

        var game = state
        if (state.week == 1 && state.semester !in state.loveCheckedSemesters) {
            game = game.copy(loveCheckedSemesters = state.loveCheckedSemesters + state.semester)
            if (eligible(game)) game = game.copy(pendingConfession =
                LoveConfession(game.semester, LoveCatalog.partners.random(random)))
        }

        game.love?.let { previous ->
            val canKeep = previous.dateIndex in schedule.indices &&
                previous.dateIndex % 4 != 3 &&
                (schedule[previous.dateIndex].activity == ActivityType.DATE || !schedule[previous.dateIndex].locked)
            val index = if (canKeep) previous.dateIndex else dateCandidates(schedule).randomOrNull(random) ?: -1
            val planned = planIncident(previous.copy(dateIndex = index), absoluteWeek(game), random)
            if (planned != game.love) game = game.copy(love = planned)
        }
        return LoveWeekPreparation(game, syncSchedule(game, schedule))
    }

    fun chooseConfession(
        state: GameState, schedule: List<TimeSlot>, key: String, accept: Boolean,
        random: Random = Random.Default
    ): LoveWeekPreparation {
        val confession = state.pendingConfession ?: return LoveWeekPreparation(state, schedule)
        if (confession.key != key || confession.choice != LoveChoice.UNDECIDED || state.day != 0 ||
            state.week != 1 || state.semester != confession.semester) return LoveWeekPreparation(state, schedule)
        if (!accept) return LoveWeekPreparation(state.copy(
            pendingConfession = confession.copy(choice = LoveChoice.DECLINE)
        ), schedule)
        if (!canAccept(state, schedule)) return LoveWeekPreparation(state, schedule)
        val love = planIncident(LoveRelationship(confession.partner, state.semester, state.week,
            dateCandidates(schedule).random(random)), absoluteWeek(state), random)
        val game = state.copy(love = love, pendingConfession = confession.copy(choice = LoveChoice.ACCEPT))
        return LoveWeekPreparation(game, syncSchedule(game, schedule))
    }

    fun completeConfession(state: GameState, key: String): GameState {
        val confession = state.pendingConfession ?: return state
        if (confession.key != key || confession.choice == LoveChoice.UNDECIDED) return state
        return state.copy(pendingConfession = null)
    }

    // 같은 주의 같은 데이트를 중복 처리하지 않는다. 성공 후에도 이전 취소 횟수는 누적된다.
    fun performDate(state: GameState, index: Int): LoveDateOutcome {
        val love = state.love
        val week = absoluteWeek(state)
        if (love == null || state.pendingConfession != null || state.day !in 0..6 ||
            index != love.dateIndex || index / 4 != state.day || love.dateHandledWeek == week) {
            return LoveDateOutcome(state, "데이트 · 진행 대상이 아니에요.")
        }
        val affordable = state.money - DATE_COST >= -50_000
        val happiness = (state.happiness + if (affordable) 25 else -6).coerceIn(0, 100)
        val game = state.copy(
            money = if (affordable) state.money - DATE_COST else state.money,
            happiness = happiness,
            love = love.copy(dateHandledWeek = week, missedDates = love.missedDates + if (affordable) 0 else 1)
        )
        val change = happiness - state.happiness
        return LoveDateOutcome(game, if (affordable) {
            "데이트 · ${love.partner}\n돈 -${DATE_COST}원 · 행복도 ${if (change > 0) "+" else ""}$change"
        } else {
            "데이트 취소 · 잔액 부족\n행복도 $change · 누적 취소 ${love.missedDates + 1}/$MISSED_LIMIT"
        })
    }

    fun applyDayIncident(state: GameState): GameState {
        val love = state.love ?: return state
        val incident = love.incident ?: return state
        if (state.day !in 0..6 || love.plannedWeek != absoluteWeek(state) ||
            incident.day != state.day || incident.applied) return state
        val option = LoveCatalog.incidents[incident.optionIndex]
        val hp = (state.hp + option.hp).coerceIn(0, state.maxHp)
        val happy = (state.happiness + option.happiness).coerceIn(0, 100)
        val money = (state.money + option.money).coerceAtLeast(-50_000)
        val changes = listOf("체력" to hp - state.hp, "행복도" to happy - state.happiness,
            "돈" to money - state.money).filter { it.second != 0 }.joinToString(" · ") { (name, value) ->
                "$name ${if (value > 0) "+" else ""}$value${if (name == "돈") "원" else ""}"
            }.ifEmpty { "수치 변화 없음" }
        return state.copy(hp = hp, happiness = happy, money = money,
            love = love.copy(incident = incident.copy(applied = true)),
            log = state.log + ("연애 소식 · ${love.partner}\n" +
                option.text.replace("{name}", love.partner) + "\n$changes"))
    }

    fun breakUp(state: GameState, reason: LoveEndReason): GameState {
        val love = state.love ?: return state
        val record = LoveRecord(love.partner, love.startSemester, love.startWeek,
            state.semester, state.week, love.weeksTogether, love.affection, love.missedDates, reason)
        val happiness = if (reason == LoveEndReason.MISSED_DATES) (state.happiness - 30).coerceAtLeast(0)
            else state.happiness
        return state.copy(love = null, happiness = happiness,
            loveHistory = state.loveHistory.filterNot { it.key == record.key } + record,
            log = state.log + ("이별 · ${love.partner}\n${reason.label}" +
                if (reason == LoveEndReason.MISSED_DATES) "\n행복도 ${happiness - state.happiness}" else ""))
    }

    fun settleWeek(state: GameState): GameState {
        val love = state.love ?: return state
        val week = absoluteWeek(state)
        if (state.day != 7 || love.lastSettledWeek != week - 1) return state
        val missing = love.dateHandledWeek != week
        var game = state.copy(
            happiness = if (missing) (state.happiness - 6).coerceAtLeast(0) else state.happiness,
            love = love.copy(weeksTogether = love.weeksTogether + 1, lastSettledWeek = week,
                dateHandledWeek = week, missedDates = love.missedDates + if (missing) 1 else 0),
            log = if (missing) state.log + "데이트 미진행 · 누적 취소 ${love.missedDates + 1}/$MISSED_LIMIT" else state.log
        )
        if (requireNotNull(game.love).missedDates >= MISSED_LIMIT) game = breakUp(game, LoveEndReason.MISSED_DATES)
        return game
    }

    fun validateState(state: GameState, schedule: List<TimeSlot>) {
        require(state.loveCheckedSemesters.distinct().size == state.loveCheckedSemesters.size)
        require(state.loveCheckedSemesters.all { it in 1..8 })
        state.pendingConfession?.let { confession ->
            require(confession.semester == state.semester && state.semester >= FIRST_SEMESTER)
            require(state.week == 1 && state.day == 0 && !state.needsRegistration && state.semesterResult == null)
            require(confession.partner in LoveCatalog.partners && confession.semester in state.loveCheckedSemesters)
            require(state.pendingEvent == null && state.exam == null)
            require(if (confession.choice == LoveChoice.ACCEPT) state.love?.partner == confession.partner else state.love == null)
        }
        state.love?.let { love ->
            require(love.partner in LoveCatalog.partners && love.startSemester in FIRST_SEMESTER..state.semester)
            require(love.startWeek in 1..16 && love.affection in 0..100 && love.missedDates in 0..MISSED_LIMIT)
            require(love.weeksTogether >= 0 && love.dateIndex in -1..27)
            require(love.dateIndex == -1 || love.dateIndex % 4 != 3)
            if (schedule.size == 28 && state.day > 0 && love.dateIndex >= 0) {
                require(schedule[love.dateIndex].activity !in setOf(ActivityType.CLASS, ActivityType.TEAM))
            }
            love.incident?.let { require(it.day in 0..6 && it.optionIndex in LoveCatalog.incidents.indices) }
        }
    }
}
