package com.example.mobile_project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelTest {
    private fun schedule(empty: Int = 0, sleep: Int = 7): List<TimeSlot> {
        val slots = MutableList(28) { TimeSlot(ActivityType.STUDY, "a") }
        var i = 0
        repeat(sleep) { slots[i++] = TimeSlot(ActivityType.SLEEP) }
        repeat(empty) { slots[i++] = TimeSlot() }
        return slots
    }

    @Test fun manWonFormats() {
        assertEquals("₩35만", manWon(350_000))
        assertEquals("₩1.3만", manWon(12_500))
        assertEquals("₩2만", manWon(20_000))
        assertEquals("₩9,000", manWon(9_000))
        assertEquals("−₩5만", manWon(-50_000))
        assertEquals("₩0", manWon(0))
    }

    @Test fun weeklyCostGrowsEveryYear() {
        assertEquals(25_000, HomeModel.weeklyCost(1))
        assertEquals(25_000, HomeModel.weeklyCost(2))
        assertEquals(30_000, HomeModel.weeklyCost(3))
        assertEquals(40_000, HomeModel.weeklyCost(8))
    }

    @Test fun missionWarnsAboutSleepFirst() {
        val m = HomeModel.mission(GameState(), schedule(empty = 3, sleep = 2))
        assertTrue(m.warn)
        assertEquals("주의", m.tag)
        assertEquals("25/28", m.progress)
    }

    @Test fun missionCountsEmptySlots() {
        val m = HomeModel.mission(GameState(), schedule(empty = 4))
        assertFalse(m.warn)
        assertEquals("시간표 빈칸 4개를 채우자", m.text)
    }

    @Test fun missionAtExamWeek() {
        val m = HomeModel.mission(GameState(week = 8), schedule())
        assertEquals("중간고사 주간! 공부 칸을 점검하자", m.text)
    }

    @Test fun missionWhenComplete() =
        assertEquals("시간표 완성! 시간을 보내자", HomeModel.mission(GameState(week = 3), schedule()).text)

    @Test fun missionWithoutSchedule() {
        val m = HomeModel.mission(GameState(), emptyList())
        assertFalse(m.warn)
        assertNull(m.progress)
    }

    @Test fun missionDuringWeek() {
        val m = HomeModel.mission(GameState(day = 2), schedule())
        assertEquals("화요일까지 진행했어요", m.text)
        assertEquals("2/7일", m.progress)
    }

    @Test fun missionWhenWeekEnded() =
        assertEquals("알림", HomeModel.mission(GameState(day = 7), schedule()).tag)

    @Test fun examCountdown() {
        assertEquals(7, HomeModel.examWeeksLeft(1))
        assertEquals(0, HomeModel.examWeeksLeft(8))
        assertEquals(7, HomeModel.examWeeksLeft(9))
        assertEquals("기말고사", HomeModel.examName(9))
    }

    @Test fun hudBadge() {
        assertEquals("중간고사", HomeModel.hudBadge(GameState(week = 8)))
        assertEquals("기말고사", HomeModel.hudBadge(GameState(week = 16)))
        assertNull(HomeModel.hudBadge(GameState(week = 3)))
    }

    @Test fun bubblePriorities() {
        assertEquals("통장이 한도까지 찼어요. 알바를 좀 해야겠어요.", HomeModel.bubble(GameState(money = -50_000)))
        assertEquals("너무 피곤해요. 이러다 늦잠 잘 것 같아요.", HomeModel.bubble(GameState(hp = 20)))
        assertEquals("중간고사 주간이에요! 이번 주가 끝나면 시험이에요.", HomeModel.bubble(GameState(week = 8)))
        assertEquals("이번 주는 어떻게 보낼까요?", HomeModel.bubble(GameState()))
    }

    @Test fun scheduleAttention() {
        assertTrue(HomeModel.scheduleNeedsAttention(GameState(), emptyList()))
        assertTrue(HomeModel.scheduleNeedsAttention(GameState(), schedule(empty = 1)))
        assertFalse(HomeModel.scheduleNeedsAttention(GameState(), schedule()))
        assertFalse(HomeModel.scheduleNeedsAttention(GameState(day = 3), schedule(empty = 5)))
    }

    @Test fun expectedAverageWithoutSubjectsIsZero() = assertEquals(0, HomeModel.expectedAverage(GameState()))

    @Test fun progressLabelDefault() {
        assertEquals("시간 보내기", HomeModel.progressLabel(GameState()))
        assertEquals("진행 결과 보기", HomeModel.progressLabel(GameState(day = 3)))
    }
}
