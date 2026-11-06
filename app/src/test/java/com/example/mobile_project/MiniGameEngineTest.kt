package com.example.mobile_project

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class MiniGameEngineTest {
    private val subjects = subjectsForSemester(1).take(3)
    private val id = subjects.first().id
    private val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
    private val never = object : Random() {
        override fun nextBits(bitCount: Int): Int = if (bitCount == 0) 0 else -1 ushr (32 - bitCount)
    }
    private fun fresh() = GameEngine.newGame(subjects.map { it.id })
    private fun plan(activity: ActivityType = ActivityType.STUDY, block: Int = 0): List<TimeSlot> =
        List(28) { if (it == block) TimeSlot(activity, if (activity == ActivityType.STUDY) id else null) else TimeSlot() }
    private fun ready(kind: MiniKind, activity: ActivityType = when (kind) {
        MiniKind.ORDER, MiniKind.CHANGE -> ActivityType.WORK
        MiniKind.TUTOR -> ActivityType.TUTOR
        MiniKind.INTERVIEW -> ActivityType.CAREER
        MiniKind.TIMING, MiniKind.HOLD -> ActivityType.RUN
        else -> ActivityType.STUDY
    }): GameState {
        var state = fresh()
        if (activity == ActivityType.CAREER) state = state.copy(semester = 5)
        if (activity == ActivityType.TUTOR) state = state.copy(transcript = listOf(
            SemesterResult(1, subjects.map { SubjectGrade(it.id, 90, "A0", 4.0) })))
        val run = MiniGameEngine.buildRun(state, plan(activity), 0, kind, Random(42))
        return state.copy(pendingMiniGame = run, miniCheckedDay = EventEngine.absoluteDay(state))
    }
    private fun play(state: GameState): GameState {
        val begun = MiniGameEngine.begin(state, requireNotNull(state.pendingMiniGame).key, 1_000L)
        return if (begun.pendingMiniGame?.phase == MiniPhase.MEMORIZE)
            MiniGameEngine.refresh(begun, requireNotNull(begun.pendingMiniGame).previewUntilMs) else begun
    }
    private fun tap(state: GameState, value: Int, now: Long = 5_000L): GameState {
        val run = requireNotNull(state.pendingMiniGame)
        return MiniGameEngine.input(state, run.key, value, run.revision, now)
    }
    private fun reward(state: GameState) = requireNotNull(state.pendingMiniGame?.reward)
    private fun answer(state: GameState, correct: Boolean = true, now: Long = 2_000L): GameState {
        val run = requireNotNull(state.pendingMiniGame)
        val q = run.questions[run.progress]
        return tap(state, if (correct) q.correctIndex else (q.correctIndex + 1) % q.options.size, now)
    }
    private fun withBonus(state: GameState, reward: MiniReward, activity: ActivityType, block: Int = 0) =
        state.copy(miniBonus = MiniGameBonus("test", state.semester, state.week, state.day, block,
            activity, if (activity == ActivityType.STUDY) id else null, reward))

    @Test fun activityFamiliesAndChancesMatchTheDesign() {
        assertEquals(7, MiniGameEngine.chances.size)
        assertEquals(0.16, MiniGameEngine.chances.getValue(ActivityType.STUDY), 0.0)
        assertEquals(0.18, MiniGameEngine.chances.getValue(ActivityType.WORK), 0.0)
        assertEquals(0.30, MiniGameEngine.chances.getValue(ActivityType.TUTOR), 0.0)
        assertEquals(0.20, MiniGameEngine.chances.getValue(ActivityType.CAREER), 0.0)
        assertEquals(0.25, MiniGameEngine.chances.getValue(ActivityType.RUN), 0.0)
        assertEquals(0.30, MiniGameEngine.chances.getValue(ActivityType.GYM), 0.0)
        assertEquals(0.30, MiniGameEngine.chances.getValue(ActivityType.PT), 0.0)
        for (activity in listOf(ActivityType.CLASS, ActivityType.DATE, ActivityType.TEAM, ActivityType.SLEEP))
            assertTrue(MiniGameEngine.kindsFor(activity).isEmpty())
    }

    @Test fun rollSelectsOneEligibleBlockAndDoesNotChangeItOnReentry() {
        val schedule = List(28) { TimeSlot(ActivityType.WORK) }
        val state = MiniGameEngine.prepareDay(fresh(), schedule, always)
        assertNotNull(state.pendingMiniGame)
        assertTrue(requireNotNull(state.pendingMiniGame).block in 0..3)
        MiniGameEngine.validateState(state, schedule)
        assertSame(state, MiniGameEngine.prepareDay(state, schedule, Random(99)))
    }

    @Test fun failedRollAndGlobalSkipAreCheckedOnlyOncePerDay() {
        for (original in listOf(fresh(), fresh().copy(skipMiniGames = true))) {
            val checked = MiniGameEngine.prepareDay(original, plan(), never)
            assertNull(checked.pendingMiniGame)
            assertEquals(EventEngine.absoluteDay(checked), checked.miniCheckedDay)
            assertSame(checked, MiniGameEngine.prepareDay(checked, plan(), always))
        }
    }

    @Test fun lockedActivitiesAndRegistrationNeverOpenAMinigame() {
        for (activity in listOf(ActivityType.CLASS, ActivityType.TEAM, ActivityType.DATE, ActivityType.CAREER, ActivityType.TUTOR))
            assertNull(MiniGameEngine.prepareDay(fresh(), plan(activity), always).pendingMiniGame)
    }

    @Test fun registrationAndPendingMinigameBlockDailyProgress() {
        val registration = fresh().copy(needsRegistration = true)
        assertSame(registration, MiniGameEngine.prepareDay(registration, plan(), always))
        val state = ready(MiniKind.FOCUS)
        assertSame(state, GameEngine.advanceDay(state, plan(), subjects))
        assertSame(state, EventEngine.advanceDay(state, plan(), subjects, always))
        assertSame(state, DayFlowEngine.advanceDay(state, plan(), subjects, always))
        assertSame(state, GameEngine.nextWeek(state))
    }

    @Test fun focusNumbersAreShuffledUniqueAndOnlyNextNumberCounts() {
        val state = play(ready(MiniKind.FOCUS))
        val run = requireNotNull(state.pendingMiniGame)
        assertEquals((1..9).map(Int::toString).toSet(), run.choices.toSet())
        assertSame(state, tap(state, run.choices.indexOf("2"), 2_000L))
        assertSame(state, MiniGameEngine.input(state, "stale", 0, run.revision, 2_000L))
    }

    @Test fun focusCompletionUsesRemainingTimeWithoutImmediatelyChangingStudy() {
        var state = play(ready(MiniKind.FOCUS))
        for (number in 1..9) state = tap(state, requireNotNull(state.pendingMiniGame).choices.indexOf(number.toString()), 2_000L)
        assertEquals(MiniPhase.RESULT, state.pendingMiniGame?.phase)
        assertEquals(1.0 + 8_000.0 / 9_000 * 0.6, reward(state).studyMultiplier, 0.000001)
        assertEquals(0, state.study.getValue(id))
        assertSame(state, tap(state, 0, 2_001L))
    }

    @Test fun staleRevisionCannotAnswerOrCountATapTwice() {
        val state = play(ready(MiniKind.HOLD))
        val run = requireNotNull(state.pendingMiniGame)
        val first = MiniGameEngine.input(state, run.key, 0, run.revision, 2_000L)
        assertEquals(1, first.pendingMiniGame?.progress)
        assertSame(first, MiniGameEngine.input(first, run.key, 0, run.revision, 2_001L))
    }

    @Test fun timedFocusExpiresAtDeadlineAndClockRollbackCannotExtendIt() {
        val state = play(ready(MiniKind.FOCUS))
        assertSame(state, MiniGameEngine.refresh(state, 9_999L))
        assertEquals(0.85, reward(MiniGameEngine.refresh(state, 10_000L)).studyMultiplier, 0.0)
        assertEquals(0.85, reward(MiniGameEngine.refresh(state, 999L)).studyMultiplier, 0.0)
        assertEquals(MiniPhase.RESULT, MiniGameEngine.input(state, requireNotNull(state.pendingMiniGame).key,
            0, 0, 10_000L).pendingMiniGame?.phase)
    }

    @Test fun careerFocusGivesSpecInsteadOfStudyMultiplier() {
        var state = play(ready(MiniKind.FOCUS, ActivityType.CAREER))
        for (number in 1..9) state = tap(state, requireNotNull(state.pendingMiniGame).choices.indexOf(number.toString()), 2_000L)
        assertEquals(3, reward(state).spec)
        assertEquals(1.0, reward(state).studyMultiplier, 0.0)
    }

    @Test fun memoryRequiresPreviewThenFourOrderedAnswers() {
        val state = ready(MiniKind.MEMORY)
        val begun = MiniGameEngine.begin(state, requireNotNull(state.pendingMiniGame).key, 1_000L)
        assertEquals(MiniPhase.MEMORIZE, begun.pendingMiniGame?.phase)
        assertSame(begun, tap(begun, 0, 4_899L))
        var active = MiniGameEngine.refresh(begun, 4_900L)
        assertEquals(MiniPhase.PLAY, active.pendingMiniGame?.phase)
        for (choice in requireNotNull(active.pendingMiniGame).sequence) active = tap(active, choice)
        assertEquals(1.5, reward(active).studyMultiplier, 0.0)
    }

    @Test fun wrongMemoryAnswerEndsGameAndPreviewCannotBeRestarted() {
        val state = play(ready(MiniKind.MEMORY))
        val run = requireNotNull(state.pendingMiniGame)
        assertSame(state, MiniGameEngine.begin(state, run.key, 10_000L))
        val wrong = (run.sequence.first() + 1) % run.choices.size
        assertEquals(0.8, reward(tap(state, wrong)).studyMultiplier, 0.0)
    }

    @Test fun calculationHasOneTotalDeadlineAndThreeCorrectAnswersGive155Percent() {
        var state = play(ready(MiniKind.CALC))
        val deadline = requireNotNull(state.pendingMiniGame).deadlineMs
        repeat(3) {
            state = answer(state)
            assertEquals(deadline, state.pendingMiniGame?.deadlineMs)
        }
        assertEquals(3, state.pendingMiniGame?.correct)
        assertEquals(1.55, reward(state).studyMultiplier, 0.000001)
    }

    @Test fun timedOutCalculationKeepsOnlyAlreadyCorrectAnswers() {
        val state = answer(play(ready(MiniKind.CALC)))
        val expired = MiniGameEngine.refresh(state, 12_000L)
        assertEquals(1.05, reward(expired).studyMultiplier, 0.000001)
        assertEquals(1, expired.pendingMiniGame?.progress)
    }

    @Test fun orderSelectionCanBeChangedAndPaysForMatchesRegardlessOfOrder() {
        var state = play(ready(MiniKind.ORDER))
        val targets = requireNotNull(state.pendingMiniGame).sequence.reversed()
        state = tap(state, targets.first())
        state = tap(state, targets.first())
        assertTrue(requireNotNull(state.pendingMiniGame).selected.isEmpty())
        for (target in targets) state = tap(state, target)
        val fourth = requireNotNull(state.pendingMiniGame).choices.indices.first { it !in targets }
        assertSame(state, tap(state, fourth))
        state = MiniGameEngine.submitOrders(state, requireNotNull(state.pendingMiniGame).key, 5_000L)
        assertEquals(12_000, reward(state).money)
        assertSame(state, MiniGameEngine.submitOrders(state, requireNotNull(state.pendingMiniGame).key, 5_001L))
    }

    @Test fun changeQuizPaysSixThousandOrLosesFourHappiness() {
        assertEquals(6_000, reward(answer(play(ready(MiniKind.CHANGE)))).money)
        val wrong = reward(answer(play(ready(MiniKind.CHANGE)), false))
        assertEquals(0, wrong.money)
        assertEquals(-4, wrong.happiness)
        assertEquals(-4, reward(MiniGameEngine.refresh(play(ready(MiniKind.CHANGE)), 10_000L)).happiness)
    }

    @Test fun tutoringRewardUsesFrozenTipAndWrongAnswerHasPenalty() {
        val state = play(ready(MiniKind.TUTOR))
        val tip = requireNotNull(state.pendingMiniGame).presetTip
        assertTrue(tip in listOf(30_000, 50_000, 80_000))
        val right = reward(answer(state))
        assertEquals(tip, right.money)
        assertEquals(4, right.happiness)
        assertEquals(-5, reward(answer(state, false)).happiness)
    }

    @Test fun interviewCorrectAnswerGivesThreeExtraSpec() {
        val state = play(ready(MiniKind.INTERVIEW))
        assertEquals(3, reward(answer(state)).spec)
        assertEquals(0, reward(answer(state, false)).spec)
    }

    @Test fun timingHasThreeAttemptsAndRestartsMarkerForEachAttempt() {
        var state = play(ready(MiniKind.TIMING))
        assertEquals(50f, MiniGameEngine.marker(requireNotNull(state.pendingMiniGame), 1_500L), 0f)
        state = tap(state, 0, 1_500L)
        assertEquals(1_500L, state.pendingMiniGame?.startedAtMs)
        state = tap(state, 0, 1_500L) // position 0: miss
        state = tap(state, 0, 2_000L)
        assertEquals(2, reward(state).maxHp)
        assertEquals(3, state.pendingMiniGame?.progress)
    }

    @Test fun holdFortyTapsEndsEarlyAndPartialTapsSurviveTimeout() {
        var full = play(ready(MiniKind.HOLD))
        repeat(40) { full = tap(full, 0, 2_000L + it) }
        assertEquals(3, reward(full).maxHp)
        var partial = play(ready(MiniKind.HOLD))
        repeat(20) { partial = tap(partial, 0, 2_000L + it) }
        assertEquals(2, reward(MiniGameEngine.refresh(partial, 7_000L)).maxHp)
    }

    @Test fun skippingBeforeStartIsNeutralAndForfeitingAfterStartIsNot() {
        val ready = ready(MiniKind.MEMORY)
        val key = requireNotNull(ready.pendingMiniGame).key
        val skipped = MiniGameEngine.skipOrForfeit(ready, key, 1_000L)
        assertEquals(1.0, reward(skipped).studyMultiplier, 0.0)
        assertSame(skipped, MiniGameEngine.skipOrForfeit(skipped, key, 1_001L))
        assertEquals(0.8, reward(MiniGameEngine.skipOrForfeit(play(ready), key, 5_000L)).studyMultiplier, 0.0)
    }

    @Test fun continuingResultAdvancesOnceAndNeverRerollsMinigameThatDay() {
        val state = ready(MiniKind.CHANGE)
        val skipped = MiniGameEngine.skipOrForfeit(state, requireNotNull(state.pendingMiniGame).key, 1_000L)
        val done = DayFlowEngine.continueMini(skipped, plan(ActivityType.WORK), subjects,
            requireNotNull(skipped.pendingMiniGame).key, never)
        assertEquals(1, done.day)
        assertNull(done.pendingMiniGame)
        assertNull(done.miniBonus)
        assertEquals(375_000, done.money)
        assertSame(done, DayFlowEngine.continueMini(done, plan(ActivityType.WORK), subjects,
            requireNotNull(skipped.pendingMiniGame).key, always))
    }

    @Test fun dilemmaKeepsBonusUntilNormalActivityIsExecuted() {
        val solved = answer(play(ready(MiniKind.CHANGE)))
        val key = requireNotNull(solved.pendingMiniGame).key
        val offered = DayFlowEngine.continueMini(solved, plan(ActivityType.WORK), subjects, key, always)
        assertEquals(0, offered.day)
        assertEquals("work", offered.pendingEvent?.eventId)
        assertNotNull(offered.miniBonus)
        assertNull(offered.pendingMiniGame)
        assertEquals(350_000, offered.money)
        val chosen = EventEngine.choose(offered, requireNotNull(offered.pendingEvent).key, 0)
        val eventKey = requireNotNull(chosen.pendingEvent).key
        val plain = EventEngine.continueEvent(chosen.copy(miniBonus = null), plan(ActivityType.WORK), subjects, eventKey)
        val done = EventEngine.continueEvent(chosen, plan(ActivityType.WORK), subjects, eventKey)
        assertEquals(plain.money + 6_000, done.money)
        assertEquals(1, done.day)
        assertNull(done.miniBonus)
        assertSame(done, EventEngine.continueEvent(done, plan(ActivityType.WORK), subjects, eventKey))
    }

    @Test fun workBonusAppliesToOneBlockOnlyAndIsConsumed() {
        val schedule = List(28) { if (it in 0..1) TimeSlot(ActivityType.WORK) else TimeSlot() }
        val state = withBonus(fresh(), MiniReward(money = 12_000), ActivityType.WORK)
        val done = GameEngine.advanceDay(state, schedule, subjects)
        assertEquals(412_000, done.money)
        assertNull(done.miniBonus)
        assertEquals(412_000, GameEngine.advanceDay(done, schedule, subjects).money)
    }

    @Test fun bonusCannotFollowAChangedActivitySubjectOrDay() {
        val state = withBonus(fresh(), MiniReward(studyMultiplier = 1.5), ActivityType.STUDY)
        assertNull(MiniGameEngine.bonusFor(state, 1, TimeSlot(ActivityType.STUDY, id)))
        assertNull(MiniGameEngine.bonusFor(state, 0, TimeSlot(ActivityType.WORK)))
        assertNull(MiniGameEngine.bonusFor(state, 0, TimeSlot(ActivityType.STUDY, subjects[1].id)))
        assertNull(MiniGameEngine.bonusFor(state.copy(day = 1), 4, TimeSlot(ActivityType.STUDY, id)))
    }

    @Test fun studyRewardAffectsOnlyTargetAndCombinesWithExistingBuff() {
        val state = fresh().copy(hp = 100, happiness = 80,
            eventBuffs = listOf(EventBuff(ActivityType.STUDY, 1.2, 0, 6, "test")))
        val schedule = List(28) { if (it in 0..1) TimeSlot(ActivityType.STUDY, id) else TimeSlot() }
        val done = GameEngine.advanceDay(withBonus(state, MiniReward(studyMultiplier = 1.5), ActivityType.STUDY), schedule, subjects)
        assertEquals(65, done.study.getValue(id)) // round(15*1.2*1.2*1.2*1.5) + round(15*1.2*1.2*1.2)
    }

    @Test fun unpaidGymCancelsBaseActivityAndItsBonus() {
        val state = withBonus(fresh().copy(money = -40_000), MiniReward(maxHp = 3), ActivityType.GYM)
        val done = GameEngine.advanceDay(state, plan(ActivityType.GYM), subjects)
        assertEquals(state.maxHp, done.maxHp)
        assertEquals(state.hp, done.hp)
        assertEquals(state.money, done.money)
        assertNull(done.miniBonus)
    }

    @Test fun workoutBonusRespectsMaxHpCapAndCareerRespectsSpecCap() {
        val exercise = withBonus(fresh().copy(maxHp = 149), MiniReward(maxHp = 3), ActivityType.RUN)
        assertEquals(150, GameEngine.advanceDay(exercise, plan(ActivityType.RUN), subjects).maxHp)
        val career = withBonus(fresh().copy(semester = 5, spec = 99), MiniReward(spec = 3), ActivityType.CAREER)
        assertEquals(100, GameEngine.advanceDay(career, plan(ActivityType.CAREER), subjects).spec)
    }

    @Test fun invalidScheduleOrCalendarIsRejectedDuringSaveValidation() {
        val state = ready(MiniKind.FOCUS)
        for (invalid in listOf(state.copy(day = 1), state.copy(miniCheckedDay = -1))) {
            try { MiniGameEngine.validateState(invalid, plan()); fail("invalid state accepted") }
            catch (_: IllegalArgumentException) { }
        }
        try { MiniGameEngine.validateState(state, plan(ActivityType.WORK)); fail("changed schedule accepted") }
        catch (_: IllegalArgumentException) { }
    }
}
