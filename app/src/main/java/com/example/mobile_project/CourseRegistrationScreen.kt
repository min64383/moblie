package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.components.ProgressTrack
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import java.text.NumberFormat
import java.util.Locale

private val SelectedBlue = Color(0xFF0B4DA2)
private val SelectedBg = Color(0xFFEDF3FB)

/**
 * 수강신청 화면 (`#sc-reg`). 과목 카드를 눌러 3과목을 고른다.
 * 아래 고정 영역에 "선택 n/3" 막대와 완료 버튼이 있다.
 */
@Composable
fun CourseRegistrationScreen(
    selectedIds: List<String>,
    subjects: List<Subject>,
    semester: Int,
    scholarship: Int,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(PooangColors.Panel)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.padding(start = 2.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${semesterLabel(semester)} 수강신청",
                        color = PooangColors.Text,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "3과목을 고르세요. 수업 시간은 자동 배정됩니다.",
                        color = Color(0xFF7A8290),
                        fontSize = 12.sp
                    )
                }
                PooangImage(PooangMood.STUDY, size = 64.dp)
            }

            if (scholarship > 0) {
                val amount = NumberFormat.getIntegerInstance(Locale.KOREA).format(scholarship)
                PooangCard(background = Color(0xFFEAF7E0), borderColor = Color(0xFFB8E0A0), contentPadding = 11.dp) {
                    Text(
                        "직전 학기 장학금 ${amount}원을 받았어요!",
                        color = PooangColors.UpText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            subjects.forEach { subject ->
                val checked = subject.id in selectedIds
                SubjectRow(
                    subject = subject,
                    checked = checked,
                    enabled = checked || selectedIds.size < 3,
                    onToggle = { onToggle(subject.id) }
                )
            }
        }

        // 아래 고정 영역
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(PooangColors.LineSoft, size = Size(size.width, 2.dp.toPx())) }
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "선택 ${selectedIds.size}/3",
                    color = PooangColors.TextMid,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                ProgressTrack(selectedIds.size / 3f, Modifier.weight(1f))
            }
            PooangButton(
                "수강신청 완료",
                onClick = onConfirm,
                enabled = selectedIds.size == 3,
                modifier = Modifier.fillMaxWidth()
            )
            PooangButton(
                "타이틀로",
                onClick = onBack,
                style = PooangButtonStyle.Ghost,
                small = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 과목 한 줄 (`.subj`). 고르면 파란 테두리 + 오른쪽 체크. */
@Composable
private fun SubjectRow(subject: Subject, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(if (checked) SelectedBg else Color.White)
            .border(2.dp, if (checked) SelectedBlue else PooangColors.Line, shape)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFEEF3FA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.pooang_ic_study), null, tint = Color(0xFF3B82C4), modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(subject.name, color = Color(0xFF22262E), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(subject.description, color = Color(0xFF7A8290), fontSize = 11.sp)
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (checked) SelectedBlue else Color.Transparent)
                .border(2.dp, if (checked) SelectedBlue else Color(0xFFC8C2B2), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(painterResource(R.drawable.pooang_ic_check), null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Preview(name = "수강신청", showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun CourseRegistrationPreview() {
    PooangTheme {
        CourseRegistrationScreen(
            selectedIds = listOf("cal", "intro"),
            subjects = subjectsForSemester(1),
            semester = 1,
            scholarship = 500_000,
            onToggle = {}, onConfirm = {}, onBack = {}
        )
    }
}
