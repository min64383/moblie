# 푸앙이 대학생활 – 화면 디자인 가이드

디자인 담당: 김상은

버튼, 카드, 말풍선, 스탯 바는 **이미지가 아니라 Compose 코드로 만듭니다**. 아래 색상과 화면 캡처를 보고 구현해 주세요.
이미지로 넣는 건 푸앙이 캐릭터, 배경, 아이콘뿐이에요.

## 폴더
| 위치 | 내용 |
|---|---|
| `design/screens/` | HTML 프로토타입 화면 캡처 (00 전체 흐름 ~ 12 엔딩) |
| `design/prototype/pooang-ui.html` | 직접 눌러볼 수 있는 프로토타입 (브라우저로 열기) |
| `app/src/main/res/drawable-nodpi/` | 푸앙이 PNG |
| `app/src/main/res/drawable/` | 아이콘·배경 벡터 (`pooang_ic_*`, `pooang_campus_*`) |

## 푸앙이 이미지 (drawable-nodpi)
| 리소스 | 쓰는 곳 |
|---|---|
| `pooang_title` | 시작(타이틀) 화면 |
| `pooang_main` | 홈 화면 가운데 메인 푸앙이 |
| `pooang_face` | 프로필 사진 |
| `pooang_grad` | 졸업 엔딩 (GraduationScreen) |
| `pooang_ok` 외 16종 | 일간·주간 로그, 이벤트 결과의 감정 표정 (고화질 교체) |

| 리소스 | 표정 |
|---|---|
| `pooang_ok` | 안녕 (기본) |
| `pooang_happy` | 좋아 |
| `pooang_tired` | 졸려 |
| `pooang_sad` | 힝ㅠㅠ |
| `pooang_sick` | 멍~ |
| `pooang_proud` | 짝짝 |
| `pooang_love` | 쪽 |
| `pooang_angry` | 교수님 미워 |
| `pooang_study` | 시험 끝내자 (합격기원) |
| `pooang_shock` | 놀람 |
| `pooang_yum` | 냠냠 |
| `pooang_hehe` | 헤헷 |
| `pooang_fire` | 화남 |
| `pooang_home` | 집가고 싶다 |
| `pooang_yawn` | 하품 |
| `pooang_class` | 수업 그만해 (새로 추가) |

감정 표정은 약 160px 투명 PNG예요. **64~96dp**로 쓰면 선명해요.

## 화면별 참고 캡처
| 캡처 | 대응 화면 |
|---|---|
| 01_course_registration | CourseRegistrationScreen |
| 02_home | MainActivity 홈 |
| 03_timetable | WeeklyScheduleScreen / RoutineScreen |
| 04_daily_log | DayReportScreen |
| 05~06_dilemma | EventScreen |
| 07_weekly_log | DayReportScreen (주간 보기) |
| 08_minigame | MiniGameScreen |
| 09_exam | ExamScreen |
| 10_semester_report | SemesterSummaryScreen |
| 11_vacation | VacationScreen |
| 12_ending | GraduationScreen |

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
- 카드: 모서리 18dp, 테두리 2dp `PuangLine`, 아래로 3dp 그림자
- 말풍선: 흰 배경, 모서리 16dp, 테두리 2.5dp `PuangAccent`
