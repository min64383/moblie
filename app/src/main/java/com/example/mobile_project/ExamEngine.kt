package com.example.mobile_project

import kotlin.math.roundToInt
import kotlin.random.Random

enum class ExamKind(val title: String) { MIDTERM("중간고사"), FINAL("기말고사") }
enum class ExamPhase { READY, QUESTION, FEEDBACK, RESULT }

data class ExamQuestion(val prompt: String, val options: List<String>)

data class ExamRun(
    val kind: ExamKind,
    val phase: ExamPhase = ExamPhase.READY,
    val questionIds: List<Int>,
    val optionOrders: List<List<Int>>,
    val optionCount: Int,
    val secondsPerQuestion: Int,
    val jokbo: Boolean,
    val index: Int = 0,
    val correct: Int = 0,
    // 문제은행에서 정답은 항상 0번. -1은 시간 초과 또는 아직 응답하지 않음.
    val selectedAnswer: Int = -1,
    val deadlineMs: Long = 0L
) {
    val rate: Double get() = correct.toDouble() / questionIds.size
    val modifier: Double get() = 0.85 + rate * 0.30
}

data class SubjectGrade(
    val subjectId: String,
    val score: Int,
    val letter: String,
    val point: Double
)

data class SemesterResult(
    val semester: Int,
    val grades: List<SubjectGrade>,
    val gpaAdjustment: Double = 0.0
) {
    val gpa: Double
        get() = if (grades.isEmpty()) 0.0 else
            (grades.map { it.point }.average() + gpaAdjustment).coerceIn(0.0, 4.5)
    val scholarship: Int
        get() = when {
            gpa >= 4.0 -> 600_000
            gpa >= 3.5 -> 350_000
            gpa >= 3.0 -> 150_000
            else -> 0
        }
}

fun semesterLabel(semester: Int): String =
    "${(semester + 1) / 2}학년 ${(semester - 1) % 2 + 1}학기"

object ExamEngine {
    // HTML의 QBANK와 같은 22문항. options[0]은 정답이며 출제할 때 보기를 섞는다.
    val questionBank = listOf(
        ExamQuestion("스택(Stack)의 자료 처리 방식은?", listOf("후입선출(LIFO)", "선입선출(FIFO)", "무작위 접근", "우선순위순")),
        ExamQuestion("이진 탐색의 평균 시간복잡도는?", listOf("O(log n)", "O(n)", "O(n log n)", "O(1)")),
        ExamQuestion("프로세스와 스레드의 차이로 옳은 것은?", listOf("스레드는 메모리를 공유한다", "프로세스가 더 가볍다", "스레드는 독립 주소공간을 갖는다", "둘은 같은 개념이다")),
        ExamQuestion("교착상태(Deadlock) 필요조건이 아닌 것은?", listOf("선점 가능", "상호 배제", "점유와 대기", "환형 대기")),
        ExamQuestion("관계형 DB에서 기본키(PK)의 성질은?", listOf("중복과 NULL을 허용하지 않는다", "NULL을 허용한다", "테이블당 여러 개 필수", "외부 테이블만 참조한다")),
        ExamQuestion("SQL에서 중복을 제거하는 키워드는?", listOf("DISTINCT", "UNIQUE", "REMOVE", "FILTER")),
        ExamQuestion("큐(Queue)를 쓰는 대표 알고리즘은?", listOf("BFS", "DFS", "퀵 정렬", "이분 탐색")),
        ExamQuestion("가상 메모리의 주된 목적은?", listOf("물리 메모리보다 큰 공간 제공", "CPU 속도 향상", "디스크 용량 증가", "네트워크 지연 감소")),
        ExamQuestion("해시 테이블의 평균 탐색 시간은?", listOf("O(1)", "O(n)", "O(log n)", "O(n²)")),
        ExamQuestion("정규화의 목적으로 옳은 것은?", listOf("데이터 중복 최소화", "쿼리 속도 무조건 향상", "테이블 수 감소", "백업 자동화")),
        ExamQuestion("페이지 교체 알고리즘이 아닌 것은?", listOf("Round Robin", "LRU", "FIFO", "Optimal")),
        ExamQuestion("트리에서 노드가 n개일 때 간선 수는?", listOf("n−1", "n", "n+1", "2n")),
        ExamQuestion("트랜잭션의 ACID 중 A는?", listOf("원자성(Atomicity)", "정확성(Accuracy)", "접근성(Access)", "비동기(Async)")),
        ExamQuestion("힙 정렬의 시간복잡도는?", listOf("O(n log n)", "O(n)", "O(n²)", "O(log n)")),
        ExamQuestion("문맥 교환이 일어나는 때는?", listOf("CPU가 다른 프로세스로 전환될 때", "파일을 저장할 때", "컴파일할 때", "부팅할 때")),
        ExamQuestion("연결 리스트의 장점은?", listOf("삽입·삭제가 빠르다", "임의 접근이 빠르다", "메모리를 적게 쓴다", "정렬이 자동이다")),
        ExamQuestion("TCP와 UDP의 차이로 옳은 것은?", listOf("TCP는 연결 지향이다", "UDP가 더 신뢰성이 높다", "TCP는 순서를 보장하지 않는다", "둘 다 핸드셰이크를 한다")),
        ExamQuestion("경사하강법에서 학습률이 너무 크면?", listOf("발산할 수 있다", "항상 더 빨리 수렴한다", "기울기가 0이 된다", "과적합이 사라진다")),
        ExamQuestion("공개키 암호에서 서명에 쓰는 키는?", listOf("개인키", "공개키", "세션키", "대칭키")),
        ExamQuestion("컴파일러의 구문 분석 단계 산출물은?", listOf("파스 트리", "토큰 열", "기계어", "심볼 테이블만")),
        ExamQuestion("오버피팅을 줄이는 방법이 아닌 것은?", listOf("학습 데이터를 줄인다", "정규화를 적용한다", "드롭아웃을 쓴다", "데이터를 늘린다")),
        ExamQuestion("RAID 1의 특징은?", listOf("미러링으로 이중화", "패리티로 복구", "스트라이핑만 수행", "압축 저장"))
    )

    fun pendingExam(state: GameState): ExamKind? {
        if (state.day != 7 || state.study.isEmpty() || state.needsRegistration || state.semesterResult != null) return null
        val ids = state.study.keys
        // 이전 단계에서 8주차를 지나친 임시 상태도 시험을 건너뛰지 않게 한다.
        return when {
            state.week >= 8 && !state.midtermScores.keys.containsAll(ids) -> ExamKind.MIDTERM
            state.week >= 16 && !state.finalScores.keys.containsAll(ids) -> ExamKind.FINAL
            else -> null
        }
    }

    fun startPendingExam(state: GameState, random: Random = Random.Default): GameState {
        if (state.exam != null) return state
        val kind = pendingExam(state) ?: return state
        val jokbo = random.nextDouble() * 100 < state.network * 0.8
        val study = if (jokbo) state.study.mapValues { it.value + 15 } else state.study
        val preparedness = (study.values.average() * examFactor(state.semester)).coerceAtMost(100.0)
        val (options, seconds, count) = when {
            preparedness >= 75 || (jokbo && preparedness >= 55) -> Triple(2, 15, 5)
            preparedness >= 45 -> Triple(3, 10, 7)
            else -> Triple(4, 7, 10)
        }
        val ids = questionBank.indices.shuffled(random).take(count)
        val orders = ids.map { id ->
            val wrongs = (1 until questionBank[id].options.size).shuffled(random).take(options - 1)
            (listOf(0) + wrongs).shuffled(random)
        }
        return state.copy(
            study = study,
            exam = ExamRun(
                kind = kind,
                questionIds = ids,
                optionOrders = orders,
                optionCount = options,
                secondsPerQuestion = seconds,
                jokbo = jokbo
            )
        )
    }

    fun beginExam(state: GameState, nowMs: Long): GameState {
        val run = state.exam ?: return state
        if (run.phase != ExamPhase.READY) return state
        return state.copy(exam = run.copy(
            phase = ExamPhase.QUESTION,
            deadlineMs = nowMs + run.secondsPerQuestion * 1_000L
        ))
    }

    fun answerQuestion(
        state: GameState,
        answer: Int,
        expectedIndex: Int,
        nowMs: Long
    ): GameState {
        val run = state.exam ?: return state
        if (run.phase != ExamPhase.QUESTION || run.index != expectedIndex) return state
        val actual = if (nowMs >= run.deadlineMs) -1 else answer
        if (actual == -1 && nowMs < run.deadlineMs) return state
        if (actual != -1 && actual !in run.optionOrders[run.index]) return state
        return state.copy(exam = run.copy(
            phase = ExamPhase.FEEDBACK,
            selectedAnswer = actual,
            correct = run.correct + if (actual == 0) 1 else 0
        ))
    }

    fun nextQuestion(state: GameState, expectedIndex: Int, nowMs: Long): GameState {
        val run = state.exam ?: return state
        if (run.phase != ExamPhase.FEEDBACK || run.index != expectedIndex) return state
        if (run.index == run.questionIds.lastIndex) return finishExam(state)
        return state.copy(exam = run.copy(
            phase = ExamPhase.QUESTION,
            index = run.index + 1,
            selectedAnswer = -1,
            deadlineMs = nowMs + run.secondsPerQuestion * 1_000L
        ))
    }

    private fun finishExam(state: GameState): GameState {
        val run = state.exam ?: return state
        val scores = state.study.mapValues { (id, study) ->
            scoreFor(study, run.correct, run.questionIds.size, state.semester, state.life.lateCounts[id] ?: 0)
        }
        val finished = run.copy(phase = ExamPhase.RESULT, deadlineMs = 0L)
        return if (run.kind == ExamKind.MIDTERM) {
            state.copy(
                midtermScores = scores,
                study = state.study.mapValues { 0 },
                exam = finished,
                life = state.life.copy(lateCounts = emptyMap())
            )
        } else {
            state.copy(finalScores = scores, exam = finished, life = state.life.copy(lateCounts = emptyMap()))
        }
    }

    fun examFactor(semester: Int): Double = 0.26 - ((semester - 1) / 2) * 0.02

    fun scoreFor(study: Int, correct: Int, total: Int, semester: Int = 1, lateCount: Int = 0): Int {
        require(total > 0 && correct in 0..total && lateCount >= 0)
        val base = (study * examFactor(semester)).coerceIn(0.0, 100.0)
        return (base * (0.85 + correct.toDouble() / total * 0.30) - lateCount * LifeEngine.LATE_PENALTY)
            .roundToInt().coerceIn(0, 100)
    }

    fun gradeFor(subjectId: String, score: Int): SubjectGrade {
        val (letter, point) = when {
            score >= 95 -> "A+" to 4.5
            score >= 90 -> "A0" to 4.0
            score >= 85 -> "B+" to 3.5
            score >= 80 -> "B0" to 3.0
            score >= 75 -> "C+" to 2.5
            score >= 70 -> "C0" to 2.0
            score >= 60 -> "D0" to 1.0
            else -> "F" to 0.0
        }
        return SubjectGrade(subjectId, score, letter, point)
    }

    fun finishSemester(state: GameState): GameState {
        if (state.semesterResult != null || state.week < 16 || state.day != 7) return state
        val ids = state.study.keys
        if (ids.isEmpty() || !state.midtermScores.keys.containsAll(ids) ||
            !state.finalScores.keys.containsAll(ids)) return state
        val grades = ids.map { id ->
            val combined = ((state.midtermScores.getValue(id) + state.finalScores.getValue(id)) / 2.0 +
                (state.teamBonuses[id] ?: 0))
                .roundToInt().coerceIn(0, 100)
            gradeFor(id, combined)
        }
        val result = SemesterResult(state.semester, grades, state.semesterGpaAdjustment)
        return state.copy(
            semesterResult = result,
            transcript = state.completedSemesters.filterNot { it.semester == state.semester } + result,
            exam = null
        )
    }
}
