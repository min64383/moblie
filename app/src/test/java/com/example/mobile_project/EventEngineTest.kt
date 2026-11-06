package com.example.mobile_project

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class EventEngineTest {
    private val subjects = firstYearSubjects.take(3)
    private fun fresh() = GameEngine.newGame(subjects.map { it.id })
    private val alwaysEvent = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }
    private val neverEvent = object : Random() {
        override fun nextBits(bitCount: Int): Int =
            if (bitCount == 0) 0 else -1 ushr (32 - bitCount)
    }
    private fun schedule(activity: ActivityType): List<TimeSlot> = List(28) {
        if (it % 4 == 0) TimeSlot(activity,
            if (activity == ActivityType.STUDY || activity == ActivityType.CLASS) subjects[0].id else null)
        else TimeSlot()
    }
    private fun workEvent(state: GameState = fresh()) =
        EventEngine.advanceDay(state, schedule(ActivityType.WORK), subjects, alwaysEvent)

    @Test fun eventOffersBeforeDailyActionsAndDoesNotReroll() {
        val offered = workEvent()
        assertEquals("work", offered.pendingEvent?.eventId)
        assertEquals(0, offered.day)
        assertEquals(350_000, offered.money)
        assertEquals(1, offered.eventHistory.size)
        assertSame(offered, workEvent(offered))
    }

    @Test fun dayWithoutEventRunsNormally() {
        val state = EventEngine.advanceDay(fresh(), schedule(ActivityType.WORK), subjects, neverEvent)
        assertNull(state.pendingEvent)
        assertEquals(1, state.day)
        assertEquals(375_000, state.money)
    }

    @Test fun choiceAndContinueCannotApplyTwice() {
        val offered = workEvent()
        val key = requireNotNull(offered.pendingEvent).key
        val chosen = EventEngine.choose(offered, key, 0)
        assertEquals(EventPhase.RESULT, chosen.pendingEvent?.phase)
        assertEquals(360_000, chosen.money)
        assertSame(chosen, EventEngine.choose(chosen, key, 0))
        val progressed = EventEngine.continueEvent(chosen, schedule(ActivityType.WORK), subjects, key)
        assertNull(progressed.pendingEvent)
        assertEquals(1, progressed.day)
        assertEquals(385_000, progressed.money)
        assertTrue(progressed.log.first().contains("뜨거운 아이스 아메리카노"))
        assertSame(progressed, EventEngine.continueEvent(progressed, schedule(ActivityType.WORK), subjects, key))
    }

    @Test fun staleKeyInvalidChoiceAndEarlyContinueAreIgnored() {
        val offered = workEvent()
        val key = requireNotNull(offered.pendingEvent).key
        assertSame(offered, EventEngine.choose(offered, "old-key", 0))
        assertSame(offered, EventEngine.choose(offered, key, 99))
        assertSame(offered, EventEngine.continueEvent(offered, schedule(ActivityType.WORK), subjects, key))
        assertSame(offered, GameEngine.advanceDay(offered, schedule(ActivityType.WORK), subjects))
    }

    @Test fun dailyEventLimitIsTwoPerWeekAndResetsNextWeek() {
        val state = fresh().copy(eventHistory = listOf(
            CampusEventRecord(1, 1, 0, "work", 0),
            CampusEventRecord(1, 1, 1, "gym", 0)
        ))
        assertTrue(EventEngine.dailyCandidates(state, schedule(ActivityType.RUN)).isEmpty())
        assertTrue(EventEngine.dailyCandidates(state.copy(week = 2), schedule(ActivityType.RUN)).isNotEmpty())
    }

    @Test fun semesterLimitIsTwoForCommonAndOneForClub() {
        val common = fresh().copy(week = 3, eventHistory = listOf(
            CampusEventRecord(1, 1, 0, "work", 0),
            CampusEventRecord(1, 2, 0, "work", 0)
        ))
        assertTrue(EventEngine.dailyCandidates(common, schedule(ActivityType.WORK)).isEmpty())
        val rare = fresh().copy(week = 3,
            eventHistory = listOf(CampusEventRecord(1, 1, 0, "club", 0)))
        assertTrue(EventEngine.dailyCandidates(rare, schedule(ActivityType.CLUB)).isEmpty())
        assertTrue(EventEngine.dailyCandidates(rare.copy(semester = 2), schedule(ActivityType.CLUB)).isNotEmpty())
    }

    @Test fun studyEventOnlyAppearsDuringExamPeriod() {
        val plan = schedule(ActivityType.STUDY)
        assertTrue(EventEngine.dailyCandidates(fresh().copy(week = 5), plan).isEmpty())
        assertEquals("study", EventEngine.dailyCandidates(fresh().copy(week = 6), plan).single().id)
        assertTrue(EventEngine.dailyCandidates(fresh().copy(week = 9), plan).isEmpty())
        assertEquals("study", EventEngine.dailyCandidates(fresh().copy(week = 16), plan).single().id)
    }

    @Test fun classEventRequiresMorningClassAndTargetsThatSubject() {
        val morning = schedule(ActivityType.CLASS)
        val afternoon = morning.toMutableList().apply {
            this[1] = this[0]
            this[0] = TimeSlot()
        }
        assertTrue(EventEngine.dailyCandidates(fresh(), afternoon).isEmpty())
        val offered = EventEngine.advanceDay(fresh().copy(hp = 99), morning, subjects, alwaysEvent)
        val run = requireNotNull(offered.pendingEvent)
        assertEquals(subjects[0].id, run.subjectId)
        val chosen = EventEngine.choose(offered, run.key, 0)
        assertEquals(100, chosen.hp)
        assertEquals(3, chosen.study[subjects[0].id])
        assertTrue(requireNotNull(chosen.pendingEvent).changes.contains("체력 +1"))
        val progressed = EventEngine.continueEvent(chosen, morning, subjects, run.key)
        assertEquals(8, progressed.study[subjects[0].id])
    }

    @Test fun insufficientMoneyDisablesPaidChoiceButFreeChoiceWorks() {
        val offered = EventEngine.startWeeklyEvent(fresh().copy(week = 2, day = 7, money = -40_000))
        val run = requireNotNull(offered.pendingEvent)
        val event = requireNotNull(EventEngine.eventForId(run.eventId))
        assertFalse(EventEngine.canChoose(offered, event.options[0]))
        assertSame(offered, EventEngine.choose(offered, run.key, 0))
        val declined = EventEngine.choose(offered, run.key, 1)
        assertEquals(-40_000, declined.money)
        assertEquals(0, declined.network)
        assertEquals(EventPhase.RESULT, declined.pendingEvent?.phase)
    }

    @Test fun mtRunsOnceInFirstYearAndDoesNotAdvanceSundayTwice() {
        val original = fresh().copy(week = 2, day = 7, money = 300_000)
        val offered = EventEngine.startWeeklyEvent(original)
        val run = requireNotNull(offered.pendingEvent)
        assertEquals("mt", run.eventId)
        assertSame(offered, GameEngine.nextWeek(offered))
        val chosen = EventEngine.choose(offered, run.key, 0)
        val done = EventEngine.continueEvent(chosen, emptyList(), subjects, run.key)
        assertEquals(7, done.day)
        assertEquals(250_000, done.money)
        assertSame(done, EventEngine.startWeeklyEvent(done))
        assertNull(EventEngine.startWeeklyEvent(original.copy(semester = 3)).pendingEvent)
        assertEquals("mt", EventEngine.startWeeklyEvent(original.copy(semester = 2)).pendingEvent?.eventId)
    }

    @Test fun festivalRunsAtWeekNineAcrossAllSemesters() {
        for (semester in 1..8) {
            val offered = EventEngine.startWeeklyEvent(fresh().copy(semester = semester, week = 9, day = 7))
            assertEquals("fest", offered.pendingEvent?.eventId)
        }
        assertNull(EventEngine.startWeeklyEvent(fresh().copy(week = 10, day = 7)).pendingEvent)
    }

    @Test fun studyBuffAppliesBeforeTodaysStudyAndExpiresAfterWeek() {
        val plan = schedule(ActivityType.STUDY)
        val offered = EventEngine.advanceDay(fresh().copy(week = 6, happiness = 80), plan, subjects, alwaysEvent)
        val run = requireNotNull(offered.pendingEvent)
        val chosen = EventEngine.choose(offered, run.key, 0)
        assertEquals(1.3, EventEngine.multiplier(chosen, ActivityType.STUDY), 0.0001)
        val done = EventEngine.continueEvent(chosen, plan, subjects, run.key)
        assertEquals(28, done.study[subjects[0].id])
        assertEquals(1.0, EventEngine.multiplier(done.copy(week = 7, day = 0), ActivityType.STUDY), 0.0001)
        assertTrue(GameEngine.nextWeek(done.copy(day = 7)).eventBuffs.isEmpty())
    }

    @Test fun clubBuffStartsNextWeekAndOnlyBoostsHappinessAndNetwork() {
        val plan = schedule(ActivityType.CLUB)
        val offered = EventEngine.advanceDay(fresh(), plan, subjects, alwaysEvent)
        val run = requireNotNull(offered.pendingEvent)
        val chosen = EventEngine.choose(offered, run.key, 0)
        assertEquals(1.0, EventEngine.multiplier(chosen, ActivityType.CLUB), 0.0001)
        val nextWeek = chosen.copy(week = 2, day = 0, pendingEvent = null, happiness = 50)
        val done = GameEngine.advanceDay(nextWeek, plan, subjects)
        assertEquals(65, done.happiness)
        assertEquals(21, done.network)
        assertEquals(73, done.hp)
        assertEquals(347_000, done.money)
    }

    @Test fun previousSemesterBuffsAreClearedWhenAdvancingSemester() {
        val result = SemesterResult(1, listOf(SubjectGrade("cal", 90, "A0", 4.0)))
        val state = fresh().copy(week = 16, day = 7, semesterResult = result,
            vacationHistory = listOf(VacationRecord(1, "work")),
            eventBuffs = listOf(EventBuff(ActivityType.CLUB, 1.5, 112, 118, "다음 주 동아리 능률 ▲50%")),
            eventHistory = listOf(CampusEventRecord(1, 1, 0, "club", 0)))
        val next = AcademicEngine.advanceSemester(state)
        assertEquals(2, next.semester)
        assertTrue(next.eventBuffs.isEmpty())
        assertEquals(state.eventHistory, next.eventHistory)
    }

    @Test fun persistedCheckPreventsRerollForTheSameDay() {
        val state = fresh().copy(eventCheckedDay = EventEngine.absoluteDay(fresh()))
        val progressed = EventEngine.advanceDay(state, schedule(ActivityType.WORK), subjects, alwaysEvent)
        assertNull(progressed.pendingEvent)
        assertEquals(1, progressed.day)
    }
}
