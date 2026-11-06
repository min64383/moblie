package com.example.mobile_project

import androidx.compose.runtime.saveable.listSaver

enum class ActivityType(val label: String, val color: Long) {
    EMPTY("비우기", 0xFFECE5D8),
    STUDY("공부", 0xFF3B82C4),
    WORK("알바", 0xFFD98C2B),
    FUN("여가", 0xFFD4537E),
    REST("휴식", 0xFF5A9E4B),
    CLUB("동아리", 0xFF7F77DD),
    RUN("한강러닝", 0xFF2E9183),
    GYM("헬스장", 0xFF2E9183),
    PT("PT", 0xFF1F7A6E),
    SLEEP("수면", 0xFF6B6BA8),
    CLASS("수업", 0xFF4A5A72),
    CAREER("취업준비", 0xFF8A6BB8),
    TUTOR("과외", 0xFFC07A2B),
    TEAM("조모임", 0xFFC0504D),
    DATE("데이트", 0xFFD4537E)
}

data class TimeSlot(val activity: ActivityType = ActivityType.EMPTY, val subjectId: String? = null) {
    val locked: Boolean get() = activity == ActivityType.CLASS || activity == ActivityType.TEAM ||
        activity == ActivityType.DATE
}

fun createWeekSchedule(subjectIds: List<String>): List<TimeSlot> {
    require(subjectIds.size == 3 && subjectIds.distinct().size == 3)
    val slots = MutableList(28) { index ->
        if (index % 4 == 3) TimeSlot(ActivityType.SLEEP) else TimeSlot()
    }
    val candidates = (0 until 5).flatMap { day ->
        (0 until 3).map { block -> day * 4 + block }
    }.shuffled()
    subjectIds.forEachIndexed { subjectIndex, id ->
        repeat(2) { lesson ->
            slots[candidates[subjectIndex * 2 + lesson]] = TimeSlot(ActivityType.CLASS, id)
        }
    }
    return slots.toList()
}

val weekScheduleSaver = listSaver<List<TimeSlot>, String>(
    save = { slots -> slots.map { "${it.activity.name}|${it.subjectId.orEmpty()}" } },
    restore = { saved -> saved.map { value ->
        val parts = value.split('|', limit = 2)
        TimeSlot(ActivityType.valueOf(parts[0]), parts.getOrNull(1)?.takeIf { it.isNotEmpty() })
    } }
)
