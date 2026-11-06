package com.example.mobile_project

import kotlin.math.roundToInt
import kotlin.random.Random

object TeamEngine {
    val START_WEEKS = setOf(4, 11)
    const val DURATION = 3
    const val COMPLETION_NETWORK = 8

    fun goodMemberChance(network: Int): Double = 0.20 + network.coerceIn(0, 100) / 100.0 * 0.45

    fun meetingCount(memberIds: List<String>): Int =
        (2 + memberIds.sumOf { TeamCatalog.memberForId(it)?.slotDelta ?: 0 }).coerceIn(1, 4)

    fun adjustStudyGain(state: GameState, subjectId: String, baseGain: Int): Int {
        val project = state.teamProject ?: return baseGain
        return if (project.subjectId == subjectId) (baseGain * project.studyMultiplier).roundToInt() else baseGain
    }

    // 수업과 밤 칸은 조모임 배정 대상에서 제외한다. 주가 바뀌어도 같은 고정 칸을 쓴다.
    fun syncSchedule(state: GameState, schedule: List<TimeSlot>): List<TimeSlot> {
        if (schedule.size != 28) return schedule
        val project = state.teamProject
        return schedule.mapIndexed { index, slot ->
            when {
                project != null && index in project.meetingIndices &&
                    slot.activity !in setOf(ActivityType.CLASS, ActivityType.DATE) -> TimeSlot(ActivityType.TEAM, project.subjectId)
                slot.activity == ActivityType.TEAM -> TimeSlot(ActivityType.REST)
                else -> slot
            }
        }
    }

    private fun planIncidents(project: TeamProject, week: Int, random: Random): TeamProject {
        if (project.incidentWeek == week) return project
        val incidents = project.members.mapIndexedNotNull { index, member ->
            val chance = if (member.bonus > 0) 0.30 else 0.40
            if (member.incidents.isNotEmpty() && random.nextDouble() < chance) {
                TeamIncident(random.nextInt(7), index, random.nextInt(member.incidents.size))
            } else null
        }
        return project.copy(incidentWeek = week, incidents = incidents)
    }

    // 배정과 주간 사건 추첨은 월요일 진행 전에 한 번만 한다. 추첨 결과도 GameState에 저장한다.
    fun prepareWeek(
        state: GameState,
        schedule: List<TimeSlot>,
        random: Random = Random.Default
    ): TeamWeekPreparation {
        if (state.day != 0 || state.needsRegistration || state.semesterResult != null ||
            state.exam != null || state.pendingEvent != null || state.pendingConfession != null || state.pendingMiniGame != null ||
            state.life.pendingMorning != null || state.life.morningNotice != null ||
            state.study.size != 3 || schedule.size != 28) {
            return TeamWeekPreparation(state, schedule)
        }

        var game = state
        var project = state.teamProject
        if (project == null && state.week in START_WEEKS &&
            state.teamResults.none { it.semester == state.semester && it.startWeek == state.week }) {
            val subjectId = state.study.keys.toList().random(random)
            val good = TeamCatalog.members.filter { it.bonus > 0 }
            val bad = TeamCatalog.members.filter { it.bonus == 0 }
            val members = List(3) {
                (if (random.nextDouble() < goodMemberChance(state.network)) good else bad).random(random)
            }
            val indices = schedule.indices
                .filter { it % 4 != 3 && !schedule[it].locked }
                .shuffled(random).take(meetingCount(members.map { it.id })).sorted()
            if (indices.isEmpty()) return TeamWeekPreparation(state, schedule)

            project = TeamProject(
                state.semester, state.week, subjectId, members.map { it.id }, indices,
                members.sumOf { it.bonus }.coerceAtMost(10)
            )
            game = game.copy(
                teamProject = project,
                happiness = (game.happiness + members.sumOf { it.happiness }).coerceIn(0, 100),
                network = (game.network + members.sumOf { it.network }).coerceIn(0, 100)
            )
        }

        if (project != null && state.week in project.startWeek..project.endWeek) {
            val planned = planIncidents(project, state.week, random)
            if (planned != game.teamProject) game = game.copy(teamProject = planned)
        }
        return TeamWeekPreparation(game, syncSchedule(game, schedule))
    }

    fun acknowledge(state: GameState, key: String): GameState {
        val project = state.teamProject ?: return state
        if (project.key != key || project.announced) return state
        return state.copy(teamProject = project.copy(announced = true))
    }

    // 그날 시간표를 모두 처리한 뒤 조원 소식을 한 번 반영한다.
    fun applyDayIncidents(state: GameState): GameState {
        val project = state.teamProject ?: return state
        if (state.day !in 0..6 || project.incidentWeek != state.week) return state
        val todays = project.incidents.filter { it.day == state.day && !it.applied }
        if (todays.isEmpty()) return state
        val messages = todays.map { incident ->
            val member = project.members[incident.memberIndex]
            member to member.incidents[incident.optionIndex]
        }
        val hp = (state.hp + messages.sumOf { it.second.hp }).coerceIn(0, state.maxHp)
        val happiness = (state.happiness + messages.sumOf { it.second.happiness }).coerceIn(0, 100)
        val delta = listOf("체력" to hp - state.hp, "행복도" to happiness - state.happiness)
            .filter { it.second != 0 }.joinToString(" · ") { (label, value) ->
                "$label ${if (value > 0) "+" else ""}$value"
            }.ifEmpty { "수치 변화 없음" }
        return state.copy(
            hp = hp, happiness = happiness,
            teamProject = project.copy(incidents = project.incidents.map {
                if (it.day == state.day && !it.applied) it.copy(applied = true) else it
            }),
            log = state.log + messages.map { "조원 소식 · ${it.first.name}\n${it.second.text}" } +
                "조원 소식 합계 · $delta"
        )
    }

    // 일요일 정산이 끝나야 1주를 이수한다. 3주째 제출 보상과 기록도 한 번만 지급한다.
    fun settleWeek(state: GameState): GameState {
        val project = state.teamProject ?: return state
        if (state.day != 7 || state.week !in project.startWeek..project.endWeek ||
            state.week != project.lastSettledWeek + 1) return state
        if (state.week < project.endWeek) {
            return state.copy(teamProject = project.copy(lastSettledWeek = state.week),
                log = state.log + "조별과제 · ${project.endWeek - state.week}주 남음")
        }
        val result = TeamProjectResult(project.semester, project.startWeek, state.week,
            project.subjectId, project.memberIds, project.bonus)
        val network = (state.network + COMPLETION_NETWORK).coerceAtMost(100)
        return state.copy(
            teamProject = null,
            teamResults = state.teamResults.filterNot { it.key == result.key } + result,
            teamBonuses = state.teamBonuses + (project.subjectId to
                ((state.teamBonuses[project.subjectId] ?: 0) + project.bonus)),
            network = network,
            log = state.log + "조별과제 제출 · ${subjectForId(project.subjectId)?.name ?: project.subjectId}\n" +
                "학기 최종 점수 가산점 +${project.bonus}점 · 인맥 +${network - state.network}"
        )
    }

    fun validateState(state: GameState, schedule: List<TimeSlot>) {
        val project = state.teamProject ?: return
        require(project.semester == state.semester && project.subjectId in state.study)
        require(project.startWeek in START_WEEKS && state.week in project.startWeek..project.endWeek)
        require(!state.needsRegistration && state.semesterResult == null && state.exam == null)
        require(project.memberIds.size == 3 && project.memberIds.all { TeamCatalog.memberForId(it) != null })
        require(project.bonus in 0..10 && project.meetingIndices.size in 1..4)
        require(project.meetingIndices.distinct().size == project.meetingIndices.size)
        require(schedule.size == 28 && project.meetingIndices.all {
            it in 0..27 && it % 4 != 3 && schedule[it].activity !in setOf(ActivityType.CLASS, ActivityType.DATE)
        })
        require(project.lastSettledWeek in (project.startWeek - 1)..state.week)
        require(project.incidentWeek in project.startWeek..state.week)
        require(state.teamResults.none { it.key == project.key })
        require(project.incidents.all { incident ->
            incident.day in 0..6 && incident.memberIndex in 0..2 &&
                incident.optionIndex in project.members[incident.memberIndex].incidents.indices
        })
    }
}
