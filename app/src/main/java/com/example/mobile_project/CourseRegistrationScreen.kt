package com.example.mobile_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CourseRegistrationScreen(
    selectedIds: List<String>,
    subjects: List<Subject>,
    semester: Int,
    scholarship: Int,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${semesterLabel(semester)} 수강신청", fontSize = 26.sp,
            fontWeight = FontWeight.Bold, color = Color(0xFF5A3A1E))
        Text("이번 학기에 들을 3과목을 선택하세요.")
        if (scholarship > 0) {
            val amount = NumberFormat.getIntegerInstance(Locale.KOREA).format(scholarship)
            Text("직전 학기 장학금 ${amount}원을 받았어요.", color = Color(0xFF2E7D52))
        }
        Text("선택한 과목: ${selectedIds.size}/3", fontWeight = FontWeight.Bold)
        subjects.forEach { subject ->
            val checked = subject.id in selectedIds
            val canToggle = checked || selectedIds.size < 3
            Row(
                Modifier.fillMaxWidth().toggleable(
                    value = checked,
                    enabled = canToggle,
                    role = Role.Checkbox,
                    onValueChange = { onToggle(subject.id) }
                ).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = canToggle)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(subject.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(subject.description, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        Button(onClick = onConfirm, enabled = selectedIds.size == 3, modifier = Modifier.fillMaxWidth()) {
            Text("수강신청 완료")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("타이틀로") }
    }
}
