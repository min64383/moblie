package com.example.mobile_project

import kotlin.random.Random
import kotlin.math.roundToInt

object EventEngine {
    // 하루 18%·주 2회에서는 거의 매주 딜레마가 떠서(학기당 약 15회)
    // 하루 10%·주 1회로 낮췄다. 평균 2주에 한 번, 학기당 약 8회.
    const val DAY_CHANCE = 0.10
    const val WEEKLY_LIMIT = 1

    fun eventForId(id: String): CampusEvent? = EventCatalog.events.find { it.id == id }

    fun absoluteDay(state: GameState): Int =
        ((state.semester - 1) * 16 + state.week - 1) * 7 + state.day

    fun multiplier(state: GameState, activity: ActivityType): Double {
        val day = absoluteDay(state)
        return state.eventBuffs
            .filter { it.activity == activity && day in it.startDay..it.endDay }
            .fold(1.0) { value, buff -> value * buff.multiplier }
    }

    fun visibleBuffs(state: GameState): List<EventBuff> =
        state.eventBuffs.filter { it.endDay >= absoluteDay(state) }

    fun dailyCandidates(state: GameState, schedule: List<TimeSlot>): List<CampusEvent> {
        if (state.day !in 0..6 || schedule.size != 28) return emptyList()
        val slots = schedule.subList(state.day * 4, state.day * 4 + 4)
        val records = state.eventHistory.filter { it.semester == state.semester }
        val usedThisWeek = records.count {
            it.week == state.week && eventForId(it.eventId)?.weeklyEvent == false
        }
        if (usedThisWeek >= WEEKLY_LIMIT) return emptyList()

        return EventCatalog.events.filter { event ->
            !event.weeklyEvent &&
                records.count { it.eventId == event.id } < event.semesterLimit &&
                when (event.id) {
                    "study" -> slots.any { it.activity == ActivityType.STUDY } &&
                        (state.week in 6..8 || state.week in 14..16)
                    "cls" -> slots.first().activity == ActivityType.CLASS
                    "love" -> state.love != null && slots.any { it.activity == ActivityType.DATE }
                    "team" -> state.teamProject != null && slots.any { it.activity == ActivityType.TEAM }
                    else -> slots.any { it.activity == event.activity }
                }
        }
    }

    private fun offer(state: GameState, event: CampusEvent, subjectId: String? = null): GameState {
        val run = CampusEventRun(event.id, state.semester, state.week, state.day, subjectId,
            contextName = if (event.id == "love") state.love?.partner.orEmpty() else "")
        return state.copy(
            pendingEvent = run,
            eventHistory = state.eventHistory + CampusEventRecord(
                state.semester, state.week, state.day, event.id
            )
        )
    }

    // 하루의 모든 진입 경로는 이 함수를 사용한다. 결과 화면을 다시 열어도 재추첨하지 않는다.
    fun advanceDay(
        state: GameState,
        schedule: List<TimeSlot>,
        subjects: List<Subject>,
        random: Random = Random.Default
    ): GameState {
        if (state.day !in 0..6 || state.needsRegistration ||
            state.semesterResult != null || state.exam != null || state.pendingEvent != null ||
            state.teamProject?.announced == false || state.pendingConfession != null ||
            state.pendingMiniGame != null || state.life.pendingMorning != null) return state
        require(schedule.size == 28)

        val today = absoluteDay(state)
        val clean = state.copy(eventBuffs = visibleBuffs(state))
        if (state.eventCheckedDay == today) {
            return GameEngine.advanceDay(clean, schedule, subjects)
        }

        val checked = clean.copy(eventCheckedDay = today)
        val candidates = dailyCandidates(checked, schedule)
        if (candidates.isNotEmpty() && random.nextDouble() < DAY_CHANCE) {
            val event = candidates[random.nextInt(candidates.size)]
            val subjectId = when (event.id) {
                "cls" -> schedule[state.day * 4].subjectId
                "team" -> state.teamProject?.subjectId
                else -> null
            }
            return offer(checked, event, subjectId)
        }
        return GameEngine.advanceDay(checked, schedule, subjects)
    }

    // 원본처럼 MT는 1학년 각 학기의 2주차, 축제는 매 학기 9주차 정산 뒤에 열린다.
    fun startWeeklyEvent(state: GameState): GameState {
        if (state.day != 7 || state.pendingEvent != null || state.pendingConfession != null || state.exam != null ||
            state.needsRegistration || state.semesterResult != null) return state

        val id = when {
            state.week == 2 && state.semester <= 2 -> "mt"
            state.week == 9 -> "fest"
            else -> return state
        }
        if (state.eventHistory.any {
                it.semester == state.semester && it.eventId == id
            }) return state
        return offer(state, requireNotNull(eventForId(id)))
    }

    fun canChoose(state: GameState, option: EventOption): Boolean =
        option.effect.money >= 0 || state.money + option.effect.money >= -50_000

    fun effectDescriptions(option: EventOption): List<String> = buildList {
        val effect = option.effect
        listOf("체력" to effect.hp, "행복도" to effect.happiness,
            "돈" to effect.money, "인맥" to effect.network,
            "매력" to effect.charm, "스펙" to effect.spec,
            "오전 수업 과목 공부량" to effect.classStudy).forEach { (name, delta) ->
            if (delta != 0) add("$name ${if (delta > 0) "+" else ""}$delta${if (name == "돈") "원" else ""}")
        }
        option.buff?.let { add(it.label) }
        if (effect.teamBonus != 0) add("현재 과제 가산점 ${if (effect.teamBonus > 0) "+" else ""}${effect.teamBonus}점")
        if (effect.gpaAdjustment != 0.0) add("학기 평점 보정 ${if (effect.gpaAdjustment > 0) "+" else ""}${effect.gpaAdjustment}")
        if (effect.affection != 0) add("애정도 ${if (effect.affection > 0) "+" else ""}${effect.affection}")
        if (effect.endLove) add("연애 종료")
        if (isEmpty()) add("수치 변화 없음")
    }

    private fun matches(state: GameState, run: CampusEventRun, key: String): Boolean =
        run.key == key && run.semester == state.semester &&
            run.week == state.week && run.day == state.day

    fun choose(state: GameState, key: String, index: Int): GameState {
        val run = state.pendingEvent ?: return state
        if (run.phase != EventPhase.CHOICE || !matches(state, run, key)) return state
        val event = eventForId(run.eventId) ?: return state
        val option = event.options.getOrNull(index) ?: return state
        if (!canChoose(state, option)) return state

        val effect = option.effect
        val subjectId = run.subjectId?.takeIf { it in state.study }
        val study = if (subjectId != null && effect.classStudy != 0) {
            state.study + (subjectId to (state.study.getValue(subjectId) + effect.classStudy).coerceAtLeast(0))
        } else state.study
        var changed = state.copy(
            hp = (state.hp + effect.hp).coerceIn(0, state.maxHp),
            happiness = (state.happiness + effect.happiness).coerceIn(0, 100),
            money = (state.money + effect.money).coerceAtLeast(-50_000),
            network = (state.network + effect.network).coerceIn(0, 100),
            charm = (state.charm + effect.charm).coerceIn(0, 100),
            spec = (state.spec + effect.spec).coerceIn(0, 100),
            study = study,
            love = state.love?.let { it.copy(affection = (it.affection + effect.affection).coerceIn(0, 100)) },
            teamProject = if (effect.teamBonus != 0) state.teamProject?.let {
                it.copy(bonus = (it.bonus + effect.teamBonus).coerceIn(0, 10))
            } else state.teamProject,
            semesterGpaAdjustment = ((state.semesterGpaAdjustment + effect.gpaAdjustment) * 100).roundToInt() / 100.0,
            eventHistory = state.eventHistory.map {
                if (it.key == key) it.copy(selectedIndex = index) else it
            }
        )

        if (effect.endLove) changed = LoveEngine.breakUp(changed, LoveEndReason.CHOICE)

        option.buff?.let { spec ->
            val weekStart = absoluteDay(state) - state.day
            val start = if (spec.scope == EventBuffScope.NEXT_WEEK) weekStart + 7 else absoluteDay(state)
            val end = if (spec.scope == EventBuffScope.NEXT_WEEK) weekStart + 13 else weekStart + 6
            changed = changed.copy(eventBuffs = changed.eventBuffs +
                EventBuff(spec.activity, spec.multiplier, start, end, spec.label))
        }

        val changes = buildList {
            listOf("체력" to changed.hp - state.hp,
                "행복도" to changed.happiness - state.happiness,
                "돈" to changed.money - state.money,
                "인맥" to changed.network - state.network,
                "매력" to changed.charm - state.charm,
                "스펙" to changed.spec - state.spec).forEach { (name, delta) ->
                if (delta != 0) add("$name ${if (delta > 0) "+" else ""}$delta${if (name == "돈") "원" else ""}")
            }
            if (subjectId != null) {
                val delta = changed.study.getValue(subjectId) - state.study.getValue(subjectId)
                if (delta != 0) add("${subjectForId(subjectId)?.name ?: subjectId} 공부량 ${if (delta > 0) "+" else ""}$delta")
            }
            option.buff?.let { add(it.label) }
            val bonusDelta = (changed.teamProject?.bonus ?: 0) - (state.teamProject?.bonus ?: 0)
            if (bonusDelta != 0) add("현재 과제 가산점 ${if (bonusDelta > 0) "+" else ""}${bonusDelta}점")
            val gpaDelta = ((changed.semesterGpaAdjustment - state.semesterGpaAdjustment) * 100).roundToInt() / 100.0
            if (gpaDelta != 0.0) add("학기 평점 보정 ${if (gpaDelta > 0) "+" else ""}$gpaDelta")
            val beforeLove = state.love
            val afterLove = changed.love
            if (beforeLove != null && afterLove != null) {
                val delta = afterLove.affection - beforeLove.affection
                if (delta != 0) add("애정도 ${if (delta > 0) "+" else ""}$delta")
            }
            if (beforeLove != null && afterLove == null) add("연애 종료 · ${beforeLove.partner}")
            if (isEmpty()) add("수치 변화 없음")
        }
        return changed.copy(pendingEvent = run.copy(
            phase = EventPhase.RESULT, selectedIndex = index, changes = changes
        ))
    }

    // 결과 확인은 한 번만 처리한다. 일간 이벤트라면 이어서 그날 시간표를 실행한다.
    fun continueEvent(
        state: GameState,
        schedule: List<TimeSlot>,
        subjects: List<Subject>,
        key: String,
        random: Random = Random.Default
    ): GameState {
        val run = state.pendingEvent ?: return state
        if (run.phase != EventPhase.RESULT || !matches(state, run, key)) return state
        val event = eventForId(run.eventId) ?: return state
        val option = event.options.getOrNull(run.selectedIndex) ?: return state
        val summary = "이벤트 · ${event.title}\n${option.label}\n${run.changes.joinToString(" · ")}" 
        val cleared = state.copy(pendingEvent = null)
        val progressed = if (event.weeklyEvent) cleared else {
            GameEngine.advanceDay(cleared, LoveEngine.syncSchedule(cleared, schedule), subjects)
        }
        return LifeEngine.settleWeek(progressed.copy(log = listOf(summary) + progressed.log), random)
    }

    // 불러온 이벤트가 현재 날짜에 속하는지 확인한다.
    fun validatePending(state: GameState) {
        val run = state.pendingEvent ?: return
        val event = requireNotNull(eventForId(run.eventId))
        require(matches(state, run, run.key))
        require(!state.needsRegistration && state.semesterResult == null && state.exam == null)
        require(if (event.weeklyEvent) run.day == 7 else run.day in 0..6)
        require(run.phase == EventPhase.CHOICE && run.selectedIndex == -1 ||
            run.phase == EventPhase.RESULT && run.selectedIndex in event.options.indices)
        if (run.subjectId != null) require(run.subjectId in state.study)
        if (run.eventId == "team") require(state.teamProject != null && run.subjectId == state.teamProject.subjectId)
        if (run.eventId == "love") {
            require(run.contextName in LoveCatalog.partners)
            if (run.phase == EventPhase.CHOICE || !event.options[run.selectedIndex].effect.endLove) {
                require(state.love?.partner == run.contextName)
            }
        }
    }
}
