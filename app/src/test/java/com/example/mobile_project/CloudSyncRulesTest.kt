package com.example.mobile_project

import org.junit.Assert.*
import org.junit.Test

class CloudSyncRulesTest {
    private val ids = subjectsForSemester(1).take(3).map { it.id }
    private val snapshot = GameSnapshot(GameEngine.newGame(ids), createWeekSchedule(ids), ids, "main")
    private fun preview(action: CloudAction = CloudAction.DOWNLOAD) = CloudTransferPreview(action, "account_a",
        snapshot, CloudSaveRecord(4L, 1_000L, "payload", SaveSummary.from(snapshot.game)), "payload", SaveSummary.from(snapshot.game))
    private fun assertRejected(block: () -> Unit) {
        try { block(); fail("unsafe transfer accepted") } catch (_: IllegalArgumentException) { }
    }

    @Test fun changedOrSignedOutAccountCannotApplyPreparedTransfer() {
        for (uid in listOf(null, "account_b")) assertRejected {
            CloudSyncRules.requireCurrent(preview(), uid, snapshot)
        }
    }

    @Test fun localProgressChangedAfterPreviewMustBeCheckedAgain() {
        for (game in listOf(snapshot.game.copy(day = 1), snapshot.game.copy(money = 400_000))) {
            assertRejected { CloudSyncRules.requireCurrent(preview(), "account_a", snapshot.copy(game = game)) }
        }
    }

    @Test fun changedScheduleOrDraftAlsoInvalidatesPreview() {
        assertRejected { CloudSyncRules.requireCurrent(preview(), "account_a", snapshot.copy(schedule = List(28) { TimeSlot() })) }
        assertRejected { CloudSyncRules.requireCurrent(preview(), "account_a", snapshot.copy(selectedIds = ids.reversed())) }
    }

    @Test fun navigatingBetweenScreensDoesNotCountAsChangingProgress() {
        CloudSyncRules.requireCurrent(preview(), "account_a", snapshot.copy(screen = "account"))
        CloudSyncRules.requireCurrent(preview(CloudAction.UPLOAD), "account_a", snapshot.copy(screen = "title"))
    }

    @Test fun revisionPreventsSilentOverwriteAfterAnotherDeviceSaved() {
        CloudSyncRules.requireRevision(4L, 4L)
        for (actual in listOf(0L, 3L, 5L)) {
            try { CloudSyncRules.requireRevision(4L, actual); fail("conflict not detected") }
            catch (_: CloudConflictException) { }
        }
    }

    @Test fun firstSaveRequiresDocumentStillMissing() {
        CloudSyncRules.requireRevision(0L, 0L)
        try { CloudSyncRules.requireRevision(0L, 1L); fail("first save overwrote new document") }
        catch (_: CloudConflictException) { }
    }

    @Test fun invalidCredentialsAreRejectedAndLoginDoesNotRequireNewPasswordPolicy() {
        assertRejected { CloudSyncRules.validateCredentials("invalid", "123456", true) }
        assertRejected { CloudSyncRules.validateCredentials("player@example.com", "", false) }
        assertRejected { CloudSyncRules.validateCredentials("player@example.com", "12345", true) }
        CloudSyncRules.validateCredentials(" player@example.com ", "123456", true)
        CloudSyncRules.validateCredentials("player@example.com", "short", false)
    }
}
