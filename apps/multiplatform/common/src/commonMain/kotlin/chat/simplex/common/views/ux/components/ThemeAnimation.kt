package chat.simplex.common.views.ux.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import chat.simplex.common.model.ChatController
import chat.simplex.common.ui.theme.DefaultTheme
import chat.simplex.common.ui.theme.ThemeManager
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

object ThemeAnimationController {
    val isAnimating = mutableStateOf(false)
    val origin = mutableStateOf(Offset(1000f, 150f))
    val targetIsDark = mutableStateOf(false)
    val animProgress = Animatable(0f)

    fun trigger(originOffset: Offset, currentlyDark: Boolean, scope: CoroutineScope) {
        val newIsDark = !currentlyDark
        origin.value = originOffset
        targetIsDark.value = newIsDark
        isAnimating.value = true

        scope.launch {
            animProgress.snapTo(0f)
            val job = launch {
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
            }
            delay(180)
            val targetTheme: String = if (newIsDark) {
                ChatController.appPrefs.systemDarkTheme.get() ?: DefaultTheme.DARK.themeName
            } else {
                DefaultTheme.LIGHT.themeName
            }
            ThemeManager.applyTheme(targetTheme)
            job.join()
            isAnimating.value = false
        }
    }
}

@Composable
fun AnimatedThemeIcon(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDark) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f)
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        if (isDark) {
            Icon(
                painter = painterResource(MR.images.ic_light_mode),
                contentDescription = "Mode Clair",
                tint = Color(0xFFE2B755),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                painter = painterResource(MR.images.ic_bedtime_moon),
                contentDescription = "Mode Sombre",
                tint = Color(0xFFD97706),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ThemeCircularRevealOverlay() {
    if (ThemeAnimationController.isAnimating.value) {
        val progress = ThemeAnimationController.animProgress.value
        val origin = ThemeAnimationController.origin.value
        val targetIsDark = ThemeAnimationController.targetIsDark.value
        val targetBgColor = if (targetIsDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        val ringColor = if (targetIsDark) Color(0xFFE2B755) else Color(0xFFD97706)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(9999f)
        ) {
            val maxRadius = hypot(size.width, size.height) * 1.25f
            val currentRadius = maxRadius * progress

            // Expanding circle filling the screen with new theme color
            drawCircle(
                color = targetBgColor,
                radius = currentRadius,
                center = origin
            )

            // Dynamic glow ring propagating at the wave edge
            if (progress in 0.01f..0.96f) {
                drawCircle(
                    color = ringColor.copy(alpha = (1f - progress * 0.8f).coerceIn(0f, 1f)),
                    radius = currentRadius,
                    center = origin,
                    style = Stroke(width = 6.dp.toPx() * (1f - progress * 0.5f))
                )
            }
        }
    }
}
