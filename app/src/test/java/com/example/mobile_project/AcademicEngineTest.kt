package com.example.mobile_project

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicEngineTest {
    private fun result(semester: Int, point: Double = 4.0): SemesterResult = SemesterResult(
        semester, subjectsForSemester(semester).take(3).map { SubjectGrade(it.id, 90, "A0", point) }
    )

    private fun completed(semester: Int = 1, money: Int = 350_000): GameState {
        val record = result(semester)
        val ids = subjectsForSemester(semester).take(3).map { it.id }
        return GameEngine.newGame(ids).copy(
            semester = semester, week = 16, day = 7, money = money,
            midtermScores = ids.associateWith { 90 }, finalScores = ids.associateWith { 90 },
            semesterResult = record, transcript = listOf(record)
        )
    }

    @Test
    fun vacationOptionsUnlockByYear() {
        assertEquals(4, AcademicEngine.vacationOptions(1).size)
        assertEquals(4, AcademicEngine.vacationOptions(2).size)
        assertEquals(5, AcademicEngine.vacationOptions(3).size)
        assertEquals(6, AcademicEngine.vacationOptions(5).size)
    }

    @Test
    fun vacationEffectsIncludeSeparateLivingExpense() {
        data class Expected(val id: String, val money: Int, val hp: Int, val happy: Int, val spec: Int, val net: Int)
        for (expected in listOf(
            Expected("work", 2_250_000, 55, 42, 0, 0),
            Expected("rest", 1_720_000, 100, 95, 0, 0),
            Expected("cert", 1_550_000, 68, 50, 10, 0),
            Expected("trip", 1_450_000, 90, 100, 0, 10),
            Expected("lang", 900_000, 80, 70, 18, 8),
            Expected("intern", 2_200_000, 60, 52, 20, 0)
        )) {
            val state = AcademicEngine.applyVacation(completed(5, 2_000_000), expected.id)
            assertEquals(expected.money, state.money)
            assertEquals(expected.hp, state.hp)
            assertEquals(expected.happy, state.happiness)
            assertEquals(expected.spec, state.spec)
            assertEquals(expected.net, state.network)
        }
    }

    @Test
    fun affordabilityIncludesDebtLimitAndBothCosts() {
        val rest = AcademicEngine.vacationOption("rest")!!
        assertTrue(AcademicEngine.canAffordVacation(completed(money = 230_000), rest))
        assertFalse(AcademicEngine.canAffordVacation(completed(money = 229_999), rest))
        assertEquals(-50_000, AcademicEngine.applyVacation(completed(money = 230_000), "rest").money)
        val poor = completed(money = 229_999)
        assertSame(poor, AcademicEngine.applyVacation(poor, "rest"))
    }

    @Test
    fun scholarshipIsNotAvailableBeforeVacationPayment() {
        val poor = completed(money = -50_000)
        assertSame(poor, AcademicEngine.applyVacation(poor, "rest"))
        val worked = AcademicEngine.applyVacation(poor, "work")
        assertEquals(200_000, worked.money)
        assertEquals(800_000, AcademicEngine.advanceSemester(worked).money)
    }

    @Test
    fun vacationAndScholarshipApplyOnlyOnce() {
        val vacation = AcademicEngine.applyVacation(completed(), "work")
        assertEquals(600_000, vacation.money)
        assertSame(vacation, AcademicEngine.applyVacation(vacation, "rest"))
        val next = AcademicEngine.advanceSemester(vacation)
        assertEquals(1_200_000, next.money)
        assertEquals(600_000, next.registrationScholarship)
        assertEquals(2, next.semester)
        assertTrue(next.needsRegistration)
        assertSame(next, AcademicEngine.advanceSemester(next))
    }

    @Test
    fun unfinishedSemesterAndLockedVacationAreRejected() {
        val fresh = GameEngine.newGame(firstYearSubjects.take(3).map { it.id })
        assertSame(fresh, AcademicEngine.applyVacation(fresh, "work"))
        val firstYear = completed(money = 2_000_000)
        assertSame(firstYear, AcademicEngine.applyVacation(firstYear, "lang"))
        assertSame(firstYear, AcademicEngine.advanceSemester(firstYear))
    }

    @Test
    fun firstRegistrationKeepsInitialStats() {
        val initial = GameState(needsRegistration = true)
        val registered = AcademicEngine.registerSubjects(initial, firstYearSubjects.take(3).map { it.id })
        assertFalse(registered.needsRegistration)
        assertEquals(initial.money, registered.money)
        assertEquals(initial.hp, registered.hp)
        assertEquals(initial.happiness, registered.happiness)
        assertEquals(3, registered.study.size)
        assertEquals(0, registered.registrationScholarship)
    }

    @Test
    fun laterRegistrationPreservesStatsAndHistory() {
        val vacation = AcademicEngine.applyVacation(completed(2, 2_000_000).copy(
            hp = 42, maxHp = 120, happiness = 45, spec = 17, network = 22, charm = 30
        ), "cert")
        val next = AcademicEngine.advanceSemester(vacation)
        val ids = subjectsForSemester(3).take(3).map { it.id }
        val registered = AcademicEngine.registerSubjects(next, ids)
        assertEquals(next.money, registered.money)
        assertEquals(next.hp, registered.hp)
        assertEquals(next.maxHp, registered.maxHp)
        assertEquals(next.happiness, registered.happiness)
        assertEquals(next.spec, registered.spec)
        assertEquals(next.network, registered.network)
        assertEquals(next.charm, registered.charm)
        assertEquals(next.transcript, registered.transcript)
        assertEquals(next.vacationHistory, registered.vacationHistory)
        assertEquals(ids.toSet(), registered.study.keys)
        assertTrue(registered.study.values.all { it == 0 })
        assertTrue(registered.midtermScores.isEmpty())
        assertTrue(registered.finalScores.isEmpty())
        assertEquals(1, registered.week)
        assertEquals(0, registered.day)
    }

    @Test
    fun duplicateRegistrationDoesNotResetGameOrReawardScholarship() {
        val next = AcademicEngine.advanceSemester(AcademicEngine.applyVacation(completed(), "work"))
        val ids = firstYearSubjects.take(3).map { it.id }
        val registered = AcademicEngine.registerSubjects(next, ids)
        assertSame(registered, AcademicEngine.registerSubjects(registered, firstYearSubjects.takeLast(3).map { it.id }))
        assertSame(registered, AcademicEngine.advanceSemester(registered))
    }

    @Test
    fun newYearRequiresNewCurriculumIds() {
        val next = AcademicEngine.advanceSemester(AcademicEngine.applyVacation(completed(2), "work"))
        assertEquals(3, next.semester)
        assertSame(next, AcademicEngine.registerSubjects(next, firstYearSubjects.take(3).map { it.id }))
        val ids = subjectsForSemester(3).take(3).map { it.id }
        val registered = AcademicEngine.registerSubjects(next, ids)
        val schedule = createWeekSchedule(registered.study.keys.toList())
        assertEquals(6, schedule.count { it.locked })
        assertTrue(schedule.filter { it.locked }.all { it.subjectId in ids })
    }

    @Test
    fun cumulativeGpaAndCurrentRecordAreNotDuplicated() {
        val first = result(1, 4.5)
        val second = result(2, 2.0)
        val state = completed(2).copy(transcript = listOf(first, second), semesterResult = second)
        assertEquals(2, state.completedSemesters.size)
        assertEquals(3.25, state.cumulativeGpa, 0.000001)
    }

    @Test
    fun oldStepFiveResultWithoutHistoryStillCarriesForward() {
        val old = completed().copy(transcript = emptyList())
        assertEquals(1, old.completedSemesters.size)
        val next = AcademicEngine.advanceSemester(AcademicEngine.applyVacation(old, "work"))
        assertEquals(1, next.transcript.size)
        assertEquals(4.0, next.cumulativeGpa, 0.000001)
    }

    @Test
    fun vacationStatsAreClampedAndRestUsesActualMaxHealth() {
        val state = completed(money = 2_000_000).copy(hp = 95, happiness = 99, network = 99, spec = 95)
        val trip = AcademicEngine.applyVacation(state, "trip")
        assertEquals(100, trip.hp)
        assertEquals(100, trip.happiness)
        assertEquals(100, trip.network)
        assertEquals(100, AcademicEngine.applyVacation(state, "cert").spec)
        assertEquals(150, AcademicEngine.applyVacation(state.copy(hp = 3, maxHp = 150), "rest").hp)
    }

    @Test
    fun careerAndTutorUnlockAtTheirActualRequirements() {
        val fresh = GameEngine.newGame(firstYearSubjects.take(3).map { it.id })
        assertFalse(AcademicEngine.canUseActivity(fresh, ActivityType.TUTOR))
        assertFalse(AcademicEngine.canUseActivity(fresh.copy(semester = 4), ActivityType.CAREER))
        assertTrue(AcademicEngine.canUseActivity(fresh.copy(semester = 5), ActivityType.CAREER))
        assertFalse(AcademicEngine.canUseActivity(fresh.copy(transcript = listOf(result(1, 2.5))), ActivityType.TUTOR))
        assertTrue(AcademicEngine.canUseActivity(fresh.copy(transcript = listOf(result(1, 3.0))), ActivityType.TUTOR))
    }

    @Test
    fun backendRejectsLockedActivitiesAndAppliesUnlockedEffects() {
        val ids = firstYearSubjects.take(3).map { it.id }
        val fresh = GameEngine.newGame(ids)
        val lockedPlan = List(28) { index -> when (index) {
            0 -> TimeSlot(ActivityType.CAREER)
            1 -> TimeSlot(ActivityType.TUTOR)
            else -> TimeSlot()
        } }
        val rejected = GameEngine.advanceDay(fresh, lockedPlan, firstYearSubjects.take(3))
        assertEquals(80, rejected.hp)
        assertEquals(60, rejected.happiness)
        assertEquals(350_000, rejected.money)
        assertEquals(0, rejected.spec)
        val careerPlan = List(28) { if (it == 0) TimeSlot(ActivityType.CAREER) else TimeSlot() }
        val career = GameEngine.advanceDay(fresh.copy(semester = 5), careerPlan, firstYearSubjects.take(3))
        assertEquals(1, career.spec)
        assertEquals(69, career.hp)
        val tutorPlan = List(28) { if (it == 0) TimeSlot(ActivityType.TUTOR) else TimeSlot() }
        val tutor = GameEngine.advanceDay(fresh.copy(transcript = listOf(result(1))), tutorPlan, firstYearSubjects.take(3))
        assertEquals(395_000, tutor.money)
        assertEquals(72, tutor.hp)
    }

    @Test
    fun weeklyExpenseIncreasesByYearAndRunsOnce() {
        val plan = List(28) { if (it % 4 == 3) TimeSlot(ActivityType.SLEEP) else TimeSlot() }
        for ((semester, cost) in listOf(1 to 25_000, 3 to 30_000, 5 to 35_000, 7 to 40_000)) {
            val subjects = subjectsForSemester(semester).take(3)
            val before = GameEngine.newGame(subjects.map { it.id }).copy(semester = semester, day = 6)
            val after = GameEngine.advanceDay(before, plan, subjects)
            assertEquals(350_000 - cost, after.money)
            assertSame(after, GameEngine.advanceDay(after, plan, subjects))
        }
    }

    @Test
    fun eightSemestersKeepAllRecordsAndNeverAdvanceToNine() {
        var state = GameState(needsRegistration = true)
        for (semester in 1..8) {
            val ids = subjectsForSemester(semester).take(3).map { it.id }
            state = AcademicEngine.registerSubjects(state, ids)
            state = ExamEngine.finishSemester(state.copy(
                week = 16, day = 7,
                midtermScores = ids.associateWith { 90 }, finalScores = ids.associateWith { 90 }
            ))
            assertEquals(semester, state.completedSemesters.size)
            if (semester < 8) {
                state = AcademicEngine.advanceSemester(AcademicEngine.applyVacation(state, "work"))
                assertEquals(semester + 1, state.semester)
            }
        }
        assertEquals(8, state.semester)
        assertEquals(7, state.vacationHistory.size)
        assertEquals(4.0, state.cumulativeGpa, 0.000001)
        assertSame(state, AcademicEngine.applyVacation(state, "work"))
        assertSame(state, AcademicEngine.advanceSemester(state))
    }

    @Test
    fun graduationBranchPriorityMatchesOriginal() {
        fun graduate(point: Double): GameState = completed(8).copy(
            transcript = (1..8).map { result(it, point) },
            semesterResult = result(8, point), happiness = 60
        )
        assertEquals("졸업 유예", AcademicEngine.graduationResult(graduate(1.0).copy(spec = 100)).title)
        assertEquals("번아웃", AcademicEngine.graduationResult(graduate(4.0).copy(happiness = 0, spec = 100)).title)
        assertEquals("대학원 진학", AcademicEngine.graduationResult(graduate(4.0).copy(spec = 60)).title)
        assertEquals("대기업 취업", AcademicEngine.graduationResult(graduate(3.5).copy(spec = 60)).title)
        assertEquals("창업", AcademicEngine.graduationResult(graduate(2.5).copy(money = 1_500_000, network = 70)).title)
        assertEquals("개발자 취업", AcademicEngine.graduationResult(graduate(2.5).copy(spec = 45)).title)
        assertEquals("공기업 합격", AcademicEngine.graduationResult(graduate(3.0).copy(spec = 30)).title)
        assertEquals("중견기업 취업", AcademicEngine.graduationResult(graduate(3.0)).title)
        assertEquals("취업 준비생", AcademicEngine.graduationResult(graduate(2.5)).title)
    }

    @Test
    fun saveRestoreKeepsVacationRegistrationAndAcademicHistory() {
        val scope = object : SaverScope { override fun canBeSaved(value: Any): Boolean = true }
        val vacation = AcademicEngine.applyVacation(completed(2, 2_000_000).copy(
            transcript = listOf(result(1), result(2)), spec = 30
        ), "cert")
        val next = AcademicEngine.advanceSemester(vacation)
        for (state in listOf(vacation, next)) {
            val encoded = with(gameStateSaver) { scope.save(state) }
            assertEquals(state, gameStateSaver.restore(requireNotNull(encoded)))
        }
    }
}
