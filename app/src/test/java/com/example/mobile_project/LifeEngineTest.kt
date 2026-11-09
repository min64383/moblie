package com.example.mobile_project

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class LifeEngineTest {
    private val subjects = subjectsForSemester(1).take(3)
    private val ids = subjects.map { it.id }
    private val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
    private val never = object : Random() {
        override fun nextBits(bitCount: Int): Int = if (bitCount == 0) 0 else -1 ushr (32 - bitCount)
    }
    private val forbiddenRandom = object : Random() {
        override fun nextBits(bitCount: Int): Int = error("unexpected random draw")
    }
    private fun fixedRoll(value: Int) = object : Random() {
        override fun nextBits(bitCount: Int): Int = error("use nextInt only")
        override fun nextInt(until: Int): Int { require(value in 0 until until); return value }
    }
    private fun fixedChance(value: Double) = object : Random() {
        override fun nextBits(bitCount: Int): Int = error("use nextDouble only")
        override fun nextDouble(): Double = value
    }
    private fun fresh() = GameEngine.newGame(ids)
    private fun plan(activity: ActivityType = ActivityType.CLASS) = List(28) {
        if (it == 0) TimeSlot(activity, if (activity == ActivityType.CLASS || activity == ActivityType.STUDY) ids[0] else null)
        else TimeSlot()
    }
    private fun ready(activity: ActivityType = ActivityType.CLASS): GameState =
        LifeEngine.prepareMorning(fresh().copy(hp = 20), plan(activity), always)
    private fun begin(state: GameState): GameState =
        LifeEngine.beginMorning(state, requireNotNull(state.life.pendingMorning).key, 1_000L)
    private fun failMorning(state: GameState = ready()): GameState = LifeEngine.refreshMorning(begin(state), 5_000L)
    private fun finishSingleQuestion(state: GameState, kind: ExamKind): GameState {
        val run = ExamRun(kind = kind, phase = ExamPhase.FEEDBACK,
            questionIds = listOf(0), optionOrders = listOf(listOf(0, 1, 2, 3)),
            optionCount = 4, secondsPerQuestion = 7, jokbo = false, correct = 1, selectedAnswer = 0)
        return ExamEngine.nextQuestion(state.copy(exam = run), 0, 1_000L)
    }

    @Test fun morningChanceUsesPercentageOfMaxHpAndExactThreshold() {
        assertEquals(0.0, LifeEngine.morningChance(fresh().copy(hp = 40)), 0.0)
        assertEquals(0.3, LifeEngine.morningChance(fresh().copy(hp = 20)), 0.000001)
        assertEquals(0.6, LifeEngine.morningChance(fresh().copy(hp = 0)), 0.000001)
        assertEquals(0.3, LifeEngine.morningChance(fresh().copy(hp = 30, maxHp = 150)), 0.000001)
    }

    @Test fun onlyMorningAppointmentsCanTriggerAndHealthyStateDoesNotRoll() {
        val healthy = LifeEngine.prepareMorning(fresh(), plan(), forbiddenRandom)
        assertNull(healthy.life.pendingMorning)
        for (activity in listOf(ActivityType.SLEEP, ActivityType.REST, ActivityType.FUN, ActivityType.RUN, ActivityType.TUTOR)) {
            assertNull(LifeEngine.prepareMorning(fresh().copy(hp = 0), plan(activity), forbiddenRandom).life.pendingMorning)
        }
        val afternoonOnly = plan().mapIndexed { i, slot -> if (i == 0) TimeSlot() else if (i == 1) TimeSlot(ActivityType.CLASS, ids[0]) else slot }
        assertNull(LifeEngine.prepareMorning(fresh().copy(hp = 0), afternoonOnly, forbiddenRandom).life.pendingMorning)
    }

    @Test fun morningIsPlannedOnceAndCannotRerollAfterFailedChance() {
        val offered = ready()
        assertNotNull(offered.life.pendingMorning)
        assertSame(offered, LifeEngine.prepareMorning(offered, plan(), forbiddenRandom))
        val checked = LifeEngine.prepareMorning(fresh().copy(hp = 20), plan(), never)
        assertNull(checked.life.pendingMorning)
        assertSame(checked, LifeEngine.prepareMorning(checked, plan(), forbiddenRandom))
    }

    @Test fun beginOnlyStartsOneFourSecondTimer() {
        val ready = ready()
        assertSame(ready, LifeEngine.refreshMorning(ready, Long.MAX_VALUE))
        assertSame(ready, LifeEngine.beginMorning(ready, "stale", 1_000L))
        val active = begin(ready)
        assertEquals(5_000L, active.life.pendingMorning?.deadlineMs)
        assertSame(active, LifeEngine.beginMorning(active, requireNotNull(active.life.pendingMorning).key, 2_000L))
    }

    @Test fun twentyTwoTapsSaveAttendanceAndStaleTapCannotCountTwice() {
        var state = begin(ready())
        val key = requireNotNull(state.life.pendingMorning).key
        state = LifeEngine.tapMorning(state, key, 0, 1_100L)
        assertSame(state, LifeEngine.tapMorning(state, key, 0, 1_101L))
        for (tap in 1 until 22) state = LifeEngine.tapMorning(state, key, tap, 1_200L + tap)
        assertEquals(MorningPhase.RESULT, state.life.pendingMorning?.phase)
        assertEquals(true, state.life.pendingMorning?.success)
        assertTrue(state.life.lateCounts.isEmpty())
        assertEquals(1, state.life.attendanceHistory.size)
        assertSame(state, LifeEngine.tapMorning(state, key, 22, 2_000L))
        LifeEngine.validateState(state, plan())
    }

    @Test fun exactDeadlineAndClockRollbackFailAndPenaltyIsAppliedOnce() {
        val active = begin(ready())
        assertSame(active, LifeEngine.refreshMorning(active, 4_999L))
        for (now in listOf(5_000L, 999L)) {
            val failed = LifeEngine.refreshMorning(active, now)
            assertEquals(false, failed.life.pendingMorning?.success)
            assertEquals(1, failed.life.lateCounts[ids[0]])
            assertEquals(1, failed.life.attendanceHistory.size)
            assertSame(failed, LifeEngine.refreshMorning(failed, now + 100_000L))
            assertSame(failed, LifeEngine.skipMorning(failed, requireNotNull(failed.life.pendingMorning).key, now))
        }
    }

    @Test fun readySkipUsesFrozenAutoResultButForfeitAfterStartFails() {
        val ready = ready().let { it.copy(life = it.life.copy(pendingMorning = requireNotNull(it.life.pendingMorning).copy(autoSuccess = true))) }
        val key = requireNotNull(ready.life.pendingMorning).key
        assertEquals(true, LifeEngine.skipMorning(ready, key, 1_000L).life.pendingMorning?.success)
        assertEquals(false, LifeEngine.skipMorning(begin(ready), key, 1_001L).life.pendingMorning?.success)
    }

    @Test fun nonClassLatenessDoesNotPenalizeAnUnrelatedSubject() {
        for (activity in listOf(ActivityType.STUDY, ActivityType.WORK)) {
            val failed = failMorning(ready(activity))
            assertTrue(failed.life.lateCounts.isEmpty())
            assertNull(failed.life.morningNotice?.subjectId)
            assertFalse(requireNotNull(failed.life.morningNotice).success)
            LifeEngine.validateState(failed, plan(activity))
        }
    }

    @Test fun disablingMinigamesAutomaticallyResolvesMorningAndPreventsAnotherRoll() {
        val state = fresh().copy(hp = 20, skipMiniGames = true)
        val resolved = LifeEngine.prepareMorning(state, plan(), always)
        assertNull(resolved.life.pendingMorning)
        assertNotNull(resolved.life.morningNotice)
        assertEquals(1, resolved.life.lateCounts[ids[0]])
        assertSame(resolved, LifeEngine.prepareMorning(resolved, plan(), forbiddenRandom))
        val done = DayFlowEngine.advanceDay(resolved, plan(), subjects, never)
        assertEquals(1, done.day)
        assertNull(done.life.morningNotice)
        assertEquals(1, done.log.count { it.startsWith("늦잠") })
    }

    @Test fun unresolvedMorningBlocksOtherSystemsAndLottery() {
        val state = ready()
        assertSame(state, GameEngine.advanceDay(state, plan(), subjects))
        assertSame(state, EventEngine.advanceDay(state, plan(), subjects))
        assertSame(state, MiniGameEngine.prepareDay(state, plan(), always))
        assertSame(state, DayFlowEngine.advanceDay(state, plan(), subjects))
        assertSame(state, TeamEngine.prepareWeek(state, plan(), always).game)
        assertSame(state, LoveEngine.prepareWeek(state, plan(), always).game)
        assertFalse(LifeEngine.canBuyLottery(state))
    }

    @Test fun continuingMorningKeepsRecordAndAdvancesOnlyOnce() {
        val failed = failMorning()
        val key = requireNotNull(failed.life.pendingMorning).key
        val done = LifeEngine.continueMorning(failed, plan(), subjects, key, never)
        assertEquals(1, done.day)
        assertNull(done.life.pendingMorning)
        assertNull(done.life.morningNotice)
        assertEquals(1, done.life.lateCounts[ids[0]])
        assertEquals(1, done.log.count { it.startsWith("늦잠") })
        assertSame(done, LifeEngine.continueMorning(done, plan(), subjects, key, forbiddenRandom))
    }

    @Test fun morningDilemmaAndActivityMinigameRunInSequenceWithoutLosingNotice() {
        val failed = failMorning(ready(ActivityType.WORK))
        val key = requireNotNull(failed.life.pendingMorning).key
        val mini = LifeEngine.continueMorning(failed, plan(ActivityType.WORK), subjects, key, always)
        assertNotNull(mini.pendingMiniGame)
        assertNull(mini.life.pendingMorning)
        assertNotNull(mini.life.morningNotice)
        val miniKey = requireNotNull(mini.pendingMiniGame).key
        val skipped = MiniGameEngine.skipOrForfeit(mini, miniKey, 5_001L)
        val dilemma = DayFlowEngine.continueMini(skipped, plan(ActivityType.WORK), subjects, miniKey, always)
        assertNotNull(dilemma.pendingEvent)
        assertNotNull(dilemma.life.morningNotice)
        val eventKey = requireNotNull(dilemma.pendingEvent).key
        val chosen = EventEngine.choose(dilemma, eventKey, 0)
        val done = EventEngine.continueEvent(chosen, plan(ActivityType.WORK), subjects, eventKey, never)
        assertEquals(1, done.day)
        assertNull(done.life.morningNotice)
        assertNull(done.miniBonus)
        assertEquals(1, done.life.attendanceHistory.size)
        assertEquals(1, done.log.count { it.startsWith("늦잠") })
    }

    @Test fun oldPendingActivityMinigameDoesNotStartANewMorningAfterResult() {
        val state = fresh().copy(hp = 10)
        val schedule = plan(ActivityType.STUDY)
        val mini = MiniGameEngine.prepareDay(state, schedule, always)
        val key = requireNotNull(mini.pendingMiniGame).key
        val skipped = MiniGameEngine.skipOrForfeit(mini, key, 1_000L)
        val done = DayFlowEngine.continueMini(skipped, schedule, subjects, key, never)
        assertNull(done.life.pendingMorning)
        assertTrue(done.life.attendanceHistory.isEmpty())
        assertEquals(1, done.day)
    }

    @Test fun gradePenaltyAppliesDuringScoreCalculationAndCannotGoBelowZero() {
        assertEquals(60, ExamEngine.scoreFor(200, 1, 1))
        assertEquals(54, ExamEngine.scoreFor(200, 1, 1, lateCount = 2))
        assertEquals(0, ExamEngine.scoreFor(0, 1, 1, lateCount = 20))
        assertEquals(100, ExamEngine.scoreFor(1_000, 1, 1, lateCount = 1))
    }

    @Test fun midtermClearsItsCountsAndFinalUsesOnlyNewCounts() {
        val initial = fresh().copy(week = 8, day = 7, study = ids.associateWith { 200 },
            life = LifeState(lateCounts = mapOf(ids[0] to 2)))
        val midterm = finishSingleQuestion(initial, ExamKind.MIDTERM)
        assertEquals(54, midterm.midtermScores[ids[0]])
        assertEquals(60, midterm.midtermScores[ids[1]])
        assertTrue(midterm.life.lateCounts.isEmpty())
        val final = finishSingleQuestion(midterm.copy(week = 16, study = ids.associateWith { 200 },
            life = midterm.life.copy(lateCounts = mapOf(ids[1] to 1))), ExamKind.FINAL)
        assertEquals(60, final.finalScores[ids[0]])
        assertEquals(57, final.finalScores[ids[1]])
        assertTrue(final.life.lateCounts.isEmpty())
    }

    @Test fun healthChanceUsesEndOfWeekPercentageAndStrictFortyFivePercent() {
        val weekend = fresh().copy(day = 7)
        assertEquals(-1, LifeEngine.settleWeek(weekend.copy(hp = 30), forbiddenRandom).life.sickWeek)
        assertEquals(1, LifeEngine.settleWeek(weekend.copy(hp = 29), fixedChance(0.449999)).life.sickWeek)
        assertEquals(-1, LifeEngine.settleWeek(weekend.copy(hp = 29), fixedChance(0.45)).life.sickWeek)
        assertEquals(-1, LifeEngine.settleWeek(weekend.copy(hp = 45, maxHp = 150), forbiddenRandom).life.sickWeek)
    }

    @Test fun sundayHealthIsCheckedOnceAndSicknessIsScheduledForNextWeek() {
        val settled = LifeEngine.settleWeek(fresh().copy(day = 7, hp = 10), always)
        assertFalse(LifeEngine.isSick(settled))
        assertEquals(1, settled.life.sickWeek)
        assertSame(settled, LifeEngine.settleWeek(settled, forbiddenRandom))
        assertTrue(LifeEngine.isSick(GameEngine.nextWeek(settled)))
    }

    @Test fun dailyAndDilemmaCompletionBothSettleSundayHealth() {
        val sunday = fresh().copy(day = 6, hp = 0, skipMiniGames = true)
        val empty = List(28) { TimeSlot() }
        val done = DayFlowEngine.advanceDay(sunday, empty, subjects, always)
        assertEquals(7, done.day)
        assertEquals(0, done.life.healthCheckedWeek)
        assertEquals(1, done.life.sickWeek)
        val work = empty.mapIndexed { i, slot -> if (i == 24) TimeSlot(ActivityType.WORK) else slot }
        val offered = EventEngine.advanceDay(sunday, work, subjects, always)
        val key = requireNotNull(offered.pendingEvent).key
        val chosen = EventEngine.choose(offered, key, 0)
        val eventDone = EventEngine.continueEvent(chosen, work, subjects, key, always)
        assertEquals(7, eventDone.day)
        assertEquals(1, eventDone.life.sickWeek)
        assertEquals(0, eventDone.life.healthCheckedWeek)
    }

    @Test fun sicknessLastsOneWeekThenEndsOrCanBeCaughtAgain() {
        val sickSunday = fresh().copy(week = 2, day = 7, hp = 100, life = LifeState(sickWeek = 1))
        val recovered = LifeEngine.settleWeek(sickSunday, forbiddenRandom)
        assertEquals(-1, recovered.life.sickWeek)
        assertFalse(LifeEngine.isSick(GameEngine.nextWeek(recovered)))
        val relapsed = LifeEngine.settleWeek(sickSunday.copy(hp = 10), always)
        assertEquals(2, relapsed.life.sickWeek)
    }

    @Test fun sicknessReducesStudyButNotWagesOrFixedClassStudy() {
        val state = fresh().copy(hp = 100, happiness = 80, life = LifeState(sickWeek = 0))
        val study = GameEngine.advanceDay(state, plan(ActivityType.STUDY), subjects)
        assertEquals(13, study.study[ids[0]]) // round(15 * 1.2 * 1.2 * 0.6)
        assertEquals(GameEngine.CLASS_GAIN, GameEngine.advanceDay(state, plan(), subjects).study[ids[0]])
        assertEquals(375_000, GameEngine.advanceDay(state, plan(ActivityType.WORK), subjects).money)
    }

    @Test fun noNextWeekSicknessIsCreatedAtSemesterEnd() {
        val ended = LifeEngine.settleWeek(fresh().copy(week = 16, day = 7, hp = 0), forbiddenRandom)
        assertEquals(-1, ended.life.sickWeek)
        assertEquals(15, ended.life.healthCheckedWeek)
    }

    @Test fun lotteryAllProbabilityBoundariesHaveCorrectPrizes() {
        val buckets = mapOf(0 to 0, 839 to 0, 840 to 1, 959 to 1, 960 to 2, 994 to 2, 995 to 3, 999 to 3)
        for ((bucket, expected) in buckets) {
            val bought = LifeEngine.buyLottery(fresh(), 1, 1, fixedRoll(bucket))
            assertEquals(expected, LifeEngine.currentTicket(bought)?.prizeIndex)
            assertEquals(350_000 - 10_000 + LifeEngine.prizes[expected].amount, bought.money)
        }
        assertEquals(1_000, LifeEngine.prizes.sumOf { it.ticketsPerThousand })
        assertEquals(7_700, LifeEngine.prizes.sumOf { it.ticketsPerThousand * it.amount } / 1_000)
    }

    @Test fun lotteryIsOnePerWeekAndCalendarIncludesSemester() {
        val bought = LifeEngine.buyLottery(fresh(), 1, 1, fixedRoll(995))
        assertSame(bought, LifeEngine.buyLottery(bought, 1, 1, forbiddenRandom))
        val nextWeek = bought.copy(week = 2)
        assertTrue(LifeEngine.canBuyLottery(nextWeek))
        assertSame(nextWeek, LifeEngine.buyLottery(nextWeek, 1, 1, forbiddenRandom))
        val nextSemester = bought.copy(semester = 2)
        assertTrue(LifeEngine.canBuyLottery(nextSemester))
        assertSame(nextSemester, LifeEngine.buyLottery(nextSemester, 1, 1, forbiddenRandom))
    }

    @Test fun lotteryMustBeAffordableBeforePrizeAndDebtBoundaryIsExact() {
        val poor = fresh().copy(money = -40_001)
        assertFalse(LifeEngine.canBuyLottery(poor))
        assertSame(poor, LifeEngine.buyLottery(poor, 1, 1, forbiddenRandom))
        val exact = LifeEngine.buyLottery(fresh().copy(money = -40_000), 1, 1, fixedRoll(0))
        assertEquals(-50_000, exact.money)
        assertNotNull(LifeEngine.currentTicket(exact))
    }

    @Test fun lotteryCannotBeBoughtDuringAnEventOrOutsideActiveDays() {
        val event = EventEngine.advanceDay(fresh(), plan(ActivityType.WORK), subjects, always)
        val mini = MiniGameEngine.prepareDay(fresh(), plan(ActivityType.STUDY), always)
        for (state in listOf(event, mini, ready(), fresh().copy(day = 7), fresh().copy(needsRegistration = true))) {
            assertFalse(LifeEngine.canBuyLottery(state))
            assertSame(state, LifeEngine.buyLottery(state, state.semester, state.week, forbiddenRandom))
        }
    }

    @Test fun nextSemesterClearsIllnessAndUnspentPenaltiesButKeepsHistories() {
        val history = AttendanceRecord(1, 1, 0, ActivityType.CLASS, ids[0], false)
        val result = SemesterResult(1, ids.map { SubjectGrade(it, 90, "A0", 4.0) })
        val finished = fresh().copy(week = 16, day = 7, semesterResult = result,
            vacationHistory = listOf(VacationRecord(1, "rest")),
            life = LifeState(sickWeek = 15, lateCounts = mapOf(ids[0] to 2),
                attendanceHistory = listOf(history), lotteryHistory = listOf(LotteryTicket(1, 1, 3))))
        val next = AcademicEngine.advanceSemester(finished)
        assertEquals(-1, next.life.sickWeek)
        assertTrue(next.life.lateCounts.isEmpty())
        assertEquals(finished.life.attendanceHistory, next.life.attendanceHistory)
        assertEquals(finished.life.lotteryHistory, next.life.lotteryHistory)
        LifeEngine.validateState(next, emptyList())
    }
}
