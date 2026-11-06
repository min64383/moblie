package com.example.mobile_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun TeamScreen(game: GameState, onConfirm: (String) -> Unit, onBack: () -> Unit) {
    val project = game.teamProject
    val blocks = listOf("오전", "오후", "저녁", "밤")

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("조별과제", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A3A1E))

        if (project != null) {
            Text("${subjectForId(project.subjectId)?.name ?: project.subjectId} 팀 프로젝트",
                fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("${project.startWeek}~${project.endWeek}주차 · 남은 진행 ${project.weeksLeft}주")
            Text("주당 조모임 ${project.meetingIndices.size}칸 · 제출 시 과목 가산점 +${project.bonus}점")
            Text(String.format(Locale.KOREA, "해당 과목 공부 효율 ×%.2f", project.studyMultiplier))
            Text("조원", fontWeight = FontWeight.Bold)
            project.members.forEachIndexed { index, member ->
                Text("${index + 1}. ${member.name}", fontWeight = FontWeight.Bold)
                Text(member.description)
            }
            Text("고정 조모임", fontWeight = FontWeight.Bold)
            project.meetingIndices.forEach { index ->
                Text("${GameEngine.dayNames[index / 4]} ${blocks[index % 4]}")
            }
            Text("수업과 밤 시간은 유지하고 자유 활동 칸을 조모임으로 바꿨어요. " +
                "조모임은 제출까지 고정되며, 제출 후에는 휴식 칸으로 돌아가요.", fontSize = 13.sp)
            Text("조원 소식은 하루 결과에 표시돼요. 제출하면 인맥이 최대 8 오르고, " +
                "가산점은 중간·기말 평균에 더해져 학기 최종 성적에 반영돼요.", fontSize = 13.sp)

            if (!project.announced) {
                GameStatusPanel(game, game.study.keys.mapNotNull(::subjectForId))
                Button(onClick = { onConfirm(project.key) }, modifier = Modifier.fillMaxWidth()) {
                    Text("조원 확인 · 시간표 보기")
                }
            }
        } else {
            Text("진행 중인 조별과제가 없어요.")
            Text("매 학기 4·11주차에 시작하고 3주간 진행돼요.")
        }

        if (game.teamBonuses.isNotEmpty()) {
            Text("이번 학기 과목 가산점", fontWeight = FontWeight.Bold)
            game.teamBonuses.forEach { (id, bonus) ->
                Text("${subjectForId(id)?.name ?: id}: +${bonus}점")
            }
        }
        if (game.semesterGpaAdjustment != 0.0) {
            Text(String.format(Locale.KOREA, "이번 학기 평점 보정 %+.2f", game.semesterGpaAdjustment))
        }
        if (game.teamResults.isNotEmpty()) {
            Text("제출 기록", fontWeight = FontWeight.Bold)
            game.teamResults.reversed().forEach { result ->
                Text("${semesterLabel(result.semester)} · ${result.startWeek}~${result.endWeek}주차\n" +
                    "${subjectForId(result.subjectId)?.name ?: result.subjectId}: 가산점 +${result.bonus}점")
            }
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
    }
}
