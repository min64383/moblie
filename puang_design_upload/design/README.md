# 푸앙이 대학생활 – 화면 디자인 가이드

김상은

버튼, 카드, 말풍선, 스탯 바는 **이미지가 아니라 Compose 코드로 만듭니다**. 아래 색상과 화면 캡처를 보고 구현해 주세요.
이미지로 넣는 건 푸앙이 캐릭터, 배경, 아이콘뿐이에요.

## 폴더

|위치|내용|
|-|-|
|`design/screens/`|HTML 프로토타입 화면 캡처 (00 전체 흐름 \~ 12 엔딩)|
|`design/prototype/pooang-ui.html`|직접 눌러볼 수 있는 프로토타입 (브라우저로 열기)|
|`app/src/main/res/drawable-nodpi/`|푸앙이 PNG|
|`app/src/main/res/drawable/`|아이콘·배경 벡터 (`pooang\_ic\_\*`, `pooang\_campus\_\*`)|

## 푸앙이 이미지 (drawable-nodpi)

|리소스|쓰는 곳|
|-|-|
|`pooang\_title`|시작(타이틀) 화면|
|`pooang\_main`|홈 화면 가운데 메인 푸앙이|
|`pooang\_face`|프로필 사진|
|`pooang\_grad`|졸업 엔딩 (GraduationScreen)|
|`pooang\_ok` 외 15종|일간·주간 로그, 이벤트 결과의 감정 표정|

감정 표정: ok(기본) · happy · tired · sad · sick · proud · love · angry · study · shock · yum · hehe · fire · home · yawn

감정 표정은 원본이 약 80px라 **48\~64dp**로 써 주세요.

## 화면별 참고 캡처

|캡처|대응 화면|
|-|-|
|01\_course\_registration|CourseRegistrationScreen|
|02\_home|MainActivity 홈|
|03\_timetable|WeeklyScheduleScreen / RoutineScreen|
|04\_daily\_log|DayReportScreen|
|05\~06\_dilemma|EventScreen|
|07\_weekly\_log|DayReportScreen (주간 보기)|
|08\_minigame|MiniGameScreen|
|09\_exam|ExamScreen|
|10\_semester\_report|SemesterSummaryScreen|
|11\_vacation|VacationScreen|
|12\_ending|GraduationScreen|

## 색상 (Color.kt에 추가 권장)

```kotlin
val PuangBg       = Color(0xFFF3DFC2) // 전체 배경
val PuangPanel    = Color(0xFFFFF4E4) // 패널 배경 (크림)
val PuangCard     = Color(0xFFFFFDF8) // 카드 배경
val PuangLine     = Color(0xFFEBD3B3) // 카드 테두리·그림자
val PuangAccent   = Color(0xFFE1B07A) // 말풍선·강조 테두리
val PuangText     = Color(0xFF5A3A1E) // 진한 글자
val CauBlue       = Color(0xFF0B4DA2) // 중앙대 블루
```

## 모양

* 카드: 모서리 18dp, 테두리 2dp `PuangLine`, 아래로 3dp 그림자
* 말풍선: 흰 배경, 모서리 16dp, 테두리 2.5dp `PuangAccent`

