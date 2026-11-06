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
    val registrationScholarship: Int = 0
) {
    val completedSemesters: List<SemesterResult>
        get() {
            val records = transcript.associateBy { it.semester }.toMutableMap()
            semesterResult?.let { records[it.semester] = it }
            return records.values.sortedBy { it.semester }
        }

    val cumulativeGpa: Double
        get() {
            val grades = completedSemesters.flatMap { it.grades }
            return if (grades.isEmpty()) 0.0 else grades.map { it.point }.average()
        }
}

private fun packTranscript(records: List<SemesterResult>): ArrayList<String> =
    ArrayList(records.flatMap { record -> record.grades.map { grade ->
        "${record.semester}|${grade.subjectId}|${grade.score}|${grade.letter}|${grade.point}"
    } })

private fun unpackTranscript(value: Any?): List<SemesterResult> =
    (value as? List<*>).orEmpty().map { entry ->
        val parts = (entry as String).split('|')
        parts[0].toInt() to SubjectGrade(parts[1], parts[2].toInt(), parts[3], parts[4].toDouble())
    }.groupBy({ it.first }, { it.second })
        .map { (semester, grades) -> SemesterResult(semester, grades) }
        .sortedBy { it.semester }

private fun packIntMap(values: Map<String, Int>): ArrayList<String> =
    ArrayList(values.map { (id, value) -> "$id:$value" })

private fun unpackIntMap(value: Any?): Map<String, Int> =
    (value as? List<*>).orEmpty().associate { entry ->
        val parts = (entry as String).split(':', limit = 2)
        parts[0] to parts[1].toInt()
    }

// 화면 회전과 시스템에 의한 화면 재생성에 필요한 임시 상태 저장.
// 앱을 완전히 종료한 뒤의 게임 저장은 이후 로컬 저장 기능에서 처리한다.
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
            study = study,
            log = (saved["log"] as List<*>).map { it as String },
            midtermScores = unpackIntMap(saved["midterm"]),
            finalScores = unpackIntMap(saved["final"]),
            exam = exam,
            semesterResult = result
        )
    }
)
