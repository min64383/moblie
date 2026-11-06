package com.example.mobile_project

data class TeamIncidentOption(val text: String, val hp: Int = 0, val happiness: Int = 0)

data class TeamMemberType(
    val id: String,
    val name: String,
    val description: String,
    val slotDelta: Int,
    val happiness: Int,
    val studyMultiplier: Double,
    val bonus: Int,
    val network: Int = 0,
    val incidents: List<TeamIncidentOption>
)

data class TeamIncident(
    val day: Int,
    val memberIndex: Int,
    val optionIndex: Int,
    val applied: Boolean = false
)

data class TeamProject(
    val semester: Int,
    val startWeek: Int,
    val subjectId: String,
    val memberIds: List<String>,
    val meetingIndices: List<Int>,
    val bonus: Int,
    val announced: Boolean = false,
    val lastSettledWeek: Int = startWeek - 1,
    val incidentWeek: Int = 0,
    val incidents: List<TeamIncident> = emptyList()
) {
    val key: String get() = "$semester:$startWeek"
    val endWeek: Int get() = startWeek + TeamEngine.DURATION - 1
    val weeksLeft: Int get() = (endWeek - lastSettledWeek).coerceIn(0, TeamEngine.DURATION)
    val members: List<TeamMemberType> get() = memberIds.mapNotNull(TeamCatalog::memberForId)
    val studyMultiplier: Double get() = members.fold(1.0) { value, member -> value * member.studyMultiplier }
}

data class TeamProjectResult(
    val semester: Int,
    val startWeek: Int,
    val endWeek: Int,
    val subjectId: String,
    val memberIds: List<String>,
    val bonus: Int
) {
    val key: String get() = "$semester:$startWeek"
}

data class TeamWeekPreparation(val game: GameState, val schedule: List<TimeSlot>)

internal object TeamStateCodec {
    fun packProject(project: TeamProject?): ArrayList<String> =
        if (project == null) arrayListOf() else arrayListOf(
            project.semester.toString(), project.startWeek.toString(), project.subjectId,
            project.memberIds.joinToString(","), project.meetingIndices.joinToString(","),
            project.bonus.toString(), project.announced.toString(),
            project.lastSettledWeek.toString(), project.incidentWeek.toString()
        )

    fun packIncidents(project: TeamProject?): ArrayList<String> =
        ArrayList(project?.incidents.orEmpty().map {
            "${it.day}|${it.memberIndex}|${it.optionIndex}|${it.applied}"
        })

    fun unpackProject(value: Any?, incidentValue: Any?): TeamProject? {
        val parts = (value as? List<*>).orEmpty().map { it as String }
        if (parts.isEmpty()) return null
        require(parts.size == 9)
        val incidents = (incidentValue as? List<*>).orEmpty().map {
            val p = (it as String).split('|')
            require(p.size == 4)
            TeamIncident(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3].toBooleanStrict())
        }
        return TeamProject(
            semester = parts[0].toInt(), startWeek = parts[1].toInt(), subjectId = parts[2],
            memberIds = parts[3].split(','), meetingIndices = parts[4].split(',').map { it.toInt() },
            bonus = parts[5].toInt(), announced = parts[6].toBooleanStrict(),
            lastSettledWeek = parts[7].toInt(), incidentWeek = parts[8].toInt(), incidents = incidents
        )
    }

    fun packResults(results: List<TeamProjectResult>): ArrayList<String> =
        ArrayList(results.map {
            "${it.semester}|${it.startWeek}|${it.endWeek}|${it.subjectId}|${it.memberIds.joinToString(",")}|${it.bonus}"
        })

    fun unpackResults(value: Any?): List<TeamProjectResult> =
        (value as? List<*>).orEmpty().map {
            val p = (it as String).split('|')
            require(p.size == 6)
            TeamProjectResult(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3], p[4].split(','), p[5].toInt())
        }
}
