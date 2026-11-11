package com.example.mobile_project

/**
 * 하루 기록(GameState.log)의 글자를 화면에 그리기 좋은 모양으로 나눈다.
 *
 * 로그 한 줄은 두 종류다.
 * - 시간대 줄: "오전 · 자료구조 공부\n체력 -9 · 행복도 -3 · 공부량 +12"
 * - 그 밖의 알림 줄: "늦잠 · 수업에 지각했어요…", "주간 결산 · 생활비 25000원 청구", "미니게임 · …"
 */
object DayLog {
    val blockNames = listOf("오전", "오후", "저녁", "밤")

    data class Delta(val name: String, val value: Int, val unit: String = "") {
        val text: String get() = "$name ${if (value > 0) "+" else if (value < 0) "−" else "±"}${kotlin.math.abs(value)}$unit"
    }

    sealed interface Entry

    /** 시간대 활동 한 칸. [note] 는 "취소" 같은 설명 글. */
    data class Block(val block: Int, val title: String, val deltas: List<Delta>, val note: String?) : Entry

    /** 시간대가 아닌 알림. [tag] 는 " · " 앞 글자(예: "늦잠"), 없으면 null. */
    data class Note(val tag: String?, val text: String, val bad: Boolean) : Entry

    private val deltaRegex = Regex("""^(.+?) ([+-]\d[\d,]*)(원)?$""")
    private val badWords = listOf("취소", "지각", "감기", "한도", "감소", "깎", "실패", "놓쳤", "늦었")

    fun parse(lines: List<String>): List<Entry> = lines.map(::parseLine)

    fun parseLine(line: String): Entry {
        val head = line.substringBefore('\n')
        val rest = if ('\n' in line) line.substringAfter('\n') else null
        val block = blockNames.indexOfFirst { head.startsWith("$it · ") }
        if (block >= 0) {
            val title = head.removePrefix("${blockNames[block]} · ")
            val deltas = rest?.let(::parseDeltas)
            val note = if (deltas == null) rest?.takeUnless { it == "변화 없음" } else null
            return Block(block, title, deltas.orEmpty(), note)
        }
        val tag = if (" · " in head) head.substringBefore(" · ") else null
        val text = if (tag != null) line.substringAfter(" · ") else line
        return Note(tag, text, badWords.any { it in line })
    }

    /** "체력 -9 · 행복도 -3 · 돈 +25000원" → 증감 목록. 하나라도 형식이 안 맞으면 null(설명 글). */
    fun parseDeltas(text: String): List<Delta>? {
        if (text == "변화 없음") return emptyList()
        val parts = text.split(" · ")
        val deltas = parts.map { part ->
            val m = deltaRegex.matchEntire(part.trim()) ?: return null
            Delta(m.groupValues[1], m.groupValues[2].replace(",", "").toInt(), m.groupValues[3])
        }
        return deltas
    }

    /** 하루 동안 스탯별 증감 합계. 예) {체력: -12, 행복도: +5, 돈: +25000} */
    fun totals(entries: List<Entry>): Map<String, Int> {
        val sum = linkedMapOf<String, Int>()
        entries.filterIsInstance<Block>().flatMap { it.deltas }.forEach { d ->
            sum[d.name] = (sum[d.name] ?: 0) + d.value
        }
        return sum
    }
}
