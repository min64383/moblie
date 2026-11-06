package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MorningScreen(
    game: GameState,
    onBegin: (String) -> Unit,
    onTap: (String, Int) -> Unit,
    onRefresh: (String, Long) -> Unit,
    onSkip: (String) -> Unit,
    onContinue: (String) -> Unit,
    onHome: () -> Unit
) {
    val run = game.life.pendingMorning ?: return
    var now by remember(run.key, run.startedAtMs) { mutableStateOf(System.currentTimeMillis()) }
    val refresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(run.key, run.phase, run.startedAtMs) {
        if (run.phase != MorningPhase.PLAY) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            refresh(run.key, now)
            delay(50L)
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("${GameEngine.dayNames[run.day]} 아침", fontSize = 18.sp)
        Text("늦잠!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC0504D))
        Text("체력이 떨어져 알람을 놓쳤어요. 오전 ${run.activity.label}에 늦을 것 같아요.")
        when (run.phase) {
            MorningPhase.READY -> {
                Text("4초 안에 달리기를 22번 누르면 제시간에 도착할 수 있어요.", fontSize = 19.sp)
                if (run.subjectId != null) Text("실패하면 ${subjectForId(run.subjectId)?.name ?: run.subjectId}의 이번 시험 계산에서 3점이 깎여요.")
                else Text("오전 수업이 아니므로 실패해도 과목 성적 감점은 없어요.")
                Button(onClick = { onBegin(run.key) }, modifier = Modifier.fillMaxWidth()) { Text("달리기 시작") }
                OutlinedButton(onClick = { onSkip(run.key) }, modifier = Modifier.fillMaxWidth()) {
                    Text("자동 판정으로 건너뛰기 (성공 확률 50%)")
                }
            }
            MorningPhase.PLAY -> {
                val left = ((run.deadlineMs - now).coerceAtLeast(0L) + 999L) / 1_000L
                Text("남은 시간 ${left}초 · ${run.taps}/${LifeEngine.MORNING_TAPS}회", fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFFC0504D))
                Box(Modifier.fillMaxWidth().height(24.dp).background(Color(0xFFE9D8C0))) {
                    Box(Modifier.fillMaxWidth(run.taps.toFloat() / LifeEngine.MORNING_TAPS).fillMaxHeight()
                        .background(Color(0xFF2E9183)))
                }
                Button(onClick = { onTap(run.key, run.taps) }, modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    Text("달리기!", fontSize = 25.sp)
                }
                OutlinedButton(onClick = { onSkip(run.key) }, modifier = Modifier.fillMaxWidth()) { Text("포기하고 결과 보기") }
            }
            MorningPhase.RESULT -> {
                Text(if (run.success == true) "간신히 도착했어요!" else "결국 지각했어요", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                game.life.morningNotice?.let { Text(LifeEngine.attendanceMessage(it)) }
                Button(onClick = { onContinue(run.key) }, modifier = Modifier.fillMaxWidth()) { Text("이어서 하루 진행") }
            }
        }
        Text("시작한 4초는 홈으로 나가거나 앱을 종료해도 계속 흘러요.", fontSize = 13.sp)
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
    }
}
