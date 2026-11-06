package com.example.mobile_project

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExamEngineTest {
    private val ids = listOf("cal", "phy", "eng")
    private val zeroRandom = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    private fun atExam(week: Int = 8, study: Int = 300): GameState =
        GameEngine.newGame(ids).copy(
            week = week,
            day = 7,
            study = ids.associateWith { study }
        )

    private fun finishAll(state: GameState, correct: Boolean = true): GameState {
        var current = ExamEngine.beginExam(ExamEngine.startPendingExam(state, zeroRandom), 0L)
        while (current.exam!!.phase != ExamPhase.RESULT) {
            val run = current.exam!!
            val answer = if (correct) 0 else run.optionOrders[run.index].first { it != 0 }
            current = ExamEngine.answerQuestion(current, answer, run.index, 100L)
            current = ExamEngine.nextQuestion(current, run.index, 100L)
        }
        return current
    }

    @Test
    fun normalWeeksDoNotStartExams() {
        assertNull(ExamEngine.pendingExam(atExam(week = 7)))
        assertNull(ExamEngine.pendingExam(atExam().copy(day = 6)))
        val state = atExam(week = 7)
        assertSame(state, ExamEngine.startPendingExam(state, zeroRandom))
    }

    @Test
    fun midtermMustFinishBeforeWeekNine() {
        val state = atExam()
        assertEquals(ExamKind.MIDTERM, ExamEngine.pendingExam(state))
        assertSame(state, GameEngine.nextWeek(state))
        val result = finishAll(state)
        val next = GameEngine.nextWeek(result)
        assertEquals(9, next.week)
        assertEquals(0, next.day)
        assertNull(next.exam)
    }

    @Test
    fun preparednessSelectsOriginalThresholds() {
        for ((study, expected) in listOf(
            173 to Triple(4, 7, 10),
            174 to Triple(3, 10, 7),
            288 to Triple(3, 10, 7),
            289 to Triple(2, 15, 5)
        )) {
            val run = ExamEngine.startPendingExam(atExam(study = study), zeroRandom).exam!!
            assertEquals(expected.first, run.optionCount)
            assertEquals(expected.second, run.secondsPerQuestion)
            assertEquals(expected.third, run.questionIds.size)
        }
    }

    @Test
    fun bankAndChosenOptionsHaveOneCorrectAnswer() {
        assertEquals(22, ExamEngine.questionBank.size)
        for (study in listOf(0, 180, 400)) {
            val run = ExamEngine.startPendingExam(atExam(study = study), Random(32)).exam!!
            assertEquals(run.questionIds.size, run.questionIds.distinct().size)
            run.optionOrders.forEach { order ->
                assertEquals(run.optionCount, order.size)
                assertEquals(order.size, order.distinct().size)
                assertEquals(1, order.count { it == 0 })
            }
        }
    }

    @Test
    fun jokboAddsFifteenOnlyOnce() {
        val state = atExam(study = 197).copy(network = 100)
        val started = ExamEngine.startPendingExam(state, zeroRandom)
        assertTrue(started.exam!!.jokbo)
        assertEquals(2, started.exam!!.optionCount)
        ids.forEach { assertEquals(212, started.study[it]) }
        assertSame(started, ExamEngine.startPendingExam(started, zeroRandom))
    }

    @Test
    fun readyPageDoesNotStartTimerUntilBegin() {
        val ready = ExamEngine.startPendingExam(atExam(), zeroRandom)
        assertEquals(0L, ready.exam!!.deadlineMs)
        val running = ExamEngine.beginExam(ready, 1_000L)
        assertEquals(16_000L, running.exam!!.deadlineMs)
        assertSame(running, ExamEngine.beginExam(running, 2_000L))
    }

    @Test
    fun exactDeadlineCountsAsTimeout() {
        val running = ExamEngine.beginExam(ExamEngine.startPendingExam(atExam(), zeroRandom), 0L)
        val result = ExamEngine.answerQuestion(running, 0, 0, running.exam!!.deadlineMs)
        assertEquals(-1, result.exam!!.selectedAnswer)
        assertEquals(0, result.exam!!.correct)
        assertEquals(ExamPhase.FEEDBACK, result.exam!!.phase)
    }

    @Test
    fun earlyTimeoutAndInvalidAnswersAreIgnored() {
        val running = ExamEngine.beginExam(ExamEngine.startPendingExam(atExam(), zeroRandom), 0L)
        assertSame(running, ExamEngine.answerQuestion(running, -1, 0, 100L))
        assertSame(running, ExamEngine.answerQuestion(running, 99, 0, 100L))
    }

    @Test
    fun answersCannotBeCountedTwice() {
        val running = ExamEngine.beginExam(ExamEngine.startPendingExam(atExam(), zeroRandom), 0L)
        val answered = ExamEngine.answerQuestion(running, 0, 0, 100L)
        assertEquals(1, answered.exam!!.correct)
        assertSame(answered, ExamEngine.answerQuestion(answered, 0, 0, 200L))
    }

    @Test
    fun staleQuestionCallbacksCannotAffectNextQuestion() {
        val running = ExamEngine.beginExam(ExamEngine.startPendingExam(atExam(), zeroRandom), 0L)
        val answered = ExamEngine.answerQuestion(running, 0, 0, 100L)
        val next = ExamEngine.nextQuestion(answered, 0, 200L)
        assertEquals(1, next.exam!!.index)
        assertSame(next, ExamEngine.nextQuestion(next, 0, 300L))
        assertSame(next, ExamEngine.answerQuestion(next, 0, 0, 300L))
    }

    @Test
    fun studyIsPrimaryScoreAndQuizModifierIsBounded() {
        assertEquals(22, ExamEngine.scoreFor(100, 0, 5))
        assertEquals(30, ExamEngine.scoreFor(100, 5, 5))
        assertEquals(85, ExamEngine.scoreFor(400, 0, 5))
        assertEquals(100, ExamEngine.scoreFor(400, 5, 5))
        assertEquals(0, ExamEngine.scoreFor(0, 5, 5))
    }

    @Test
    fun laterYearsUseOriginalExamFactor() {
        assertEquals(0.26, ExamEngine.examFactor(1), 0.000001)
        assertEquals(0.24, ExamEngine.examFactor(3), 0.000001)
        assertEquals(0.22, ExamEngine.examFactor(5), 0.000001)
        assertEquals(0.20, ExamEngine.examFactor(7), 0.000001)
    }

    @Test
    fun midtermStoresScoresAndResetsStudyOnce() {
        val result = finishAll(atExam(study = 300))
        ids.forEach {
            assertEquals(90, result.midtermScores[it])
            assertEquals(0, result.study[it])
        }
        assertTrue(result.finalScores.isEmpty())
        assertSame(result, ExamEngine.nextQuestion(result, result.exam!!.index, 1_000L))
    }

    @Test
    fun finalKeepsItsStudyAndCannotAdvanceToWeekSeventeen() {
        val initial = atExam(week = 16, study = 300).copy(midtermScores = ids.associateWith { 80 })
        assertEquals(ExamKind.FINAL, ExamEngine.pendingExam(initial))
        val result = finishAll(initial)
        ids.forEach {
            assertEquals(90, result.finalScores[it])
            assertEquals(300, result.study[it])
        }
        assertSame(result, GameEngine.nextWeek(result))
    }

    @Test
    fun gradeThresholdsMatchHtml() {
        val expected = mapOf(
            95 to ("A+" to 4.5), 94 to ("A0" to 4.0),
            90 to ("A0" to 4.0), 89 to ("B+" to 3.5),
            85 to ("B+" to 3.5), 84 to ("B0" to 3.0),
            80 to ("B0" to 3.0), 79 to ("C+" to 2.5),
            75 to ("C+" to 2.5), 74 to ("C0" to 2.0),
            70 to ("C0" to 2.0), 69 to ("D0" to 1.0),
            60 to ("D0" to 1.0), 59 to ("F" to 0.0)
        )
        expected.forEach { (score, grade) ->
            val actual = ExamEngine.gradeFor("cal", score)
            assertEquals(grade.first, actual.letter)
            assertEquals(grade.second, actual.point, 0.000001)
        }
    }

    @Test
    fun semesterAveragesTwoExamsThenAveragesGradePoints() {
        val initial = atExam(week = 16).copy(
            midtermScores = mapOf("cal" to 94, "phy" to 90, "eng" to 85),
            finalScores = mapOf("cal" to 95, "phy" to 90, "eng" to 85)
        )
        val result = ExamEngine.finishSemester(initial)
        assertEquals(95, result.semesterResult!!.grades.first().score)
        assertEquals(4.0, result.semesterResult!!.gpa, 0.000001)
        assertEquals(600_000, result.semesterResult!!.scholarship)
        assertEquals(initial.money, result.money)
        assertSame(result, ExamEngine.finishSemester(result))
    }

    @Test
    fun incompleteFinalCannotCreateSemesterResult() {
        val initial = atExam(week = 16).copy(midtermScores = ids.associateWith { 90 })
        assertSame(initial, ExamEngine.finishSemester(initial))
        assertNull(initial.semesterResult)
    }

    @Test
    fun scholarshipBoundariesMatchHtml() {
        for ((point, amount) in listOf(4.0 to 600_000, 3.5 to 350_000, 3.0 to 150_000, 2.5 to 0)) {
            val result = SemesterResult(1, ids.map { SubjectGrade(it, 80, "B0", point) })
            assertEquals(amount, result.scholarship)
        }
    }

    @Test
    fun saveRestoreKeepsQuestionOrderDeadlineAndScores() {
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
        val running = ExamEngine.beginExam(ExamEngine.startPendingExam(atExam(), zeroRandom), 10_000L)
        val answered = ExamEngine.answerQuestion(running, 0, 0, 11_000L)
        val finished = finishAll(atExam())
        val semester = ExamEngine.finishSemester(atExam(week = 16).copy(
            midtermScores = ids.associateWith { 90 },
            finalScores = ids.associateWith { 95 }
        ))
        for (state in listOf(running, answered, finished, semester)) {
            val saved = with(gameStateSaver) { scope.save(state) }
            assertNotNull(saved)
            assertEquals(state, gameStateSaver.restore(requireNotNull(saved)))
        }
    }
}
