package com.example.mobile_project

enum class EventPhase { CHOICE, RESULT }
enum class EventBuffScope { THIS_WEEK, NEXT_WEEK }

data class EventEffect(
    val hp: Int = 0,
    val happiness: Int = 0,
    val money: Int = 0,
    val network: Int = 0,
    val charm: Int = 0,
    val spec: Int = 0,
    val classStudy: Int = 0,
    val teamBonus: Int = 0,
    val gpaAdjustment: Double = 0.0,
    val affection: Int = 0,
    val endLove: Boolean = false
)

data class EventBuffSpec(
    val activity: ActivityType,
    val multiplier: Double,
    val scope: EventBuffScope,
    val label: String
)

data class EventOption(
    val label: String,
    val result: String,
    val effect: EventEffect = EventEffect(),
    val buff: EventBuffSpec? = null
)

data class CampusEvent(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val options: List<EventOption>,
    val activity: ActivityType? = null,
    val semesterLimit: Int = 2,
    val weeklyEvent: Boolean = false
)

// day: 하루 진행 전에는 0~6, 주간 행사에서는 정산이 끝난 7.
data class CampusEventRun(
    val eventId: String,
    val semester: Int,
    val week: Int,
    val day: Int,
    val subjectId: String? = null,
    val phase: EventPhase = EventPhase.CHOICE,
    val selectedIndex: Int = -1,
    val changes: List<String> = emptyList(),
    val contextName: String = ""
) {
    val key: String get() = "$semester:$week:$day:$eventId"
}

data class CampusEventRecord(
    val semester: Int,
    val week: Int,
    val day: Int,
    val eventId: String,
    val selectedIndex: Int = -1
) {
    val key: String get() = "$semester:$week:$day:$eventId"
}

data class EventBuff(
    val activity: ActivityType,
    val multiplier: Double,
    val startDay: Int,
    val endDay: Int,
    val label: String
)

// 기존 gameStateSaver와 같은 문자열·숫자·ArrayList 형식으로 저장한다.
internal object EventStateCodec {
    fun packRun(run: CampusEventRun?): ArrayList<String> =
        if (run == null) arrayListOf() else arrayListOf(
            run.eventId, run.semester.toString(), run.week.toString(),
            run.day.toString(), run.subjectId.orEmpty(), run.phase.name,
            run.selectedIndex.toString(), run.contextName
        )

    fun unpackRun(value: Any?, changes: Any?): CampusEventRun? {
        val parts = (value as? List<*>).orEmpty()
        if (parts.isEmpty()) return null
        require(parts.size in 7..8)
        return CampusEventRun(
            eventId = parts[0] as String,
            semester = (parts[1] as String).toInt(),
            week = (parts[2] as String).toInt(),
            day = (parts[3] as String).toInt(),
            subjectId = (parts[4] as String).takeIf { it.isNotEmpty() },
            phase = EventPhase.valueOf(parts[5] as String),
            selectedIndex = (parts[6] as String).toInt(),
            changes = (changes as? List<*>).orEmpty().map { it as String },
            contextName = parts.getOrNull(7) as? String ?: ""
        )
    }

    fun packHistory(records: List<CampusEventRecord>): ArrayList<String> =
        ArrayList(records.map {
            "${it.semester}|${it.week}|${it.day}|${it.eventId}|${it.selectedIndex}"
        })

    fun unpackHistory(value: Any?): List<CampusEventRecord> =
        (value as? List<*>).orEmpty().map {
            val p = (it as String).split('|')
            require(p.size == 5)
            CampusEventRecord(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3], p[4].toInt())
        }

    fun packBuffs(buffs: List<EventBuff>): ArrayList<String> =
        ArrayList(buffs.map {
            "${it.activity.name}|${it.multiplier}|${it.startDay}|${it.endDay}|${it.label}"
        })

    fun unpackBuffs(value: Any?): List<EventBuff> =
        (value as? List<*>).orEmpty().map {
            val p = (it as String).split('|', limit = 5)
            require(p.size == 5)
            EventBuff(ActivityType.valueOf(p[0]), p[1].toDouble(),
                p[2].toInt(), p[3].toInt(), p[4])
        }
}
