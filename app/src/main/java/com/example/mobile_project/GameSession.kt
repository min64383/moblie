package com.example.mobile_project

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class GameSession(
    private val store: LocalGameStore,
    private val online: FirebaseGameService
) : ViewModel() {
    val gameState = mutableStateOf(GameState())
    val scheduleState = mutableStateOf<List<TimeSlot>>(emptyList())
    val draftState = mutableStateOf<List<String>>(emptyList())
    val screenState = mutableStateOf("title")

    var ready by mutableStateOf(false)
        private set

    var saveStatus by mutableStateOf("불러오는 중")
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    var routines by mutableStateOf<List<ScheduleRoutine>>(emptyList())
        private set

    var routineError by mutableStateOf<String?>(null)
        private set

    var routineBusy by mutableStateOf(false)
        private set

    var message by mutableStateOf("")
        private set

    val onlineConfigured: Boolean get() = online.configured
    val onlineSetupMessage: String get() = online.setupMessage
    var onlineUser by mutableStateOf<OnlineUser?>(null)
        private set
    var onlineBusy by mutableStateOf(false)
        private set
    var onlineStatus by mutableStateOf("")
        private set
    var onlineError by mutableStateOf<String?>(null)
        private set
    var cloudRecord by mutableStateOf<CloudSaveRecord?>(null)
        private set
    var cloudPreview by mutableStateOf<CloudTransferPreview?>(null)
        private set

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var allowSaving = false
    private var lastScreen = "main"
    private var saveRequest = 0L
    private var restoringCloud = false
    private val authSubscription = online.observeUser { user ->
        if (user?.uid != onlineUser?.uid) {
            cloudPreview = null
            cloudRecord = null
            onlineStatus = ""
        }
        onlineUser = user
    }

    val hasGame: Boolean
        get() = gameState.value.let {
            it.needsRegistration ||
                    it.study.isNotEmpty() ||
                    it.completedSemesters.isNotEmpty()
        }

    val editable: Boolean
        get() = gameState.value.let {
            it.day == 0 &&
                    !it.needsRegistration &&
                    it.pendingEvent == null &&
                    it.pendingConfession == null &&
                    it.pendingMiniGame == null && it.miniBonus == null &&
                    it.life.pendingMorning == null && it.life.morningNotice == null &&
                    it.teamProject?.announced != false &&
                    it.semesterResult == null &&
                    scheduleState.value.size == 28
        }

    init {
        scope.launch {
            readGame()
            readRoutines()
            ready = true

            snapshotFlow {
                GameSnapshot(
                    gameState.value,
                    scheduleState.value,
                    draftState.value,
                    screenState.value
                )
            }.collectLatest {
                if (allowSaving && hasGame) {
                    persist()
                }
            }
        }
    }

    private suspend fun readGame() {
        allowSaving = false

        store.loadGame().fold(
            onSuccess = { saved ->
                if (saved != null) {
                    gameState.value = saved.game
                    scheduleState.value = saved.schedule
                    draftState.value = saved.selectedIds
                    lastScreen = saved.screen
                    saveStatus = "저장된 게임을 불러왔어요"
                } else {
                    saveStatus = "저장된 게임이 없어요"
                }

                screenState.value = "title"
                loadError = null
                allowSaving = true
            },
            onFailure = {
                loadError = "저장 데이터를 불러오지 못했어요."
                saveStatus = "불러오기 실패"
            }
        )
    }

    private suspend fun readRoutines() {
        store.loadRoutines().fold(
            onSuccess = {
                routines = it
                routineError = null
            },
            onFailure = {
                routineError = "루틴을 불러오지 못했어요."
            }
        )
    }

    private suspend fun persist() {
        if (!allowSaving || !hasGame || restoringCloud) return

        val currentScreen = screenState.value
        if (currentScreen !in setOf("title", "account")) lastScreen = currentScreen

        val snapshot = GameSnapshot(
            gameState.value,
            scheduleState.value,
            draftState.value,
            lastScreen
        )

        val request = ++saveRequest
        saveStatus = "저장 중"

        val result = store.saveGame(snapshot, request)

        if (request == saveRequest) {
            saveStatus = if (result.isSuccess) {
                "저장 완료"
            } else {
                "저장 실패 · 다시 저장해 주세요"
            }
        }
    }

    fun saveNow() {
        if (ready) scope.launch { persist() }
    }

    fun retryLoad() {
        if (!ready) return

        ready = false
        scope.launch {
            readGame()
            ready = true
        }
    }

    fun retryRoutines() {
        if (routineBusy) return

        routineBusy = true
        scope.launch {
            try {
                readRoutines()
            } finally {
                routineBusy = false
            }
        }
    }

    fun startNewGame() {
        if (onlineBusy) return
        cloudPreview = null
        allowSaving = true
        loadError = null
        gameState.value = GameState(needsRegistration = true)
        scheduleState.value = emptyList()
        draftState.value = emptyList()
        lastScreen = "register"
        screenState.value = "register"
    }

    fun resumeGame() {
        val game = gameState.value

        screenState.value = when {
            game.needsRegistration -> "register"
            game.pendingConfession != null -> "love"
            game.life.pendingMorning != null -> "morning"
            game.pendingMiniGame != null -> "mini"
            game.pendingEvent != null -> "event"
            game.teamProject?.announced == false -> "team"
            game.exam != null -> "exam"

            game.semesterResult != null -> when {
                lastScreen == "vacation" && game.semester < 8 ->
                    "vacation"
                lastScreen == "graduation" && game.semester == 8 ->
                    "graduation"
                else -> "semester"
            }

            lastScreen == "schedule" && game.day == 0 ->
                "schedule"

            lastScreen in setOf("main", "report", "grades", "routines", "team", "love", "life") ->
                lastScreen

            else -> "main"
        }
    }

    private fun validName(name: String): String? {
        val clean = name.trim()

        if (clean.length !in 1..30) {
            message = "루틴 이름을 1~30자로 입력해 주세요."
            return null
        }

        return clean
    }

    private fun changeRoutines(
        next: List<ScheduleRoutine>,
        successMessage: String
    ) {
        if (routineBusy) return

        if (routineError != null) {
            message = "루틴을 먼저 다시 불러와 주세요."
            return
        }

        routineBusy = true

        scope.launch {
            try {
                store.saveRoutines(next).fold(
                    onSuccess = {
                        routines = next
                        message = successMessage
                    },
                    onFailure = {
                        message = "루틴 저장에 실패했어요."
                    }
                )
            } finally {
                routineBusy = false
            }
        }
    }

    fun createRoutine(name: String) {
        if (!editable) return
        val clean = validName(name) ?: return

        if (routines.any { it.name.equals(clean, ignoreCase = true) }) {
            message = "같은 이름의 루틴이 있어요."
            return
        }

        val routine = ScheduleRoutine(
            UUID.randomUUID().toString(),
            clean,
            RoutineRules.capture(scheduleState.value)
        )

        changeRoutines(routines + routine, "루틴을 저장했어요.")
    }

    fun renameRoutine(id: String, name: String) {
        val clean = validName(name) ?: return
        if (routines.none { it.id == id }) return

        if (routines.any {
                it.id != id && it.name.equals(clean, ignoreCase = true)
            }) {
            message = "같은 이름의 루틴이 있어요."
            return
        }

        changeRoutines(
            routines.map {
                if (it.id == id) it.copy(name = clean) else it
            },
            "루틴 이름을 변경했어요."
        )
    }

    fun updateRoutine(id: String) {
        if (!editable || routines.none { it.id == id }) return
        val slots = RoutineRules.capture(scheduleState.value)

        changeRoutines(
            routines.map {
                if (it.id == id) it.copy(slots = slots) else it
            },
            "현재 시간표로 루틴을 수정했어요."
        )
    }

    fun deleteRoutine(id: String) {
        if (routines.none { it.id == id }) return

        changeRoutines(
            routines.filterNot { it.id == id },
            "루틴을 삭제했어요."
        )
    }

    fun applyRoutine(id: String) {
        if (!editable || routineBusy || routineError != null) return
        val routine = routines.find { it.id == id } ?: return

        scheduleState.value = RoutineRules.apply(
            routine,
            scheduleState.value,
            gameState.value
        )

        screenState.value = "schedule"
        message = "루틴을 적용했어요. 공부할 과목을 확인해 주세요."
    }

    fun clearMessage() {
        message = ""
    }

    private fun backupSnapshot() = GameSnapshot(gameState.value, scheduleState.value, draftState.value, lastScreen)

    private fun onlineFailure(error: Exception): String = when (error) {
        is CloudConflictException -> requireNotNull(error.message)
        is FirebaseNetworkException -> "인터넷에 연결하지 못했어요. 기기의 게임은 유지돼요. 연결 후 다시 시도해 주세요."
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "이미 가입된 이메일이에요. 로그인을 이용해 주세요."
            "ERROR_WEAK_PASSWORD" -> "Firebase 프로젝트의 비밀번호 조건에 맞게 입력해 주세요."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Firebase Console에서 이메일/비밀번호 로그인을 활성화해 주세요."
            "ERROR_TOO_MANY_REQUESTS" -> "요청이 많아요. 잠시 후 다시 시도해 주세요."
            else -> "로그인하지 못했어요. 이메일과 비밀번호를 확인해 주세요."
        }
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "클라우드 접근이 거부됐어요. 로그인 계정과 Firestore 규칙을 확인해 주세요."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "서버에 연결하지 못했어요. 인터넷과 Firestore 데이터베이스 설정을 확인해 주세요."
            FirebaseFirestoreException.Code.ABORTED -> "다른 저장 요청과 겹쳤어요. 클라우드 내용을 새로 확인해 주세요."
            else -> "클라우드 요청에 실패했어요. Firestore 설정과 연결 상태를 확인해 주세요."
        }
        is IllegalArgumentException, is IllegalStateException -> error.message ?: "저장 데이터를 확인하지 못했어요."
        else -> "요청을 처리하지 못했어요. 연결 상태와 Firebase 설정을 확인해 주세요."
    }

    private fun launchOnline(action: suspend () -> Unit) {
        if (onlineBusy) return
        onlineBusy = true
        onlineError = null
        scope.launch {
            try {
                check(onlineConfigured) { onlineSetupMessage }
                action()
            } catch (_: TimeoutCancellationException) {
                cloudPreview = null
                onlineError = "응답 시간이 길어졌어요. 요청이 서버에 반영됐을 수 있으니 새로고침으로 확인해 주세요."
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                cloudPreview = null
                onlineError = onlineFailure(error)
            } finally {
                onlineBusy = false
            }
        }
    }

    fun authenticate(email: String, password: String, register: Boolean) {
        launchOnline {
            CloudSyncRules.validateCredentials(email, password, register)
            withTimeout(20_000L) { online.signIn(email, password, register) }
            onlineStatus = if (register) "가입하고 로그인했어요. 기기의 게임은 그대로예요." else "로그인했어요. 기기의 게임은 그대로예요."
        }
    }

    fun signOut() {
        if (onlineBusy) return
        online.signOut()
        cloudPreview = null
        cloudRecord = null
        onlineUser = null
        onlineError = null
        onlineStatus = "로그아웃했어요. 기기의 게임과 루틴은 계속 사용할 수 있어요."
    }

    private suspend fun readCloud(uid: String): CloudSaveRecord? {
        val remote = withTimeout(20_000L) { online.read(uid) } ?: return null
        val decoded = GameBackupCodec.decode(remote.payload, System.currentTimeMillis(), SystemClock.elapsedRealtime())
        return remote.copy(summary = SaveSummary.from(decoded.game))
    }

    fun refreshCloud() {
        launchOnline {
            val uid = requireNotNull(onlineUser?.uid) { "먼저 로그인해 주세요." }
            val remote = readCloud(uid)
            check(onlineUser?.uid == uid) { "로그인 계정이 변경됐어요." }
            cloudRecord = remote
            cloudPreview = null
            onlineStatus = if (remote == null) "이 계정에 저장된 게임이 없어요." else "서버의 저장 내용을 확인했어요."
        }
    }

    fun prepareCloudTransfer(action: CloudAction) {
        launchOnline {
            val uid = requireNotNull(onlineUser?.uid) { "먼저 로그인해 주세요." }
            val local = backupSnapshot()
            if (action == CloudAction.UPLOAD) check(hasGame && loadError == null) { "먼저 기기에서 게임을 시작하거나 불러와 주세요." }
            val remote = readCloud(uid)
            check(onlineUser?.uid == uid) { "로그인 계정이 변경됐어요." }
            val payload = if (action == CloudAction.UPLOAD) {
                GameBackupCodec.encode(local, System.currentTimeMillis(), SystemClock.elapsedRealtime())
            } else requireNotNull(remote) { "이 계정에 저장된 게임이 없어요." }.payload
            val summary = if (action == CloudAction.UPLOAD) SaveSummary.from(local.game) else requireNotNull(remote).summary
            cloudRecord = remote
            cloudPreview = CloudTransferPreview(action, uid, local, remote, payload, summary)
            onlineStatus = "내용을 확인한 뒤 적용해 주세요."
        }
    }

    fun cancelCloudPreview() { if (!onlineBusy) cloudPreview = null }

    fun confirmCloudTransfer() {
        val preview = cloudPreview ?: return
        launchOnline {
            CloudSyncRules.requireCurrent(preview, onlineUser?.uid, backupSnapshot())
            if (preview.action == CloudAction.UPLOAD) {
                withTimeout(20_000L) {
                    online.upload(preview.uid, preview.remote?.revision ?: 0L, preview.payload, preview.sourceSummary)
                }
                cloudRecord = CloudSaveRecord((preview.remote?.revision ?: 0L) + 1L, 0L, preview.payload, preview.sourceSummary)
                cloudPreview = null
                onlineStatus = "서버에 게임을 저장했어요. 저장 시각은 새로고침으로 확인할 수 있어요."
            } else {
                val latest = readCloud(preview.uid)
                CloudSyncRules.requireRevision(requireNotNull(preview.remote).revision, latest?.revision ?: 0L)
                require(latest?.payload == preview.payload) { "저장 내용이 변경됐어요. 다시 확인해 주세요." }
                CloudSyncRules.requireCurrent(preview, onlineUser?.uid, backupSnapshot())
                val restored = GameBackupCodec.decode(preview.payload, System.currentTimeMillis(), SystemClock.elapsedRealtime())
                // 디스크 저장 성공 후에만 화면과 진행을 교체한다. 이전 자동 저장은 revision으로 차단한다.
                restoringCloud = true
                try {
                    val request = ++saveRequest
                    store.saveGame(restored, request).getOrThrow()
                    gameState.value = restored.game
                    scheduleState.value = restored.schedule
                    draftState.value = restored.selectedIds
                    lastScreen = restored.screen
                    loadError = null
                    allowSaving = true
                    saveStatus = "저장 완료"
                    cloudPreview = null
                    cloudRecord = latest
                    onlineStatus = "클라우드 게임을 이 기기에 불러왔어요. 저장해 둔 루틴 목록은 유지돼요."
                    resumeGame()
                } finally {
                    restoringCloud = false
                }
            }
        }
    }

    override fun onCleared() {
        authSubscription.close()
        scope.cancel()
        super.onCleared()
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameSession::class.java))
            return GameSession(LocalGameStore(appContext), FirebaseGameService(appContext)) as T
        }
    }
}
