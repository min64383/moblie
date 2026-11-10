package com.example.mobile_project

// 결과 화면 진행 방식: 하루씩(직접 넘김) / 한 주 자동(하루씩 자동으로 넘어감)
enum class ProgressMode(val label: String) {
    DAY("하루씩"),
    WEEK("한 주 자동");

    companion object {
        // 원본 HTML의 autoWeekPace(950ms)와 같은 간격
        const val AUTO_DELAY_MS = 950L

        fun fromName(name: String): ProgressMode = values().find { it.name == name } ?: DAY
    }
}

data class DayLogEntry(val day: Int, val lines: List<String>) {
    val dayName: String get() = GameEngine.dayNames[day.coerceIn(0, 6)]
}

// 주간 보기에서 이번 주에 지나간 날들의 기록을 모아 둔다.
// rememberSaveable에 그대로 넣을 수 있게 문자열 하나로 인코딩한다.
object WeekLogBook {
    private const val ENTRY = '\u001E'
    private const val LINE = '\u001F'

    private fun weekKey(state: GameState) = "${state.semester}-${state.week}"

    fun decode(encoded: String, state: GameState): List<DayLogEntry> {
        val parts = encoded.split(ENTRY)
        if (parts.isEmpty() || parts.first() != weekKey(state)) return emptyList()
        return parts.drop(1).mapNotNull { raw ->
            val fields = raw.split(LINE)
            val day = fields.firstOrNull()?.toIntOrNull() ?: return@mapNotNull null
            DayLogEntry(day, fields.drop(1))
        }.sortedBy { it.day }
    }

    // 하루가 끝난 상태(day = 끝난 요일 + 1)의 로그를 기록한다. 같은 날은 한 번만 기록한다.
    fun record(encoded: String, state: GameState): String {
        if (state.day !in 1..7 || state.log.isEmpty()) return encoded
        val finished = state.day - 1
        val current = decode(encoded, state)
        if (current.any { it.day == finished }) return encoded
        return encode(state, current + DayLogEntry(finished, state.log))
    }

    private fun encode(state: GameState, entries: List<DayLogEntry>): String =
        (listOf(weekKey(state)) + entries.sortedBy { it.day }.map { entry ->
            (listOf(entry.day.toString()) + entry.lines.map { it.replace(ENTRY, ' ').replace(LINE, ' ') })
                .joinToString(LINE.toString())
        }).joinToString(ENTRY.toString())

    // 자동 진행을 이어 가도 되는 상태인지 (사건·미니게임·시험 등 사용자 입력이 필요한 상태가 아닌지)
    fun canAutoAdvance(state: GameState): Boolean =
        state.day in 1..6 && !state.needsRegistration && state.semesterResult == null &&
            state.exam == null && state.pendingEvent == null && state.pendingMiniGame == null &&
            state.pendingConfession == null && state.life.pendingMorning == null &&
            state.teamProject?.announced != false
}
