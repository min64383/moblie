package com.example.mobile_project

import androidx.compose.runtime.saveable.SaverScope
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class MiniGameSaveTest {
    private val scope = object : SaverScope { override fun canBeSaved(value: Any): Boolean = true }
    private val subjects = subjectsForSemester(1).take(3)
    private val never = object : Random() {
        override fun nextBits(bitCount: Int): Int = if (bitCount == 0) 0 else -1 ushr (32 - bitCount)
    }
    private val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
    private fun fresh() = GameEngine.newGame(subjects.map { it.id })
    private fun activity(kind: MiniKind) = when (kind) {
        MiniKind.ORDER, MiniKind.CHANGE -> ActivityType.WORK
        MiniKind.TUTOR -> ActivityType.TUTOR
        MiniKind.INTERVIEW -> ActivityType.CAREER
        MiniKind.TIMING, MiniKind.HOLD -> ActivityType.RUN
        else -> ActivityType.STUDY
    }
    private fun plan(kind: MiniKind) = List(28) {
        if (it == 0) TimeSlot(activity(kind), if (activity(kind) == ActivityType.STUDY) subjects[0].id else null)
        else TimeSlot()
    }
    private fun ready(kind: MiniKind): GameState {
        var state = fresh()
        if (kind == MiniKind.INTERVIEW) state = state.copy(semester = 5)
        if (kind == MiniKind.TUTOR) state = state.copy(transcript = listOf(
            SemesterResult(1, subjects.map { SubjectGrade(it.id, 90, "A0", 4.0) })))
        val run = MiniGameEngine.buildRun(state, plan(kind), 0, kind, Random(20))
        return state.copy(pendingMiniGame = run, miniCheckedDay = EventEngine.absoluteDay(state))
    }
    private fun roundTrip(state: GameState): GameState {
        val raw = requireNotNull(with(gameStateSaver) { scope.save(state) })
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(raw) }
        val loaded = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        return requireNotNull(gameStateSaver.restore(loaded))
    }

    @Test fun allNineKindsKeepTheirFrozenQuestionsChoicesTipsAndTimers() {
        for (kind in MiniKind.values()) {
            val original = ready(kind)
            val restored = roundTrip(original)
            assertEquals(original, restored)
            MiniGameEngine.validateState(restored, plan(kind))
            assertSame(restored, MiniGameEngine.prepareDay(restored, plan(kind), Random(999)))
            val begun = MiniGameEngine.begin(restored, requireNotNull(restored.pendingMiniGame).key, 10_000L)
            val active = roundTrip(begun)
            assertEquals(begun, active)
            MiniGameEngine.validateState(active, plan(kind))
        }
    }

    @Test fun quizProgressAndDeadlineContinueFromSavedState() {
        val ready = ready(MiniKind.CALC)
        val begun = MiniGameEngine.begin(ready, requireNotNull(ready.pendingMiniGame).key, 1_000L)
        val run = requireNotNull(begun.pendingMiniGame)
        val answered = MiniGameEngine.input(begun, run.key, run.questions[0].correctIndex, run.revision, 2_000L)
        val restored = roundTrip(answered)
        assertEquals(answered, restored)
        assertEquals(1, restored.pendingMiniGame?.progress)
        assertEquals(12_000L, restored.pendingMiniGame?.deadlineMs)
        val expired = MiniGameEngine.refresh(restored, 12_000L)
        assertEquals(MiniPhase.RESULT, expired.pendingMiniGame?.phase)
        assertEquals(1.05, requireNotNull(expired.pendingMiniGame?.reward).studyMultiplier, 0.000001)
    }

    @Test fun backgroundExpiryDoesNotStartANewTimerWhenReopened() {
        val ready = ready(MiniKind.HOLD)
        val begun = MiniGameEngine.begin(ready, requireNotNull(ready.pendingMiniGame).key, 1_000L)
        val restored = roundTrip(begun)
        val expired = MiniGameEngine.refresh(restored, 60_000L)
        assertEquals(MiniPhase.RESULT, expired.pendingMiniGame?.phase)
        assertSame(expired, MiniGameEngine.begin(expired, requireNotNull(expired.pendingMiniGame).key, 60_001L))
    }

    @Test fun orderPreviewAndSelectedOrdersSurviveSaving() {
        val ready = ready(MiniKind.ORDER)
        val key = requireNotNull(ready.pendingMiniGame).key
        val begun = MiniGameEngine.begin(ready, key, 1_000L)
        val restoredPreview = roundTrip(begun)
        assertSame(restoredPreview, MiniGameEngine.refresh(restoredPreview, 3_999L))
        val active = MiniGameEngine.refresh(restoredPreview, 4_000L)
        val run = requireNotNull(active.pendingMiniGame)
        val selected = MiniGameEngine.input(active, key, run.sequence.first(), run.revision, 4_100L)
        val restored = roundTrip(selected)
        assertEquals(selected, restored)
        assertEquals(listOf(run.sequence.first()), restored.pendingMiniGame?.selected)
        MiniGameEngine.validateState(restored, plan(MiniKind.ORDER))
    }

    @Test fun resultCanBeReloadedAndContinuedExactlyOnce() {
        val ready = ready(MiniKind.CHANGE)
        val key = requireNotNull(ready.pendingMiniGame).key
        val begun = MiniGameEngine.begin(ready, key, 1_000L)
        val run = requireNotNull(begun.pendingMiniGame)
        val solved = MiniGameEngine.input(begun, key, run.questions[0].correctIndex, run.revision, 2_000L)
        val restored = roundTrip(solved)
        assertEquals(solved, restored)
        MiniGameEngine.validateState(restored, plan(MiniKind.CHANGE))
        val done = DayFlowEngine.continueMini(restored, plan(MiniKind.CHANGE), subjects, key, never)
        assertEquals(381_000, done.money)
        assertNull(done.miniBonus)
        val savedDone = roundTrip(done)
        assertSame(savedDone, DayFlowEngine.continueMini(savedDone, plan(MiniKind.CHANGE), subjects, key, always))
    }

    @Test fun bonusSurvivesDilemmaSaveAndIsAppliedOnlyAfterDilemma() {
        val ready = ready(MiniKind.CHANGE)
        val key = requireNotNull(ready.pendingMiniGame).key
        val begun = MiniGameEngine.begin(ready, key, 1_000L)
        val run = requireNotNull(begun.pendingMiniGame)
        val solved = MiniGameEngine.input(begun, key, run.questions[0].correctIndex, run.revision, 2_000L)
        val offered = DayFlowEngine.continueMini(solved, plan(MiniKind.CHANGE), subjects, key, always)
        val restored = roundTrip(offered)
        assertEquals(offered, restored)
        assertEquals(350_000, restored.money)
        MiniGameEngine.validateState(restored, plan(MiniKind.CHANGE))
        val eventKey = requireNotNull(restored.pendingEvent).key
        val chosen = EventEngine.choose(restored, eventKey, 0)
        val savedChoice = roundTrip(chosen)
        val done = EventEngine.continueEvent(savedChoice, plan(MiniKind.CHANGE), subjects, eventKey)
        assertNull(done.miniBonus)
        assertEquals(1, done.day)
        assertEquals(done, roundTrip(done))
    }

    @Test fun stageTenSaveWithoutNewFieldsLoadsWithMinigamesEnabled() {
        val original = fresh()
        val raw = requireNotNull(with(gameStateSaver) { scope.save(original) }) as List<*>
        val values = raw.chunked(2).associate { (it[0] as String) to it[1] }.toMutableMap()
        for (key in listOf("miniRun", "miniBonus", "miniCheckedDay", "skipMiniGames")) values.remove(key)
        val loaded = requireNotNull(gameStateSaver.restore(ArrayList(values.flatMap { listOf(it.key, it.value) })))
        assertEquals(original, loaded)
        assertNull(loaded.pendingMiniGame)
        assertNull(loaded.miniBonus)
        assertEquals(-1, loaded.miniCheckedDay)
        assertFalse(loaded.skipMiniGames)
    }

    @Test fun disabledPreferenceAndCheckedDayRemainSaved() {
        val state = MiniGameEngine.prepareDay(fresh().copy(skipMiniGames = true), plan(MiniKind.CALC), always)
        val restored = roundTrip(state)
        assertTrue(restored.skipMiniGames)
        assertSame(restored, MiniGameEngine.prepareDay(restored, plan(MiniKind.CALC), always))
    }
}
