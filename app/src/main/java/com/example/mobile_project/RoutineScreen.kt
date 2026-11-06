package com.example.mobile_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoutineScreen(
    session: GameSession,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf("") }

    val selected = session.routines.find { it.id == selectedId }
    val available = !session.routineBusy &&
            session.routineError == null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "시간표 루틴",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "현재 수업은 그대로 두고 자유 시간의 활동만 저장하고 적용해요."
        )

        if (!session.editable) {
            Text("시간표 저장·적용은 주간 진행 전에 할 수 있어요.")
        }

        session.routineError?.let { error ->
            Text(error)
            OutlinedButton(
                onClick = { session.retryRoutines() },
                enabled = !session.routineBusy
            ) {
                Text("다시 불러오기")
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = {
                if (it.length <= 30) name = it
            },
            label = { Text("루틴 이름") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { session.createRoutine(name) },
            enabled = available &&
                    session.editable &&
                    name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("현재 시간표를 새 루틴으로 저장")
        }

        Text("저장된 루틴", fontWeight = FontWeight.Bold)

        if (session.routines.isEmpty() && session.routineError == null) {
            Text("저장된 루틴이 없어요.")
        }

        session.routines.forEach { routine ->
            OutlinedButton(
                onClick = {
                    selectedId = routine.id
                    name = routine.name
                },
                enabled = available,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (routine.id == selectedId) {
                        "✓ ${routine.name}"
                    } else {
                        routine.name
                    }
                )
            }
        }

        selected?.let { routine ->
            Text(
                "선택: ${routine.name}",
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { session.applyRoutine(routine.id) },
                enabled = available && session.editable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("시간표에 적용")
            }

            OutlinedButton(
                onClick = {
                    session.renameRoutine(routine.id, name)
                },
                enabled = available && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("입력한 이름으로 변경")
            }

            OutlinedButton(
                onClick = {
                    session.updateRoutine(routine.id)
                },
                enabled = available && session.editable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("현재 시간표로 내용 수정")
            }

            OutlinedButton(
                onClick = {
                    session.deleteRoutine(routine.id)
                },
                enabled = available,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("선택한 루틴 삭제")
            }
        }

        if (session.routineBusy) Text("처리 중이에요.")

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("홈으로")
        }
    }
}