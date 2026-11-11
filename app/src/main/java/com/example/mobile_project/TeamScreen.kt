package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.ChipTone
import com.example.mobile_project.ui.components.PanelScaffold
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangChip
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import java.util.Locale

private val TeamRed = Color(0xFFC0504D)

/**
 * 조별과제 패널 (`#p-team`).
 * 새 과제가 발표되면(announced=false) 조원 소개 후 "조원 확인 · 시간표 보기" 버튼으로 넘어간다.
 * 평소에는 진행 상황·조원·고정 조모임·가산점·제출 기록을 보여 준다.
 */
@Composable
fun TeamScreen(game: GameState, onConfirm: (String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val project = game.teamProject
    val announcing = project != null && !project.announced

    PanelScaffold(
        title = if (announcing) "조별과제 발표!" else "조별과제",
        onClose = onBack,
        modifier = modifier,
        icon = R.drawable.pooang_ic_team,
        iconTint = TeamRed,
        caption = project?.let { "남은 ${it.weeksLeft}주" },
        footer = if (announcing) {
            {
                PooangButton("조원 확인 · 시간표 보기", onClick = { onConfirm(project!!.key) },
                    modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_cal)
            }
        } else null
    ) {
        if (project != null) {
            ProjectCard(project)
            MembersCard(project)
            MeetingsCard(project)
            Text(
                "수업과 밤 시간은 그대로 두고 자유 칸을 조모임으로 바꿨어요. 조모임은 제출까지 고정되고, " +
                    "제출하면 휴식 칸으로 돌아가요. 제출하면 인맥이 최대 8 오르고, 가산점은 중간·기말 평균에 더해져요.",
                color = Color(0xFF7A8290), fontSize = 12.sp, lineHeight = 18.sp
            )
        } else {
            PooangCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PooangImage(PooangMood.HEHE, size = 64.dp)
                    Column {
                        Text("진행 중인 조별과제가 없어요", color = PooangColors.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        Text("매 학기 4·11주차에 시작해서 3주 동안 진행돼요.", color = Color(0xFF7A8290), fontSize = 12.sp)
                    }
                }
            }
        }

        if (game.teamBonuses.isNotEmpty() || game.semesterGpaAdjustment != 0.0) {
            PooangCard(title = "이번 학기 가산점", titleIcon = R.drawable.pooang_ic_spec, titleIconTint = PooangColors.UpText) {
                game.teamBonuses.forEach { (id, bonus) ->
                    InfoLine(subjectForId(id)?.name ?: id, "+${bonus}점", PooangColors.UpText)
                }
                if (game.semesterGpaAdjustment != 0.0) {
                    InfoLine("학기 평점 보정", String.format(Locale.KOREA, "%+.2f", game.semesterGpaAdjustment),
                        if (game.semesterGpaAdjustment > 0) PooangColors.UpText else PooangColors.Warn)
                }
            }
        }

        if (game.teamResults.isNotEmpty()) {
            PooangCard(title = "제출 기록", titleIcon = R.drawable.pooang_ic_check, titleIconTint = TeamRed) {
                game.teamResults.reversed().forEach { r ->
                    InfoLine(
                        "${semesterLabel(r.semester)} · ${r.startWeek}~${r.endWeek}주차\n${subjectForId(r.subjectId)?.name ?: r.subjectId}",
                        "+${r.bonus}점", PooangColors.UpText
                    )
                }
            }
        }

        if (!announcing) {
            PooangButton("홈으로", onClick = onBack, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectCard(project: TeamProject) {
    PooangCard(
        title = "${subjectForId(project.subjectId)?.name ?: project.subjectId} 팀 프로젝트",
        titleIcon = R.drawable.pooang_ic_team,
        titleIconTint = TeamRed
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            PooangChip("${project.startWeek}~${project.endWeek}주차", ChipTone.Even)
            PooangChip("남은 진행 ${project.weeksLeft}주", ChipTone.Even)
            PooangChip("조모임 주 ${project.meetingIndices.size}칸", ChipTone.Down)
            PooangChip("제출 시 가산점 +${project.bonus}점", ChipTone.of(project.bonus))
            val m = project.studyMultiplier
            PooangChip(String.format(Locale.KOREA, "공부 효율 ×%.2f", m), ChipTone.of(((m - 1.0) * 100).toInt()))
        }
    }
}

/** 조원 목록 (`.member`): 성향에 맞는 표정 + 이름 + 설명. */
@Composable
private fun MembersCard(project: TeamProject) {
    PooangCard(title = "조원", trailing = {
        Text("${project.members.size}명", color = PooangColors.TextSub, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            project.members.forEach { member ->
                val good = member.bonus > 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White)
                        .border(1.dp, PooangColors.Line, RoundedCornerShape(11.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (good) Color(0xFFE9F5EE) else Color(0xFFFDF0EF)),
                        contentAlignment = Alignment.Center
                    ) {
                        PooangImage(memberMood(member.id), size = 36.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(member.name, color = Color(0xFF22262E), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        Text(member.description, color = Color(0xFF7A8290), fontSize = 11.sp)
                    }
                    PooangChip(if (good) "든든" else "주의", if (good) ChipTone.Up else ChipTone.Down, fontSize = 10f)
                }
            }
        }
    }
}

/** 프로토타입의 조원별 표정 (`mood: 'sad' | 'tired' | 'happy'`) */
private fun memberMood(id: String): PooangMood = when (id) {
    "ppt", "good" -> PooangMood.HAPPY
    "namu" -> PooangMood.TIRED
    "ego" -> PooangMood.ANGRY
    else -> PooangMood.SAD
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeetingsCard(project: TeamProject) {
    PooangCard(title = "고정 조모임", titleIcon = R.drawable.pooang_ic_cal, titleIconTint = Color(0xFF3B82C4)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            project.meetingIndices.forEach { index ->
                Text(
                    "${GameEngine.dayNames[index / 4]} ${DayLog.blockNames[index % 4]}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(TeamRed)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun InfoLine(label: String, value: String, valueColor: Color = PooangColors.Text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PooangColors.TextBody, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp,
            modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Preview(name = "조별과제 - 발표", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun TeamAnnouncePreview() {
    val project = TeamProject(1, 4, "cal", listOf("ghost", "ppt", "namu"), listOf(5, 14, 22), bonus = 5)
    PooangTheme { TeamScreen(GameState(teamProject = project), {}, {}) }
}
