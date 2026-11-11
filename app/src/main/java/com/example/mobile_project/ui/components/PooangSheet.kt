package com.example.mobile_project.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.theme.PooangColors

/**
 * 아래에서 올라오는 시트 (`.sheet`). 화면 전체를 덮는 Box 안, 맨 마지막에 놓아야 다른 내용 위로 보인다.
 * 뒤 배경을 누르거나 뒤로 가기를 누르면 [onDismiss] 가 불린다.
 *
 * 예)
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     Content()
 *     PooangSheet(visible = open, onDismiss = { open = false }, title = "공부 현황") { ... }
 * }
 * ```
 */
@Composable
fun PooangSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.84f).dp

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(PooangColors.Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(240)) { it },
            exit = slideOutVertically(tween(200)) { it }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(PooangColors.Panel)
                    // 시트 안을 눌러도 뒤 배경 클릭으로 닫히지 않게 막는다.
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 15.dp, end = 15.dp, top = 16.dp, bottom = 26.dp)
            ) {
                if (title != null) {
                    Text(
                        title,
                        color = PooangColors.Text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                }
                content()
            }
        }
    }
}

/**
 * 시트 안 선택지 한 줄 (`.opt`). 아이콘 + 이름 + 효과 설명.
 */
@Composable
fun SheetOption(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    iconTint: Color = PooangColors.StatIcon,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 7.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, PooangColors.Line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(PooangColors.Panel)
                    .border(2.dp, PooangColors.Line, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(icon),
                    null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                name,
                color = if (enabled) Color(0xFF22262E) else PooangColors.TextFaint,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (description != null) {
                Text(description, color = Color(0xFF7A8290), fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}
