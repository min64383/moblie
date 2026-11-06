package com.example.mobile_project

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class LoveEngineTest {
    private val subjects = subjectsForSemester(3).take(3)
    private val partner = LoveCatalog.partners.first()
    private val never = object : Random() {
        override fun nextBits(bitCount: Int): Int = if (bitCount == 0) 0 else -1 ushr (32 - bitCount)
    }
    private val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
    private fun fresh(semester: Int = 3, week: Int = 1): GameState {
        val ids = subjectsForSemester(semester).take(3).map { it.id }
        return GameEngine.newGame(ids).copy(semester = semester, week = week, charm = 40, network = 30)
    }
    private fun plan(): List<TimeSlot> = List(28) { index -> when (index) {
        0, 4 -> TimeSlot(ActivityType.CLASS, subjects[0].id)
        8, 12 -> TimeSlot(ActivityType.CLASS, subjects[1].id)
        16, 17 -> TimeSlot(ActivityType.CLASS, subjects[2].id)
        else -> if (index % 4 == 3) TimeSlot(ActivityType.SLEEP) else TimeSlot()
    } }
    private fun active(week: Int = 1): GameState = fresh(week = week).copy(
        love = LoveRelationship(partner, 3, 1, 20, weeksTogether = week - 1,
            lastSettledWeek = 32 + week - 2, plannedWeek = 32 + week - 1),
        loveCheckedSemesters = listOf(3)
    )
    private fun offer(state: GameState = fresh()) = LoveEngine.prepareWeek(state, plan(), always)

    @Test fun thresholdsAndSemesterAreExact() {
        assertFalse(LoveEngine.eligible(fresh(2)))
        assertFalse(LoveEngine.eligible(fresh().copy(charm = 39)))
        assertFalse(LoveEngine.eligible(fresh().copy(network = 29)))
        assertTrue(LoveEngine.eligible(fresh()))
        assertFalse(LoveEngine.eligible(active()))
    }

    @Test fun confessionIsOnlyCheckedAtSemesterStartAndDoesNotReroll() {
        assertNull(offer(fresh(week = 2)).game.pendingConfession)
        assertNull(offer(fresh().copy(day = 1)).game.pendingConfession)
        val first = offer()
        val second = LoveEngine.prepareWeek(first.game, first.schedule, Random(999))
        assertSame(first.game, second.game)
        assertEquals(first.game.pendingConfession, second.game.pendingConfession)
        assertEquals(listOf(3), first.game.loveCheckedSemesters)
    }

    @Test fun ineligibleSemesterStartIsNotRepeatedLaterThatWeek() {
        val checked = offer(fresh().copy(charm = 39)).game
        assertNull(checked.pendingConfession)
        assertTrue(3 in checked.loveCheckedSemesters)
        val upgraded = LoveEngine.prepareWeek(checked.copy(charm = 40), plan(), always)
        assertNull(upgraded.game.pendingConfession)
    }

    @Test fun declineHasNoStatCostAndCannotProduceAnotherConfession() {
        val offered = offer()
        val key = requireNotNull(offered.game.pendingConfession).key
        val chosen = LoveEngine.chooseConfession(offered.game, offered.schedule, key, false)
        assertEquals(LoveChoice.DECLINE, chosen.game.pendingConfession?.choice)
        assertNull(chosen.game.love)
        assertEquals(offered.game.money, chosen.game.money)
        assertEquals(offered.game.happiness, chosen.game.happiness)
        val done = LoveEngine.completeConfession(chosen.game, key)
        assertNull(LoveEngine.prepareWeek(done, chosen.schedule, always).game.pendingConfession)
    }

    @Test fun acceptLocksOneDateWithoutChangingImmediateStats() {
        val original = plan()
        val offered = LoveEngine.prepareWeek(fresh(), original, always)
        val key = requireNotNull(offered.game.pendingConfession).key
        val accepted = LoveEngine.chooseConfession(offered.game, offered.schedule, key, true, always)
        assertEquals(50, accepted.game.love?.affection)
        assertEquals(offered.game.money, accepted.game.money)
        assertEquals(offered.game.happiness, accepted.game.happiness)
        assertEquals(1, accepted.schedule.count { it.activity == ActivityType.DATE })
        val date = requireNotNull(accepted.game.love).dateIndex
        assertTrue(date / 4 in 5..6)
        assertTrue(accepted.schedule[date].locked)
        for (i in original.indices) if (original[i].locked || i % 4 == 3) assertEquals(original[i], accepted.schedule[i])
        assertSame(accepted.game, LoveEngine.chooseConfession(accepted.game, accepted.schedule, key, true).game)
        assertSame(accepted.game, LoveEngine.chooseConfession(accepted.game, accepted.schedule, "stale", false).game)
    }

    @Test fun pendingConfessionBlocksProgressUntilResultIsAcknowledged() {
        val offered = offer()
        assertSame(offered.game, GameEngine.advanceDay(offered.game, offered.schedule, subjects))
        assertSame(offered.game, EventEngine.advanceDay(offered.game, offered.schedule, subjects))
        val key = requireNotNull(offered.game.pendingConfession).key
        assertSame(offered.game, LoveEngine.completeConfession(offered.game, key))
        val accepted = LoveEngine.chooseConfession(offered.game, offered.schedule, key, true, never)
        assertSame(accepted.game, GameEngine.advanceDay(accepted.game, accepted.schedule, subjects))
        assertNull(LoveEngine.completeConfession(accepted.game, key).pendingConfession)
    }

    @Test fun noFreeSlotDisablesAcceptanceButDeclineStillWorks() {
        val full = List(28) { TimeSlot(ActivityType.CLASS, subjects[0].id) }
        val offered = LoveEngine.prepareWeek(fresh(), full, always)
        val key = requireNotNull(offered.game.pendingConfession).key
        assertFalse(LoveEngine.canAccept(offered.game, full))
        assertSame(offered.game, LoveEngine.chooseConfession(offered.game, full, key, true).game)
        assertEquals(LoveChoice.DECLINE, LoveEngine.chooseConfession(offered.game, full, key, false).game.pendingConfession?.choice)
    }

    @Test fun successfulDateCostsTwentyThousandAndIsAppliedOnce() {
        val state = active().copy(day = 5, happiness = 60)
        val result = LoveEngine.performDate(state, 20)
        assertEquals(330_000, result.game.money)
        assertEquals(85, result.game.happiness)
        assertEquals(0, result.game.love?.missedDates)
        assertEquals(50, result.game.love?.affection)
        assertSame(result.game, LoveEngine.performDate(result.game, 20).game)
        assertSame(state, LoveEngine.performDate(state, 21).game)
    }

    @Test fun dateRespectsDebtBoundaryAndHappinessCap() {
        val paid = LoveEngine.performDate(active().copy(day = 5, money = -30_000, happiness = 99), 20).game
        assertEquals(-50_000, paid.money)
        assertEquals(100, paid.happiness)
        val missed = LoveEngine.performDate(active().copy(day = 5, money = -30_001), 20).game
        assertEquals(-30_001, missed.money)
        assertEquals(54, missed.happiness)
        assertEquals(1, missed.love?.missedDates)
        assertSame(missed, LoveEngine.performDate(missed, 20).game)
    }

    @Test fun successfulDateDoesNotResetAccumulatedMisses() {
        val state = active().copy(day = 5, love = requireNotNull(active().love).copy(missedDates = 2))
        val done = LoveEngine.performDate(state, 20).game
        assertEquals(2, done.love?.missedDates)
        assertEquals(1, LoveEngine.settleWeek(done.copy(day = 7)).love?.weeksTogether)
    }

    @Test fun thirdMissEndsRelationshipAtWeekendAndPenaltyIsNotRepeated() {
        val state = active().copy(day = 5, money = -50_000,
            love = requireNotNull(active().love).copy(missedDates = 2))
        val missed = LoveEngine.performDate(state, 20).game
        assertNotNull(missed.love)
        val ended = LoveEngine.settleWeek(missed.copy(day = 7))
        assertNull(ended.love)
        assertEquals(24, ended.happiness)
        assertEquals(1, ended.loveHistory.size)
        assertEquals(3, ended.loveHistory.single().missedDates)
        assertEquals(LoveEndReason.MISSED_DATES, ended.loveHistory.single().reason)
        assertSame(ended, LoveEngine.settleWeek(ended))
    }

    @Test fun missingDateSlotCountsOnceAtWeeklySettlement() {
        val state = active().copy(day = 7, love = requireNotNull(active().love).copy(dateIndex = -1))
        val settled = LoveEngine.settleWeek(state)
        assertEquals(1, settled.love?.missedDates)
        assertEquals(54, settled.happiness)
        assertSame(settled, LoveEngine.settleWeek(settled))
    }

    @Test fun weeklyIncidentIsPlannedOnceAndCanAlsoBeAbsent() {
        val base = active().copy(love = requireNotNull(active().love).copy(plannedWeek = -1))
        val planned = LoveEngine.prepareWeek(base, plan(), always)
        assertNotNull(planned.game.love?.incident)
        val second = LoveEngine.prepareWeek(planned.game, planned.schedule, never)
        assertSame(planned.game, second.game)
        val absent = LoveEngine.prepareWeek(base, plan(), never)
        assertNull(absent.game.love?.incident)
        assertEquals(LoveEngine.absoluteWeek(base), absent.game.love?.plannedWeek)
        assertSame(absent.game, LoveEngine.prepareWeek(absent.game, absent.schedule, always).game)
    }

    @Test fun weeklyIncidentAppliesItsActualClampedChangesOnce() {
        val state = active().copy(hp = 99, happiness = 99,
            love = requireNotNull(active().love).copy(incident = LoveIncident(0, 0)))
        val applied = LoveEngine.applyDayIncident(state)
        assertEquals(100, applied.hp)
        assertEquals(100, applied.happiness)
        assertTrue(requireNotNull(applied.love?.incident).applied)
        assertTrue(applied.log.last().contains(partner))
        assertSame(applied, LoveEngine.applyDayIncident(applied))
    }

    @Test fun routinesKeepDatesAndFixedSlotsAreNotStoredInRoutine() {
        val state = active()
        val locked = LoveEngine.syncSchedule(state, plan())
        assertEquals(ActivityType.EMPTY, RoutineRules.capture(locked)[20].activity)
        val routine = ScheduleRoutine("work", "알바", List(28) { TimeSlot(ActivityType.WORK) })
        val applied = RoutineRules.apply(routine, locked, state)
        assertEquals(ActivityType.DATE, applied[20].activity)
        assertEquals(locked[0], applied[0])
    }

    @Test fun teamAssignmentsAndDatesNeverOverwriteEachOther() {
        var state = active(week = 4)
        var schedule = LoveEngine.syncSchedule(state, plan())
        val prepared = TeamEngine.prepareWeek(state, schedule, Random(7))
        state = prepared.game; schedule = prepared.schedule
        assertTrue(requireNotNull(state.teamProject).meetingIndices.none { it == 20 })
        val lovePrepared = LoveEngine.prepareWeek(state, schedule, never)
        assertEquals(ActivityType.DATE, lovePrepared.schedule[20].activity)
        assertTrue(requireNotNull(state.teamProject).meetingIndices.all {
            lovePrepared.schedule[it].activity == ActivityType.TEAM
        })
        TeamEngine.validateState(lovePrepared.game, lovePrepared.schedule)
        LoveEngine.validateState(lovePrepared.game, lovePrepared.schedule)
    }

    @Test fun changedSemesterRelocatesDateIfNewClassUsesItsOldSlot() {
        val previous = active(week = 16).copy(day = 7,
            love = requireNotNull(active(week = 16).love).copy(lastSettledWeek = 47, dateHandledWeek = 47))
        val record = SemesterResult(3, subjects.map { SubjectGrade(it.id, 90, "A0", 4.0) })
        val completed = previous.copy(semesterResult = record, vacationHistory = listOf(VacationRecord(3, "work")))
        val next = AcademicEngine.advanceSemester(completed)
        assertNotNull(next.love)
        val registered = AcademicEngine.registerSubjects(next, subjects.map { it.id })
        val newPlan = plan().toMutableList().apply { this[20] = TimeSlot(ActivityType.CLASS, subjects[0].id) }
        val prepared = LoveEngine.prepareWeek(registered, newPlan, never)
        assertEquals(ActivityType.CLASS, prepared.schedule[20].activity)
        assertNotEquals(20, prepared.game.love?.dateIndex)
        assertEquals(1, prepared.schedule.count { it.activity == ActivityType.DATE })
        assertNull(prepared.game.pendingConfession)
    }

    @Test fun loveDilemmaChangesAffectionWithinBounds() {
        val state = active().copy(love = requireNotNull(active().love).copy(dateIndex = 1, affection = 98))
        val schedule = LoveEngine.syncSchedule(state, List(28) { TimeSlot() })
        val offered = EventEngine.advanceDay(state, schedule, subjects, always)
        assertEquals("love", offered.pendingEvent?.eventId)
        val key = requireNotNull(offered.pendingEvent).key
        val selected = EventEngine.choose(offered, key, 0)
        assertEquals(100, selected.love?.affection)
        assertSame(selected, EventEngine.choose(selected, key, 0))
        assertTrue(requireNotNull(selected.pendingEvent).changes.contains("애정도 +2"))
    }

    @Test fun dilemmaBreakupReleasesDateBeforeTheDayRunsWithoutExtraPenalty() {
        val state = active().copy(love = requireNotNull(active().love).copy(dateIndex = 1))
        val schedule = LoveEngine.syncSchedule(state, List(28) { TimeSlot() })
        val offered = EventEngine.advanceDay(state, schedule, subjects, always)
        val key = requireNotNull(offered.pendingEvent).key
        val selected = EventEngine.choose(offered, key, 2)
        assertNull(selected.love)
        assertEquals(60, selected.happiness)
        assertEquals(1, selected.loveHistory.size)
        assertEquals(partner, selected.pendingEvent?.contextName)
        EventEngine.validatePending(selected)
        val progressed = EventEngine.continueEvent(selected, schedule, subjects, key)
        assertEquals(91, progressed.hp)
        assertEquals(64, progressed.happiness)
        assertEquals(350_000, progressed.money)
        assertEquals(ActivityType.REST, LoveEngine.syncSchedule(progressed, schedule)[1].activity)
    }

    @Test fun fullSemesterKeepsOneDatePerWeekAndCoexistsWithBothProjects() {
        var state = active()
        var schedule = plan()
        for (week in 1..16) {
            val team = TeamEngine.prepareWeek(state, schedule, Random(10 + week))
            val romance = LoveEngine.prepareWeek(team.game, team.schedule, never)
            state = romance.game; schedule = romance.schedule
            state.teamProject?.let { state = TeamEngine.acknowledge(state, it.key) }
            assertEquals(1, schedule.count { it.activity == ActivityType.DATE })
            // 생활비·데이트 비용을 지불할 잔액을 유지해 일정/상태 진행만 확인한다.
            state = state.copy(money = 2_000_000)
            repeat(7) {
                state = GameEngine.advanceDay(state, schedule, subjects)
                schedule = LoveEngine.syncSchedule(state, TeamEngine.syncSchedule(state, schedule))
            }
            assertEquals(1_950_000, state.money)
            assertEquals(week, state.love?.weeksTogether)
            assertEquals(0, state.love?.missedDates)
            if (week == 8) state = state.copy(midtermScores = state.study.keys.associateWith { 80 })
            if (week < 16) state = GameEngine.nextWeek(state)
        }
        assertEquals(2, state.teamResults.size)
        assertEquals(16, state.love?.weeksTogether)
        assertTrue(state.loveHistory.isEmpty())
    }
}
