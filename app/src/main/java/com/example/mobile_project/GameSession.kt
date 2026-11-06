package com.example.mobile_project

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GameSession(
    private val store: LocalGameStore
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

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var allowSaving = false
    private var lastScreen = "main"
    private var saveRequest = 0L

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
        if (!allowSaving || !hasGame) return

        val currentScreen = screenState.value
        if (currentScreen != "title") lastScreen = currentScreen

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

            lastScreen in setOf("main", "report", "grades", "routines") ->
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

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameSession::class.java))
            return GameSession(LocalGameStore(appContext)) as T
        }
    }
}