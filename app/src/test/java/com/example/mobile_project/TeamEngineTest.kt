package com.example.mobile_project

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class TeamEngineTest {
    private val subjects = firstYearSubjects.take(3)
    private fun fresh(week: Int = 4) = GameEngine.newGame(subjects.map { it.id }).copy(week = week)
    private fun plan(): List<TimeSlot> = List(28) { index ->
        when (index) {
            0, 4 -> TimeSlot(ActivityType.CLASS, subjects[0].id)
            8, 12 -> TimeSlot(ActivityType.CLASS, subjects[1].id)
            16, 17 -> TimeSlot(ActivityType.CLASS, subjects[2].id)
            else -> if (index % 4 == 3) TimeSlot(ActivityType.SLEEP) else TimeSlot()
        }
    }
    private fun active(
        week: Int = 4,
        members: List<String> = listOf("good", "ppt", "ghost"),
        bonus: Int = 8,
        incidents: List<TeamIncident> = emptyList()
    ): GameState = fresh(week).copy(teamProject = TeamProject(
        semester = 1, startWeek = 4, subjectId = subjects[0].id,
        memberIds = members, meetingIndices = listOf(1, 5), bonus = bonus,
        announced = true, lastSettledWeek = week - 1, incidentWeek = week, incidents = incidents
    ))

    private class CountingRandom(seed: Int) : Random() {
        private val delegate = Random(seed)
        var calls = 0
        override fun nextBits(bitCount: Int): Int {
            calls++
            return if (bitCount == 0) 0 else delegate.nextInt().ushr(32 - bitCount)
        }
    }

    @Test fun startsOnlyAtWeeksFourAndElevenBeforeMonday() {
        for (week in 1..16) {
            val prepared = TeamEngine.prepareWeek(fresh(week), plan(), Random(week))
            assertEquals(week in setOf(4, 11), prepared.game.teamProject != null)
        }
        assertNull(TeamEngine.prepareWeek(fresh().copy(day = 2), plan()).game.teamProject)
    }

    @Test fun assignmentProtectsClassesAndNightsAndLimitsMeetingCount() {
        val original = plan()
        val prepared = TeamEngine.prepareWeek(fresh(), original, Random(5))
        val project = requireNotNull(prepared.game.teamProject)
        assertEquals(3, project.memberIds.size)
        assertTrue(project.meetingIndices.size in 1..4)
        for (index in original.indices) {
            if (original[index].locked || index % 4 == 3) assertEquals(original[index], prepared.schedule[index])
        }
        assertTrue(project.meetingIndices.all { prepared.schedule[it].activity == ActivityType.TEAM })
        assertTrue(project.meetingIndices.all { prepared.schedule[it].locked })
        TeamEngine.validateState(prepared.game, prepared.schedule)
    }

    @Test fun assignmentAndIncidentPlanningDoNotRerollOnReentry() {
        val random = CountingRandom(33)
        val first = TeamEngine.prepareWeek(fresh(), plan(), random)
        val calls = random.calls
        val second = TeamEngine.prepareWeek(first.game, first.schedule, random)
        assertSame(first.game, second.game)
        assertEquals(first.schedule, second.schedule)
        assertEquals(calls, random.calls)
        val project = requireNotNull(first.game.teamProject)
        val acknowledged = TeamEngine.acknowledge(first.game, project.key)
        assertEquals(first.game.happiness, acknowledged.happiness)
        assertEquals(first.game.network, acknowledged.network)
        assertSame(acknowledged, TeamEngine.acknowledge(acknowledged, project.key))
    }

    @Test fun networkAndMemberTraitsUseOriginalRules() {
        assertEquals(0.20, TeamEngine.goodMemberChance(0), 0.000001)
        assertEquals(0.65, TeamEngine.goodMemberChance(100), 0.000001)
        assertEquals(4, TeamEngine.meetingCount(listOf("ghost", "free", "ghost")))
        assertEquals(1, TeamEngine.meetingCount(listOf("good", "good", "good")))
        val state = active(members = listOf("namu", "namu", "namu"))
        assertEquals(7, TeamEngine.adjustStudyGain(state, subjects[0].id, 21))
        assertEquals(21, TeamEngine.adjustStudyGain(state, subjects[1].id, 21))
    }

    @Test fun unacknowledgedAssignmentCannotAdvanceTheDay() {
        val prepared = TeamEngine.prepareWeek(fresh(), plan(), Random(4))
        assertSame(prepared.game, GameEngine.advanceDay(prepared.game, prepared.schedule, subjects))
        assertSame(prepared.game, EventEngine.advanceDay(prepared.game, prepared.schedule, subjects))
        assertSame(prepared.game, TeamEngine.acknowledge(prepared.game, "old-key"))
    }

    @Test fun meetingCostsHealthAndHappinessWithoutAddingStudy() {
        val state = active()
        val empty = List(28) { TimeSlot() }
        val locked = TeamEngine.syncSchedule(state, empty)
        val progressed = GameEngine.advanceDay(state, locked, subjects)
        assertEquals(71, progressed.hp)
        assertEquals(55, progressed.happiness)
        assertEquals(0, progressed.study.getValue(subjects[0].id))
        assertEquals(1, progressed.day)
    }

    @Test fun teammateIncidentsAreAggregatedClampedAndAppliedOnce() {
        val state = active(incidents = listOf(TeamIncident(0, 0, 0), TeamIncident(0, 2, 0)))
            .copy(hp = 95, happiness = 95)
        val changed = TeamEngine.applyDayIncidents(state)
        assertEquals(89, changed.hp)
        assertEquals(95, changed.happiness)
        assertTrue(requireNotNull(changed.teamProject).incidents.all { it.applied })
        assertSame(changed, TeamEngine.applyDayIncidents(changed))
    }

    @Test fun eachNewWeekPlansIncidentsOnceEvenWhenThePreviousListWasEmpty() {
        val random = CountingRandom(9)
        val state = active().copy(week = 5,
            teamProject = requireNotNull(active().teamProject).copy(lastSettledWeek = 4))
        val first = TeamEngine.prepareWeek(state, plan(), random)
        assertEquals(5, first.game.teamProject?.incidentWeek)
        val calls = random.calls
        TeamEngine.prepareWeek(first.game, first.schedule, random)
        assertEquals(calls, random.calls)
    }

    @Test fun threeWeeklySettlementsAwardBonusAndNetworkOnce() {
        var state = active(bonus = 7).copy(day = 7, teamBonuses = mapOf(subjects[0].id to 2), network = 10)
        val first = TeamEngine.settleWeek(state)
        assertEquals(2, first.teamProject?.weeksLeft)
        assertSame(first, TeamEngine.settleWeek(first))
        state = TeamEngine.settleWeek(first.copy(week = 5))
        assertEquals(1, state.teamProject?.weeksLeft)
        val finished = TeamEngine.settleWeek(state.copy(week = 6))
        assertNull(finished.teamProject)
        assertEquals(18, finished.network)
        assertEquals(9, finished.teamBonuses.getValue(subjects[0].id))
        assertEquals(1, finished.teamResults.size)
        assertEquals(6, finished.teamResults.single().endWeek)
        assertSame(finished, TeamEngine.settleWeek(finished))
    }

    @Test fun skippedWeekDoesNotCompleteProjectAndNetworkRewardIsClamped() {
        val skipped = active().copy(week = 6, day = 7)
        assertSame(skipped, TeamEngine.settleWeek(skipped))
        val finalWeek = active(week = 6).copy(day = 7, network = 99)
        assertEquals(100, TeamEngine.settleWeek(finalWeek).network)
    }

    @Test fun finishedMeetingsReleaseToRestWhileClassesAndSleepStay() {
        val state = active(week = 6).copy(day = 7)
        val locked = TeamEngine.syncSchedule(state, plan())
        val finished = TeamEngine.settleWeek(state)
        val released = TeamEngine.syncSchedule(finished, locked)
        assertEquals(ActivityType.REST, released[1].activity)
        assertEquals(ActivityType.REST, released[5].activity)
        for (index in plan().indices) {
            if (plan()[index].locked || index % 4 == 3) assertEquals(plan()[index], released[index])
        }
    }

    @Test fun routinesKeepCurrentMeetingsAndDoNotStoreFixedSlots() {
        val state = active()
        val locked = TeamEngine.syncSchedule(state, plan())
        val captured = RoutineRules.capture(locked)
        assertEquals(ActivityType.EMPTY, captured[0].activity)
        assertEquals(ActivityType.EMPTY, captured[1].activity)
        val routine = ScheduleRoutine("test", "알바 위주", List(28) { TimeSlot(ActivityType.WORK) })
        val applied = RoutineRules.apply(routine, locked, state)
        assertEquals(locked[0], applied[0])
        assertEquals(locked[1], applied[1])
        assertEquals(ActivityType.WORK, applied[2].activity)
    }

    @Test fun teamDilemmaUsesMeetingDaysAndCannotAwardGpaTwice() {
        val state = active()
        val schedule = TeamEngine.syncSchedule(state, List(28) { TimeSlot() })
        val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
        val offered = EventEngine.advanceDay(state, schedule, subjects, always)
        assertEquals("team", offered.pendingEvent?.eventId)
        val key = requireNotNull(offered.pendingEvent).key
        val chosen = EventEngine.choose(offered, key, 0)
        assertEquals(0.04, chosen.semesterGpaAdjustment, 0.000001)
        assertEquals(60, chosen.hp)
        assertSame(chosen, EventEngine.choose(chosen, key, 0))
        val noTeamEvent = chosen.copy(week = 5, pendingEvent = null, eventCheckedDay = -1)
        assertTrue(EventEngine.dailyCandidates(noTeamEvent, schedule).none { it.id == "team" })
    }

    @Test fun teamDilemmaPenaltyCannotMakeBonusOrNetworkNegative() {
        val state = active(bonus = 2).copy(network = 5)
        val schedule = TeamEngine.syncSchedule(state, List(28) { TimeSlot() })
        val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
        val offered = EventEngine.advanceDay(state, schedule, subjects, always)
        val changed = EventEngine.choose(offered, requireNotNull(offered.pendingEvent).key, 2)
        assertEquals(0, changed.teamProject?.bonus)
        assertEquals(0, changed.network)
        assertTrue(requireNotNull(changed.pendingEvent).changes.contains("현재 과제 가산점 -2점"))
    }

    @Test fun semesterScoreAddsProjectBonusOnceAndGpaAdjustmentIsSeparate() {
        val ids = subjects.map { it.id }
        val state = fresh(16).copy(day = 7, midtermScores = ids.associateWith { 80 },
            finalScores = ids.associateWith { 80 }, teamBonuses = mapOf(ids[0] to 7),
            semesterGpaAdjustment = 0.04)
        val finished = ExamEngine.finishSemester(state)
        val result = requireNotNull(finished.semesterResult)
        assertEquals(87, result.grades.first { it.subjectId == ids[0] }.score)
        assertEquals(80, result.grades.first { it.subjectId == ids[1] }.score)
        assertEquals((3.5 + 3.0 + 3.0) / 3 + 0.04, result.gpa, 0.000001)
        assertEquals(result.gpa, finished.cumulativeGpa, 0.000001)
        assertSame(finished, ExamEngine.finishSemester(finished))
    }

    @Test fun finalScoreAndGpaStayWithinTheirCaps() {
        val ids = subjects.map { it.id }
        val state = fresh(16).copy(day = 7, midtermScores = ids.associateWith { 100 },
            finalScores = ids.associateWith { 100 }, teamBonuses = mapOf(ids[0] to 20),
            semesterGpaAdjustment = 0.04)
        val result = requireNotNull(ExamEngine.finishSemester(state).semesterResult)
        assertTrue(result.grades.all { it.score == 100 })
        assertEquals(4.5, result.gpa, 0.000001)
    }

    @Test fun newSemesterClearsCurrentBonusesButKeepsSubmissionHistory() {
        val record = SemesterResult(1, subjects.map { SubjectGrade(it.id, 90, "A0", 4.0) }, 0.04)
        val history = TeamProjectResult(1, 4, 6, subjects[0].id, listOf("ppt", "good", "ghost"), 8)
        val state = fresh(16).copy(day = 7, semesterResult = record,
            vacationHistory = listOf(VacationRecord(1, "work")), teamResults = listOf(history),
            teamBonuses = mapOf(subjects[0].id to 8), semesterGpaAdjustment = 0.04)
        val next = AcademicEngine.advanceSemester(state)
        assertTrue(next.teamBonuses.isEmpty())
        assertNull(next.teamProject)
        assertEquals(0.0, next.semesterGpaAdjustment, 0.000001)
        assertEquals(state.teamResults, next.teamResults)
        assertEquals(4.04, next.cumulativeGpa, 0.000001)
    }

    @Test fun fullSemesterCompletesBothProjectsAndKeepsScheduleUsable() {
        var state = fresh(1)
        var schedule = plan()
        for (week in 1..16) {
            val prepared = TeamEngine.prepareWeek(state, schedule, Random(100 + week))
            state = prepared.game
            schedule = prepared.schedule
            state.teamProject?.let { state = TeamEngine.acknowledge(state, it.key) }
            repeat(7) {
                state = GameEngine.advanceDay(state, schedule, subjects)
                schedule = TeamEngine.syncSchedule(state, schedule)
            }
            assertEquals(7, state.day)
            if (week == 8) state = state.copy(midtermScores = state.study.keys.associateWith { 80 })
            if (week < 16) {
                state = GameEngine.nextWeek(state)
                assertEquals(week + 1, state.week)
            }
        }
        assertNull(state.teamProject)
        assertEquals(listOf(4, 11), state.teamResults.map { it.startWeek })
        assertEquals(listOf(6, 13), state.teamResults.map { it.endWeek })
        assertEquals(state.teamResults.sumOf { it.bonus }, state.teamBonuses.values.sum())
        assertTrue(schedule.none { it.activity == ActivityType.TEAM })
        assertEquals(6, schedule.count { it.activity == ActivityType.CLASS })
        assertEquals(7, schedule.count { it.activity == ActivityType.SLEEP })
    }
}
