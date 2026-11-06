package com.example.mobile_project

import android.content.Context
import android.os.SystemClock
import android.util.AtomicFile
import androidx.compose.runtime.saveable.SaverScope
import java.io.File
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class GameSnapshot(
    val game: GameState,
    val schedule: List<TimeSlot>,
    val selectedIds: List<String>,
    val screen: String
)

data class ScheduleRoutine(
    val id: String,
    val name: String,
    val slots: List<TimeSlot>
)

object RoutineRules {
    fun capture(schedule: List<TimeSlot>): List<TimeSlot> {
        require(schedule.size == 28)

        return schedule.map { slot ->
            if (slot.locked) TimeSlot() else slot.copy()
        }
    }

    fun apply(
        routine: ScheduleRoutine,
        current: List<TimeSlot>,
        game: GameState
    ): List<TimeSlot> {
        require(current.size == 28 && routine.slots.size == 28)
        val firstSubject = game.study.keys.firstOrNull()

        return current.mapIndexed { index, currentSlot ->
            val saved = routine.slots[index]

            when {
                currentSlot.locked -> currentSlot
                saved.locked -> TimeSlot()

                !AcademicEngine.canUseActivity(game, saved.activity) ->
                    TimeSlot()

                saved.activity == ActivityType.STUDY -> {
                    val subjectId = saved.subjectId
                        ?.takeIf { it in game.study }
                        ?: firstSubject

                    if (subjectId == null) {
                        TimeSlot()
                    } else {
                        TimeSlot(ActivityType.STUDY, subjectId)
                    }
                }

                else -> TimeSlot(saved.activity)
            }
        }
    }
}

class LocalGameStore(context: Context) {
    private val gameFile = AtomicFile(
        File(context.applicationContext.filesDir, "pooang-game-v1.bin")
    )

    private val routineFile = AtomicFile(
        File(context.applicationContext.filesDir, "pooang-routines-v1.bin")
    )

    private val mutex = Mutex()
    private var lastGameRevision = 0L

    private val saverScope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    private suspend fun <T> disk(block: () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                currentCoroutineContext().ensureActive()

                try {
                    Result.success(block())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

    private fun write(
        file: AtomicFile,
        block: (ObjectOutputStream) -> Unit
    ) {
        val stream = file.startWrite()

        try {
            val output = ObjectOutputStream(stream)
            block(output)
            output.flush()
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }

    private fun <T> read(
        file: AtomicFile,
        block: (ObjectInputStream) -> T
    ): T? {
        val stream = try {
            file.openRead()
        } catch (_: FileNotFoundException) {
            return null
        }

        return ObjectInputStream(stream).use { block(it) }
    }

    private fun packSlots(slots: List<TimeSlot>): ArrayList<String> =
        ArrayList(slots.map {
            "${it.activity.name}|${it.subjectId.orEmpty()}"
        })

    private fun unpackSlots(value: Any): List<TimeSlot> =
        (value as List<*>).map { entry ->
            val parts = (entry as String).split('|', limit = 2)

            TimeSlot(
                ActivityType.valueOf(parts[0]),
                parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
            )
        }

    suspend fun saveGame(
        snapshot: GameSnapshot,
        revision: Long
    ): Result<Unit> = disk {
        // 늦게 도착한 과거 저장 요청이 최신 파일을 덮지 않게 한다.
        if (revision > lastGameRevision) {
            val rawGame = with(gameStateSaver) {
                saverScope.save(snapshot.game)
            } ?: error("게임 상태를 변환하지 못했습니다.")

            val savedAt = System.currentTimeMillis()
            val now = SystemClock.elapsedRealtime()

            val remaining = snapshot.game.exam
                ?.takeIf { it.phase == ExamPhase.QUESTION }
                ?.let { (it.deadlineMs - now).coerceAtLeast(0L) }
                ?: 0L

            write(gameFile) { output ->
                output.writeUTF("POOANG_GAME_1")
                output.writeObject(rawGame)
                output.writeObject(packSlots(snapshot.schedule))
                output.writeObject(ArrayList(snapshot.selectedIds))
                output.writeUTF(snapshot.screen)
                output.writeLong(savedAt)
                output.writeLong(remaining)
            }

            lastGameRevision = revision
        }

        Unit
    }

    suspend fun loadGame(): Result<GameSnapshot?> = disk {
        read(gameFile) { input ->
            check(input.readUTF() == "POOANG_GAME_1")

            var game = gameStateSaver.restore(
                requireNotNull(input.readObject())
            ) ?: error("게임 상태를 복원하지 못했습니다.")

            game = MiniGameEngine.refresh(game, System.currentTimeMillis())
            game = LifeEngine.refreshMorning(game, System.currentTimeMillis())
            val schedule = unpackSlots(input.readObject())
            val selectedIds = (input.readObject() as List<*>)
                .map { it as String }
                .distinct()
                .take(3)

            val screen = input.readUTF()
            val savedAt = input.readLong()
            val remaining = input.readLong()

            require(schedule.isEmpty() || schedule.size == 28)

            if (game.study.isNotEmpty()) {
                require(game.study.size == 3)
                require(schedule.size == 28)
                require(game.study.keys.all { subjectForId(it) != null })
            }

            EventEngine.validatePending(game)
            TeamEngine.validateState(game, schedule)
            LoveEngine.validateState(game, schedule)
            MiniGameEngine.validateState(game, schedule)
            LifeEngine.validateState(game, schedule)

            game.exam?.let { run ->
                require(run.questionIds.isNotEmpty())
                require(run.index in run.questionIds.indices)
                require(run.optionOrders.size == run.questionIds.size)

                require(run.questionIds.all {
                    it in ExamEngine.questionBank.indices
                })

                require(run.optionOrders.all { order ->
                    order.size == run.optionCount &&
                            order.distinct().size == order.size &&
                            0 in order &&
                            order.all { it in 0..3 }
                })

                if (run.phase == ExamPhase.QUESTION) {
                    val elapsed = System.currentTimeMillis() - savedAt

                    val left = if (elapsed < 0L) {
                        0L
                    } else {
                        (remaining - elapsed).coerceIn(
                            0L,
                            run.secondsPerQuestion * 1_000L
                        )
                    }

                    game = game.copy(
                        exam = run.copy(
                            deadlineMs = SystemClock.elapsedRealtime() + left
                        )
                    )
                }
            }

            GameSnapshot(game, LoveEngine.syncSchedule(game, TeamEngine.syncSchedule(game, schedule)), selectedIds, screen)
        }
    }

    suspend fun saveRoutines(
        routines: List<ScheduleRoutine>
    ): Result<Unit> = disk {
        write(routineFile) { output ->
            output.writeUTF("POOANG_ROUTINES_1")
            output.writeInt(routines.size)

            routines.forEach { routine ->
                output.writeUTF(routine.id)
                output.writeUTF(routine.name)
                output.writeObject(packSlots(routine.slots))
            }
        }
    }

    suspend fun loadRoutines(): Result<List<ScheduleRoutine>> = disk {
        read(routineFile) { input ->
            check(input.readUTF() == "POOANG_ROUTINES_1")

            val count = input.readInt()
            require(count >= 0)

            buildList {
                repeat(count) {
                    val id = input.readUTF()
                    val name = input.readUTF()
                    val slots = unpackSlots(input.readObject())

                    require(slots.size == 28)
                    add(ScheduleRoutine(id, name, slots))
                }
            }
        } ?: emptyList()
    }
}
