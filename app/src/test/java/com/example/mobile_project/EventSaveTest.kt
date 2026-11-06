package com.example.mobile_project

import androidx.compose.runtime.saveable.SaverScope
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class EventSaveTest {
    private val saverScope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }
    private val subjects = firstYearSubjects.take(3)
    private fun fresh() = GameEngine.newGame(subjects.map { it.id })

    private fun roundTrip(state: GameState): GameState {
        val raw = requireNotNull(with(gameStateSaver) { saverScope.save(state) })
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(raw) }
        val restoredRaw = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        return requireNotNull(gameStateSaver.restore(restoredRaw))
    }

    @Test fun unansweredEventRoundTripsWithIdentityAndHistory() {
        val offered = EventEngine.startWeeklyEvent(fresh().copy(week = 2, day = 7))
        val restored = roundTrip(offered)
        assertEquals(offered, restored)
        EventEngine.validatePending(restored)
        assertSame(restored, EventEngine.startWeeklyEvent(restored))
    }

    @Test fun answeredEventRoundTripsAndCannotRewardAgain() {
        val offered = EventEngine.startWeeklyEvent(fresh().copy(week = 2, day = 7))
        val key = requireNotNull(offered.pendingEvent).key
        val chosen = EventEngine.choose(offered, key, 0)
        val restored = roundTrip(chosen)
        assertEquals(chosen, restored)
        EventEngine.validatePending(restored)
        assertSame(restored, EventEngine.choose(restored, key, 0))
    }

    @Test fun eventBuffsAndCheckDaySurviveObjectSerialization() {
        val state = fresh().copy(eventCheckedDay = 13,
            eventBuffs = listOf(EventBuff(ActivityType.STUDY, 1.3, 7, 13, "이번 주 공부 능률 ▲30%")))
        assertEquals(state, roundTrip(state))
    }

    @Test fun oldSaveWithoutEventKeysLoadsWithDefaults() {
        val original = fresh().copy(week = 5, day = 2, money = 222_222)
        val raw = requireNotNull(with(gameStateSaver) { saverScope.save(original) }) as List<*>
        val newKeys = setOf("campusEvent", "eventChanges", "eventHistory", "eventBuffs", "eventCheckedDay")
        val legacyRaw = ArrayList(raw.chunked(2).filterNot { it[0] in newKeys }.flatten())
        val restored = requireNotNull(gameStateSaver.restore(legacyRaw))
        assertEquals(original, restored)
        assertNull(restored.pendingEvent)
        assertTrue(restored.eventBuffs.isEmpty())
        assertTrue(restored.eventHistory.isEmpty())
        assertEquals(-1, restored.eventCheckedDay)
    }

    @Test fun examStateStillRoundTripsWhenThereIsNoCampusEvent() {
        val pending = ExamEngine.startPendingExam(fresh().copy(week = 8, day = 7), Random(7))
        val answering = ExamEngine.beginExam(pending, 123_456L)
        assertEquals(answering, roundTrip(answering))
    }
}
