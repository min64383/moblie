package com.example.mobile_project

import androidx.compose.runtime.saveable.mapSaver

data class GameState(
    val semester: Int = 1,
    val week: Int = 1,
    // 완료한 날짜 수: 0 = 월요일 진행 전, 7 = 일요일 정산 완료
    val day: Int = 0,
    val hp: Int = 80,
    val maxHp: Int = 100,
    val happiness: Int = 60,
    val money: Int = 350_000,
    val network: Int = 0,
    val charm: Int = 10,
    val spec: Int = 0,
    val study: Map<String, Int> = emptyMap(),
    val log: List<String> = emptyList(),
    val midtermScores: Map<String, Int> = emptyMap(),
    val finalScores: Map<String, Int> = emptyMap(),
    val exam: ExamRun? = null,
    val semesterResult: SemesterResult? = null,
    val transcript: List<SemesterResult> = emptyList(),
    val vacationHistory: List<VacationRecord> = emptyList(),
    val needsRegistration: Boolean = false,
    val registrationScholarship: Int = 0,
    val pendingEvent: CampusEventRun? = null,
    val eventHistory: List<CampusEventRecord> = emptyList(),
    val eventBuffs: List<EventBuff> = emptyList(),
    val eventCheckedDay: Int = -1,
    val teamProject: TeamProject? = null,
    val teamResults: List<TeamProjectResult> = emptyList(),
    val teamBonuses: Map<String, Int> = emptyMap(),
    val semesterGpaAdjustment: Double = 0.0,
    val pendingConfession: LoveConfession? = null,
    val love: LoveRelationship? = null,
    val loveHistory: List<LoveRecord> = emptyList(),
    val loveCheckedSemesters: List<Int> = emptyList(),
    val pendingMiniGame: MiniGameRun? = null,
    val miniBonus: MiniGameBonus? = null,
    val miniCheckedDay: Int = -1,
    val skipMiniGames: Boolean = false,
    val life: LifeState = LifeState()
) {
    val completedSemesters: List<SemesterResult>
        get() {
            val records = transcript.associateBy { it.semester }.toMutableMap()
            semesterResult?.let { records[it.semester] = it }
            return records.values.sortedBy { it.semester }
        }

    val cumulativeGpa: Double
        get() {
            val records = completedSemesters
            val count = records.sumOf { it.grades.size }
            return if (count == 0) 0.0 else records.sumOf { it.gpa * it.grades.size } / count
        }
}

private fun packTranscript(records: List<SemesterResult>): ArrayList<String> =
    ArrayList(records.flatMap { record -> record.grades.map { grade ->
        "${record.semester}|${grade.subjectId}|${grade.score}|${grade.letter}|${grade.point}|${record.gpaAdjustment}"
    } })

private fun unpackTranscript(value: Any?): List<SemesterResult> =
    (value as? List<*>).orEmpty().map { entry ->
        (entry as String).split('|')
    }.groupBy { it[0].toInt() }
        .map { (semester, rows) -> SemesterResult(
            semester,
            rows.map { SubjectGrade(it[1], it[2].toInt(), it[3], it[4].toDouble()) },
            rows.first().getOrNull(5)?.toDouble() ?: 0.0
        ) }
        .sortedBy { it.semester }

private fun packIntMap(values: Map<String, Int>): ArrayList<String> =
    ArrayList(values.map { (id, value) -> "$id:$value" })

private fun unpackIntMap(value: Any?): Map<String, Int> =
    (value as? List<*>).orEmpty().associate { entry ->
        val parts = (entry as String).split(':', limit = 2)
        parts[0] to parts[1].toInt()
    }

// 기존 임시 상태와 자동 저장이 함께 사용하는 저장 형식. 새 필드는 기본값으로 복원한다.
val gameStateSaver = mapSaver<GameState>(
    save = { state ->
        val exam = state.exam
        val result = state.semesterResult
        mapOf(
            "semester" to state.semester,
            "week" to state.week,
            "day" to state.day,
            "hp" to state.hp,
            "maxHp" to state.maxHp,
            "happiness" to state.happiness,
            "money" to state.money,
            "network" to state.network,
            "charm" to state.charm,
            "spec" to state.spec,
            "transcript" to packTranscript(state.transcript),
            "vacations" to ArrayList(state.vacationHistory.map { "${it.semester}:${it.activityId}" }),
            "needsRegistration" to state.needsRegistration,
            "registrationScholarship" to state.registrationScholarship,
            "campusEvent" to EventStateCodec.packRun(state.pendingEvent),
            "eventChanges" to ArrayList(state.pendingEvent?.changes.orEmpty()),
            "eventHistory" to EventStateCodec.packHistory(state.eventHistory),
            "eventBuffs" to EventStateCodec.packBuffs(state.eventBuffs),
            "eventCheckedDay" to state.eventCheckedDay,
            "teamProject" to TeamStateCodec.packProject(state.teamProject),
            "teamIncidents" to TeamStateCodec.packIncidents(state.teamProject),
            "teamResults" to TeamStateCodec.packResults(state.teamResults),
            "teamBonuses" to packIntMap(state.teamBonuses),
            "semesterGpaAdjustment" to state.semesterGpaAdjustment,
            "loveConfession" to LoveStateCodec.packConfession(state.pendingConfession),
            "loveRelationship" to LoveStateCodec.packRelationship(state.love),
            "loveHistory" to LoveStateCodec.packHistory(state.loveHistory),
            "loveCheckedSemesters" to ArrayList(state.loveCheckedSemesters),
            "miniRun" to MiniStateCodec.packRun(state.pendingMiniGame),
            "miniBonus" to MiniStateCodec.packBonus(state.miniBonus),
            "miniCheckedDay" to state.miniCheckedDay,
            "skipMiniGames" to state.skipMiniGames,
            "life" to LifeStateCodec.pack(state.life),
            "study" to packIntMap(state.study),
            "log" to ArrayList(state.log),
            "midterm" to packIntMap(state.midtermScores),
            "final" to packIntMap(state.finalScores),
            "examKind" to (exam?.kind?.name ?: ""),
            "examPhase" to (exam?.phase?.name ?: ""),
            "examQuestions" to ArrayList(exam?.questionIds.orEmpty()),
            "examOptions" to ArrayList(exam?.optionOrders.orEmpty().map { it.joinToString(",") }),
            "examOptionCount" to (exam?.optionCount ?: 0),
            "examSeconds" to (exam?.secondsPerQuestion ?: 0),
            "examJokbo" to (exam?.jokbo ?: false),
            "examIndex" to (exam?.index ?: 0),
            "examCorrect" to (exam?.correct ?: 0),
            "examAnswer" to (exam?.selectedAnswer ?: -1),
            "examDeadline" to (exam?.deadlineMs ?: 0L),
            "resultSemester" to (result?.semester ?: 0),
            "resultGpaAdjustment" to (result?.gpaAdjustment ?: 0.0),
            "resultGrades" to ArrayList(result?.grades.orEmpty().map {
                "${it.subjectId}:${it.score}:${it.letter}:${it.point}"
            })
        )
    },
    restore = { saved ->
        val examKind = saved["examKind"] as? String
        val exam = if (examKind.isNullOrEmpty()) null else ExamRun(
            kind = ExamKind.valueOf(examKind),
            phase = ExamPhase.valueOf(saved["examPhase"] as String),
            questionIds = (saved["examQuestions"] as List<*>).map { it as Int },
            optionOrders = (saved["examOptions"] as List<*>).map { entry ->
                (entry as String).split(',').map { it.toInt() }
            },
            optionCount = saved["examOptionCount"] as Int,
            secondsPerQuestion = saved["examSeconds"] as Int,
            jokbo = saved["examJokbo"] as Boolean,
            index = saved["examIndex"] as Int,
            correct = saved["examCorrect"] as Int,
            selectedAnswer = saved["examAnswer"] as Int,
            deadlineMs = saved["examDeadline"] as Long
        )
        val resultSemester = saved["resultSemester"] as? Int ?: 0
        val result = if (resultSemester == 0) null else SemesterResult(
            semester = resultSemester,
            gpaAdjustment = saved["resultGpaAdjustment"] as? Double ?: 0.0,
            grades = (saved["resultGrades"] as List<*>).map { entry ->
                val parts = (entry as String).split(':')
                SubjectGrade(parts[0], parts[1].toInt(), parts[2], parts[3].toDouble())
            }
        )
        val study = unpackIntMap(saved["study"])
        GameState(
            semester = (saved["semester"] as? Int ?: 1).coerceIn(1, 8),
            week = (saved["week"] as Int).coerceIn(1, 16),
            day = (saved["day"] as Int).coerceIn(0, 7),
            hp = saved["hp"] as Int,
            maxHp = saved["maxHp"] as Int,
            happiness = saved["happiness"] as Int,
            money = saved["money"] as Int,
            network = saved["network"] as Int,
            charm = saved["charm"] as Int,
            spec = saved["spec"] as? Int ?: 0,
            transcript = unpackTranscript(saved["transcript"]),
            vacationHistory = (saved["vacations"] as? List<*>).orEmpty().map { entry ->
                val parts = (entry as String).split(':', limit = 2)
                VacationRecord(parts[0].toInt(), parts[1])
            },
            needsRegistration = saved["needsRegistration"] as? Boolean ?: study.isEmpty(),
            registrationScholarship = saved["registrationScholarship"] as? Int ?: 0,
            pendingEvent = EventStateCodec.unpackRun(saved["campusEvent"], saved["eventChanges"]),
            eventHistory = EventStateCodec.unpackHistory(saved["eventHistory"]),
            eventBuffs = EventStateCodec.unpackBuffs(saved["eventBuffs"]),
            eventCheckedDay = saved["eventCheckedDay"] as? Int ?: -1,
            teamProject = TeamStateCodec.unpackProject(saved["teamProject"], saved["teamIncidents"]),
            teamResults = TeamStateCodec.unpackResults(saved["teamResults"]),
            teamBonuses = unpackIntMap(saved["teamBonuses"]),
            semesterGpaAdjustment = saved["semesterGpaAdjustment"] as? Double ?: 0.0,
            pendingConfession = LoveStateCodec.unpackConfession(saved["loveConfession"]),
            love = LoveStateCodec.unpackRelationship(saved["loveRelationship"]),
            loveHistory = LoveStateCodec.unpackHistory(saved["loveHistory"]),
            loveCheckedSemesters = (saved["loveCheckedSemesters"] as? List<*>).orEmpty().map { it as Int },
            pendingMiniGame = MiniStateCodec.unpackRun(saved["miniRun"]),
            miniBonus = MiniStateCodec.unpackBonus(saved["miniBonus"]),
            miniCheckedDay = saved["miniCheckedDay"] as? Int ?: -1,
            skipMiniGames = saved["skipMiniGames"] as? Boolean ?: false,
            life = LifeStateCodec.unpack(saved["life"]),
            study = study,
            log = (saved["log"] as List<*>).map { it as String },
            midtermScores = unpackIntMap(saved["midterm"]),
            finalScores = unpackIntMap(saved["final"]),
            exam = exam,
            semesterResult = result
        )
    }
)
