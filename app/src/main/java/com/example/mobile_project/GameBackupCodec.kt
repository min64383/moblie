package com.example.mobile_project

import androidx.compose.runtime.saveable.SaverScope
import org.json.JSONArray
import org.json.JSONObject

// 클라우드에는 JSON 문자열 하나를 저장한다. Int/Long/Double 형식을 보존하여 기존 Saver를 그대로 사용한다.
object GameBackupCodec {
    const val VERSION = 1
    const val MAX_BYTES = 800_000
    private val scope = object : SaverScope { override fun canBeSaved(value: Any): Boolean = true }

    private fun encodeValue(value: Any): Any = when (value) {
        is String, is Boolean -> value
        is Int -> JSONObject().put("i", value.toString())
        is Long -> JSONObject().put("l", value.toString())
        is Double -> { require(value.isFinite()); JSONObject().put("d", value.toString()) }
        is List<*> -> JSONArray().also { array -> value.forEach { array.put(encodeValue(requireNotNull(it))) } }
        else -> error("지원하지 않는 저장 형식이에요.")
    }

    private fun decodeValue(value: Any, depth: Int = 0): Any {
        require(depth <= 32) { "저장 데이터 구조가 너무 깊어요." }
        return when (value) {
            is String, is Boolean -> value
            is JSONArray -> ArrayList((0 until value.length()).map { decodeValue(value.get(it), depth + 1) })
            is JSONObject -> {
                require(value.length() == 1)
                when {
                    value.has("i") -> value.getString("i").toInt()
                    value.has("l") -> value.getString("l").toLong()
                    value.has("d") -> value.getString("d").toDouble().also { require(it.isFinite()) }
                    else -> error("알 수 없는 숫자 형식이에요.")
                }
            }
            else -> error("손상된 저장 데이터예요.")
        }
    }

    fun validate(snapshot: GameSnapshot) {
        val game = snapshot.game
        require(game.semester in 1..8 && game.week in 1..16 && game.day in 0..7)
        require(game.maxHp in 50..150 && game.hp in 0..game.maxHp && game.happiness in 0..100)
        require(game.network in 0..100 && game.charm in 0..100 && game.spec in 0..100 && game.money >= -50_000)
        require(game.study.isNotEmpty() || game.needsRegistration || game.completedSemesters.isNotEmpty())
        require(game.study.isEmpty() || game.study.size == 3)
        require(game.study.all { subjectForId(it.key) != null && it.value >= 0 })
        require(snapshot.schedule.isEmpty() || snapshot.schedule.size == 28)
        if (game.study.isNotEmpty()) require(snapshot.schedule.size == 28)
        require(snapshot.selectedIds.size <= 3 && snapshot.selectedIds.distinct().size == snapshot.selectedIds.size)
        val available = subjectsForSemester(game.semester).map { it.id }
        require(snapshot.selectedIds.all { it in available })
        snapshot.schedule.forEach { slot ->
            if (slot.activity in setOf(ActivityType.STUDY, ActivityType.CLASS)) require(slot.subjectId in game.study)
        }
        EventEngine.validatePending(game)
        TeamEngine.validateState(game, snapshot.schedule)
        LoveEngine.validateState(game, snapshot.schedule)
        MiniGameEngine.validateState(game, snapshot.schedule)
        LifeEngine.validateState(game, snapshot.schedule)
        game.exam?.let { run ->
            require(run.questionIds.isNotEmpty() && run.index in run.questionIds.indices)
            require(run.questionIds.all { it in ExamEngine.questionBank.indices })
            require(run.optionCount in 2..4 && run.secondsPerQuestion in 1..60)
            require(run.correct in 0..run.questionIds.size && run.optionOrders.size == run.questionIds.size)
            require(run.optionOrders.all { order -> order.size == run.optionCount && order.distinct().size == order.size &&
                0 in order && order.all { it in 0..3 } })
        }
    }

    fun encode(snapshot: GameSnapshot, nowEpochMs: Long, nowElapsedMs: Long): String {
        validate(snapshot)
        val raw = requireNotNull(with(gameStateSaver) { scope.save(snapshot.game) })
        val remaining = snapshot.game.exam?.takeIf { it.phase == ExamPhase.QUESTION }?.let {
            (it.deadlineMs - nowElapsedMs).coerceIn(0L, it.secondsPerQuestion * 1_000L)
        } ?: 0L
        val slots = JSONArray().also { array -> snapshot.schedule.forEach { slot ->
            array.put(JSONObject().put("activity", slot.activity.name).put("subject", slot.subjectId.orEmpty()))
        } }
        val result = JSONObject().put("version", VERSION).put("savedAt", nowEpochMs.toString())
            .put("examRemaining", remaining.toString()).put("game", encodeValue(raw))
            .put("schedule", slots).put("selected", JSONArray(snapshot.selectedIds))
            .put("screen", snapshot.screen).toString()
        require(result.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "저장 데이터가 클라우드 크기 한도를 넘었어요." }
        return result
    }

    fun decode(payload: String, nowEpochMs: Long, nowElapsedMs: Long): GameSnapshot {
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "저장 데이터가 너무 커요." }
        val root = JSONObject(payload)
        require(root.getInt("version") == VERSION) { "지원하지 않는 클라우드 저장 버전이에요. 앱 버전을 확인해 주세요." }
        val raw = decodeValue(root.get("game")) as List<*>
        require(raw.size % 2 == 0)
        val fields = raw.chunked(2).associate { (it[0] as String) to it[1] }
        require(fields.size * 2 == raw.size)
        require((fields["semester"] as Int) in 1..8 && (fields["week"] as Int) in 1..16 && (fields["day"] as Int) in 0..7)
        var game = requireNotNull(gameStateSaver.restore(raw))
        game.exam?.takeIf { it.phase == ExamPhase.QUESTION }?.let { run ->
            val savedAt = root.getString("savedAt").toLong()
            val elapsed = nowEpochMs - savedAt
            val stored = root.getString("examRemaining").toLong()
            require(stored in 0..run.secondsPerQuestion * 1_000L)
            val left = if (elapsed < 0L) 0L else (stored - elapsed).coerceIn(0L, run.secondsPerQuestion * 1_000L)
            game = game.copy(exam = run.copy(deadlineMs = nowElapsedMs + left))
        }
        game = MiniGameEngine.refresh(game, nowEpochMs)
        game = LifeEngine.refreshMorning(game, nowEpochMs)
        val slots = root.getJSONArray("schedule")
        val schedule = (0 until slots.length()).map { index ->
            val slot = slots.getJSONObject(index)
            TimeSlot(ActivityType.valueOf(slot.getString("activity")), slot.getString("subject").takeIf(String::isNotEmpty))
        }
        val selected = root.getJSONArray("selected")
        val snapshot = GameSnapshot(game, schedule, (0 until selected.length()).map(selected::getString), root.getString("screen"))
        validate(snapshot)
        return snapshot.copy(schedule = LoveEngine.syncSchedule(game, TeamEngine.syncSchedule(game, schedule)))
    }
}
