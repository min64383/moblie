package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.PanelScaffold
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme

/** 루틴 화면. 실제 동작은 [GameSession] 에 그대로 맡기고 화면만 그린다. */
@Composable
fun RoutineScreen(
    session: GameSession,
    onBack: () -> Unit
) {
    RoutineContent(
        routines = session.routines,
        editable = session.editable,
        busy = session.routineBusy,
        error = session.routineError,
        onBack = onBack,
        onRetry = { session.retryRoutines() },
        onCreate = { session.createRoutine(it) },
        onApply = { session.applyRoutine(it) },
        onRename = { id, name -> session.renameRoutine(id, name) },
        onUpdate = { session.updateRoutine(it) },
        onDelete = { session.deleteRoutine(it) }
    )
}

/**
 * 루틴 패널. 저장된 루틴을 고르면 미니 시간표로 미리 보여 주고, 적용·이름 변경·덮어쓰기·삭제를 할 수 있다.
 * (세션 없이도 미리보기할 수 있도록 값과 동작을 따로 받는다)
 */
@Composable
fun RoutineContent(
    routines: List<ScheduleRoutine>,
    editable: Boolean,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCreate: (String) -> Unit,
    onApply: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onUpdate: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf("") }
    val selected = routines.find { it.id == selectedId }
    val available = !busy && error == null

    PanelScaffold(
        title = "시간표 루틴",
        onClose = onBack,
        icon = R.drawable.pooang_ic_max,
        iconTint = Color(0xFFE0A33E),
        caption = if (busy) "처리 중…" else "${routines.size}개 저장됨"
    ) {
        Text(
            "수업·조모임·데이트 칸은 그대로 두고, 자유 시간에 넣은 활동만 저장하고 적용해요.",
            color = Color(0xFF7A8290),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        if (!editable) {
            Notice("시간표 저장·적용은 한 주를 시작하기 전에만 할 수 있어요.")
        }

        if (error != null) {
            Notice(error)
            PooangButton("다시 불러오기", onClick = onRetry, enabled = !busy, style = PooangButtonStyle.Ghost, small = true,
                modifier = Modifier.fillMaxWidth())
        }

        PooangCard(title = "새 루틴 저장", titleIcon = R.drawable.pooang_ic_save, titleIconTint = Color(0xFF4A5A72)) {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                placeholder = { Text("예: 시험 기간 루틴", color = PooangColors.TextFaint) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PooangColors.OrangeBottom,
                    unfocusedBorderColor = PooangColors.Line,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PooangColors.OrangeBottom
                ),
                modifier = Modifier.fillMaxWidth()
            )
            PooangButton(
                "지금 시간표를 새 루틴으로 저장",
                onClick = { onCreate(name) },
                enabled = available && editable && name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }

        PooangCard(title = "저장된 루틴", titleIcon = R.drawable.pooang_ic_cal, titleIconTint = Color(0xFF3B82C4)) {
            if (routines.isEmpty() && error == null) {
                Text("아직 저장된 루틴이 없어요.", color = PooangColors.TextSub, fontSize = 12.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                routines.forEach { routine ->
                    RoutineRow(
                        routine = routine,
                        selected = routine.id == selectedId,
                        enabled = available,
                        onClick = {
                            selectedId = routine.id
                            name = routine.name
                        }
                    )
                }
            }
        }

        selected?.let { routine ->
            PooangCard(title = "선택: ${routine.name}") {
                PooangButton(
                    "시간표에 적용",
                    onClick = { onApply(routine.id) },
                    enabled = available && editable,
                    modifier = Modifier.fillMaxWidth(),
                    style = PooangButtonStyle.Go,
                    icon = R.drawable.pooang_ic_check
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PooangButton("이름 바꾸기", onClick = { onRename(routine.id, name) },
                        enabled = available && name.isNotBlank(), style = PooangButtonStyle.Ghost, small = true,
                        modifier = Modifier.weight(1f))
                    PooangButton("지금 시간표로 덮기", onClick = { onUpdate(routine.id) },
                        enabled = available && editable, style = PooangButtonStyle.Ghost, small = true,
                        modifier = Modifier.weight(1f))
                }
                PooangButton("삭제", onClick = {
                    onDelete(routine.id)
                    selectedId = ""
                }, enabled = available, style = PooangButtonStyle.Warn, small = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Text(
                    "이름 바꾸기는 위 입력칸에 쓴 이름으로 바뀌어요.",
                    color = PooangColors.TextFaint,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Text(
        text,
        color = Color(0xFF7A3835),
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFDF0EF))
            .border(1.5.dp, Color(0xFFF0C8C5), RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp)
    )
}

/** 루틴 한 줄: 이름 + 7×4 미니 시간표. 고르면 주황 테두리. */
@Composable
private fun RoutineRow(routine: ScheduleRoutine, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color(0xFFFFF4E4) else Color.White)
            .border(2.dp, if (selected) PooangColors.OrangeBottom else PooangColors.Line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MiniSchedule(routine.slots)
        Column(Modifier.weight(1f)) {
            Text(routine.name, color = Color(0xFF22262E), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            val filled = routine.slots.count { it.activity != ActivityType.EMPTY }
            val sleep = routine.slots.count { it.activity == ActivityType.SLEEP }
            Text("자유 시간 ${filled}칸 · 수면 ${sleep}칸", color = Color(0xFF7A8290), fontSize = 11.sp)
        }
        if (selected) {
            Icon(painterResource(R.drawable.pooang_ic_check), null, tint = PooangColors.OrangeBottom, modifier = Modifier.size(18.dp))
        }
    }
}

/** 작은 7×4 색 격자 미리보기 */
@Composable
private fun MiniSchedule(slots: List<TimeSlot>) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
        repeat(7) { day ->
            Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                repeat(4) { block ->
                    val slot = slots.getOrNull(day * 4 + block) ?: TimeSlot()
                    Box(
                        Modifier
                            .width(7.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (slot.activity == ActivityType.EMPTY) Color(0xFFEDE7DA)
                                else ActivityStyle.color(slot.activity)
                            )
                    )
                }
            }
        }
    }
}

@Preview(name = "루틴", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun RoutinePreview() {
    val slots = List(28) { i ->
        when {
            i % 4 == 3 -> TimeSlot(ActivityType.SLEEP)
            i % 5 == 0 -> TimeSlot(ActivityType.WORK)
            i % 3 == 0 -> TimeSlot(ActivityType.REST)
            else -> TimeSlot()
        }
    }
    PooangTheme {
        RoutineContent(
            routines = listOf(ScheduleRoutine("a", "시험 기간 루틴", slots), ScheduleRoutine("b", "알바 많이", slots.reversed())),
            editable = true, busy = false, error = null,
            onBack = {}, onRetry = {}, onCreate = {}, onApply = {}, onRename = { _, _ -> }, onUpdate = {}, onDelete = {}
        )
    }
}
