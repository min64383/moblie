package com.example.mobile_project

import kotlin.math.roundToInt

object GameEngine {
    // 공부 1칸 기본 공부량 (효율 배율 적용 전)과 수업 1칸 공부량 (원본 HTML과 같은 값)
    const val STUDY_GAIN = 15
    const val CLASS_GAIN = 5
    val dayNames = listOf("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일")
    private val blockNames = listOf("오전", "오후", "저녁", "밤")

    private data class Effect(
        val hp: Int = 0,
        val happiness: Int = 0,
        val money: Int = 0,
        val network: Int = 0,
        val charm: Int = 0,
        val maxHp: Int = 0,
        val spec: Int = 0
    )

    private val effects = mapOf(
        ActivityType.EMPTY to Effect(),
        ActivityType.STUDY to Effect(hp = -9, happiness = -3),
        ActivityType.WORK to Effect(hp = -13, happiness = -6, money = 25_000),
        ActivityType.FUN to Effect(hp = 2, happiness = 18, money = -7_000),
        ActivityType.REST to Effect(hp = 11, happiness = 4),
        ActivityType.CLUB to Effect(hp = -7, happiness = 10, money = -3_000, network = 4),
        ActivityType.RUN to Effect(hp = -10, happiness = 3, maxHp = 1),
        ActivityType.GYM to Effect(hp = -12, maxHp = 3, charm = 2, money = -15_000),
        ActivityType.PT to Effect(hp = -10, happiness = 2, maxHp = 5, charm = 4, money = -40_000),
        ActivityType.SLEEP to Effect(hp = 17),
        ActivityType.CLASS to Effect(hp = -6, happiness = -2),
        ActivityType.CAREER to Effect(hp = -11, happiness = -7, spec = 1),
        ActivityType.TUTOR to Effect(hp = -8, happiness = -2, money = 45_000),
        ActivityType.TEAM to Effect(hp = -9, happiness = -5),
        ActivityType.DATE to Effect(happiness = 25, money = -20_000)
    )

    fun newGame(subjectIds: List<String>): GameState {
        require(subjectIds.size == 3 && subjectIds.distinct().size == 3)
        return GameState(study = subjectIds.associateWith { 0 })
    }

    private fun coefficient(value: Double): Double = when {
        value >= 70 -> 1.2
        value >= 40 -> 1.0
        value >= 20 -> 0.7
        else -> 0.4
    }

    fun advanceDay(
        state: GameState,
        schedule: List<TimeSlot>,
        subjects: List<Subject>
    ): GameState {
        // 일요일 정산이 끝나면 진행을 막아 생활비 중복 처리를 방지한다.
        if (state.day >= 7 || state.needsRegistration || state.semesterResult != null ||
            state.pendingEvent != null || state.teamProject?.announced == false ||
            state.pendingConfession != null || state.pendingMiniGame != null || state.life.pendingMorning != null) return state
        require(state.day >= 0 && schedule.size == 28)

        var current = state
        val logs = mutableListOf<String>()
        current.life.morningNotice?.let { logs += LifeEngine.attendanceMessage(it) }

        repeat(4) { block ->
            val slot = schedule[state.day * 4 + block]
            val effect = effects.getValue(slot.activity)
            val mini = MiniGameEngine.bonusFor(current, state.day * 4 + block, slot)
            val before = current
            val subject = subjects.find { it.id == slot.subjectId }
            val actionName = if (slot.activity == ActivityType.EMPTY) {
                "아무 활동 없음"
            } else {
                listOfNotNull(subject?.name, slot.activity.label).joinToString(" ")
            }

            if (!AcademicEngine.canUseActivity(current, slot.activity)) {
                logs += "${blockNames[block]} · $actionName 취소\n아직 이용 조건을 충족하지 못했어요." +
                    if (mini != null) "\n미니게임 보너스도 적용되지 않았어요." else ""
                return@repeat
            }

            if (slot.activity == ActivityType.DATE) {
                val outcome = LoveEngine.performDate(current, state.day * 4 + block)
                current = outcome.game
                logs += "${blockNames[block]} · ${outcome.message}"
                return@repeat
            }

            // 비용이 있는 활동은 빚 한도(-50,000원)를 넘으면 취소한다.
            if (effect.money < 0 && current.money + effect.money < -50_000) {
                logs += "${blockNames[block]} · $actionName 취소\n잔액 한도 때문에 진행하지 못했어요." +
                    if (mini != null) "\n미니게임 보너스도 적용되지 않았어요." else ""
                return@repeat
            }

            val studyGain = if (subject == null) {
                0
            } else {
                when (slot.activity) {
                    ActivityType.STUDY -> {
                        val efficiency = coefficient(current.hp * 100.0 / current.maxHp) *
                            coefficient(current.happiness.toDouble())
                        val base = (STUDY_GAIN * efficiency * LifeEngine.studyMultiplier(current) * EventEngine.multiplier(current, ActivityType.STUDY) *
                            (mini?.studyMultiplier ?: 1.0)).roundToInt()
                        TeamEngine.adjustStudyGain(current, subject.id, base)
                    }
                    ActivityType.CLASS -> CLASS_GAIN
                    else -> 0
                }
            }

            val recoveryBoost = when {
                slot.activity == ActivityType.FUN && current.happiness < 30 -> 1.6
                slot.activity == ActivityType.REST &&
                    current.hp * 100.0 / current.maxHp < 30 -> 1.6
                else -> 1.0
            }

            val newMaxHp = (current.maxHp + effect.maxHp + (mini?.maxHp ?: 0)).coerceIn(50, 150)
            val clubMultiplier = if (slot.activity == ActivityType.CLUB) {
                EventEngine.multiplier(current, ActivityType.CLUB)
            } else 1.0
            val maxHpGain = newMaxHp - current.maxHp
            val newStudy = if (subject != null && studyGain > 0) {
                current.study + (subject.id to ((current.study[subject.id] ?: 0) + studyGain))
            } else {
                current.study
            }

            current = current.copy(
                hp = (current.hp + (effect.hp * recoveryBoost).roundToInt() + maxHpGain)
                    .coerceIn(0, newMaxHp),
                maxHp = newMaxHp,
                happiness = (current.happiness + (effect.happiness * recoveryBoost * clubMultiplier).roundToInt() +
                    (mini?.happiness ?: 0))
                    .coerceIn(0, 100),
                money = (current.money + effect.money + (mini?.money ?: 0)).coerceAtLeast(-50_000),
                network = (current.network + (effect.network * clubMultiplier).roundToInt()).coerceIn(0, 100),
                charm = (current.charm + effect.charm).coerceIn(0, 100),
                spec = (current.spec + effect.spec + (mini?.spec ?: 0)).coerceIn(0, 100),
                study = newStudy
            )

            val changes = listOf(
                "체력" to current.hp - before.hp,
                "최대 체력" to current.maxHp - before.maxHp,
                "행복도" to current.happiness - before.happiness,
                "돈" to current.money - before.money,
                "인맥" to current.network - before.network,
                "매력" to current.charm - before.charm,
                "스펙" to current.spec - before.spec
            ).filter { it.second != 0 }
                .map { (name, delta) ->
                    val sign = if (delta > 0) "+" else ""
                    "$name $sign$delta${if (name == "돈") "원" else ""}"
                }
                .toMutableList()

            if (studyGain > 0) changes += "공부량 +$studyGain"
            val summary = changes.joinToString(" · ").ifEmpty { "변화 없음" }
            logs += "${blockNames[block]} · $actionName\n$summary"
            if (mini != null) logs += "미니게임 · ${mini.description}"
        }

        current = TeamEngine.applyDayIncidents(current.copy(log = logs.toList()))
        current = LoveEngine.applyDayIncident(current)
        current = current.copy(day = state.day + 1, miniBonus = null,
            life = current.life.copy(morningNotice = null))
        return if (current.day == 7) settleWeek(current, schedule) else current
    }

    private fun settleWeek(state: GameState, schedule: List<TimeSlot>): GameState {
        val sleepCount = schedule.count { it.activity == ActivityType.SLEEP }
        val sleepShortage = (5 - sleepCount).coerceAtLeast(0)
        val newMaxHp = (state.maxHp - sleepShortage * 2).coerceAtLeast(50)
        val weeklyCost = 25_000 + ((state.semester - 1) / 2) * 5_000
        val logs = state.log.toMutableList()
        logs += "주간 결산 · 생활비 ${weeklyCost}원 청구"
        logs += "주간 인맥·매력 각각 1 감소 (최소 0)"

        if (state.money - weeklyCost < -50_000) {
            logs += "잔액이 빚 한도 -50,000원에 도달했어요."
        }
        if (sleepShortage > 0) {
            logs += "수면 $sleepCount/5칸 · 최대 체력 ${newMaxHp - state.maxHp}"
        }

        val settled = state.copy(
            hp = state.hp.coerceAtMost(newMaxHp),
            maxHp = newMaxHp,
            money = (state.money - weeklyCost).coerceAtLeast(-50_000),
            network = (state.network - 1).coerceAtLeast(0),
            charm = (state.charm - 1).coerceAtLeast(0),
            log = logs.toList()
        )
        return LoveEngine.settleWeek(TeamEngine.settleWeek(settled))
    }

    fun nextWeek(state: GameState): GameState {
        if (state.pendingEvent != null || state.pendingConfession != null ||
            state.pendingMiniGame != null || state.miniBonus != null || state.life.pendingMorning != null) return state
        if (state.day != 7) return state
        if (state.week >= 16 || state.semesterResult != null) return state
        if (ExamEngine.pendingExam(state) != null) return state
        if (state.exam != null && state.exam.phase != ExamPhase.RESULT) return state
        val next = state.copy(week = state.week + 1, day = 0, log = emptyList(), exam = null)
        return next.copy(eventBuffs = EventEngine.visibleBuffs(next))
    }
}
