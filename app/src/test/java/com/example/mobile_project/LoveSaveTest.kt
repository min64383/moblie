package com.example.mobile_project

import androidx.compose.runtime.saveable.SaverScope
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class LoveSaveTest {
    private val scope = object : SaverScope { override fun canBeSaved(value: Any): Boolean = true }
    private val subjects = subjectsForSemester(3).take(3)
    private val partner = LoveCatalog.partners.first()
    private val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
    private fun fresh() = GameEngine.newGame(subjects.map { it.id })
        .copy(semester = 3, charm = 40, network = 30)
    private fun active() = fresh().copy(
        love = LoveRelationship(partner, 3, 1, 20, plannedWeek = 32), loveCheckedSemesters = listOf(3))
    private fun plan() = createWeekSchedule(subjects.map { it.id })
    private fun roundTrip(state: GameState): GameState {
        val raw = requireNotNull(with(gameStateSaver) { scope.save(state) })
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(raw) }
        val restoredRaw = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        return requireNotNull(gameStateSaver.restore(restoredRaw))
    }

    @Test fun pendingConfessionRestoresSameCandidateAndCanBeDeclinedOnce() {
        val prepared = LoveEngine.prepareWeek(fresh(), plan(), always)
        val restored = roundTrip(prepared.game)
        assertEquals(prepared.game, restored)
        LoveEngine.validateState(restored, prepared.schedule)
        assertSame(restored, LoveEngine.prepareWeek(restored, prepared.schedule, Random(100)).game)
        val key = requireNotNull(restored.pendingConfession).key
        val declined = LoveEngine.chooseConfession(restored, prepared.schedule, key, false)
        val loadedResult = roundTrip(declined.game)
        assertEquals(LoveChoice.DECLINE, loadedResult.pendingConfession?.choice)
        assertSame(loadedResult, LoveEngine.chooseConfession(loadedResult, declined.schedule, key, true).game)
        val completed = LoveEngine.completeConfession(loadedResult, key)
        assertNull(LoveEngine.prepareWeek(completed, declined.schedule, always).game.pendingConfession)
    }

    @Test fun acceptedConfessionAndItsDateRemainLockedAfterReload() {
        val prepared = LoveEngine.prepareWeek(fresh(), plan(), always)
        val key = requireNotNull(prepared.game.pendingConfession).key
        val accepted = LoveEngine.chooseConfession(prepared.game, prepared.schedule, key, true, always)
        val restored = roundTrip(accepted.game)
        assertEquals(accepted.game, restored)
        LoveEngine.validateState(restored, accepted.schedule)
        val slots = requireNotNull(with(weekScheduleSaver) { scope.save(accepted.schedule) })
        val schedule = requireNotNull(weekScheduleSaver.restore(slots))
        val index = requireNotNull(restored.love).dateIndex
        assertEquals(ActivityType.DATE, schedule[index].activity)
        assertTrue(schedule[index].locked)
        assertSame(restored, LoveEngine.chooseConfession(restored, schedule, key, true).game)
    }

    @Test fun completedDateCannotChargeMoneyAgainAfterReload() {
        val paid = LoveEngine.performDate(active().copy(day = 5), 20).game
        val restored = roundTrip(paid)
        assertEquals(paid, restored)
        assertSame(restored, LoveEngine.performDate(restored, 20).game)
        assertEquals(330_000, restored.money)
    }

    @Test fun appliedLoveIncidentDoesNotRepeatAfterReload() {
        val original = active().copy(love = requireNotNull(active().love).copy(incident = LoveIncident(0, 0)))
        val applied = LoveEngine.applyDayIncident(original)
        val restored = roundTrip(applied)
        assertEquals(applied, restored)
        assertSame(restored, LoveEngine.applyDayIncident(restored))
    }

    @Test fun oldSaveWithoutLoveFieldsAndWithSevenEventFieldsStillLoads() {
        val schedule = List(28) { if (it == 0) TimeSlot(ActivityType.WORK) else TimeSlot() }
        val original = EventEngine.advanceDay(fresh(), schedule, subjects, always)
        assertEquals("work", original.pendingEvent?.eventId)
        val raw = requireNotNull(with(gameStateSaver) { scope.save(original) }) as List<*>
        val saved = raw.chunked(2).associate { (it[0] as String) to it[1] }.toMutableMap()
        for (key in listOf("loveConfession", "loveRelationship", "loveHistory", "loveCheckedSemesters")) saved.remove(key)
        saved["campusEvent"] = ArrayList((saved["campusEvent"] as List<*>).take(7))
        val restored = requireNotNull(gameStateSaver.restore(ArrayList(saved.flatMap { listOf(it.key, it.value) })))
        assertEquals(original, restored)
        assertNull(restored.love)
        assertNull(restored.pendingConfession)
        assertTrue(restored.loveHistory.isEmpty())
    }

    @Test fun breakupDilemmaKeepsPartnerNameForItsSavedResult() {
        val original = active().copy(love = requireNotNull(active().love).copy(dateIndex = 1))
        val schedule = LoveEngine.syncSchedule(original, List(28) { TimeSlot() })
        val offered = EventEngine.advanceDay(original, schedule, subjects, always)
        val chosen = EventEngine.choose(offered, requireNotNull(offered.pendingEvent).key, 2)
        val restored = roundTrip(chosen)
        assertNull(restored.love)
        assertEquals(partner, restored.pendingEvent?.contextName)
        assertEquals(chosen.loveHistory, restored.loveHistory)
        EventEngine.validatePending(restored)
        LoveEngine.validateState(restored, LoveEngine.syncSchedule(restored, schedule))
        assertSame(restored, EventEngine.choose(restored, requireNotNull(restored.pendingEvent).key, 2))
    }

    @Test fun relationshipCanBeSavedDuringRegistrationForTheNextSemester() {
        val record = SemesterResult(3, subjects.map { SubjectGrade(it.id, 90, "A0", 4.0) })
        val completed = active().copy(week = 16, day = 7, semesterResult = record,
            vacationHistory = listOf(VacationRecord(3, "work")),
            love = requireNotNull(active().love).copy(lastSettledWeek = 47, weeksTogether = 16))
        val next = AcademicEngine.advanceSemester(completed)
        val restored = roundTrip(next)
        assertEquals(next, restored)
        assertTrue(restored.needsRegistration)
        assertNotNull(restored.love)
        LoveEngine.validateState(restored, emptyList())
    }

    @Test fun zeroAffectionIsPreservedInsteadOfResettingToFifty() {
        val original = active().copy(love = requireNotNull(active().love).copy(affection = 0))
        val restored = roundTrip(original)
        assertEquals(0, restored.love?.affection)
        LoveEngine.validateState(restored, LoveEngine.syncSchedule(restored, plan()))
    }
}
