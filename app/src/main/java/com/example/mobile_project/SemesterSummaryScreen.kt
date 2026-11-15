package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.ChipTone
import com.example.mobile_project.ui.components.PanelScaffold
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangChip
import com.example.mobile_project.ui.components.SummaryTile
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import java.text.NumberFormat
import java.util.Locale

/** 평점 글자: 3.85 → "3.85" */
internal fun gpaText(value: Double): String = String.format(Locale.US, "%.2f", value)

/** 학점 글자색: A 초록, B 주황, C 이하 빨강 */
internal fun letterColor(letter: String): Color = when (letter.firstOrNull()) {
    'A' -> PooangColors.UpText
    'B' -> PooangColors.GhostText
    else -> PooangColors.Warn
}

/** 평점에 맞는 표정 */
internal fun gpaMood(gpa: Double): PooangMood = when {
    gpa >= 4.0 -> PooangMood.PROUD
    gpa >= 3.0 -> PooangMood.HAPPY
    gpa >= 2.0 -> PooangMood.OK
    else -> PooangMood.SAD
}

/**
 * 학기 결산 화면 (`#sc-sem`). 기말고사가 끝나면 나온다.
 * 학기·누적 평점 큰 칸, 과목별 점수와 학점, 장학금, 학기별 평점 기록, 다음 단계(방학/졸업 심사) 버튼.
 */
@Composable
fun SemesterSummaryScreen(
    game: GameState,
    subjects: List<Subject>,
    onContinue: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val result = game.semesterResult ?: return

    ChallengeScaffold(
        mood = gpaMood(result.gpa),
        kicker = "학기 결산",
        title = "${semesterLabel(result.semester)} 종료",
        subtitle = "수고했어요! 이번 학기 성적이에요.",
        badgeIcon = R.drawable.pooang_ic_cap,
        badgeColor = PooangColors.Gold,
        heroColors = listOf(Color(0xFFFFF3D6), Color(0xFFFFDCA0)),
        modifier = modifier
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SummaryTile("학기 평점", "${gpaText(result.gpa)} / 4.5", Modifier.weight(1f),
                sub = if (result.gpaAdjustment != 0.0) String.format(Locale.KOREA, "이벤트 보정 %+.2f 포함", result.gpaAdjustment) else null)
            SummaryTile("누적 평점", "${gpaText(game.cumulativeGpa)} / 4.5", Modifier.weight(1f),
                sub = "${game.completedSemesters.size}학기 이수")
        }

        PooangCard(title = "과목별 성적", titleIcon = R.drawable.pooang_ic_exam, titleIconTint = Color(0xFF8A6BB8)) {
            result.grades.forEachIndexed { i, grade ->
                if (i > 0) Divider()
                val name = subjects.find { it.id == grade.subjectId }?.name ?: subjectForId(grade.subjectId)?.name ?: grade.subjectId
                val bonus = game.teamBonuses[grade.subjectId] ?: 0
                GradeRow(name, grade.score, grade.letter, if (bonus > 0) "조별과제 가산점 +${bonus}점 포함" else null)
            }
        }

        if (result.scholarship > 0 && game.semester < 8) {
            val amount = NumberFormat.getIntegerInstance(Locale.KOREA).format(result.scholarship)
            PooangCard(background = Color(0xFFEAF7E0), borderColor = Color(0xFFB8E0A0), contentPadding = 12.dp) {
                Text("장학금 ${amount}원!", color = PooangColors.UpText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("다음 학기 수강신청 때 받아요.", color = Color(0xFF2A5A3E), fontSize = 12.sp)
            }
        }

        SemesterHistoryCard(game.completedSemesters)

        PooangButton(
            when {
                game.semester >= 8 -> "졸업 심사 받기"
                AcademicEngine.currentVacation(game) != null -> "방학 결과 보기"
                else -> "방학으로"
            },
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            style = if (game.semester >= 8) PooangButtonStyle.Gold else PooangButtonStyle.Go,
            icon = if (game.semester >= 8) R.drawable.pooang_ic_cap else R.drawable.pooang_ic_plane
        )
        PooangButton("홈으로", onClick = onHome, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEFEBE0)))

/** 과목 한 줄 (`.grade`): 이름 · 점수 · 큰 학점 글자 */
@Composable
private fun GradeRow(name: String, score: Int, letter: String, note: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, color = PooangColors.TextBody, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (note != null) Text(note, color = PooangColors.UpText, fontSize = 11.sp)
        }
        Text("${score}점", color = PooangColors.TextSub, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 12.dp))
        Text(letter, color = letterColor(letter), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End, modifier = Modifier.size(width = 38.dp, height = 28.dp))
    }
}

/** 학기별 평점 (`.semrow`) — 막대로 비교해서 보여 준다. */
@Composable
internal fun SemesterHistoryCard(records: List<SemesterResult>) {
    if (records.isEmpty()) return
    PooangCard(title = "학기별 평점", titleIcon = R.drawable.pooang_ic_cal, titleIconTint = Color(0xFF3B82C4)) {
        records.forEachIndexed { i, r ->
            if (i > 0) Divider()
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(semesterLabel(r.semester), color = PooangColors.TextBody, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.size(width = 92.dp, height = 20.dp))
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(5.dp)).background(PooangColors.Track)) {
                    Box(Modifier.fillMaxWidth((r.gpa / 4.5).toFloat().coerceIn(0f, 1f)).height(8.dp)
                        .clip(RoundedCornerShape(5.dp)).background(PooangColors.OrangeBottom))
                }
                Text(gpaText(r.gpa), color = PooangColors.GhostText, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/**
 * 성적 기록 패널. 이번 학기 중간·기말 점수와 지난 학기 학점을 모두 보여 준다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradeRecordScreen(game: GameState, subjects: List<Subject>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    PanelScaffold(
        title = "성적 기록",
        onClose = onBack,
        modifier = modifier,
        icon = R.drawable.pooang_ic_exam,
        iconTint = Color(0xFF8A6BB8),
        caption = if (game.completedSemesters.isNotEmpty()) "누적 ${gpaText(game.cumulativeGpa)}" else null
    ) {
        if (subjects.isNotEmpty()) {
            PooangCard(title = "${semesterLabel(game.semester)} 시험 점수") {
                subjects.forEachIndexed { i, subject ->
                    if (i > 0) Divider()
                    Column(Modifier.padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(subject.name, color = PooangColors.TextBody, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            ScoreChip("중간", game.midtermScores[subject.id])
                            ScoreChip("기말", game.finalScores[subject.id])
                            val bonus = game.teamBonuses[subject.id] ?: 0
                            if (bonus > 0) {
                                PooangChip("조별과제 +${bonus}점" + if (game.semesterResult == null) " 예정" else " 반영", ChipTone.Up)
                            }
                        }
                    }
                }
            }
        }

        SemesterHistoryCard(game.completedSemesters)

        game.completedSemesters.reversed().forEach { record ->
            PooangCard(
                title = "${semesterLabel(record.semester)} 성적표",
                trailing = { Text(gpaText(record.gpa), color = PooangColors.GhostText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
            ) {
                record.grades.forEachIndexed { i, grade ->
                    if (i > 0) Divider()
                    GradeRow(subjectForId(grade.subjectId)?.name ?: grade.subjectId, grade.score, grade.letter, null)
                }
                if (record.gpaAdjustment != 0.0) {
                    Text(String.format(Locale.KOREA, "이벤트 평점 보정 %+.2f", record.gpaAdjustment),
                        color = PooangColors.TextSub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        if (subjects.isEmpty() && game.completedSemesters.isEmpty()) {
            Text("아직 성적 기록이 없어요.", color = PooangColors.TextSub, fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        PooangButton("홈으로", onClick = onBack, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
    }
}

@Composable
private fun ScoreChip(label: String, score: Int?) {
    PooangChip(
        if (score == null) "$label 미응시" else "$label ${score}점",
        when {
            score == null -> ChipTone.Even
            score >= 70 -> ChipTone.Up
            else -> ChipTone.Down
        }
    )
}

@Preview(name = "학기 결산", showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun SemesterSummaryPreview() {
    val subjects = subjectsForSemester(1).take(3)
    val result = SemesterResult(1, listOf(
        SubjectGrade(subjects[0].id, 94, "A+", 4.5), SubjectGrade(subjects[1].id, 82, "B+", 3.5),
        SubjectGrade(subjects[2].id, 68, "C+", 2.5)))
    PooangTheme {
        SemesterSummaryScreen(GameState(semesterResult = result, transcript = listOf(result)), subjects, {}, {})
    }
}
