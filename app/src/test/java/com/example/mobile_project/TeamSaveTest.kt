package com.example.mobile_project

import androidx.compose.runtime.saveable.SaverScope
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class TeamSaveTest {
    private val scope = object : SaverScope { override fun canBeSaved(value: Any): Boolean = true }
    private val subjects = firstYearSubjects.take(3)
    private fun fresh() = GameEngine.newGame(subjects.map { it.id }).copy(week = 4)
    private fun plan() = createWeekSchedule(subjects.map { it.id })
    private fun roundTrip(state: GameState): GameState {
        val raw = requireNotNull(with(gameStateSaver) { scope.save(state) })
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(raw) }
        val restoredRaw = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        return requireNotNull(gameStateSaver.restore(restoredRaw))
    }

    @Test fun unannouncedAssignmentAndIncidentsRoundTripWithoutReroll() {
        val prepared = TeamEngine.prepareWeek(fresh(), plan(), Random(7))
        val restored = roundTrip(prepared.game)
        assertEquals(prepared.game, restored)
        TeamEngine.validateState(restored, prepared.schedule)
        val second = TeamEngine.prepareWeek(restored, prepared.schedule, Random(999))
        assertEquals(restored, second.game)
        assertEquals(prepared.schedule, second.schedule)
    }

    @Test fun appliedIncidentFlagSurvivesReload() {
        val prepared = TeamEngine.prepareWeek(fresh(), plan(), Random(7))
        val project = requireNotNull(prepared.game.teamProject).copy(
            memberIds = listOf("good", "ppt", "ghost"), announced = true,
            incidents = listOf(TeamIncident(0, 0, 0)))
        val applied = TeamEngine.applyDayIncidents(prepared.game.copy(teamProject = project))
        val restored = roundTrip(applied)
        assertEquals(applied, restored)
        assertSame(restored, TeamEngine.applyDayIncidents(restored))
    }

    @Test fun completedProjectsBonusesAndGpaHistoryRoundTrip() {
        val record = SemesterResult(1, subjects.map { SubjectGrade(it.id, 90, "A0", 4.0) }, 0.04)
        val result = TeamProjectResult(1, 4, 6, subjects[0].id, listOf("good", "ppt", "ghost"), 8)
        val state = fresh().copy(week = 16, day = 7, semesterResult = record, transcript = listOf(record),
            teamResults = listOf(result), teamBonuses = mapOf(subjects[0].id to 8), semesterGpaAdjustment = 0.04)
        val restored = roundTrip(state)
        assertEquals(state, restored)
        assertEquals(4.04, restored.cumulativeGpa, 0.000001)
        assertSame(restored, TeamEngine.settleWeek(restored))
    }

    @Test fun oldSaveWithoutTeamFieldsOrGpaColumnsLoadsWithDefaults() {
        val record = SemesterResult(1, subjects.map { SubjectGrade(it.id, 90, "A0", 4.0) })
        val original = fresh().copy(week = 16, day = 7, semesterResult = record, transcript = listOf(record))
        val raw = requireNotNull(with(gameStateSaver) { scope.save(original) }) as List<*>
        val saved = raw.chunked(2).associate { (it[0] as String) to it[1] }.toMutableMap()
        for (key in listOf("teamProject", "teamIncidents", "teamResults", "teamBonuses",
            "semesterGpaAdjustment", "resultGpaAdjustment")) saved.remove(key)
        saved["transcript"] = ArrayList((saved["transcript"] as List<*>).map {
            (it as String).split('|').take(5).joinToString("|")
        })
        val legacy = ArrayList(saved.flatMap { listOf(it.key, it.value) })
        val restored = requireNotNull(gameStateSaver.restore(legacy))
        assertEquals(original, restored)
        assertNull(restored.teamProject)
        assertTrue(restored.teamResults.isEmpty())
        assertTrue(restored.teamBonuses.isEmpty())
    }

    @Test fun scheduleSaverUsesTeamNameAndRestoresFixedMeetings() {
        val prepared = TeamEngine.prepareWeek(fresh(), plan(), Random(3))
        val raw = requireNotNull(with(weekScheduleSaver) { scope.save(prepared.schedule) })
        val restored = requireNotNull(weekScheduleSaver.restore(raw))
        assertEquals(prepared.schedule, restored)
        assertTrue(requireNotNull(prepared.game.teamProject).meetingIndices.all { restored[it].locked })
    }

    @Test fun teamDilemmaResultSavesGpaAdjustmentAndCannotRepeat() {
        val state = fresh().copy(teamProject = TeamProject(1, 4, subjects[0].id,
            listOf("good", "ppt", "ghost"), listOf(1), 8, announced = true, incidentWeek = 4))
        val schedule = TeamEngine.syncSchedule(state, List(28) { TimeSlot() })
        val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
        val offered = EventEngine.advanceDay(state, schedule, subjects, always)
        val key = requireNotNull(offered.pendingEvent).key
        val chosen = EventEngine.choose(offered, key, 1)
        val restored = roundTrip(chosen)
        assertEquals(chosen, restored)
        EventEngine.validatePending(restored)
        TeamEngine.validateState(restored, schedule)
        assertSame(restored, EventEngine.choose(restored, key, 1))
        assertEquals(0.02, restored.semesterGpaAdjustment, 0.000001)
    }

    @Test fun invalidMeetingThatOverlapsClassIsRejected() {
        val prepared = TeamEngine.prepareWeek(fresh(), plan(), Random(3))
        val project = requireNotNull(prepared.game.teamProject)
        val invalid = prepared.schedule.toMutableList().apply {
            this[project.meetingIndices.first()] = TimeSlot(ActivityType.CLASS, subjects[0].id)
        }
        assertThrows(IllegalArgumentException::class.java) { TeamEngine.validateState(prepared.game, invalid) }
    }
}
