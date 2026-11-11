package com.example.mobile_project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.PooangArt
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.components.SummaryTile
import com.example.mobile_project.ui.components.hardShadow
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * 졸업 엔딩 화면 (`#sc-end`, `.ending-badge`).
 * 금빛 배지(햇살 무늬) 안에 졸업 푸앙이와 진로 이름, 아래에 최종 스탯·학기별 평점·방학 기록.
 * 졸업 유예·번아웃이면 졸업 푸앙이 대신 그에 맞는 표정을 보여 준다.
 */
@Composable
fun GraduationScreen(game: GameState, onTitle: () -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
    val ending = AcademicEngine.graduationResult(game)
    val graduated = ending.title != "졸업 유예"

    Column(
        modifier
            .fillMaxSize()
            .background(PooangColors.Panel)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        EndingBadge(ending, graduated, game)

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SummaryTile("누적 평점", "${gpaText(game.cumulativeGpa)} / 4.5", Modifier.weight(1f))
            SummaryTile("스펙", "${game.spec}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SummaryTile("인맥", "${game.network}", Modifier.weight(1f))
            SummaryTile("최종 잔액", NumberFormat.getIntegerInstance(Locale.KOREA).format(game.money) + "원", Modifier.weight(1f))
        }

        game.love?.let { love ->
            PooangCard(background = Color(0xFFFFF1F5), borderColor = Color(0xFFF5C2D3), contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PooangImage(PooangMood.LOVE, size = 44.dp)
                    Text("${love.partner}와 ${love.weeksTogether}주째 연애 중이에요.", color = Color(0xFFA8395C),
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        SemesterHistoryCard(game.completedSemesters)

        if (game.vacationHistory.isNotEmpty()) {
            PooangCard(title = "방학 기록", titleIcon = R.drawable.pooang_ic_plane, titleIconTint = Color(0xFF3B82C4)) {
                game.vacationHistory.forEach { record ->
                    InfoLine(vacationLabel(record.semester),
                        AcademicEngine.vacationOption(record.activityId)?.title ?: record.activityId, PooangColors.GhostText)
                }
            }
        }

        PooangButton("타이틀로", onClick = onTitle, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Gold,
            icon = R.drawable.pooang_ic_home)
        PooangButton("홈으로", onClick = onHome, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
    }
}

/** 금빛 엔딩 배지. 뒤에 햇살 무늬(10° 간격 흰 부채꼴)를 그린다. */
@Composable
private fun EndingBadge(ending: GraduationResult, graduated: Boolean, game: GameState) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .hardShadow(Color(0xFFD9A066), 4.dp, 22.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF3D6), Color(0xFFFFDCA0))))
            .border(3.dp, PooangColors.Accent, shape)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width / 2, size.height * 0.32f)
            val r = size.width + size.height
            for (i in 0 until 18) {
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = i * 20f,
                    sweepAngle = 10f,
                    useCenter = true,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2, r * 2)
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.height(170.dp).padding(top = 10.dp), contentAlignment = Alignment.Center) {
                val bob = !LocalInspectionMode.current
                if (graduated && game.happiness >= 15) {
                    PooangImage(PooangArt.Graduation, "졸업한 푸앙이", size = 140.dp, bob = bob)
                } else {
                    PooangImage(if (graduated) PooangMood.TIRED else PooangMood.SAD, size = 122.dp, bob = bob)
                }
            }
            Text(
                "졸업 심사 결과",
                color = Color(0xFFFFE3B5),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.verticalGradient(listOf(PooangColors.WoodTop, PooangColors.WoodBottom)))
                    .padding(horizontal = 14.dp, vertical = 3.dp)
            )
            Text(
                ending.title,
                style = TextStyle(
                    color = PooangColors.Text,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    shadow = Shadow(Color.White, Offset(0f, 4f), 0f)
                ),
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
            )
            Text(ending.description, color = PooangColors.TextMid, fontSize = 12.5.sp, lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 22.dp))
            Text("이수 학기 ${game.completedSemesters.size}/8", color = PooangColors.TextSub, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 9.dp))
        }
    }
}

@Preview(name = "졸업 엔딩", showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun GraduationPreview() {
    val grades = subjectsForSemester(1).take(3).map { SubjectGrade(it.id, 90, "A", 4.0) }
    val transcript = (1..8).map { SemesterResult(it, grades) }
    PooangTheme {
        GraduationScreen(
            GameState(semester = 8, spec = 62, network = 40, money = 1_250_000, happiness = 70, transcript = transcript,
                vacationHistory = (1..7).map { VacationRecord(it, listOf("work", "rest", "trip", "cert")[it % 4]) }),
            {}, {}
        )
    }
}
