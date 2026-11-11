package com.example.mobile_project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.theme.PooangColors

/** 버튼 종류. 프로토타입의 `.btn`, `.btn.ghost`, `.btn.warn`, `.btn.gold`, `.btn.go.ready` 에 해당. */
enum class PooangButtonStyle(
    val face: Brush,
    val content: Color,
    val shadow: Color,
    val border: BorderStroke?
) {
    /** 기본 주황 버튼 */
    Primary(Brush.verticalGradient(listOf(PooangColors.OrangeTop, PooangColors.OrangeBottom)), Color.White, PooangColors.OrangeShadow, null),

    /** 흰 바탕 보조 버튼 */
    Ghost(Brush.verticalGradient(listOf(Color.White, Color.White)), PooangColors.GhostText, PooangColors.GhostShadow, BorderStroke(2.dp, PooangColors.GhostBorder)),

    /** 위험·포기 버튼 */
    Warn(Brush.verticalGradient(listOf(PooangColors.Warn, PooangColors.Warn)), Color.White, PooangColors.WarnShadow, null),

    /** 시험 제출 등 강조 버튼 */
    Gold(Brush.verticalGradient(listOf(PooangColors.Gold, PooangColors.Gold)), Color.White, PooangColors.GoldShadow, null),

    /** "한 주 시작하기" 같은 진행 버튼 (분홍) */
    Go(Brush.verticalGradient(listOf(PooangColors.PinkTop, PooangColors.PinkBottom)), Color.White, PooangColors.PinkShadow, null)
}

/**
 * 누르면 아래로 쑥 들어가는 게임 버튼.
 * 그림자 두께만큼 아래 공간을 미리 차지하므로 눌러도 주변 레이아웃이 흔들리지 않는다.
 *
 * @param small 작은 버튼 (`.btn.sm`): 높이·글자·그림자가 줄어든다.
 * @param icon 글자 왼쪽에 넣을 res/drawable 아이콘 (선택)
 */
@Composable
fun PooangButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PooangButtonStyle = PooangButtonStyle.Primary,
    small: Boolean = false,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = if (style == PooangButtonStyle.Go && !small) 4.dp else if (small) 2.dp else 3.dp
    val corner = if (small) 10.dp else 16.dp
    val shift by animateDpAsState(if (pressed && enabled) depth else 0.dp, tween(70), label = "press")
    val shape = RoundedCornerShape(corner)

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .padding(bottom = depth)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        propagateMinConstraints = true
    ) {
        Box(
            modifier = Modifier
                .offset(y = shift)
                .hardShadow(style.shadow, depth - shift, corner)
                .clip(shape)
                .background(style.face)
                .then(if (style.border != null) Modifier.border(style.border, shape) else Modifier)
                .padding(vertical = if (small) 9.dp else 14.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = style.content,
                        modifier = Modifier.size(if (small) 16.dp else 20.dp)
                    )
                }
                Text(
                    text = text,
                    color = style.content,
                    fontSize = if (small) 13.sp else 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 흰 원형 아이콘 버튼. HUD 메뉴 버튼(`.hud-menu`)과 패널 닫기 버튼(`.px`)에 쓴다.
 * [icon] 이 없으면 [label] 글자(예: "×")를 가운데에 그린다.
 */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    label: String = "",
    size: Dp = 36.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 2.dp
    val shift by animateDpAsState(if (pressed) depth else 0.dp, tween(70), label = "press")

    Box(
        modifier = modifier
            .size(size + depth)
            .padding(bottom = depth)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = shift)
                .hardShadow(PooangColors.Accent, depth - shift, size / 2)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.5.dp, PooangColors.Accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = PooangColors.TextMid,
                    modifier = Modifier.size(size / 2)
                )
            } else {
                Text(label, color = PooangColors.TextMid, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
