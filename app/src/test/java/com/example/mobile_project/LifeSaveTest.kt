package com.example.mobile_project

import androidx.compose.runtime.saveable.SaverScope
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class LifeSaveTest {
    private val scope = object : SaverScope { override fun canBeSaved(value: Any): Boolean = true }
    private val subjects = subjectsForSemester(1).take(3)
    private val ids = subjects.map { it.id }
    private val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
    private val forbiddenRandom = object : Random() { override fun nextBits(bitCount: Int): Int = error("unexpected reroll") }
    private val never = object : Random() {
        override fun nextBits(bitCount: Int): Int = if (bitCount == 0) 0 else -1 ushr (32 - bitCount)
    }
    private fun fresh() = GameEngine.newGame(ids)
    private fun plan(activity: ActivityType = ActivityType.CLASS): List<TimeSlot> = List(28) {
        if (it == 0) TimeSlot(activity, if (activity == ActivityType.CLASS) ids[0] else null) else TimeSlot()
    }
    private fun ready() = LifeEngine.prepareMorning(fresh().copy(hp = 20), plan(), always)
    private fun roundTrip(state: GameState): GameState {
        val raw = requireNotNull(with(gameStateSaver) { scope.save(state) })
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(raw) }
        val restoredRaw = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        return requireNotNull(gameStateSaver.restore(restoredRaw))
    }

    @Test fun stageElevenSaveWithoutLifeFieldStillLoads() {
        val original = fresh()
        val raw = requireNotNull(with(gameStateSaver) { scope.save(original) }) as List<*>
        val fields = raw.chunked(2).associate { (it[0] as String) to it[1] }.toMutableMap()
        fields.remove("life")
        val restored = requireNotNull(gameStateSaver.restore(ArrayList(fields.flatMap { listOf(it.key, it.value) })))
        assertEquals(original, restored)
        assertEquals(LifeState(), restored.life)
        LifeEngine.validateState(restored, plan())
    }

    @Test fun readyMorningKeepsSameAutomaticResultWithoutRerolling() {
        val original = ready()
        val restored = roundTrip(original)
        assertEquals(original, restored)
        assertSame(restored, LifeEngine.prepareMorning(restored, plan(), forbiddenRandom))
        LifeEngine.validateState(restored, plan())
        val key = requireNotNull(restored.life.pendingMorning).key
        val expected = requireNotNull(original.life.pendingMorning).autoSuccess
        assertEquals(expected, LifeEngine.skipMorning(restored, key, 1_000L).life.pendingMorning?.success)
    }

    @Test fun savedTapCountAndDeadlineContinueAndExpireWhileAppIsClosed() {
        val ready = ready()
        val key = requireNotNull(ready.life.pendingMorning).key
        val begun = LifeEngine.beginMorning(ready, key, 1_000L)
        val tapped = LifeEngine.tapMorning(begun, key, 0, 1_500L)
        val restored = roundTrip(tapped)
        assertEquals(tapped, restored)
        assertEquals(1, restored.life.pendingMorning?.taps)
        assertEquals(5_000L, restored.life.pendingMorning?.deadlineMs)
        LifeEngine.validateState(restored, plan())
        val expired = LifeEngine.refreshMorning(restored, 9_000L)
        assertEquals(MorningPhase.RESULT, expired.life.pendingMorning?.phase)
        assertEquals(1, expired.life.lateCounts[ids[0]])
        assertEquals(expired, roundTrip(expired))
        assertSame(expired, LifeEngine.refreshMorning(expired, 99_000L))
    }

    @Test fun failedResultCannotApplyAnotherPenaltyAfterReload() {
        val ready = ready()
        val key = requireNotNull(ready.life.pendingMorning).key
        val failed = LifeEngine.skipMorning(LifeEngine.beginMorning(ready, key, 1_000L), key, 1_001L)
        val restored = roundTrip(failed)
        LifeEngine.validateState(restored, plan())
        assertSame(restored, LifeEngine.skipMorning(restored, key, 1_002L))
        val done = LifeEngine.continueMorning(restored, plan(), subjects, key, never)
        val loaded = roundTrip(done)
        assertEquals(1, loaded.life.lateCounts[ids[0]])
        assertEquals(1, loaded.life.attendanceHistory.size)
        assertNull(loaded.life.morningNotice)
        assertSame(loaded, LifeEngine.continueMorning(loaded, plan(), subjects, key, forbiddenRandom))
    }

    @Test fun morningNoticeSurvivesSavedActivityMiniAndDilemma() {
        val schedule = plan(ActivityType.WORK)
        val offered = LifeEngine.prepareMorning(fresh().copy(hp = 20), schedule, always)
        val key = requireNotNull(offered.life.pendingMorning).key
        val result = LifeEngine.skipMorning(offered, key, 1_000L)
        val mini = LifeEngine.continueMorning(result, schedule, subjects, key, always)
        val loadedMini = roundTrip(mini)
        assertNotNull(loadedMini.pendingMiniGame)
        assertEquals(mini.life.morningNotice, loadedMini.life.morningNotice)
        LifeEngine.validateState(loadedMini, schedule)
        val miniKey = requireNotNull(loadedMini.pendingMiniGame).key
        val skipped = MiniGameEngine.skipOrForfeit(loadedMini, miniKey, 1_001L)
        val dilemma = DayFlowEngine.continueMini(skipped, schedule, subjects, miniKey, always)
        val loadedDilemma = roundTrip(dilemma)
        LifeEngine.validateState(loadedDilemma, schedule)
        assertNotNull(loadedDilemma.pendingEvent)
        assertNotNull(loadedDilemma.life.morningNotice)
    }

    @Test fun savedSicknessAndWeeklyCheckCannotBeRerolled() {
        val settled = LifeEngine.settleWeek(fresh().copy(day = 7, hp = 0), always)
        val restored = roundTrip(settled)
        assertEquals(settled, restored)
        assertSame(restored, LifeEngine.settleWeek(restored, forbiddenRandom))
        assertTrue(LifeEngine.isSick(GameEngine.nextWeek(restored)))
        LifeEngine.validateState(restored, plan())
    }

    @Test fun lotteryWinAndLossBothPersistWithoutChargingOrPayingAgain() {
        for (bucket in listOf(0, 995)) {
            val random = object : Random() {
                override fun nextBits(bitCount: Int): Int = error("use nextInt only")
                override fun nextInt(until: Int): Int = bucket
            }
            val bought = LifeEngine.buyLottery(fresh(), 1, 1, random)
            val restored = roundTrip(bought)
            assertEquals(bought, restored)
            assertSame(restored, LifeEngine.buyLottery(restored, 1, 1, forbiddenRandom))
            LifeEngine.validateState(restored, plan())
            assertTrue(LifeEngine.canBuyLottery(restored.copy(week = 2)))
        }
    }

    @Test fun duplicateLotteryWeekAndWrongMorningCalendarAreRejected() {
        val invalidLottery = fresh().copy(life = LifeState(lotteryHistory = listOf(
            LotteryTicket(1, 1, 0), LotteryTicket(1, 1, 3))))
        for (state in listOf(invalidLottery, ready().copy(day = 1))) {
            try { LifeEngine.validateState(state, plan()); fail("invalid save accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
