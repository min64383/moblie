package com.example.mobile_project

object LoveCatalog {
    val partners = listOf("같은 과 동기", "동아리 선배", "교양 수업 조원", "학회 후배")
    val incidents = listOf(
        LoveIncidentOption("{name}가 시험기간이라고 커피랑 간식을 들고 왔습니다.", hp = 6, happiness = 12, money = 0),
        LoveIncidentOption("{name}와 밤새 통화했습니다. 즐거웠지만 다음 날 눈이 안 떠졌습니다.", hp = -9, happiness = 14, money = 0),
        LoveIncidentOption("기념일을 깜빡했습니다. 분위기가 싸늘합니다.", hp = 0, happiness = -14, money = 0),
        LoveIncidentOption("사소한 걸로 다퉜습니다. 결국 제가 먼저 연락했습니다.", hp = -4, happiness = -11, money = 0),
        LoveIncidentOption("{name}가 과제를 같이 봐줬습니다. 설명을 참 잘합니다.", hp = 0, happiness = 9, money = 0),
        LoveIncidentOption("{name}와 학교 앞에서 저녁을 먹었습니다. 계산은 제가 했습니다.", hp = 2, happiness = 10, money = -18_000),
        LoveIncidentOption("{name}가 친구들에게 저를 자랑했다고 합니다. 괜히 뿌듯합니다.", hp = 0, happiness = 13, money = 0),
        LoveIncidentOption("약속 시간에 늦어서 한 시간을 기다리게 했습니다.", hp = -3, happiness = -9, money = 0),
    )
}
