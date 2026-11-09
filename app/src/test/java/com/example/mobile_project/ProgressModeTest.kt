package com.example.mobile_project

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressModeTest {
    private val subjects = firstYearSubjects.take(3)
    private val ids = subjects.map { it.id }

    private fun dayDone(state: GameState, day: Int, vararg lines: String) =
        state.copy(day = day + 1, log = lines.toList())

    @Test
    fun recordsEachFinishedDayOnceInOrder() {
        val base = GameEngine.newGame(ids)
        var book = ""
        book = WeekLogBook.record(book, dayDone(base, 1, "화요일 기록"))
        book = WeekLogBook.record(book, dayDone(base, 0, "월요일 기록", "둘째 줄"))
        book = WeekLogBook.record(book, dayDone(base, 0, "중복 기록"))
        val entries = WeekLogBook.decode(book, base.copy(day = 2))
        assertEquals(listOf(0, 1), entries.map { it.day })
        assertEquals(listOf("월요일 기록", "둘째 줄"), entries[0].lines)
        assertEquals("월요일", entries[0].dayName)
    }

    @Test
    fun newWeekStartsWithEmptyLog() {
        val base = GameEngine.newGame(ids)
        val book = WeekLogBook.record("", dayDone(base, 0, "1주차 월요일"))
        val nextWeek = base.copy(week = 2, day = 1, log = listOf("2주차 월요일"))
        assertTrue(WeekLogBook.decode(book, nextWeek).isEmpty())
        val updated = WeekLogBook.record(book, nextWeek)
        assertEquals(listOf("2주차 월요일"), WeekLogBook.decode(updated, nextWeek).single().lines)
    }

    @Test
    fun weekBeforeAnyDayOrEmptyLogIsNotRecorded() {
        val base = GameEngine.newGame(ids)
        assertEquals("", WeekLogBook.record("", base))
        assertEquals("", WeekLogBook.record("", base.copy(day = 1, log = emptyList())))
    }

    @Test
    fun autoAdvanceStopsForPlayerInputAndAtWeekEnd() {
        val running = GameEngine.newGame(ids).copy(day = 3)
        assertTrue(WeekLogBook.canAutoAdvance(running))
        assertFalse(WeekLogBook.canAutoAdvance(running.copy(day = 0)))
        assertFalse(WeekLogBook.canAutoAdvance(running.copy(day = 7)))
        assertFalse(WeekLogBook.canAutoAdvance(running.copy(
            pendingEvent = CampusEventRun("work", 1, 1, 3, null))))
    }

    @Test
    fun modeNameRoundTripsAndFallsBackToDay() {
        ProgressMode.values().forEach { assertEquals(it, ProgressMode.fromName(it.name)) }
        assertEquals(ProgressMode.DAY, ProgressMode.fromName("broken"))
    }

    // 딜레마는 한 주에 한 번까지만 뜬다.
    @Test
    fun dilemmaLimitIsOncePerWeek() {
        val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
        val work = List(28) { if (it % 4 == 0) TimeSlot(ActivityType.WORK) else TimeSlot() }
        val first = EventEngine.advanceDay(GameEngine.newGame(ids), work, subjects, always)
        assertEquals("work", first.pendingEvent?.eventId)
        val tuesday = first.copy(pendingEvent = null, day = 1)
        assertTrue(EventEngine.dailyCandidates(tuesday, work).isEmpty())
        assertEquals(1, EventEngine.WEEKLY_LIMIT)
    }

    // 하루 10%·주 1회 → 평균 2주에 한 번 정도 (학기 16주 기준 약 8회)
    @Test
    fun dilemmaRateIsAboutOnceEveryTwoWeeks() {
        val random = Random(42)
        var events = 0
        val weeks = 4000
        repeat(weeks) {
            var shown = 0
            repeat(7) { if (shown < EventEngine.WEEKLY_LIMIT && random.nextDouble() < EventEngine.DAY_CHANCE) shown++ }
            events += shown
        }
        val perWeek = events.toDouble() / weeks
        assertTrue("주당 $perWeek 회", perWeek in 0.45..0.60)
    }
}
