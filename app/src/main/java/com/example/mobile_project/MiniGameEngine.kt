package com.example.mobile_project

import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

object MiniGameEngine {
    val chances = mapOf(ActivityType.STUDY to 0.16, ActivityType.WORK to 0.18,
        ActivityType.TUTOR to 0.30, ActivityType.CAREER to 0.20,
        ActivityType.RUN to 0.25, ActivityType.GYM to 0.30, ActivityType.PT to 0.30)

    fun kindsFor(activity: ActivityType): List<MiniKind> = when (activity) {
        ActivityType.STUDY -> listOf(MiniKind.FOCUS, MiniKind.MEMORY, MiniKind.CALC)
        ActivityType.WORK -> listOf(MiniKind.ORDER, MiniKind.CHANGE)
        ActivityType.TUTOR -> listOf(MiniKind.TUTOR)
        ActivityType.CAREER -> listOf(MiniKind.FOCUS, MiniKind.INTERVIEW)
        ActivityType.RUN, ActivityType.GYM, ActivityType.PT -> listOf(MiniKind.TIMING, MiniKind.HOLD)
        else -> emptyList()
    }

    private fun duration(kind: MiniKind): Long = when (kind) {
        MiniKind.FOCUS, MiniKind.CHANGE -> 9_000L
        MiniKind.CALC, MiniKind.TUTOR -> 11_000L
        MiniKind.INTERVIEW -> 12_000L
        MiniKind.HOLD -> 6_000L
        else -> 0L
    }

    private fun shuffleQuestion(question: MiniQuestion, random: Random): MiniQuestion {
        val answer = question.options[question.correctIndex]
        val options = question.options.shuffled(random)
        return MiniQuestion(question.prompt, options, options.indexOf(answer))
    }

    internal fun buildRun(
        state: GameState, schedule: List<TimeSlot>, block: Int, kind: MiniKind, random: Random
    ): MiniGameRun {
        require(state.day in 0..6 && block in 0..3 && schedule.size == 28)
        val slot = schedule[state.day * 4 + block]
        require(kind in kindsFor(slot.activity) && AcademicEngine.canUseActivity(state, slot.activity))
        var run = MiniGameRun(state.semester, state.week, state.day, block, slot.activity, kind,
            slot.subjectId, durationMs = duration(kind))
        run = when (kind) {
            MiniKind.FOCUS -> run.copy(choices = (1..9).map { it.toString() }.shuffled(random))
            MiniKind.MEMORY, MiniKind.ORDER -> {
                val words = if (kind == MiniKind.MEMORY) MiniGameCatalog.words else MiniGameCatalog.orders
                val count = if (kind == MiniKind.MEMORY) 4 else 3
                val target = words.shuffled(random).take(count)
                val pool = (target + words.filterNot { it in target }.shuffled(random).take(count)).shuffled(random)
                run.copy(choices = pool, sequence = target.map(pool::indexOf))
            }
            MiniKind.CALC -> run.copy(questions = List(3) {
                val a = random.nextInt(12, 52)
                val b = random.nextInt(3, 15)
                val op = listOf("+", "−", "×").random(random)
                val answer = when (op) { "+" -> a + b; "−" -> a - b; else -> a * b }
                val options = listOf(answer, answer + listOf(1, 2, 3, -1, -2).random(random),
                    answer + listOf(7, -7, 11, -11).random(random), answer + listOf(20, -20, 13).random(random))
                shuffleQuestion(MiniQuestion("$a $op $b = ?", options.map(Int::toString), 0), random)
            })
            MiniKind.CHANGE -> {
                val price = random.nextInt(3, 12) * 1_000 + listOf(0, 500).random(random)
                val paid = ceil((price + listOf(1_000, 2_000, 5_000, 10_000).random(random)) / 1_000.0).toInt() * 1_000
                val answer = paid - price
                val options = listOf(answer, answer + 500, answer - 500, answer + 1_000).filter { it > 0 }.distinct()
                run.copy(questions = listOf(shuffleQuestion(MiniQuestion(
                    "주문 금액 ${price}원 · 받은 돈 ${paid}원\n거스름돈은 얼마일까요?", options.map(Int::toString), 0), random)))
            }
            MiniKind.TUTOR -> run.copy(questions = listOf(shuffleQuestion(MiniGameCatalog.tutorQuestions.random(random), random)),
                presetTip = listOf(30_000, 50_000, 50_000, 80_000).random(random))
            MiniKind.INTERVIEW -> run.copy(questions = listOf(shuffleQuestion(MiniGameCatalog.interviewQuestions.random(random), random)))
            MiniKind.TIMING, MiniKind.HOLD -> run
        }
        return run
    }

    fun prepareDay(state: GameState, schedule: List<TimeSlot>, random: Random = Random.Default): GameState {
        if (state.day !in 0..6 || state.needsRegistration || state.semesterResult != null || state.exam != null ||
            state.pendingEvent != null || state.pendingConfession != null || state.pendingMiniGame != null ||
            state.life.pendingMorning != null ||
            state.teamProject?.announced == false || schedule.size != 28) return state
        val today = EventEngine.absoluteDay(state)
        if (state.miniCheckedDay == today) return state
        val checked = state.copy(miniCheckedDay = today)
        if (state.skipMiniGames) return checked
        val candidates = (0..3).filter { block ->
            val activity = schedule[state.day * 4 + block].activity
            AcademicEngine.canUseActivity(state, activity) && chances[activity]?.let { random.nextDouble() < it } == true
        }
        if (candidates.isEmpty()) return checked
        val block = candidates.random(random)
        val activity = schedule[state.day * 4 + block].activity
        return checked.copy(pendingMiniGame = buildRun(checked, schedule, block, kindsFor(activity).random(random), random))
    }

    private fun current(state: GameState, run: MiniGameRun, key: String): Boolean =
        run.key == key && run.semester == state.semester && run.week == state.week && run.day == state.day

    fun begin(state: GameState, key: String, nowMs: Long): GameState {
        val run = state.pendingMiniGame ?: return state
        if (!current(state, run, key) || run.phase != MiniPhase.READY) return state
        val preview = when (run.kind) { MiniKind.MEMORY -> 3_900L; MiniKind.ORDER -> 3_000L; else -> 0L }
        return state.copy(pendingMiniGame = run.copy(
            phase = if (preview > 0) MiniPhase.MEMORIZE else MiniPhase.PLAY,
            startedAtMs = nowMs, deadlineMs = if (run.durationMs > 0) nowMs + run.durationMs else 0L,
            previewUntilMs = if (preview > 0) nowMs + preview else 0L
        ))
    }

    fun marker(run: MiniGameRun, nowMs: Long): Float {
        val time = (nowMs - run.startedAtMs).coerceAtLeast(0L) % 2_000L
        return if (time <= 1_000L) time / 10f else (2_000L - time) / 10f
    }

    private fun finish(state: GameState, run: MiniGameRun, nowMs: Long,
        failure: Boolean = false, skipped: Boolean = false): GameState {
        val remaining = if (run.durationMs > 0) ((run.deadlineMs - nowMs).toDouble() / run.durationMs).coerceIn(0.0, 1.0) else 0.0
        val reward: MiniReward
        val title: String
        if (skipped) {
            reward = MiniReward(description = "건너뜀 · 추가 보너스 없음")
            title = "미니게임 건너뜀"
        } else when (run.kind) {
            MiniKind.FOCUS -> {
                val success = run.progress == 9 && !failure
                title = if (success) "집중 성공" else "집중 실패"
                reward = if (run.activity == ActivityType.CAREER) {
                    val spec = if (success) 1 + (remaining * 2).roundToInt() else 0
                    MiniReward(spec = spec, description = "스펙 +$spec 추가")
                } else {
                    val multiplier = if (success) 1.0 + remaining * 0.6 else 0.85
                    MiniReward(studyMultiplier = multiplier, description = "이 공부 칸의 공부량 ${(multiplier * 100).roundToInt()}%")
                }
            }
            MiniKind.MEMORY -> {
                val success = run.progress == run.sequence.size && !failure
                title = if (success) "암기 성공" else "순서 실수"
                val mul = if (success) 1.5 else 0.8
                reward = MiniReward(studyMultiplier = mul, description = "이 공부 칸의 공부량 ${(mul * 100).roundToInt()}%")
            }
            MiniKind.CALC -> {
                val mul = 0.8 + run.correct * 0.25
                title = "${run.correct}/3 정답"
                reward = MiniReward(studyMultiplier = mul, description = "이 공부 칸의 공부량 ${(mul * 100).roundToInt()}%")
            }
            MiniKind.ORDER -> {
                val hits = run.selected.count { it in run.sequence }
                title = "$hits/3 주문 기억"
                reward = MiniReward(money = hits * 4_000, description = "알바 팁 +${hits * 4_000}원")
            }
            MiniKind.CHANGE -> {
                val success = run.correct == 1
                title = if (success) "정확한 계산" else "계산 실수"
                reward = MiniReward(money = if (success) 6_000 else 0, happiness = if (success) 0 else -4,
                    description = if (success) "알바 팁 +6,000원" else "행복도 -4")
            }
            MiniKind.TUTOR -> {
                val success = run.correct == 1
                title = if (success) "설명 성공" else "설명이 아쉬워요"
                reward = MiniReward(money = if (success) run.presetTip else 0, happiness = if (success) 4 else -5,
                    description = if (success) "과외 보너스 +${run.presetTip}원 · 행복도 +4" else "행복도 -5")
            }
            MiniKind.INTERVIEW -> {
                title = if (run.correct == 1) "좋은 답변" else "아쉬운 답변"
                reward = MiniReward(spec = if (run.correct == 1) 3 else 0,
                    description = if (run.correct == 1) "스펙 +3 추가" else "추가 보너스 없음")
            }
            MiniKind.TIMING -> {
                title = "${run.correct}/3 성공"
                reward = MiniReward(maxHp = run.correct, description = "최대 체력 +${run.correct} 추가")
            }
            MiniKind.HOLD -> {
                val hp = (run.progress.coerceAtMost(40) / 40.0 * 3).roundToInt()
                title = "${run.progress}/40회"
                reward = MiniReward(maxHp = hp, description = "최대 체력 +$hp 추가")
            }
        }
        return state.copy(pendingMiniGame = run.copy(phase = MiniPhase.RESULT, reward = reward,
            resultTitle = title, revision = run.revision + 1))
    }

    fun refresh(state: GameState, nowMs: Long): GameState {
        val run = state.pendingMiniGame ?: return state
        if (run.phase == MiniPhase.MEMORIZE && (nowMs >= run.previewUntilMs || nowMs < run.startedAtMs)) {
            return state.copy(pendingMiniGame = run.copy(phase = MiniPhase.PLAY, revision = run.revision + 1))
        }
        if (run.phase == MiniPhase.PLAY && run.durationMs > 0 &&
            (nowMs >= run.deadlineMs || nowMs < run.startedAtMs)) return finish(state, run, nowMs, failure = true)
        return state
    }

    fun input(state: GameState, key: String, value: Int, expectedRevision: Int, nowMs: Long): GameState {
        val game = refresh(state, nowMs)
        val run = game.pendingMiniGame ?: return game
        if (!current(game, run, key) || run.phase != MiniPhase.PLAY || run.revision != expectedRevision) return game
        val revision = run.revision + 1
        return when (run.kind) {
            MiniKind.FOCUS -> {
                if (run.choices.getOrNull(value)?.toIntOrNull() != run.progress + 1) return game
                val next = run.copy(progress = run.progress + 1, revision = revision)
                if (next.progress == 9) finish(game, next, nowMs) else game.copy(pendingMiniGame = next)
            }
            MiniKind.MEMORY -> {
                if (value !in run.choices.indices) return game
                if (value != run.sequence[run.progress]) return finish(game, run, nowMs, failure = true)
                val next = run.copy(progress = run.progress + 1, revision = revision)
                if (next.progress == run.sequence.size) finish(game, next, nowMs) else game.copy(pendingMiniGame = next)
            }
            MiniKind.ORDER -> {
                if (value !in run.choices.indices) return game
                val selected = if (value in run.selected) run.selected - value else {
                    if (run.selected.size >= 3) return game
                    run.selected + value
                }
                game.copy(pendingMiniGame = run.copy(selected = selected, revision = revision))
            }
            MiniKind.CALC, MiniKind.CHANGE, MiniKind.TUTOR, MiniKind.INTERVIEW -> {
                val question = run.questions.getOrNull(run.progress) ?: return game
                if (value !in question.options.indices) return game
                val next = run.copy(progress = run.progress + 1,
                    correct = run.correct + if (value == question.correctIndex) 1 else 0, revision = revision)
                if (next.progress == run.questions.size) finish(game, next, nowMs) else game.copy(pendingMiniGame = next)
            }
            MiniKind.TIMING -> {
                val next = run.copy(progress = run.progress + 1, revision = revision,
                    correct = run.correct + if (marker(run, nowMs) in 39f..61f) 1 else 0, startedAtMs = nowMs)
                if (next.progress == 3) finish(game, next, nowMs) else game.copy(pendingMiniGame = next)
            }
            MiniKind.HOLD -> {
                val next = run.copy(progress = (run.progress + 1).coerceAtMost(40), revision = revision)
                if (next.progress == 40) finish(game, next, nowMs) else game.copy(pendingMiniGame = next)
            }
        }
    }

    fun submitOrders(state: GameState, key: String, nowMs: Long): GameState {
        val run = state.pendingMiniGame ?: return state
        if (!current(state, run, key) || run.kind != MiniKind.ORDER || run.phase != MiniPhase.PLAY) return state
        return finish(state, run, nowMs)
    }

    fun skipOrForfeit(state: GameState, key: String, nowMs: Long): GameState {
        val game = refresh(state, nowMs)
        val run = game.pendingMiniGame ?: return game
        if (!current(game, run, key) || run.phase == MiniPhase.RESULT) return game
        return finish(game, run, nowMs, failure = true, skipped = run.phase == MiniPhase.READY)
    }

    fun bonusFor(state: GameState, index: Int, slot: TimeSlot): MiniReward? {
        val bonus = state.miniBonus ?: return null
        return bonus.reward.takeIf { bonus.semester == state.semester && bonus.week == state.week &&
            bonus.day == state.day && bonus.block == index % 4 && index / 4 == state.day &&
            bonus.activity == slot.activity && (slot.activity != ActivityType.STUDY || bonus.subjectId == slot.subjectId) }
    }

    fun validateState(state: GameState, schedule: List<TimeSlot>) {
        val run = state.pendingMiniGame
        if (run != null) {
            require(current(state, run, run.key) && state.day in 0..6 && run.block in 0..3)
            require(schedule.size == 28 && schedule[state.day * 4 + run.block].activity == run.activity)
            require(state.pendingEvent == null && state.pendingConfession == null && state.exam == null &&
                !state.needsRegistration && state.semesterResult == null && state.miniBonus == null)
            require(run.kind in kindsFor(run.activity) && run.durationMs == duration(run.kind))
            require(state.miniCheckedDay == EventEngine.absoluteDay(state))
            require(run.progress >= 0 && run.correct >= 0 && run.revision >= 0)
            require((run.phase == MiniPhase.RESULT) == (run.reward != null))
            require(run.questions.all { it.correctIndex in it.options.indices && it.options.distinct().size == it.options.size })
            when (run.kind) {
                MiniKind.FOCUS -> require(run.choices.toSet() == (1..9).map(Int::toString).toSet() && run.choices.size == 9 && run.progress <= 9)
                MiniKind.MEMORY, MiniKind.ORDER -> {
                    require(run.sequence.size == if (run.kind == MiniKind.MEMORY) 4 else 3)
                    require(run.choices.distinct().size == run.choices.size && run.sequence.all { it in run.choices.indices })
                    require(run.sequence.distinct().size == run.sequence.size && run.progress <= run.sequence.size)
                    require(run.selected.distinct().size == run.selected.size && run.selected.size <= 3 && run.selected.all { it in run.choices.indices })
                }
                MiniKind.CALC -> require(run.questions.size == 3 && run.progress <= 3 && run.correct <= run.progress)
                MiniKind.CHANGE, MiniKind.TUTOR, MiniKind.INTERVIEW -> require(run.questions.size == 1 && run.progress <= 1 && run.correct <= run.progress)
                MiniKind.TIMING -> require(run.progress <= 3 && run.correct <= run.progress)
                MiniKind.HOLD -> require(run.progress <= 40)
            }
        }
        state.miniBonus?.let { bonus ->
            require(state.day in 0..6 && schedule.size == 28 && bonus.block in 0..3)
            require(bonusFor(state, state.day * 4 + bonus.block, schedule[state.day * 4 + bonus.block]) != null)
        }
    }
}

object DayFlowEngine {
    fun advanceDay(state: GameState, schedule: List<TimeSlot>, subjects: List<Subject>,
        random: Random = Random.Default): GameState {
        if (state.pendingMiniGame != null || state.pendingEvent != null || state.life.pendingMorning != null) return state
        val morning = LifeEngine.prepareMorning(state, schedule, random)
        if (morning.life.pendingMorning != null) return morning
        val prepared = MiniGameEngine.prepareDay(morning, schedule, random)
        if (prepared.pendingMiniGame != null) return prepared
        return LifeEngine.settleWeek(EventEngine.advanceDay(prepared, schedule, subjects, random), random)
    }

    fun continueMini(state: GameState, schedule: List<TimeSlot>, subjects: List<Subject>,
        key: String, random: Random = Random.Default): GameState {
        val run = state.pendingMiniGame ?: return state
        if (run.key != key || run.phase != MiniPhase.RESULT || run.semester != state.semester ||
            run.week != state.week || run.day != state.day) return state
        val reward = run.reward ?: return state
        val ready = state.copy(pendingMiniGame = null, miniBonus = MiniGameBonus(run.key,
            run.semester, run.week, run.day, run.block, run.activity, run.subjectId, reward))
        return advanceDay(ready, schedule, subjects, random)
    }
}
