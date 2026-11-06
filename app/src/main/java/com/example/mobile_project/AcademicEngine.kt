package com.example.mobile_project

data class VacationRecord(val semester: Int, val activityId: String)

data class VacationOption(
    val id: String,
    val title: String,
    val cost: Int,
    val description: String,
    val hp: Int = 0,
    val happiness: Int = 0,
    val spec: Int = 0,
    val network: Int = 0,
    val fullHeal: Boolean = false,
    val minYear: Int = 1
)

data class GraduationResult(val title: String, val description: String)

fun vacationLabel(semester: Int): String =
    "${(semester + 1) / 2}학년 ${if (semester % 2 == 1) "여름" else "겨울"}방학"

object AcademicEngine {
    const val VACATION_EXPENSE = 200_000

    private val vacationCatalog = listOf(
        VacationOption("work", "알바 집중", -450_000,
            "돈 +450,000 · 체력 -25 · 행복 -18", hp = -25, happiness = -18),
        VacationOption("rest", "푹 쉬기", 80_000,
            "돈 -80,000 · 체력 완전 회복 · 행복 +35", happiness = 35, fullHeal = true),
        VacationOption("cert", "자격증 공부", 250_000,
            "돈 -250,000 · 스펙 +10 · 체력 -12 · 행복 -10", hp = -12, happiness = -10, spec = 10),
        VacationOption("trip", "여행", 350_000,
            "돈 -350,000 · 행복 +45 · 인맥 +10 · 체력 +10", hp = 10, happiness = 45, network = 10),
        VacationOption("lang", "어학연수", 900_000,
            "돈 -900,000 · 스펙 +18 · 인맥 +8 · 행복 +10", happiness = 10, spec = 18, network = 8, minYear = 2),
        VacationOption("intern", "인턴", -400_000,
            "돈 +400,000 · 스펙 +20 · 체력 -20 · 행복 -8", hp = -20, happiness = -8, spec = 20, minYear = 3)
    )

    fun vacationOptions(semester: Int): List<VacationOption> =
        vacationCatalog.filter { it.minYear <= (semester + 1) / 2 }

    fun vacationOption(id: String): VacationOption? = vacationCatalog.find { it.id == id }

    fun currentVacation(state: GameState): VacationRecord? =
        state.vacationHistory.find { it.semester == state.semester }

    fun canAffordVacation(state: GameState, option: VacationOption): Boolean =
        state.money - option.cost - VACATION_EXPENSE >= -50_000

    fun applyVacation(state: GameState, optionId: String): GameState {
        if (state.semester >= 8 || state.needsRegistration || state.pendingEvent != null ||
            state.pendingConfession != null || state.pendingMiniGame != null || state.life.pendingMorning != null ||
            state.semesterResult?.semester != state.semester || currentVacation(state) != null) return state
        val option = vacationOptions(state.semester).find { it.id == optionId } ?: return state
        if (!canAffordVacation(state, option)) return state
        return state.copy(
            money = state.money - option.cost - VACATION_EXPENSE,
            hp = if (option.fullHeal) state.maxHp else (state.hp + option.hp).coerceIn(0, state.maxHp),
            happiness = (state.happiness + option.happiness).coerceIn(0, 100),
            spec = (state.spec + option.spec).coerceIn(0, 100),
            network = (state.network + option.network).coerceIn(0, 100),
            vacationHistory = state.vacationHistory + VacationRecord(state.semester, option.id)
        )
    }

    fun advanceSemester(state: GameState): GameState {
        val result = state.semesterResult ?: return state
        if (state.semester >= 8 || state.needsRegistration || state.pendingEvent != null ||
            state.pendingConfession != null || state.pendingMiniGame != null || state.life.pendingMorning != null ||
            result.semester != state.semester || currentVacation(state) == null) return state
        return state.copy(
            semester = state.semester + 1,
            week = 1,
            day = 0,
            money = state.money + result.scholarship,
            study = emptyMap(),
            log = emptyList(),
            midtermScores = emptyMap(),
            finalScores = emptyMap(),
            exam = null,
            semesterResult = null,
            transcript = state.completedSemesters,
            needsRegistration = true,
            registrationScholarship = result.scholarship,
            pendingEvent = null,
            eventBuffs = emptyList(),
            eventCheckedDay = -1,
            teamProject = null,
            teamBonuses = emptyMap(),
            semesterGpaAdjustment = 0.0,
            pendingMiniGame = null, miniBonus = null, miniCheckedDay = -1,
            life = state.life.copy(pendingMorning = null, morningNotice = null, morningCheckedDay = -1,
                lateCounts = emptyMap(), sickWeek = -1)
        )
    }

    fun registerSubjects(state: GameState, subjectIds: List<String>): GameState {
        if (!state.needsRegistration || state.pendingConfession != null || state.life.pendingMorning != null ||
            subjectIds.size != 3 || subjectIds.distinct().size != 3) return state
        val allowed = subjectsForSemester(state.semester).map { it.id }.toSet()
        if (!allowed.containsAll(subjectIds)) return state
        return state.copy(
            week = 1,
            day = 0,
            study = subjectIds.associateWith { 0 },
            log = emptyList(),
            midtermScores = emptyMap(),
            finalScores = emptyMap(),
            exam = null,
            semesterResult = null,
            needsRegistration = false,
            registrationScholarship = 0,
            pendingEvent = null,
            eventBuffs = emptyList(),
            eventCheckedDay = -1,
            teamProject = null,
            teamBonuses = emptyMap(),
            semesterGpaAdjustment = 0.0,
            pendingMiniGame = null, miniBonus = null, miniCheckedDay = -1,
            life = state.life.copy(pendingMorning = null, morningNotice = null, morningCheckedDay = -1,
                lateCounts = emptyMap(), sickWeek = -1)
        )
    }

    fun canUseActivity(state: GameState, activity: ActivityType): Boolean = when (activity) {
        ActivityType.CAREER -> state.semester >= 5
        ActivityType.TUTOR -> state.completedSemesters.isNotEmpty() && state.cumulativeGpa >= 3.0
        ActivityType.TEAM -> state.teamProject != null
        ActivityType.DATE -> state.love != null
        else -> true
    }

    fun graduationResult(state: GameState): GraduationResult {
        val gpa = state.cumulativeGpa
        return when {
            state.completedSemesters.map { it.semester }.toSet() != (1..8).toSet() || gpa < 2.0 ->
                GraduationResult("졸업 유예", "졸업에 필요한 8학기와 누적 평점 2.0을 채우지 못했어요.")
            state.happiness < 15 ->
                GraduationResult("번아웃", "졸업장은 받았지만, 잠시 쉬어 갈 시간이 필요해요.")
            gpa >= 4.0 && state.spec >= 50 ->
                GraduationResult("대학원 진학", "꾸준히 쌓은 공부와 경험을 바탕으로 연구를 이어 가요.")
            gpa >= 3.5 && state.spec >= 60 ->
                GraduationResult("대기업 취업", "학점과 스펙을 모두 갖추고 최종 합격 통보를 받았어요.")
            state.money >= 1_500_000 && state.network >= 70 ->
                GraduationResult("창업", "모아 둔 자본과 사람들을 바탕으로 새로운 도전을 시작해요.")
            state.spec >= 45 ->
                GraduationResult("개발자 취업", "쌓아 온 경험과 포트폴리오를 인정받았어요.")
            gpa >= 3.0 && state.spec >= 30 ->
                GraduationResult("공기업 합격", "학점과 준비한 경험을 바탕으로 안정적인 진로를 선택했어요.")
            gpa >= 2.7 ->
                GraduationResult("중견기업 취업", "대학생활을 마무리하고 첫 직장으로 출근해요.")
            else -> GraduationResult("취업 준비생", "졸업 후에도 원하는 진로를 향해 준비를 이어 가요.")
        }
    }
}
