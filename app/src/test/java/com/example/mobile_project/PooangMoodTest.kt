package com.example.mobile_project

import org.junit.Assert.assertEquals
import org.junit.Test

class PooangMoodTest {
    private fun mood(hp: Int = 80, hap: Int = 60, sick: Boolean = false, love: Boolean = false) =
        PooangMood.of(hp = hp, maxHp = 100, happiness = hap, sick = sick, inLove = love)

    @Test fun sickBeatsEverything() = assertEquals(PooangMood.SICK, mood(hap = 95, sick = true, love = true))
    @Test fun lowHappinessIsSad() = assertEquals(PooangMood.SAD, mood(hp = 10, hap = 19))
    @Test fun lowHpIsTired() = assertEquals(PooangMood.TIRED, mood(hp = 24, hap = 90))
    @Test fun loveNeedsHappiness70() {
        assertEquals(PooangMood.LOVE, mood(hap = 70, love = true))
        assertEquals(PooangMood.OK, mood(hap = 69, love = true))
    }
    @Test fun happyNeedsHpAndHappiness() {
        assertEquals(PooangMood.HAPPY, mood(hp = 60, hap = 80))
        assertEquals(PooangMood.OK, mood(hp = 59, hap = 80))
    }
    @Test fun midHappinessIsTired() = assertEquals(PooangMood.TIRED, mood(hap = 39))
    @Test fun defaultIsOk() = assertEquals(PooangMood.OK, mood())
    @Test fun zeroMaxHpDoesNotCrash() =
        assertEquals(PooangMood.TIRED, PooangMood.of(0, 0, 60, sick = false, inLove = false))

    @Test fun fromKeyFallsBackToOk() {
        assertEquals(PooangMood.YUM, PooangMood.fromKey("yum"))
        assertEquals(PooangMood.OK, PooangMood.fromKey("unknown"))
        assertEquals(PooangMood.OK, PooangMood.fromKey(null))
    }

    @Test fun defaultGameStateIsOk() = assertEquals(PooangMood.OK, PooangMood.of(GameState()))
}
