package com.example.mobile_project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_project.ui.theme.PooangColors

/**
 * 기본 카드 (`.card`). 크림색 바탕 + 2dp 테두리 + 아래 3dp 그림자.
 *
 * @param title 카드 제목 (`.card h3`). 비우면 제목 줄이 없다.
 * @param titleIcon 제목 왼쪽 아이콘
 * @param trailing 제목 줄 오른쪽에 붙일 내용 (예: "2/3" 같은 진행도)
 */
@Composable
fun PooangCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    @DrawableRes titleIcon: Int? = null,
    titleIconTint: Color = PooangColors.StatIcon,
    background: Color = PooangColors.Card,
    borderColor: Color = PooangColors.Line,
    contentPadding: Dp = 14.dp,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .padding(bottom = 3.dp)
            .hardShadow(borderColor, 3.dp, 18.dp)
            .clip(shape)
            .background(background)
            .border(2.dp, borderColor, shape)
            .padding(contentPadding)
    ) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (titleIcon != null) {
                    Icon(painterResource(titleIcon), null, tint = titleIconTint, modifier = Modifier.size(18.dp))
                }
                Text(
                    title,
                    color = PooangColors.Text,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke(this)
            }
        }
        content()
    }
}

/**
 * 나무 판자 느낌의 진한 갈색 패널 (`.pm-date`, `.pm-row`, `.wd-head`).
 * 안의 글자는 [PooangColors.WoodText] 계열을 쓰면 잘 보인다.
 */
@Composable
fun WoodPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    contentPadding: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = modifier
            .padding(bottom = 3.dp)
            .hardShadow(PooangColors.WoodShadow, 3.dp, corner)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PooangColors.WoodTop, PooangColors.WoodBottom)))
            .border(3.dp, PooangColors.WoodBorder, shape)
            .padding(contentPadding),
        content = content
    )
}

/**
 * 둥근 사각형 아이콘 타일 (`.sbi`, `.blk .bi`, `.vac .vi`, `.pchip .sq`).
 * 활동별 색 바탕 + 흰 아이콘에 아래쪽 안쪽 그림자가 들어간다.
 */
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    iconSize: Dp = size * 0.6f,
    corner: Dp = size * 0.3f,
    tint: Color = Color.White,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(background)
            .insetBottomShade(),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** 말풍선 꼬리 방향 */
enum class BubbleTail { None, Left, Bottom }

/**
 * 말풍선 (`.stage-bubble`, `.bubble`). 흰 바탕 + 주황빛 테두리 + 꼬리.
 * 꼬리는 [BubbleTail.Bottom] 이면 아래 가운데, [BubbleTail.Left] 면 왼쪽 위에 붙는다.
 */
@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    tail: BubbleTail = BubbleTail.Bottom,
    borderColor: Color = PooangColors.Accent,
    textColor: Color = PooangColors.Text,
    textAlign: TextAlign = if (tail == BubbleTail.Bottom) TextAlign.Center else TextAlign.Start
) {
    val tailSize = 8.dp
    val shape = BubbleShape(tail, corner = 16.dp, tailSize = tailSize)
    Box(
        modifier = modifier
            .background(Color.White, shape)
            .border(2.5.dp, borderColor, shape)
            .padding(
                start = 11.dp + if (tail == BubbleTail.Left) tailSize else 0.dp,
                end = 11.dp,
                top = 8.dp,
                bottom = 8.dp + if (tail == BubbleTail.Bottom) tailSize else 0.dp
            )
    ) {
        Text(
            text,
            color = textColor,
            fontSize = 12.5.sp,
            lineHeight = 17.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = textAlign
        )
    }
}

/** 둥근 사각형 몸통 + 삼각형 꼬리를 하나로 합친 모양. 테두리가 꼬리까지 이어진다. */
private class BubbleShape(
    private val tail: BubbleTail,
    private val corner: Dp,
    private val tailSize: Dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { corner.toPx() }
        val t = with(density) { tailSize.toPx() }
        val body = when (tail) {
            BubbleTail.None -> Rect(0f, 0f, size.width, size.height)
            BubbleTail.Left -> Rect(t, 0f, size.width, size.height)
            BubbleTail.Bottom -> Rect(0f, 0f, size.width, size.height - t)
        }
        val bodyPath = Path().apply { addRoundRect(RoundRect(body, CornerRadius(r))) }
        if (tail == BubbleTail.None) return Outline.Generic(bodyPath)

        val tailPath = Path().apply {
            when (tail) {
                BubbleTail.Bottom -> {
                    val cx = size.width / 2
                    moveTo(cx - t, body.bottom - 1f)
                    lineTo(cx, size.height)
                    lineTo(cx + t, body.bottom - 1f)
                }
                else -> { // Left: 위에서 19dp 쯤 (프로토타입 `.bubble:before`)
                    val cy = minOf(with(density) { 24.dp.toPx() }, body.height / 2)
                    moveTo(body.left + 1f, cy - t)
                    lineTo(0f, cy)
                    lineTo(body.left + 1f, cy + t)
                }
            }
            close()
        }
        return Outline.Generic(Path.combine(PathOperation.Union, bodyPath, tailPath))
    }
}

/**
 * 버튼으로 여는 화면의 머리 줄 (`.ph`). 제목 + 보조 글자 + 닫기(×) 버튼.
 */
@Composable
fun PanelHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    iconTint: Color = PooangColors.StatIcon,
    caption: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(PooangColors.HudTop, PooangColors.HudBottom)))
            .drawBehind {
                val h = 3.dp.toPx()
                drawRect(PooangColors.Accent, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
            }
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (icon != null) {
                Icon(painterResource(icon), null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Text(title, color = PooangColors.Text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (caption != null) {
            Text(caption, color = PooangColors.TextSub, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        CircleIconButton(onClick = onClose, contentDescription = "닫기", label = "×")
    }
}
