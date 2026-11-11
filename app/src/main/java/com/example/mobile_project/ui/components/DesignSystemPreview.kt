package com.example.mobile_project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.PooangMood
import com.example.mobile_project.R
import com.example.mobile_project.ui.theme.PooangColors
import com.example.mobile_project.ui.theme.PooangTheme

/**
 * 공통 컴포넌트 모음. Android Studio 의 Preview(Split/Design 탭)에서 한눈에 확인한다.
 * 실제 화면에는 쓰이지 않는다.
 */
@Composable
fun DesignSystemGallery() {
    var sheetOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(PooangColors.Panel)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text("디자인 시스템", color = PooangColors.Text, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)

            PooangCard(title = "버튼", titleIcon = R.drawable.pooang_ic_play) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PooangButton("수강신청 완료", onClick = {}, modifier = Modifier.fillMaxWidth())
                    PooangButton("타이틀로", onClick = {}, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost)
                    PooangButton("한 주 시작하기", onClick = { sheetOpen = true }, modifier = Modifier.fillMaxWidth(),
                        style = PooangButtonStyle.Go, icon = R.drawable.pooang_ic_play)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PooangButton("포기", onClick = {}, modifier = Modifier.weight(1f), style = PooangButtonStyle.Warn, small = true)
                        PooangButton("제출", onClick = {}, modifier = Modifier.weight(1f), style = PooangButtonStyle.Gold, small = true)
                        PooangButton("비활성", onClick = {}, modifier = Modifier.weight(1f), small = true, enabled = false)
                    }
                }
            }

            PooangCard(title = "HUD 스탯", trailing = {
                CircleIconButton(onClick = {}, contentDescription = "메뉴", icon = R.drawable.pooang_ic_menu)
            }) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    StatTile(R.drawable.pooang_ic_hp, "체력", "80/100", Modifier.weight(1f), 0.8f, PooangColors.Hp)
                    StatTile(R.drawable.pooang_ic_hap, "행복도", "60", Modifier.weight(1f), 0.6f, PooangColors.Happy)
                    StatTile(R.drawable.pooang_ic_spec, "스펙", "12", Modifier.weight(1f), 0.12f, PooangColors.Spec)
                    StatTile(R.drawable.pooang_ic_net, "인맥", "5", Modifier.weight(1f), 0.05f, PooangColors.Network)
                    StatTile(R.drawable.pooang_ic_charm, "매력", "10", Modifier.weight(1f), 0.1f, PooangColors.Charm)
                    StatTile(R.drawable.pooang_ic_money, "돈", "35만", Modifier.weight(1f), valueColor = PooangColors.MoneyFlat)
                }
            }

            PooangCard(title = "칩·막대") {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DeltaChip("체력", -5)
                    DeltaChip("행복도", 3)
                    DeltaChip("돈", 0)
                }
                ProgressTrack(0.66f, Modifier.padding(top = 10.dp))
            }

            PooangCard(title = "말풍선·아이콘") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PooangImage(PooangMood.HAPPY, size = 74.dp, bob = true)
                    SpeechBubble("오늘도 힘내 보자!", Modifier.weight(1f), tail = BubbleTail.Left)
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconTile(R.drawable.pooang_ic_study, PooangColors.Hp)
                    IconTile(R.drawable.pooang_ic_work, PooangColors.Gold)
                    IconTile(R.drawable.pooang_ic_fun, PooangColors.PinkBottom)
                    IconTile(R.drawable.pooang_ic_sleep, PooangColors.CauBlue)
                    NoticeDot()
                }
            }

            PooangCard(title = "표정 ${PooangMood.entries.size}종") {
                PooangMood.entries.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { mood ->
                            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                PooangImage(mood, size = 64.dp)
                                Text(mood.label, color = PooangColors.TextSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            WoodPanel {
                Text("3월 1주 · 월요일", color = PooangColors.WoodText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text("나무 패널", color = PooangColors.WoodSub, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        PooangSheet(visible = sheetOpen, onDismiss = { sheetOpen = false }, title = "바텀시트") {
            SheetOption("공부", onClick = { sheetOpen = false }, description = "선택 과목 공부량 +", icon = R.drawable.pooang_ic_study)
            SheetOption("알바", onClick = { sheetOpen = false }, description = "돈 +, 체력 -", icon = R.drawable.pooang_ic_work)
        }
    }
}

@Preview(name = "디자인 시스템", showBackground = true, widthDp = 380, heightDp = 1500)
@Composable
private fun DesignSystemGalleryPreview() {
    PooangTheme { DesignSystemGallery() }
}

@Preview(name = "패널 머리", showBackground = true, widthDp = 380)
@Composable
private fun PanelHeaderPreview() {
    PooangTheme {
        PanelHeader(title = "공부 현황", onClose = {}, icon = R.drawable.pooang_ic_study, caption = "1주차")
    }
}
