package com.example.mobile_project

enum class LoveChoice { UNDECIDED, ACCEPT, DECLINE }
enum class LoveEndReason(val label: String) {
    MISSED_DATES("데이트 누적 3회 취소"), CHOICE("선택 이벤트로 이별")
}

data class LoveConfession(
    val semester: Int,
    val partner: String,
    val choice: LoveChoice = LoveChoice.UNDECIDED
) {
    val key: String get() = "$semester:$partner"
}

data class LoveIncidentOption(
    val text: String, val hp: Int = 0, val happiness: Int = 0, val money: Int = 0
)

data class LoveIncident(val day: Int, val optionIndex: Int, val applied: Boolean = false)

data class LoveRelationship(
    val partner: String,
    val startSemester: Int,
    val startWeek: Int,
    val dateIndex: Int,
    val weeksTogether: Int = 0,
    val missedDates: Int = 0,
    val affection: Int = 50,
    val lastSettledWeek: Int = (startSemester - 1) * 16 + startWeek - 2,
    val plannedWeek: Int = -1,
    val dateHandledWeek: Int = -1,
    val incident: LoveIncident? = null
) {
    val key: String get() = "$startSemester:$startWeek:$partner"
}

data class LoveRecord(
    val partner: String,
    val startSemester: Int,
    val startWeek: Int,
    val endSemester: Int,
    val endWeek: Int,
    val weeksTogether: Int,
    val affection: Int,
    val missedDates: Int,
    val reason: LoveEndReason
) {
    val key: String get() = "$startSemester:$startWeek:$partner"
}

data class LoveWeekPreparation(val game: GameState, val schedule: List<TimeSlot>)
data class LoveDateOutcome(val game: GameState, val message: String)

internal object LoveStateCodec {
    fun packConfession(value: LoveConfession?): ArrayList<String> =
        if (value == null) arrayListOf() else arrayListOf(
            value.semester.toString(), value.partner, value.choice.name
        )

    fun unpackConfession(value: Any?): LoveConfession? {
        val p = (value as? List<*>).orEmpty().map { it as String }
        if (p.isEmpty()) return null
        require(p.size == 3)
        return LoveConfession(p[0].toInt(), p[1], LoveChoice.valueOf(p[2]))
    }

    fun packRelationship(value: LoveRelationship?): ArrayList<String> =
        if (value == null) arrayListOf() else arrayListOf(
            value.partner, value.startSemester.toString(), value.startWeek.toString(),
            value.dateIndex.toString(), value.weeksTogether.toString(), value.missedDates.toString(),
            value.affection.toString(), value.lastSettledWeek.toString(), value.plannedWeek.toString(),
            value.dateHandledWeek.toString(), value.incident?.day?.toString().orEmpty(),
            value.incident?.optionIndex?.toString().orEmpty(), value.incident?.applied?.toString().orEmpty()
        )

    fun unpackRelationship(value: Any?): LoveRelationship? {
        val p = (value as? List<*>).orEmpty().map { it as String }
        if (p.isEmpty()) return null
        require(p.size == 13)
        return LoveRelationship(
            partner = p[0], startSemester = p[1].toInt(), startWeek = p[2].toInt(),
            dateIndex = p[3].toInt(), weeksTogether = p[4].toInt(), missedDates = p[5].toInt(),
            affection = p[6].toInt(), lastSettledWeek = p[7].toInt(), plannedWeek = p[8].toInt(),
            dateHandledWeek = p[9].toInt(), incident = if (p[10].isEmpty()) null else
                LoveIncident(p[10].toInt(), p[11].toInt(), p[12].toBooleanStrict())
        )
    }

    fun packHistory(values: List<LoveRecord>): ArrayList<String> = ArrayList(values.map {
        "${it.partner}|${it.startSemester}|${it.startWeek}|${it.endSemester}|${it.endWeek}|" +
            "${it.weeksTogether}|${it.affection}|${it.missedDates}|${it.reason.name}"
    })

    fun unpackHistory(value: Any?): List<LoveRecord> = (value as? List<*>).orEmpty().map {
        val p = (it as String).split('|')
        require(p.size == 9)
        LoveRecord(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(),
            p[5].toInt(), p[6].toInt(), p[7].toInt(), LoveEndReason.valueOf(p[8]))
    }
}
