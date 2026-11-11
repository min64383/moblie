package com.example.mobile_project

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/**
 * 활동별 아이콘과 효과 설명. 시간표·로그 화면이 함께 쓴다.
 * 효과 글자는 GameEngine 의 effects 표와 같은 값이다 (바꾸면 같이 고칠 것).
 */
object ActivityStyle {
    @DrawableRes
    fun icon(activity: ActivityType): Int = when (activity) {
        ActivityType.EMPTY -> R.drawable.pooang_ic_empty
        ActivityType.STUDY -> R.drawable.pooang_ic_study
        ActivityType.WORK -> R.drawable.pooang_ic_work
        ActivityType.FUN -> R.drawable.pooang_ic_fun
        ActivityType.REST -> R.drawable.pooang_ic_rest
        ActivityType.CLUB -> R.drawable.pooang_ic_club
        ActivityType.RUN -> R.drawable.pooang_ic_run
        ActivityType.GYM -> R.drawable.pooang_ic_gym
        ActivityType.PT -> R.drawable.pooang_ic_pt
        ActivityType.SLEEP -> R.drawable.pooang_ic_sleep
        ActivityType.CLASS -> R.drawable.pooang_ic_cls
        ActivityType.CAREER -> R.drawable.pooang_ic_career
        ActivityType.TUTOR -> R.drawable.pooang_ic_tutor
        ActivityType.TEAM -> R.drawable.pooang_ic_team
        ActivityType.DATE -> R.drawable.pooang_ic_heart
    }

    /** 칸 색. 빈칸은 프로토타입처럼 회갈색 아이콘만 보인다. */
    fun color(activity: ActivityType): Color =
        if (activity == ActivityType.EMPTY) Color(0xFFB0AA9A) else Color(activity.color)

    fun effect(activity: ActivityType): String = when (activity) {
        ActivityType.EMPTY -> "그 시간은 그냥 흘러갑니다"
        ActivityType.STUDY -> "공부량 +${GameEngine.STUDY_GAIN}×효율 · 체력 −9 · 행복 −3"
        ActivityType.WORK -> "돈 +25,000 · 체력 −13 · 행복 −6"
        ActivityType.FUN -> "행복 +18 · 체력 +2 · 돈 −7,000 (지칠수록 효과 ↑)"
        ActivityType.REST -> "체력 +11 · 행복 +4 (지칠수록 효과 ↑)"
        ActivityType.CLUB -> "행복 +10 · 인맥 +4 · 체력 −7 · 돈 −3,000"
        ActivityType.RUN -> "무료 · 최대체력 +1 · 행복 +3 · 체력 −10"
        ActivityType.GYM -> "₩15,000 · 최대체력 +3 · 매력 +2 · 체력 −12"
        ActivityType.PT -> "₩40,000 · 최대체력 +5 · 매력 +4 · 체력 −10"
        ActivityType.SLEEP -> "체력 +17 · 주당 ${HomeModel.MIN_SLEEP}칸은 자야 합니다"
        ActivityType.CLASS -> "고정 · 공부량 +${GameEngine.CLASS_GAIN} · 체력 −6 · 행복 −2"
        ActivityType.CAREER -> "스펙 +1 · 체력 −11 · 행복 −7 (3학년부터)"
        ActivityType.TUTOR -> "돈 +45,000 · 체력 −8 · 행복 −2 (누적 평점 3.0 이상)"
        ActivityType.TEAM -> "고정 · 체력 −9 · 행복 −5"
        ActivityType.DATE -> "고정 · 행복 +25 · 돈 −20,000"
    }
}
