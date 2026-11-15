package com.example.mobile_project

import com.example.mobile_project.ui.components.ChipTone
import org.junit.Assert.assertEquals
import org.junit.Test

class EventToneTest {
    @Test fun signs() {
        assertEquals(ChipTone.Up, EventTone.of("체력 +5"))
        assertEquals(ChipTone.Down, EventTone.of("돈 -10000원"))
        assertEquals(ChipTone.Up, EventTone.of("이번 주 공부 능률 ▲30%"))
        assertEquals(ChipTone.Down, EventTone.of("이번 주 공부 능률 ▼10%"))
        assertEquals(ChipTone.Down, EventTone.of("연애 종료"))
        assertEquals(ChipTone.Even, EventTone.of("수치 변화 없음"))
    }

    @Test fun overall() {
        assertEquals(ChipTone.Up, EventTone.overall(listOf("체력 +5", "행복도 +3", "돈 -1000원")))
        assertEquals(ChipTone.Down, EventTone.overall(listOf("체력 -5")))
    }
}
