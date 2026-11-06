package com.example.mobile_project

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class GameBackupCodecTest {
    private val subjects = subjectsForSemester(1).take(3)
    private val ids = subjects.map { it.id }
    private val always = object : Random() { override fun nextBits(bitCount: Int): Int = 0 }
    private fun fresh() = GameEngine.newGame(ids)
    private fun plan(activity: ActivityType = ActivityType.STUDY, id: String = ids[0]) = List(28) {
        if (it == 0) TimeSlot(activity, if (activity == ActivityType.STUDY || activity == ActivityType.CLASS) id else null) else TimeSlot()
    }
    private fun snapshot(game: GameState = fresh(), schedule: List<TimeSlot> = plan()) =
        GameSnapshot(game, schedule, game.study.keys.toList(), "main")
    private fun roundTrip(value: GameSnapshot, epoch: Long = 1_000_000L) =
        GameBackupCodec.decode(GameBackupCodec.encode(value, epoch, 10_000L), epoch, 10_000L)
    private fun assertRejected(block: () -> Unit) {
        try { block(); fail("invalid backup accepted") } catch (_: IllegalArgumentException) { }
    }

    @Test fun allGameFieldsAndKoreanTextSurviveJsonTransport() {
        val game = fresh().copy(money = -40_000, study = ids.associateWith { 123 },
            log = listOf("푸앙이의 기록 😊\n\"따옴표\"와 \\ 기호"),
            life = LifeState(sickWeek = 1, healthCheckedWeek = 0, lotteryHistory = listOf(LotteryTicket(1, 1, 3))))
        val original = snapshot(game)
        assertEquals(original, roundTrip(original))
        assertTrue(GameBackupCodec.encode(original, 1_000_000L, 10_000L).startsWith("{"))
    }

    @Test fun partiallySelectedRegistrationCanBeBackedUp() {
        val original = GameSnapshot(GameState(needsRegistration = true), emptyList(), ids.take(2), "register")
        assertEquals(original, roundTrip(original))
    }

    @Test fun pendingEventKeepsFrozenChoiceAndChanges() {
        val schedule = plan(ActivityType.WORK)
        val offered = EventEngine.advanceDay(fresh(), schedule, subjects, always)
        val key = requireNotNull(offered.pendingEvent).key
        for (state in listOf(offered, EventEngine.choose(offered, key, 0))) {
            assertEquals(snapshot(state, schedule), roundTrip(snapshot(state, schedule)))
        }
    }

    @Test fun teamAssignmentAndScheduleStayTogether() {
        val state = fresh().copy(week = 4)
        val prepared = TeamEngine.prepareWeek(state, createWeekSchedule(ids), Random(10))
        assertNotNull(prepared.game.teamProject)
        val original = snapshot(prepared.game, prepared.schedule)
        assertEquals(original, roundTrip(original))
    }

    @Test fun confessionAndAcceptedRelationshipKeepTheirDateSlots() {
        val subjects = subjectsForSemester(3).take(3)
        val state = GameEngine.newGame(subjects.map { it.id }).copy(semester = 3, charm = 40, network = 30)
        val prepared = LoveEngine.prepareWeek(state, createWeekSchedule(state.study.keys.toList()), Random(20))
        assertNotNull(prepared.game.pendingConfession)
        assertEquals(snapshot(prepared.game, prepared.schedule), roundTrip(snapshot(prepared.game, prepared.schedule)))
        val accepted = LoveEngine.chooseConfession(prepared.game, prepared.schedule, requireNotNull(prepared.game.pendingConfession).key, true, Random(30))
        assertNotNull(accepted.game.love)
        assertEquals(snapshot(accepted.game, accepted.schedule), roundTrip(snapshot(accepted.game, accepted.schedule)))
    }

    @Test fun allNineMinigamesPreserveReadyPreviewPlayAndResultStates() {
        for (kind in MiniKind.values()) {
            val activity = when (kind) {
                MiniKind.ORDER, MiniKind.CHANGE -> ActivityType.WORK
                MiniKind.TUTOR -> ActivityType.TUTOR
                MiniKind.INTERVIEW -> ActivityType.CAREER
                MiniKind.TIMING, MiniKind.HOLD -> ActivityType.RUN
                else -> ActivityType.STUDY
            }
            val semester = if (kind == MiniKind.INTERVIEW) 5 else if (kind == MiniKind.TUTOR) 2 else 1
            val currentIds = subjectsForSemester(semester).take(3).map { it.id }
            var state = GameEngine.newGame(currentIds).copy(semester = semester)
            if (kind == MiniKind.TUTOR) state = state.copy(transcript = listOf(
                SemesterResult(1, ids.map { SubjectGrade(it, 90, "A0", 4.0) })))
            val schedule = plan(activity, currentIds[0])
            val run = MiniGameEngine.buildRun(state, schedule, 0, kind, Random(40))
            state = state.copy(pendingMiniGame = run, miniCheckedDay = EventEngine.absoluteDay(state))
            assertEquals(snapshot(state, schedule), roundTrip(snapshot(state, schedule)))
            val begun = MiniGameEngine.begin(state, run.key, 1_000_000L)
            assertEquals(snapshot(begun, schedule), roundTrip(snapshot(begun, schedule)))
            val result = MiniGameEngine.skipOrForfeit(begun, run.key, 1_000_000L)
            assertEquals(snapshot(result, schedule), roundTrip(snapshot(result, schedule)))
        }
    }

    @Test fun savedMiniTimeoutUsesWallClockAndDoesNotReapplyReward() {
        val schedule = plan()
        val run = MiniGameEngine.buildRun(fresh(), schedule, 0, MiniKind.FOCUS, Random(50))
        val prepared = fresh().copy(pendingMiniGame = run, miniCheckedDay = 0)
        val active = MiniGameEngine.begin(prepared, run.key, 1_000_000L)
        val encoded = GameBackupCodec.encode(snapshot(active, schedule), 1_000_100L, 10_000L)
        val restored = GameBackupCodec.decode(encoded, 1_010_000L, 500L)
        assertEquals(MiniPhase.RESULT, restored.game.pendingMiniGame?.phase)
        assertEquals(0.85, requireNotNull(restored.game.pendingMiniGame?.reward).studyMultiplier, 0.0)
        assertEquals(0, restored.game.study[ids[0]])
    }

    @Test fun morningTimeoutAndAttendanceCountRestoreOnce() {
        val schedule = plan(ActivityType.CLASS)
        val offered = LifeEngine.prepareMorning(fresh().copy(hp = 20), schedule, always)
        val key = requireNotNull(offered.life.pendingMorning).key
        val active = LifeEngine.beginMorning(offered, key, 1_000_000L)
        val encoded = GameBackupCodec.encode(snapshot(active, schedule), 1_000_100L, 10_000L)
        val restored = GameBackupCodec.decode(encoded, 1_010_000L, 500L)
        assertEquals(MorningPhase.RESULT, restored.game.life.pendingMorning?.phase)
        assertEquals(1, restored.game.life.lateCounts[ids[0]])
        val reloaded = roundTrip(restored, 1_010_000L)
        assertEquals(1, reloaded.game.life.lateCounts[ids[0]])
        assertEquals(1, reloaded.game.life.attendanceHistory.size)
    }

    @Test fun examRemainingTimeIsRebasedOntoAnotherDevicesUptime() {
        val prepared = ExamEngine.startPendingExam(fresh().copy(week = 8, day = 7), Random(60))
        val active = ExamEngine.beginExam(prepared, 10_000L)
        val originalRemaining = requireNotNull(active.exam).deadlineMs - 10_500L
        val encoded = GameBackupCodec.encode(snapshot(active), 1_000_000L, 10_500L)
        val restored = GameBackupCodec.decode(encoded, 1_003_000L, 200L)
        assertEquals(200L + (originalRemaining - 3_000L).coerceAtLeast(0L), restored.game.exam?.deadlineMs)
        assertEquals(active.exam?.questionIds, restored.game.exam?.questionIds)
    }

    @Test fun expiredOrReversedClockNeverExtendsExamTime() {
        val active = ExamEngine.beginExam(ExamEngine.startPendingExam(fresh().copy(week = 8, day = 7), Random(70)), 10_000L)
        val encoded = GameBackupCodec.encode(snapshot(active), 1_000_000L, 10_500L)
        for (epoch in listOf(999_000L, 2_000_000L)) {
            assertEquals(200L, GameBackupCodec.decode(encoded, epoch, 200L).game.exam?.deadlineMs)
        }
    }

    @Test fun missingStageTwelveLifeFieldRestoresDefaultValues() {
        val encoded = JSONObject(GameBackupCodec.encode(snapshot(), 1_000_000L, 10_000L))
        val raw = encoded.getJSONArray("game")
        val reduced = JSONArray()
        for (i in 0 until raw.length() step 2) if (raw.getString(i) != "life") {
            reduced.put(raw.get(i)); reduced.put(raw.get(i + 1))
        }
        encoded.put("game", reduced)
        assertEquals(LifeState(), GameBackupCodec.decode(encoded.toString(), 1_000_000L, 10_000L).game.life)
    }

    @Test fun unknownVersionWrongStatsAndChangedSubjectAreRejectedBeforeApplying() {
        val original = GameBackupCodec.encode(snapshot(), 1_000_000L, 10_000L)
        assertRejected { GameBackupCodec.decode(JSONObject(original).put("version", 2).toString(), 1_000_000L, 10_000L) }
        val badStats = JSONObject(original)
        val raw = badStats.getJSONArray("game")
        for (i in 0 until raw.length() step 2) if (raw.getString(i) == "hp") raw.put(i + 1, JSONObject().put("i", "200"))
        assertRejected { GameBackupCodec.decode(badStats.toString(), 1_000_000L, 10_000L) }
        val badSubject = JSONObject(original)
        badSubject.getJSONArray("schedule").getJSONObject(0).put("subject", "unknown_subject")
        assertRejected { GameBackupCodec.decode(badSubject.toString(), 1_000_000L, 10_000L) }
    }

    @Test fun duplicateSaverKeysAndOversizePayloadsAreRejected() {
        val root = JSONObject(GameBackupCodec.encode(snapshot(), 1_000_000L, 10_000L))
        root.getJSONArray("game").put("hp").put(JSONObject().put("i", "20"))
        assertRejected { GameBackupCodec.decode(root.toString(), 1_000_000L, 10_000L) }
        assertRejected { GameBackupCodec.encode(snapshot(fresh().copy(log = listOf("x".repeat(GameBackupCodec.MAX_BYTES)))), 1_000_000L, 10_000L) }
        assertRejected { GameBackupCodec.decode("x".repeat(GameBackupCodec.MAX_BYTES + 1), 1_000_000L, 10_000L) }
    }
}
