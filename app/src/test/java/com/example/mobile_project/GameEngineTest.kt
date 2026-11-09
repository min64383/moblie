package com.example.mobile_project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private val subjects = listOf(
        Subject("cal", "미적분학", ""),
        Subject("phy", "일반물리학", ""),
        Subject("eng", "대학영어", "")
    )
    private val ids = subjects.map { it.id }
    private fun plan(vararg slots: TimeSlot): List<TimeSlot> =
        List(28) { index -> slots.getOrElse(index) { TimeSlot() } }

    @Test
    fun initialValuesMatchHtml() {
        val state = GameEngine.newGame(ids)
        assertEquals(80, state.hp)
        assertEquals(100, state.maxHp)
        assertEquals(60, state.happiness)
        assertEquals(350_000, state.money)
        assertEquals(ids.toSet(), state.study.keys)
    }

    @Test
    fun studyEfficiencyUsesStatsBeforeEachSlot() {
        val result = GameEngine.advanceDay(
            GameEngine.newGame(ids),
            plan(TimeSlot(ActivityType.STUDY, "cal"), TimeSlot(ActivityType.STUDY, "cal")),
            subjects
        )
        // 첫 공부: 80%×행복60 -> 18. 두 번째: 71%×행복57 -> 18.
        assertEquals(36, result.study["cal"])
        assertEquals(62, result.hp)
        assertEquals(54, result.happiness)
    }

    @Test
    fun lowStatsReduceStudyGain() {
        val initial = GameEngine.newGame(ids).copy(hp = 10, happiness = 10)
        val result = GameEngine.advanceDay(initial, plan(TimeSlot(ActivityType.STUDY, "cal")), subjects)
        assertEquals(2, result.study["cal"])
        assertEquals(1, result.hp)
        assertEquals(7, result.happiness)
    }

    @Test
    fun laterStudySeesEarlierWorkFatigue() {
        val result = GameEngine.advanceDay(
            GameEngine.newGame(ids),
            plan(
                TimeSlot(ActivityType.WORK),
                TimeSlot(ActivityType.STUDY, "cal"),
                TimeSlot(ActivityType.SLEEP)
            ),
            subjects
        )
        assertEquals(GameEngine.STUDY_GAIN, result.study["cal"])
        assertEquals(75, result.hp)
        assertEquals(375_000, result.money)
    }

    @Test
    fun paidActivityIsCancelledBeyondDebtLimit() {
        val initial = GameEngine.newGame(ids).copy(money = -44_000)
        val result = GameEngine.advanceDay(initial, plan(TimeSlot(ActivityType.FUN)), subjects)
        assertEquals(initial.hp, result.hp)
        assertEquals(initial.happiness, result.happiness)
        assertEquals(initial.money, result.money)
        assertTrue(result.log.first().contains("취소"))
    }

    @Test
    fun exactDebtLimitStillAllowsActivity() {
        val initial = GameEngine.newGame(ids).copy(money = -43_000)
        val result = GameEngine.advanceDay(initial, plan(TimeSlot(ActivityType.FUN)), subjects)
        assertEquals(-50_000, result.money)
        assertEquals(82, result.hp)
        assertEquals(78, result.happiness)
    }

    @Test
    fun restBoostRoundsNegativeAndPositiveEffectsLikeHtml() {
        val result = GameEngine.advanceDay(
            GameEngine.newGame(ids).copy(hp = 10),
            plan(TimeSlot(ActivityType.REST)),
            subjects
        )
        assertEquals(28, result.hp)
        assertEquals(66, result.happiness)
    }

    @Test
    fun gymIncreasesCurrentHealthOnlyByActualCapacityGain() {
        val initial = GameEngine.newGame(ids).copy(hp = 100, maxHp = 149)
        val result = GameEngine.advanceDay(initial, plan(TimeSlot(ActivityType.GYM)), subjects)
        assertEquals(150, result.maxHp)
        assertEquals(89, result.hp)
        assertEquals(12, result.charm)
        assertEquals(335_000, result.money)
    }

    @Test
    fun classesGiveStudyToTheirOwnSubjects() {
        val result = GameEngine.advanceDay(
            GameEngine.newGame(ids),
            plan(TimeSlot(ActivityType.CLASS, "phy")),
            subjects
        )
        assertEquals(GameEngine.CLASS_GAIN, result.study["phy"])
        assertEquals(0, result.study["cal"])
        assertEquals(74, result.hp)
        assertEquals(58, result.happiness)
    }

    @Test
    fun weekSettlementRunsOnceAndCanResumeNextWeek() {
        val schedule = List(28) { index ->
            if (index % 4 == 3) TimeSlot(ActivityType.SLEEP) else TimeSlot()
        }
        var state = GameEngine.newGame(ids)
        repeat(7) { state = GameEngine.advanceDay(state, schedule, subjects) }
        assertEquals(7, state.day)
        assertEquals(325_000, state.money)
        assertEquals(100, state.maxHp)
        assertEquals(9, state.charm)
        assertSame(state, GameEngine.advanceDay(state, schedule, subjects))
        val next = GameEngine.nextWeek(state)
        assertEquals(2, next.week)
        assertEquals(0, next.day)
        assertEquals(325_000, next.money)
        assertSame(next, GameEngine.nextWeek(next))
    }

    @Test
    fun insufficientSleepReducesCapacityWithoutDroppingBelowFloor() {
        val initial = GameEngine.newGame(ids).copy(day = 6, hp = 51, maxHp = 51, money = -49_000)
        val result = GameEngine.advanceDay(initial, plan(), subjects)
        assertEquals(50, result.maxHp)
        assertEquals(50, result.hp)
        assertEquals(-50_000, result.money)
    }

    @Test
    fun randomClassScheduleHasSixDistinctLockedWeekdaySlots() {
        repeat(50) {
            val schedule = createWeekSchedule(ids)
            assertEquals(28, schedule.size)
            assertEquals(6, schedule.count { it.locked })
            ids.forEach { id ->
                assertEquals(2, schedule.count { it.locked && it.subjectId == id })
            }
            schedule.forEachIndexed { index, slot ->
                if (slot.locked) {
                    assertTrue(index / 4 < 5)
                    assertFalse(index % 4 == 3)
                }
            }
            assertEquals(7, schedule.count { it.activity == ActivityType.SLEEP })
        }
    }
}
