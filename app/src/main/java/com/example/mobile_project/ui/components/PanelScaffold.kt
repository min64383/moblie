package com.example.mobile_project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mobile_project.ui.theme.PooangColors

/**
 * 버튼으로 여는 전체 화면 패널 (`.panel`): 머리 줄(.ph) + 스크롤 본문(.pb) + 아래 고정 영역(.pf).
 * 상태 표시줄·내비게이션 바 여백을 스스로 처리하므로 화면 전체에 그대로 놓으면 된다.
 *
 * @param footer 아래에 고정할 버튼 영역. null 이면 없음.
 */
@Composable
fun PanelScaffold(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    iconTint: Color = PooangColors.StatIcon,
    caption: String? = null,
    closable: Boolean = true,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxSize().background(PooangColors.Panel)) {
        PanelHeader(
            title = title,
            onClose = onClose,
            icon = icon,
            iconTint = iconTint,
            caption = caption,
            insetStatusBar = true,
            closable = closable
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
                .then(if (footer == null) Modifier.navigationBarsPadding() else Modifier),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            content = content
        )
        if (footer != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawBehind { drawRect(PooangColors.LineSoft, size = Size(size.width, 2.dp.toPx())) }
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                content = footer
            )
        }
    }
}
