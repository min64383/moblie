package com.example.mobile_project

// 원본 HTML의 조원 성향 6종과 조원 소식 13종.
object TeamCatalog {
    val members: List<TeamMemberType> = listOf(
        TeamMemberType(
            id = "ghost", name = "잠수함",
            description = "연락이 안 됩니다. 조모임 1칸 추가",
            slotDelta = 1, happiness = 0,
            studyMultiplier = 1.0, bonus = 0, network = 0,
            incidents = listOf(
                TeamIncidentOption("발표 이틀 전까지 파트를 안 보냈습니다. 결국 대신 썼습니다.", hp = -14, happiness = -10),
                TeamIncidentOption("단톡방에서 읽씹만 사흘째입니다. 혼자 자료를 정리했습니다.", hp = -10, happiness = -12),
            )
        ),
        TeamMemberType(
            id = "namu", name = "나무위키 전문가",
            description = "자료를 전부 검증해야 합니다",
            slotDelta = 0, happiness = -3,
            studyMultiplier = 0.7, bonus = 0, network = 0,
            incidents = listOf(
                TeamIncidentOption("참고문헌에 나무위키가 박혀 있었습니다. 밤새 출처를 바꿨습니다.", hp = -12, happiness = -8),
                TeamIncidentOption("\"여기 다 나와 있던데요\"라며 가져온 자료가 전부 틀렸습니다.", hp = -9, happiness = -10),
            )
        ),
        TeamMemberType(
            id = "free", name = "무임승차자",
            description = "작업을 전가합니다. 조모임 1칸 추가",
            slotDelta = 1, happiness = -8,
            studyMultiplier = 1.0, bonus = 0, network = 0,
            incidents = listOf(
                TeamIncidentOption("회의에 와서 \"뭐 하면 돼?\"만 묻고 갔습니다.", hp = -8, happiness = -12),
                TeamIncidentOption("맡은 분량을 한 문단 써 왔습니다. 나머지는 제 몫이 됐습니다.", hp = -15, happiness = -9),
            )
        ),
        TeamMemberType(
            id = "ego", name = "독불장군",
            description = "회의마다 충돌합니다",
            slotDelta = 0, happiness = -12,
            studyMultiplier = 1.0, bonus = 0, network = 0,
            incidents = listOf(
                TeamIncidentOption("새벽 2시에 전체 구조를 갈아엎자고 연락이 왔습니다.", hp = -13, happiness = -11),
                TeamIncidentOption("두 시간짜리 회의가 말다툼으로 끝났습니다.", hp = -7, happiness = -15),
            )
        ),
        TeamMemberType(
            id = "ppt", name = "PPT 장인",
            description = "결과물 퀄리티가 올라갑니다",
            slotDelta = 0, happiness = 2,
            studyMultiplier = 1.0, bonus = 5, network = 0,
            incidents = listOf(
                TeamIncidentOption("밤새 만든 발표자료를 보내왔습니다. 디자인이 반칙 수준입니다.", hp = 0, happiness = 9),
                TeamIncidentOption("\"폰트랑 간격은 제가 다 맞췄어요\"라며 최종본을 올렸습니다.", hp = 2, happiness = 7),
            )
        ),
        TeamMemberType(
            id = "good", name = "성실한 조원",
            description = "알아서 다 해옵니다. 조모임 1칸 감소",
            slotDelta = -1, happiness = 5,
            studyMultiplier = 1.0, bonus = 3, network = 5,
            incidents = listOf(
                TeamIncidentOption("제 파트까지 초안을 잡아 왔습니다. 덕분에 하루를 벌었습니다.", hp = 8, happiness = 10),
                TeamIncidentOption("회의 전에 자료를 미리 정리해 공유했습니다. 이런 조원 처음입니다.", hp = 5, happiness = 8),
                TeamIncidentOption("커피를 사 들고 와서는 \"제가 할게요\"라고 했습니다.", hp = 3, happiness = 12),
            )
        ),
    )

    fun memberForId(id: String): TeamMemberType? = members.find { it.id == id }
}
