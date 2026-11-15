package com.example.mobile_project

import androidx.annotation.DrawableRes

/**
 * 푸앙이 표정. 각 표정은 res/drawable-nodpi 의 PNG 하나와 연결된다.
 * key 는 프로토타입(PUANG_MOOD)의 이름과 같다.
 */
enum class PooangMood(val key: String, @DrawableRes val drawable: Int, val label: String) {
    OK("ok", R.drawable.pooang_ok, "그럭저럭"),
    HAPPY("happy", R.drawable.pooang_happy, "아주 좋음"),
    TIRED("tired", R.drawable.pooang_tired, "지쳤음"),
    SAD("sad", R.drawable.pooang_sad, "우울함"),
    SICK("sick", R.drawable.pooang_sick, "아픔"),
    PROUD("proud", R.drawable.pooang_proud, "뿌듯함"),
    LOVE("love", R.drawable.pooang_love, "설렘"),
    ANGRY("angry", R.drawable.pooang_angry, "교수님 미워"),
    STUDY("study", R.drawable.pooang_study, "시험 끝내자"),
    SHOCK("shock", R.drawable.pooang_shock, "놀람"),
    YUM("yum", R.drawable.pooang_yum, "냠냠"),
    HEHE("hehe", R.drawable.pooang_hehe, "헤헷"),
    FIRE("fire", R.drawable.pooang_fire, "화남"),
    HOME("home", R.drawable.pooang_home, "집가고 싶다"),
    YAWN("yawn", R.drawable.pooang_yawn, "하품"),
    CLASS("class", R.drawable.pooang_class, "수업 그만해");

    companion object {
        /** 이름으로 표정을 찾는다. 모르는 이름이면 기본 표정(OK). */
        fun fromKey(key: String?): PooangMood = entries.firstOrNull { it.key == key } ?: OK

        /**
         * 현재 상태에 맞는 표정. 프로토타입 moodOf() 와 같은 순서로 판단한다.
         * 아픔 → 우울(행복 20 미만) → 지침(체력 25% 미만) → 설렘(연애 중·행복 70 이상)
         * → 아주 좋음(행복 80 이상·체력 60% 이상) → 지침(행복 40 미만) → 그럭저럭
         */
        fun of(state: GameState): PooangMood = of(
            hp = state.hp,
            maxHp = state.maxHp,
            happiness = state.happiness,
            sick = LifeEngine.isSick(state),
            inLove = state.love != null
        )

        fun of(hp: Int, maxHp: Int, happiness: Int, sick: Boolean, inLove: Boolean): PooangMood {
            val hpRate = if (maxHp <= 0) 0.0 else hp * 100.0 / maxHp
            return when {
                sick -> SICK
                happiness < 20 -> SAD
                hpRate < 25 -> TIRED
                inLove && happiness >= 70 -> LOVE
                happiness >= 80 && hpRate >= 60 -> HAPPY
                happiness < 40 -> TIRED
                else -> OK
            }
        }
    }
}
