package com.example.mobile_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountScreen(session: GameSession, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    // 비밀번호는 화면 저장이나 게임 백업에 넣지 않는다.
    var password by remember { mutableStateOf("") }
    val user = session.onlineUser
    val busy = session.onlineBusy
    val preview = session.cloudPreview
    LaunchedEffect(user?.uid) { if (user != null) password = "" }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("계정 · 클라우드 저장", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A3A1E))
        Text("이 기기의 게임", fontWeight = FontWeight.Bold)
        Text(if (session.hasGame) SaveSummary.from(session.gameState.value).label else "저장된 게임이 없어요.")
        Text("로그인하면 게임 진행과 현재 시간표를 계정에 백업할 수 있어요. 저장해 둔 루틴 목록은 이 기기에 보관돼요.", fontSize = 14.sp)

        if (!session.onlineConfigured) {
            Text("온라인 기능이 아직 연결되지 않았어요. 설정 후 다시 실행하면 로그인할 수 있어요.")
            Text("로컬 게임과 자동 저장은 계속 이용할 수 있어요.")
        } else if (user == null) {
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("이메일") },
                singleLine = true, enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("비밀번호") },
                singleLine = true, enabled = !busy, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
            Button(onClick = { session.authenticate(email, password, false) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("로그인")
            }
            OutlinedButton(onClick = { session.authenticate(email, password, true) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("이 이메일로 회원가입")
            }
            Text("회원가입 비밀번호는 6자 이상이에요. 로그인 후에도 기기의 게임을 자동으로 바꾸지 않아요.", fontSize = 13.sp)
        } else {
            Text("로그인: ${user.email}", fontWeight = FontWeight.Bold)
            val remote = session.cloudRecord
            Text("클라우드", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            if (remote != null) {
                Text(remote.summary.label)
                if (remote.updatedAtMs > 0L) {
                    Text("저장 시각: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(remote.updatedAtMs))}", fontSize = 13.sp)
                }
            } else Text("'저장 내용 새로고침'으로 서버에 저장이 있는지 확인할 수 있어요.")
            OutlinedButton(onClick = { session.refreshCloud() }, enabled = !busy && preview == null, modifier = Modifier.fillMaxWidth()) {
                Text("저장 내용 새로고침")
            }
            Button(onClick = { session.prepareCloudTransfer(CloudAction.UPLOAD) },
                enabled = !busy && preview == null && session.hasGame && session.loadError == null, modifier = Modifier.fillMaxWidth()) {
                Text("이 기기의 게임을 클라우드에 저장")
            }
            OutlinedButton(onClick = { session.prepareCloudTransfer(CloudAction.DOWNLOAD) },
                enabled = !busy && preview == null, modifier = Modifier.fillMaxWidth()) {
                Text("클라우드 게임을 이 기기에 불러오기")
            }
            if (preview != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("적용할 내용 확인", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("기기: ${if (session.hasGame) SaveSummary.from(preview.local.game).label else "저장 없음"}")
                        Text("클라우드: ${preview.remote?.summary?.label ?: "저장 없음"}")
                        Text(if (preview.action == CloudAction.UPLOAD)
                            "기기의 게임으로 이 계정의 클라우드 저장을 바꿔요. 기기 진행은 유지돼요."
                        else "클라우드 게임으로 이 기기의 진행과 현재 시간표를 바꿔요. 저장한 루틴 목록은 유지돼요.")
                        Button(onClick = { session.confirmCloudTransfer() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(if (preview.action == CloudAction.UPLOAD) {
                                if (preview.remote == null) "이 내용으로 클라우드에 저장" else "클라우드 저장 덮어쓰기"
                            } else if (session.hasGame) "기기의 게임을 이 내용으로 바꾸기" else "게임 불러오기")
                        }
                        OutlinedButton(onClick = { session.cancelCloudPreview() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("취소") }
                    }
                }
            }
            OutlinedButton(onClick = { session.signOut() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("로그아웃") }
            Text("로그아웃해도 이 기기의 게임과 루틴은 남아요.", fontSize = 13.sp)
        }
        if (busy) Text("처리 중이에요…", fontWeight = FontWeight.Bold)
        if (session.onlineStatus.isNotBlank()) Text(session.onlineStatus, color = Color(0xFF2E7D52))
        session.onlineError?.let { Text(it, color = Color(0xFFC0504D)) }
        OutlinedButton(onClick = onBack, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("돌아가기") }
    }
}
