package com.example.mobile_project

enum class MiniKind(val title: String) {
    FOCUS("숫자 집중"), MEMORY("용어 암기"), CALC("암산 연습"), ORDER("주문 기억"),
    CHANGE("거스름돈 계산"), TUTOR("학생의 질문"), INTERVIEW("모의 면접"),
    TIMING("운동 타이밍"), HOLD("마지막 세트")
}
enum class MiniPhase { READY, MEMORIZE, PLAY, RESULT }

data class MiniQuestion(val prompt: String, val options: List<String>, val correctIndex: Int)

data class MiniReward(
    val studyMultiplier: Double = 1.0,
    val money: Int = 0,
    val maxHp: Int = 0,
    val spec: Int = 0,
    val happiness: Int = 0,
    val description: String = "추가 보너스 없음"
)

data class MiniGameRun(
    val semester: Int,
    val week: Int,
    val day: Int,
    val block: Int,
    val activity: ActivityType,
    val kind: MiniKind,
    val subjectId: String? = null,
    val phase: MiniPhase = MiniPhase.READY,
    val choices: List<String> = emptyList(),
    val sequence: List<Int> = emptyList(),
    val questions: List<MiniQuestion> = emptyList(),
    val selected: List<Int> = emptyList(),
    val progress: Int = 0,
    val correct: Int = 0,
    val revision: Int = 0,
    val durationMs: Long = 0L,
    val startedAtMs: Long = 0L,
    val deadlineMs: Long = 0L,
    val previewUntilMs: Long = 0L,
    val presetTip: Int = 0,
    val reward: MiniReward? = null,
    val resultTitle: String = ""
) {
    val key: String get() = "$semester:$week:$day:$block:${kind.name}"
}

data class MiniGameBonus(
    val runKey: String,
    val semester: Int,
    val week: Int,
    val day: Int,
    val block: Int,
    val activity: ActivityType,
    val subjectId: String?,
    val reward: MiniReward
)

// 저장값은 문자열·정수·ArrayList뿐이며 Android 파일 저장과 JVM 저장 테스트가 함께 사용한다.
internal object MiniStateCodec {
    private fun packReward(reward: MiniReward?): ArrayList<String> =
        if (reward == null) arrayListOf() else arrayListOf(reward.studyMultiplier.toString(),
            reward.money.toString(), reward.maxHp.toString(), reward.spec.toString(),
            reward.happiness.toString(), reward.description)

    private fun unpackReward(value: Any?): MiniReward? {
        val p = (value as? List<*>).orEmpty().map { it as String }
        if (p.isEmpty()) return null
        require(p.size == 6)
        return MiniReward(p[0].toDouble(), p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5])
    }

    fun packRun(run: MiniGameRun?): ArrayList<Any> {
        if (run == null) return arrayListOf()
        val meta = arrayListOf(run.semester.toString(), run.week.toString(), run.day.toString(),
            run.block.toString(), run.activity.name, run.kind.name, run.subjectId.orEmpty(), run.phase.name,
            run.progress.toString(), run.correct.toString(), run.revision.toString(), run.durationMs.toString(),
            run.startedAtMs.toString(), run.deadlineMs.toString(), run.previewUntilMs.toString(),
            run.presetTip.toString(), run.resultTitle)
        val questions = ArrayList(run.questions.map {
            arrayListOf<Any>(it.prompt, ArrayList(it.options), it.correctIndex)
        })
        return arrayListOf(meta, ArrayList(run.choices), ArrayList(run.sequence),
            questions, ArrayList(run.selected), packReward(run.reward))
    }

    fun unpackRun(value: Any?): MiniGameRun? {
        val p = (value as? List<*>).orEmpty()
        if (p.isEmpty()) return null
        require(p.size == 6)
        val m = (p[0] as List<*>).map { it as String }
        require(m.size == 17)
        val questions = (p[3] as List<*>).map {
            val q = it as List<*>
            MiniQuestion(q[0] as String, (q[1] as List<*>).map { option -> option as String }, q[2] as Int)
        }
        return MiniGameRun(m[0].toInt(), m[1].toInt(), m[2].toInt(), m[3].toInt(),
            ActivityType.valueOf(m[4]), MiniKind.valueOf(m[5]), m[6].takeIf { it.isNotEmpty() },
            MiniPhase.valueOf(m[7]), (p[1] as List<*>).map { it as String },
            (p[2] as List<*>).map { it as Int }, questions, (p[4] as List<*>).map { it as Int },
            m[8].toInt(), m[9].toInt(), m[10].toInt(), m[11].toLong(), m[12].toLong(),
            m[13].toLong(), m[14].toLong(), m[15].toInt(), unpackReward(p[5]), m[16])
    }

    fun packBonus(value: MiniGameBonus?): ArrayList<Any> = if (value == null) arrayListOf() else arrayListOf(
        arrayListOf(value.runKey, value.semester.toString(), value.week.toString(), value.day.toString(),
            value.block.toString(), value.activity.name, value.subjectId.orEmpty()), packReward(value.reward)
    )

    fun unpackBonus(value: Any?): MiniGameBonus? {
        val p = (value as? List<*>).orEmpty()
        if (p.isEmpty()) return null
        require(p.size == 2)
        val m = (p[0] as List<*>).map { it as String }
        require(m.size == 7)
        return MiniGameBonus(m[0], m[1].toInt(), m[2].toInt(), m[3].toInt(), m[4].toInt(),
            ActivityType.valueOf(m[5]), m[6].takeIf { it.isNotEmpty() }, requireNotNull(unpackReward(p[1])))
    }
}
