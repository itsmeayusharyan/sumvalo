package com.acuranza.app.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.acuranza.app.designsystem.R

/** Background shape of [SumvaloLogo]. */
enum class LogoShape { Circle, RoundedSquare, None }

/**
 * The Sumvalo mark: the official logo rendered from the supplied PNG asset.
 *
 * [shape] is retained for API compatibility: [LogoShape.RoundedSquare] matches the asset's
 * baked-in rounded-square tile, [LogoShape.Circle] and [LogoShape.None] are accepted but
 * the image is rendered as-is (the PNG already carries its own clipping).
 *
 * [markColor] and [accentColor] are retained for API compatibility but have no effect
 * on the image-rendered logo — colors are baked into the asset.
 */
@Composable
fun SumvaloLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    shape: LogoShape = LogoShape.RoundedSquare,
    markColor: Color = Color.White,
    accentColor: Color = Color(0xFF0C56D8),
) {
    Image(
        painter = painterResource(id = R.drawable.acuranza_logo),
        contentDescription = "Sumvalo logo",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}
