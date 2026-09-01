package chat.simplex.common.views.ux.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import chat.simplex.common.ui.theme.*
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
fun MineralPullToRefreshIndicator(
    pullFraction: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isInDarkTheme()
    val scale = if (isRefreshing) 1f else pullFraction.coerceIn(0f, 1f)
    val alpha = if (isRefreshing) 1f else (pullFraction * 1.5f).coerceIn(0f, 1f)

    if (pullFraction > 0.05f || isRefreshing) {
        Box(
            modifier = modifier
                .scale(scale)
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xF01E293B) else Color(0xF5FFFFFF))
                .border(1.dp, if (isDark) Color(0x40E2B755) else Color(0x30F59E0B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                // #99: the rotation clock exists only while actually refreshing -
                // created here, inside the branch, so the home screen carries no
                // infinite frame-clock wakeup while the indicator is invisible.
                val infiniteTransition = rememberInfiniteTransition()
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
                // Haute Horlogerie rotating gold/cyan gear spinner
                Canvas(modifier = Modifier.size(24.dp).rotate(rotation)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0x00E2B755),
                                Color(0x6638BDF8),
                                AmberGold
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            } else {
                // Progressive arc filling with pull gesture
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawArc(
                        color = if (isDark) AmberGold else AmberGold,
                        startAngle = -90f,
                        sweepAngle = (pullFraction * 360f).coerceIn(0f, 360f),
                        useCenter = false,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                Icon(
                    painter = painterResource(MR.images.ic_arrow_downward),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate((pullFraction * 180f).coerceIn(0f, 180f)),
                    tint = if (isDark) AmberGold else AmberGold
                )
            }
        }
    }
}
