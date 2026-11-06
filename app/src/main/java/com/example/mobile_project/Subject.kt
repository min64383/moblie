package com.example.mobile_project

data class Subject(val id: String, val name: String, val description: String)

val curriculum = mapOf(
    1 to listOf(
        Subject("cal", "미적분학", "교양필수 · 계산량 많음"),
        Subject("phy", "일반물리학", "교양필수 · 실험 포함"),
        Subject("intro", "컴퓨터개론", "전공기초 · 무난함"),
        Subject("eng", "대학영어", "교양 · 출석 중요"),
        Subject("phi", "철학의 이해", "교양 · 꿀강")
    ),
    2 to listOf(
        Subject("ds", "자료구조", "전공필수 · 과제 많음"),
        Subject("dm", "이산수학", "전공필수 · 증명 위주"),
        Subject("ca", "컴퓨터구조", "전공필수 · 암기 많음"),
        Subject("oop", "객체지향프로그래밍", "전공 · 프로젝트형"),
        Subject("prob", "확률과통계", "전공기초 · 계산 위주")
    ),
    3 to listOf(
        Subject("os", "운영체제", "전공필수 · 시험 어려움"),
        Subject("algo", "알고리즘", "전공필수 · 난이도 최상"),
        Subject("db", "데이터베이스", "전공 · 무난함"),
        Subject("net", "컴퓨터네트워크", "전공 · 암기 많음"),
        Subject("se", "소프트웨어공학", "전공 · 팀플 확정")
    ),
    4 to listOf(
        Subject("ml", "기계학습", "전공심화 · 수식 많음"),
        Subject("comp", "컴파일러", "전공심화 · 구현 과제"),
        Subject("sec", "정보보호", "전공심화 · 암호 수학"),
        Subject("dist", "분산시스템", "전공심화 · 난이도 상"),
        Subject("cap", "캡스톤디자인", "졸업요건 · 팀플 확정")
    )
)

val firstYearSubjects = curriculum.getValue(1)
val allSubjects = curriculum.values.flatten()

fun subjectsForSemester(semester: Int): List<Subject> {
    require(semester in 1..8)
    return curriculum.getValue((semester + 1) / 2)
}

fun subjectForId(id: String): Subject? = allSubjects.find { it.id == id }
