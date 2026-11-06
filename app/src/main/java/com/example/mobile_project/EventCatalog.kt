package com.example.mobile_project

// 활동·조별과제·연애 딜레마와 학과 행사의 문구와 수치.
object EventCatalog {
    val events: List<CampusEvent> = listOf(
        CampusEvent(
            id = "club",
            category = "동아리",
            title = "차기 회장 선출의 정적",
            description = "동아리 정기총회 마지막 안건, 차기 회장 선출입니다. 현 회장이 \"하고 싶은 사람?\"이라고 묻자 동아리방에 3분째 정적이 흐르고 있습니다. 모두가 바닥만 보고 있는데, 하필 푸앙이와 회장의 눈이 딱 마주쳤습니다…",
            activity = ActivityType.CLUB,
            semesterLimit = 1,
            options = listOf(
                EventOption(
                    label = "손 들고 회장 출마",
                    result = "박수갈채 속에 푸앙이는 만장일치로 회장이 되었습니다. 감격도 잠시, 인수인계 파일에는 \"MT 숙소 예약 아직 안 함\", \"회비 통장 비밀번호 모름\"이라고 적혀 있었습니다.",
                    effect = EventEffect(network = 15),
                    buff = EventBuffSpec(ActivityType.CLUB, 1.5,
                        EventBuffScope.NEXT_WEEK, "다음 주 동아리 능률 ▲50%")
                ),
                EventOption(
                    label = "옆 사람 추천하기",
                    result = "\"저는 OO이가 진짜 잘할 것 같아요!\" 지목당한 동기의 눈빛이 배신감으로 흔들렸습니다. 결국 동기가 회장이 되었고, 푸앙이는 자동으로 부회장이 되었습니다. 추천인 책임제라나요.",
                    effect = EventEffect(network = -10)
                ),
                EventOption(
                    label = "화장실 다녀오기",
                    result = "푸앙이는 슬그머니 자리를 떴습니다. 10분 뒤 돌아오자 칠판에 \"차기 회장: 푸앙이 (부재중 추대)\"라고 적혀 있었습니다. 자리에 없는 사람이 제일 만만한 법입니다.",
                    effect = EventEffect(),
                    buff = EventBuffSpec(ActivityType.CLUB, 0.5,
                        EventBuffScope.NEXT_WEEK, "다음 주 동아리 능률 ▼50%")
                ),
            )
        ),
        CampusEvent(
            id = "study",
            category = "공부",
            title = "도서관 유령 좌석",
            description = "시험기간의 중앙도서관은 빈자리 하나 없이 꽉 찼습니다. 그런데 창가 명당 자리에 전공책 한 권과 다 식은 라떼만 놓여 있습니다. 주인은 6시간째 나타나지 않았고, 라떼 위에는 얇은 막이 생겼습니다…",
            activity = ActivityType.STUDY,
            semesterLimit = 2,
            options = listOf(
                EventOption(
                    label = "그냥 앉아버리기",
                    result = "푸앙이는 책을 한쪽으로 밀고 앉아 공부에 몰입했습니다. 저녁 9시, 진짜 주인이 나타나 \"여기 제 자리인데요?\"라고 말했습니다. 푸앙이는 라떼 막을 가리켰습니다. 주인은 말없이 돌아갔습니다.",
                    effect = EventEffect(),
                    buff = EventBuffSpec(ActivityType.STUDY, 1.3,
                        EventBuffScope.THIS_WEEK, "이번 주 공부 능률 ▲30%")
                ),
                EventOption(
                    label = "쪽지 남기고 기다리기",
                    result = "푸앙이는 \"자리 비우셨으면 사용해도 될까요?\"라는 쪽지를 붙이고 계단에 앉아 기다렸습니다. 두 시간 뒤 돌아와 보니 다른 사람이 쪽지를 메모지 삼아 쓰면서 그 자리에 앉아 있었습니다.",
                    effect = EventEffect(),
                    buff = EventBuffSpec(ActivityType.STUDY, 0.9,
                        EventBuffScope.THIS_WEEK, "이번 주 공부 능률 ▼10%")
                ),
                EventOption(
                    label = "포기하고 카페로 가기",
                    result = "푸앙이는 흑석동 카페로 향했습니다. 공부를 하려고 했지만 크로플이 너무 맛있었고, 옆 테이블의 연애 상담 이야기가 너무 흥미진진했습니다. 오늘 읽은 페이지는 2쪽입니다.",
                    effect = EventEffect(money = -10_000),
                    buff = EventBuffSpec(ActivityType.STUDY, 0.9,
                        EventBuffScope.THIS_WEEK, "이번 주 공부 능률 ▼10%")
                ),
            )
        ),
        CampusEvent(
            id = "cls",
            category = "수업",
            title = "310관 엘리베이터 대란",
            description = "오전 9시 출석 체크 수업, 강의실은 310관 고층입니다. 지금 시각은 8시 56분, 엘리베이터 앞 대기줄이 입구까지 늘어서 있습니다. 교수님은 지각 세 번이면 결석 한 번으로 치는 분입니다…",
            activity = ActivityType.CLASS,
            semesterLimit = 2,
            options = listOf(
                EventOption(
                    label = "계단으로 전력 질주",
                    result = "푸앙이는 계단을 두 칸씩 뛰어올라 8시 59분에 강의실에 도착했습니다. 숨을 헐떡이며 자리에 앉는 순간 공지가 떴습니다. \"교수님 사정으로 오늘 휴강합니다.\"",
                    effect = EventEffect(hp = 10, classStudy = 3)
                ),
                EventOption(
                    label = "엘리베이터 줄 서기",
                    result = "엘리베이터는 모든 층에 한 번씩 멈췄습니다. 9시 7분, 푸앙이가 뒷문으로 조심스럽게 들어가자 교수님이 마침 이름을 부르고 있었습니다. \"푸앙이?\" \"네!\" 기적의 타이밍입니다.",
                    effect = EventEffect(happiness = -5, classStudy = 1)
                ),
                EventOption(
                    label = "자체 휴강 선언",
                    result = "푸앙이는 발길을 돌려 학식으로 향했습니다. 여유로운 아침을 즐기던 중 단톡방에 \"오늘 시험 범위 발표했어\"라는 메시지가 올라왔습니다. 범위는 아무도 받아 적지 않았습니다.",
                    effect = EventEffect(spec = -2, happiness = 10, classStudy = -2)
                ),
            )
        ),
        CampusEvent(
            id = "work",
            category = "카페알바",
            title = "뜨거운 아이스 아메리카노",
            description = "마감 10분 전, 손님 한 분이 카운터에 섰습니다. \"아이스 아메리카노 주세요. 아, 얼음은 빼고 따뜻하게요.\" 푸앙이는 주문을 세 번 되새겨 봤지만 그건 그냥 따뜻한 아메리카노입니다. 손님은 아주 진지한 표정입니다…",
            activity = ActivityType.WORK,
            semesterLimit = 2,
            options = listOf(
                EventOption(
                    label = "따뜻한 아메리카노 내주기",
                    result = "푸앙이는 아무 말 없이 따뜻한 아메리카노를 내줬습니다. 손님은 한 모금 마시더니 \"역시 여기 아이스가 제일 맛있어\"라며 만족스럽게 나갔습니다. 아이스 가격으로 결제된 건 비밀입니다.",
                    effect = EventEffect(money = 10_000)
                ),
                EventOption(
                    label = "차이를 친절하게 설명하기",
                    result = "\"손님, 얼음을 빼고 따뜻하게 하면 그건 핫 아메리카노인데요…\" 손님은 \"아이스는 맛의 이름이지 온도가 아니잖아요\"라고 반박했습니다. 대화는 마감 시간을 넘겨 이어졌습니다.",
                    effect = EventEffect(happiness = -5)
                ),
                EventOption(
                    label = "얼음 딱 한 알 띄우기",
                    result = "푸앙이는 따뜻한 아메리카노 위에 얼음 한 알을 동동 띄워 내밀었습니다. 손님은 감탄하며 사진을 찍어 갔고, 다음 날부터 카페 앞에 \"그 메뉴\"를 찾는 손님들이 줄을 섰습니다. 사장님은 메뉴판에 추가할지 고민 중입니다.",
                    effect = EventEffect(money = 20_000)
                ),
            )
        ),
        CampusEvent(
            id = "gym",
            category = "헬스장",
            title = "스쿼트랙의 주인",
            description = "오늘은 푸앙이의 하체 데이입니다. 그런데 헬스장에 하나뿐인 스쿼트랙에서 누군가 바벨 컬을 하고 있습니다. 정확히는 한 세트 하고 15분째 휴대폰으로 릴스를 보는 중입니다. 벌써 30분이 지났습니다…",
            activity = ActivityType.GYM,
            semesterLimit = 2,
            options = listOf(
                EventOption(
                    label = "정중하게 물어보기",
                    result = "\"혹시 몇 세트 남으셨어요?\" 상대는 깜짝 놀라며 \"아 쓰세요!\"라고 바로 비켜줬습니다. 알고 보니 같은 과 선배였고, 다음 날 족보를 넘겨받았습니다. 하체 데이가 학점 데이가 되었습니다.",
                    effect = EventEffect(network = 5)
                ),
                EventOption(
                    label = "옆에서 무언의 압박",
                    result = "푸앙이는 랙 옆에서 원판을 쿵쿵 내려놓으며 스트레칭을 했습니다. 상대는 눈치를 채더니 세트를 다시 시작했습니다. 정확히 10세트를 더 했습니다.",
                    effect = EventEffect(happiness = -5)
                ),
                EventOption(
                    label = "레그프레스로 타협하기",
                    result = "오기가 생긴 푸앙이는 평소보다 원판을 두 장 더 끼웠습니다. 밀어내는 데는 성공했지만 다리가 후들거려 계단을 내려가는 데 5분이 걸렸습니다.",
                    effect = EventEffect(hp = -5)
                ),
            )
        ),
        CampusEvent(
            id = "run",
            category = "한강러닝",
            title = "러닝크루의 배신",
            description = "푸앙이는 처음으로 흑석 러닝크루 정기런에 나왔습니다. 공지에는 분명 \"초보 환영, 편하게 7분 페이스\"라고 적혀 있었는데, 출발 3분 만에 앞사람들이 점점 작아지고 있습니다. 시계는 5분 페이스를 가리키고, 반포 쪽 편의점 불빛이 유난히 따뜻해 보입니다…",
            activity = ActivityType.RUN,
            semesterLimit = 2,
            options = listOf(
                EventOption(
                    label = "이 악물고 따라가기",
                    result = "푸앙이는 끝까지 따라붙어 완주했습니다. 단체 사진에서는 혼자 얼굴이 하얗게 질려 있었지만, 크루장이 \"다음 주 하프 마라톤 같이 나가요!\"라고 했습니다. 다음 날 아침 푸앙이는 침대에서 일어날 수 없었습니다.",
                    effect = EventEffect(hp = 5)
                ),
                EventOption(
                    label = "\"앗, 신발끈이…\"",
                    result = "푸앙이는 쭈그려 앉아 신발끈을 묶는 척했습니다. 크루장이 돌아와서 같이 기다려 주겠다고 했는데, 그제야 둘 다 푸앙이의 신발에 끈이 없다는 걸 알았습니다.",
                    effect = EventEffect(charm = -5)
                ),
                EventOption(
                    label = "편의점으로 조용히 이탈",
                    result = "푸앙이는 대열에서 조용히 빠져나와 한강 라면을 끓였습니다. 강바람을 맞으며 국물을 마시는데, 반환점을 돌아오던 크루원들이 일제히 푸앙이를 쳐다봤습니다. 그날 크루 단톡방 이름은 \"라면런\"으로 바뀌었습니다.",
                    effect = EventEffect(happiness = 5, hp = -5)
                ),
            )
        ),
        CampusEvent(
            id = "pt",
            category = "PT",
            title = "식단 인증의 시간",
            description = "PT 트레이너에게서 카톡이 왔습니다. \"회원님~ 어제 저녁 식단 사진 보내주세요!\" 문제는 어제 저녁이 동아리 뒤풀이의 반반치킨과 치즈볼이었다는 것입니다. 앨범에는 지난주에 찍은 닭가슴살 샐러드 사진이 남아 있습니다…",
            activity = ActivityType.PT,
            semesterLimit = 2,
            options = listOf(
                EventOption(
                    label = "치킨 사진 정직하게 보내기",
                    result = "푸앙이는 치킨 사진을 그대로 보냈습니다. 트레이너는 \"솔직해서 좋아요 ^^\"라고 답했습니다. 다음 PT 시간, 푸앙이는 치즈볼 개수만큼 버피를 했습니다. 열두 개였습니다.",
                    effect = EventEffect(hp = 7)
                ),
                EventOption(
                    label = "지난주 샐러드 사진 재활용",
                    result = "트레이너는 사진을 확대하더니 답장했습니다. \"회원님, 이 샐러드 옆 영수증 날짜가 지난주인데요?\" 그날 인바디 결과는 사진보다 더 정직했습니다.",
                    effect = EventEffect(charm = -5)
                ),
                EventOption(
                    label = "\"치킨도 닭이잖아요\" 논리로 우기기",
                    result = "푸앙이는 \"치킨도 결국 닭가슴살 포함이에요\"라고 당당하게 답했습니다. 트레이너는 한참 동안 읽고 답하지 않더니 짧게 보냈습니다. \"내일 하체 2시간 잡아둘게요.\"",
                    effect = EventEffect(happiness = 5)
                ),
            )
        ),
        CampusEvent(
            id = "love",
            category = "연애",
            title = "썸의 수제 도시락",
            description = "청룡연못 벤치에서 {name}와 점심을 먹고 있습니다. " +
                "새벽부터 준비했다는 도시락을 먹는데 계란 껍데기가 씹힙니다. 상대는 반짝이는 눈으로 반응을 기다립니다.",
            activity = ActivityType.DATE,
            semesterLimit = 1,
            options = listOf(
                EventOption("맛있다며 싹싹 비우기",
                    "푸앙이는 미소를 잃지 않고 도시락을 비웠습니다. {name}는 내일도 싸 오겠다고 했습니다.",
                    EventEffect(hp = -10, affection = 10)),
                EventOption("솔직하게 말해주기",
                    "잠시 어색한 정적이 흘렀지만, 다음 주말에는 함께 요리를 배우기로 했습니다.",
                    EventEffect(affection = -5)),
                EventOption("청룡에게 바치기",
                    "도시락을 연못에 던지는 장면이 사진에 찍혔습니다. {name}와는 그날로 아는 사이가 되었습니다.",
                    EventEffect(charm = 10, endLove = true))
            )
        ),
        CampusEvent(
            id = "team",
            category = "조별과제",
            title = "사라지지 않는 숫자 1",
            description = "발표 전날 밤, 한 조원이 맡은 PPT가 아직 올라오지 않았습니다. " +
                "단톡방의 숫자 1은 사흘째 그대로인데, 방금 올라온 SNS 사진에는 술자리가 보입니다. 어떻게 할까요?",
            activity = ActivityType.TEAM,
            semesterLimit = 1,
            options = listOf(
                EventOption(
                    label = "혼자 밤새서 다 하기",
                    result = "푸앙이는 새벽까지 PPT를 완성했습니다. 발표는 성공했지만 교수님은 훌륭한 팀워크를 칭찬했습니다.",
                    effect = EventEffect(hp = -20, gpaAdjustment = 0.04)
                ),
                EventOption(
                    label = "기여도 평가에 솔직하게 쓰기",
                    result = "기여도 평가에 실제 작업 내용을 적었습니다. 할 말을 하고 나니 마음이 조금 가벼워졌습니다.",
                    effect = EventEffect(happiness = 10, gpaAdjustment = 0.02)
                ),
                EventOption(
                    label = "단톡방에 공개 저격",
                    result = "메시지를 보내자 숫자 1은 사라졌습니다. 하지만 분위기는 싸늘해졌고 결과물도 아쉬워졌습니다.",
                    effect = EventEffect(network = -10, teamBonus = -3)
                )
            )
        ),
        CampusEvent(
            id = "mt",
            category = "학과 행사",
            title = "신입생 MT",
            description = "과 대표에게서 연락이 왔습니다. 1박 2일 MT, 회비는 50,000원입니다. 참가할까요?",
            weeklyEvent = true,
            options = listOf(
                EventOption(
                    label = "갈게요",
                    result = "밤새 놀았더니 몸은 축났지만 동기들과 많이 친해졌습니다.",
                    effect = EventEffect(money = -50_000, hp = -25, happiness = 30, network = 15)
                ),
                EventOption(
                    label = "안 갈래요",
                    result = "주말을 통째로 아꼈습니다. 다만 단톡방 분위기에서 살짝 밀려났습니다.",
                    effect = EventEffect(network = -5)
                ),
            )
        ),
        CampusEvent(
            id = "fest",
            category = "학과 행사",
            title = "학과 축제",
            description = "학과에서 주점 부스를 운영한다고 합니다. 참가비 30,000원을 내고 축제를 즐길까요?",
            weeklyEvent = true,
            options = listOf(
                EventOption(
                    label = "참가",
                    result = "부스에서 하루 종일 떠들었습니다. 즐거운 추억과 새로운 인연을 얻었습니다.",
                    effect = EventEffect(money = -30_000, hp = -15, happiness = 25, network = 10)
                ),
                EventOption(
                    label = "패스",
                    result = "조용한 캠퍼스에서 평소대로 지냈습니다.",
                    effect = EventEffect()
                ),
            )
        ),
    )
}
