package com.example.mobile_project

data class OnlineUser(val uid: String, val email: String)

data class SaveSummary(val semester: Int, val week: Int, val day: Int, val registration: Boolean = false) {
    val label: String get() = if (registration) "${semesterLabel(semester)} · 수강신청 중" else
        "${semesterLabel(semester)} ${week}주차 · ${if (day == 0) "월요일 진행 전" else "${day}일 진행 완료"}"
    companion object {
        fun from(game: GameState) = SaveSummary(game.semester, game.week, game.day, game.needsRegistration)
    }
}

data class CloudSaveRecord(
    val revision: Long,
    val updatedAtMs: Long,
    val payload: String,
    val summary: SaveSummary
)

enum class CloudAction { UPLOAD, DOWNLOAD }

data class CloudTransferPreview(
    val action: CloudAction,
    val uid: String,
    val local: GameSnapshot,
    val remote: CloudSaveRecord?,
    val payload: String,
    val sourceSummary: SaveSummary
)

class CloudConflictException : IllegalStateException("클라우드 저장이 다른 기기에서 변경됐어요. 다시 확인해 주세요.")

object CloudSyncRules {
    fun validateCredentials(email: String, password: String, register: Boolean) {
        require(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())) { "이메일 주소를 확인해 주세요." }
        require(password.isNotEmpty()) { "비밀번호를 입력해 주세요." }
        if (register) require(password.length >= 6) { "회원가입 비밀번호는 6자 이상으로 입력해 주세요." }
    }

    fun requireRevision(expected: Long, actual: Long) {
        if (expected != actual) throw CloudConflictException()
    }

    fun requireCurrent(preview: CloudTransferPreview, uid: String?, current: GameSnapshot) {
        require(preview.uid == uid) { "로그인 계정이 변경됐어요. 다시 확인해 주세요." }
        require(preview.local.game == current.game && preview.local.schedule == current.schedule &&
            preview.local.selectedIds == current.selectedIds) { "기기의 진행 내용이 바뀌었어요. 저장 내용을 다시 확인해 주세요." }
    }
}
