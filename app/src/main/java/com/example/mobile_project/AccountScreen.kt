package com.example.mobile_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.components.PanelScaffold
import com.example.mobile_project.ui.components.PooangButton
import com.example.mobile_project.ui.components.PooangButtonStyle
import com.example.mobile_project.ui.components.PooangCard
import com.example.mobile_project.ui.components.PooangImage
import com.example.mobile_project.ui.theme.PooangColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 계정 · 클라우드 저장 패널. 로그인/회원가입, 클라우드 저장·불러오기(확인 단계 포함), 로그아웃.
 * 동작은 모두 [GameSession] 에 맡기고, 이 화면은 버튼과 상태만 그린다. (예전 화면과 같은 조건)
 */
@Composable
fun AccountScreen(session: GameSession, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    // 비밀번호는 화면 저장이나 게임 백업에 넣지 않는다.
    var password by remember { mutableStateOf("") }
    val user = session.onlineUser
    val busy = session.onlineBusy
    val preview = session.cloudPreview
    LaunchedEffect(user?.uid) { if (user != null) password = "" }

    PanelScaffold(
        title = "계정 · 클라우드 저장",
        onClose = { if (!busy) onBack() },
        icon = R.drawable.pooang_ic_globe,
        iconTint = Color(0xFF3B82C4),
        caption = if (busy) "처리 중…" else null
    ) {
        // 이 기기의 게임
        PooangCard(title = "이 기기의 게임", titleIcon = R.drawable.pooang_ic_save, titleIconTint = Color(0xFF4A5A72)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PooangImage(if (session.hasGame) PooangMood.OK else PooangMood.HEHE, size = 48.dp)
                Text(
                    if (session.hasGame) SaveSummary.from(session.gameState.value).label else "저장된 게임이 없어요.",
                    color = PooangColors.TextBody, fontSize = 13.5.sp, fontWeight = FontWeight.Bold
                )
            }
            Text(
                "로그인하면 게임 진행과 현재 시간표를 계정에 백업할 수 있어요. 저장해 둔 루틴 목록은 이 기기에 보관돼요.",
                color = Color(0xFF7A8290), fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (!session.onlineConfigured) {
            Notice("온라인 기능이 아직 연결되지 않았어요. 설정 후 다시 실행하면 로그인할 수 있어요.\n로컬 게임과 자동 저장은 계속 이용할 수 있어요.", Tone.Info)
        } else if (user == null) {
            PooangCard(title = "로그인", titleIcon = R.drawable.pooang_ic_globe, titleIconTint = Color(0xFF3B82C4)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it }, label = { Text("이메일") },
                        singleLine = true, enabled = !busy, shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, label = { Text("비밀번호") },
                        singleLine = true, enabled = !busy, shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    PooangButton("로그인", onClick = { session.authenticate(email, password, false) }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    PooangButton("이 이메일로 회원가입", onClick = { session.authenticate(email, password, true) }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost, small = true)
                    Text("회원가입 비밀번호는 6자 이상이에요. 로그인 후에도 기기의 게임을 자동으로 바꾸지 않아요.",
                        color = Color(0xFF7A8290), fontSize = 11.5.sp, lineHeight = 17.sp)
                }
            }
        } else {
            val remote = session.cloudRecord
            PooangCard(
                title = "클라우드",
                titleIcon = R.drawable.pooang_ic_globe,
                titleIconTint = Color(0xFF3B82C4),
                trailing = { Text(user.email.orEmpty(), color = PooangColors.TextSub, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
            ) {
                if (remote != null) {
                    Text(remote.summary.label, color = PooangColors.TextBody, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    if (remote.updatedAtMs > 0L) {
                        Text("저장 시각: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(remote.updatedAtMs))}",
                            color = PooangColors.TextSub, fontSize = 11.5.sp)
                    }
                } else {
                    Text("'저장 내용 새로고침'으로 서버에 저장이 있는지 확인할 수 있어요.", color = Color(0xFF7A8290), fontSize = 12.sp)
                }
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PooangButton("이 기기의 게임을 클라우드에 저장",
                        onClick = { session.prepareCloudTransfer(CloudAction.UPLOAD) },
                        enabled = !busy && preview == null && session.hasGame && session.loadError == null,
                        modifier = Modifier.fillMaxWidth(), icon = R.drawable.pooang_ic_save)
                    PooangButton("클라우드 게임을 이 기기에 불러오기",
                        onClick = { session.prepareCloudTransfer(CloudAction.DOWNLOAD) },
                        enabled = !busy && preview == null, modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Ghost)
                    PooangButton("저장 내용 새로고침", onClick = { session.refreshCloud() },
                        enabled = !busy && preview == null, modifier = Modifier.fillMaxWidth(),
                        style = PooangButtonStyle.Ghost, small = true)
                }
            }

            if (preview != null) {
                PooangCard(
                    title = "적용할 내용 확인",
                    titleIcon = R.drawable.pooang_ic_warn,
                    titleIconTint = PooangColors.Gold,
                    background = Color(0xFFFFF8E8),
                    borderColor = PooangColors.Accent
                ) {
                    InfoLine("기기", if (session.hasGame) SaveSummary.from(preview.local.game).label else "저장 없음")
                    InfoLine("클라우드", preview.remote?.summary?.label ?: "저장 없음")
                    Text(
                        if (preview.action == CloudAction.UPLOAD) "기기의 게임으로 이 계정의 클라우드 저장을 바꿔요. 기기 진행은 유지돼요."
                        else "클라우드 게임으로 이 기기의 진행과 현재 시간표를 바꿔요. 저장한 루틴 목록은 유지돼요.",
                        color = PooangColors.Warn, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 8.dp)
                    )
                    PooangButton(
                        if (preview.action == CloudAction.UPLOAD) {
                            if (preview.remote == null) "이 내용으로 클라우드에 저장" else "클라우드 저장 덮어쓰기"
                        } else if (session.hasGame) "기기의 게임을 이 내용으로 바꾸기" else "게임 불러오기",
                        onClick = { session.confirmCloudTransfer() }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Gold
                    )
                    PooangButton("취소", onClick = { session.cancelCloudPreview() }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp), style = PooangButtonStyle.Ghost, small = true)
                }
            }

            PooangButton("로그아웃", onClick = { session.signOut() }, enabled = !busy,
                modifier = Modifier.fillMaxWidth(), style = PooangButtonStyle.Warn, small = true)
            Text("로그아웃해도 이 기기의 게임과 루틴은 남아요.", color = PooangColors.TextFaint, fontSize = 11.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        if (busy) Notice("처리 중이에요…", Tone.Info)
        if (session.onlineStatus.isNotBlank()) Notice(session.onlineStatus, Tone.Good)
        session.onlineError?.let { Notice(it, Tone.Bad) }

        PooangButton("돌아가기", onClick = onBack, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            style = PooangButtonStyle.Ghost, small = true)
    }
}

private enum class Tone { Good, Bad, Info }

@Composable
private fun Notice(text: String, tone: Tone) {
    val (bg, fg) = when (tone) {
        Tone.Good -> Color(0xFFE9F5EE) to Color(0xFF2A5A3E)
        Tone.Bad -> Color(0xFFFDF0EF) to Color(0xFF7A3835)
        Tone.Info -> Color(0xFFEDF3FB) to Color(0xFF2B4C7E)
    }
    Text(
        text, color = fg, fontSize = 12.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 10.dp)
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PooangColors.OrangeBottom,
    unfocusedBorderColor = PooangColors.Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = PooangColors.OrangeBottom,
    focusedLabelColor = PooangColors.GhostText
)
