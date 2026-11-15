package com.example.mobile_project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mobile_project.PooangMood
import com.example.mobile_project.R

/**
 * 표정 푸앙이. 표정 PNG 는 약 160px 이라 64~96dp 로 쓰면 선명하다.
 *
 * 예) `PooangImage(PooangMood.of(game), size = 84.dp, bob = true)`
 */
@Composable
fun PooangImage(
    mood: PooangMood,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    bob: Boolean = false
) {
    PooangImage(mood.drawable, "푸앙이 (${mood.label})", modifier, size, bob)
}

/** 특정 푸앙이 그림 (타이틀·홈·졸업·프로필 등). */
@Composable
fun PooangImage(
    @DrawableRes drawable: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    bob: Boolean = false
) {
    Image(
        painter = painterResource(drawable),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .then(if (bob) Modifier.bob() else Modifier)
            .size(size)
    )
}

/** 자주 쓰는 큰 그림 */
object PooangArt {
    @DrawableRes val Title = R.drawable.pooang_title
    @DrawableRes val Main = R.drawable.pooang_main
    @DrawableRes val Face = R.drawable.pooang_face
    @DrawableRes val Graduation = R.drawable.pooang_grad
}
