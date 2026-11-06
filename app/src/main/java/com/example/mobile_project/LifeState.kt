package com.example.mobile_project

enum class MorningPhase { READY, PLAY, RESULT }

data class MorningRun(
    val semester: Int,
    val week: Int,
    val day: Int,
    val activity: ActivityType,
    val subjectId: String? = null,
    val autoSuccess: Boolean = false,
    val phase: MorningPhase = MorningPhase.READY,
    val taps: Int = 0,
    val startedAtMs: Long = 0L,
    val deadlineMs: Long = 0L,
    val success: Boolean? = null
) {
    val key: String get() = "$semester:$week:$day:morning"
}

data class AttendanceRecord(
    val semester: Int, val week: Int, val day: Int,
    val activity: ActivityType, val subjectId: String?, val success: Boolean
)

data class LotteryPrize(val title: String, val amount: Int, val ticketsPerThousand: Int)
data class LotteryTicket(val semester: Int, val week: Int, val prizeIndex: Int)

data class LifeState(
    val pendingMorning: MorningRun? = null,
    val morningCheckedDay: Int = -1,
    val morningNotice: AttendanceRecord? = null,
    // 중간 또는 기말고사 한 번에 적용할 과목별 지각 횟수. 시험이 끝나면 비운다.
    val lateCounts: Map<String, Int> = emptyMap(),
    val attendanceHistory: List<AttendanceRecord> = emptyList(),
    // (학기 - 1) * 16 + 주차 - 1. 다음 학기와 같은 주차가 겹치지 않는다.
    val sickWeek: Int = -1,
    val healthCheckedWeek: Int = -1,
    val lotteryHistory: List<LotteryTicket> = emptyList()
)

internal object LifeStateCodec {
    private fun packAttendance(value: AttendanceRecord): String =
        "${value.semester}|${value.week}|${value.day}|${value.activity.name}|${value.subjectId.orEmpty()}|${value.success}"

    private fun unpackAttendance(value: String): AttendanceRecord {
        val p = value.split('|')
        require(p.size == 6)
        return AttendanceRecord(p[0].toInt(), p[1].toInt(), p[2].toInt(),
            ActivityType.valueOf(p[3]), p[4].takeIf(String::isNotEmpty), p[5].toBooleanStrict())
    }

    fun pack(life: LifeState): ArrayList<Any> {
        val run = life.pendingMorning
        val morning = if (run == null) arrayListOf() else arrayListOf(
            run.semester.toString(), run.week.toString(), run.day.toString(), run.activity.name,
            run.subjectId.orEmpty(), run.autoSuccess.toString(), run.phase.name, run.taps.toString(),
            run.startedAtMs.toString(), run.deadlineMs.toString(), run.success?.toString().orEmpty())
        return arrayListOf(
            arrayListOf(life.morningCheckedDay.toString(), life.sickWeek.toString(), life.healthCheckedWeek.toString()),
            morning, arrayListOf(life.morningNotice?.let(::packAttendance).orEmpty()),
            ArrayList(life.lateCounts.map { "${it.key}|${it.value}" }),
            ArrayList(life.attendanceHistory.map(::packAttendance)),
            ArrayList(life.lotteryHistory.map { "${it.semester}|${it.week}|${it.prizeIndex}" })
        )
    }

    fun unpack(value: Any?): LifeState {
        val p = (value as? List<*>).orEmpty()
        if (p.isEmpty()) return LifeState()
        require(p.size == 6)
        fun strings(index: Int) = (p[index] as List<*>).map { it as String }
        val meta = strings(0)
        val m = strings(1)
        require(meta.size == 3 && (m.isEmpty() || m.size == 11))
        val run = if (m.isEmpty()) null else MorningRun(m[0].toInt(), m[1].toInt(), m[2].toInt(),
            ActivityType.valueOf(m[3]), m[4].takeIf(String::isNotEmpty), m[5].toBooleanStrict(),
            MorningPhase.valueOf(m[6]), m[7].toInt(), m[8].toLong(), m[9].toLong(),
            m[10].takeIf(String::isNotEmpty)?.toBooleanStrict())
        return LifeState(run, meta[0].toInt(), strings(2).single().takeIf(String::isNotEmpty)?.let(::unpackAttendance),
            strings(3).associate { val v = it.split('|'); v[0] to v[1].toInt() },
            strings(4).map(::unpackAttendance), meta[1].toInt(), meta[2].toInt(),
            strings(5).map { val v = it.split('|'); LotteryTicket(v[0].toInt(), v[1].toInt(), v[2].toInt()) })
    }
}
