package com.example.mobile_project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayLogTest {
    @Test fun parsesBlockWithDeltas() {
        val e = DayLog.parseLine("오전 · 자료구조 공부\n체력 -9 · 행복도 -3 · 공부량 +12") as DayLog.Block
        assertEquals(0, e.block)
        assertEquals("자료구조 공부", e.title)
        assertEquals(listOf(DayLog.Delta("체력", -9), DayLog.Delta("행복도", -3), DayLog.Delta("공부량", 12)), e.deltas)
        assertNull(e.note)
    }

    @Test fun parsesMoneyWithUnit() {
        val e = DayLog.parseLine("저녁 · 알바\n체력 -13 · 돈 +25000원") as DayLog.Block
        assertEquals(DayLog.Delta("돈", 25000, "원"), e.deltas[1])
        assertEquals("돈 +25000원", e.deltas[1].text)
    }

    @Test fun cancelledBlockKeepsNote() {
        val e = DayLog.parseLine("밤 · PT 취소\n잔액 한도 때문에 진행하지 못했어요.") as DayLog.Block
        assertEquals(3, e.block)
        assertTrue(e.deltas.isEmpty())
        assertEquals("잔액 한도 때문에 진행하지 못했어요.", e.note)
    }

    @Test fun noChange() {
        val e = DayLog.parseLine("오후 · 아무 활동 없음\n변화 없음") as DayLog.Block
        assertTrue(e.deltas.isEmpty())
        assertNull(e.note)
    }

    @Test fun notesAreTaggedAndJudged() {
        val late = DayLog.parseLine("늦잠 · 자료구조 수업에 지각했어요.") as DayLog.Note
        assertEquals("늦잠", late.tag)
        assertTrue(late.bad)
        val ok = DayLog.parseLine("늦잠 · 전력질주로 오전 일정에 제시간에 도착했어요.") as DayLog.Note
        assertEquals(false, ok.bad)
        val plain = DayLog.parseLine("조원이 자료를 보내 왔어요") as DayLog.Note
        assertNull(plain.tag)
    }

    @Test fun totalsSumBlocksOnly() {
        val entries = DayLog.parse(listOf(
            "오전 · 공부\n체력 -9 · 행복도 -3",
            "오후 · 휴식\n체력 +11 · 행복도 +4",
            "주간 결산 · 생활비 25000원 청구"
        ))
        assertEquals(mapOf("체력" to 2, "행복도" to 1), DayLog.totals(entries))
    }
}
