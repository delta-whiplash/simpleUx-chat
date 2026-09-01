package chat.simplex.common.views.ux.components

import chat.simplex.common.ui.theme.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import chat.simplex.common.platform.SimpleUXHapticType
import chat.simplex.common.platform.performHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

enum class EmojiBurstType {
    HEARTS,
    FIREWORKS,
    CONFETTI,
    FIRE,
    THUMBS_UP
}

data class Particle(
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val color: Color
)

object FullscreenEmojiEffectManager {
    val isPlaying = mutableStateOf(false)
    val currentType = mutableStateOf(EmojiBurstType.HEARTS)
    val animProgress = Animatable(0f)
    val particles = mutableStateListOf<Particle>()

    fun trigger(emoji: String, scope: CoroutineScope) {
        val type = when (emoji.trim()) {
            "❤️", "💖", "💕", "💘" -> EmojiBurstType.HEARTS
            "🎉", "🥳", "🎊" -> EmojiBurstType.CONFETTI
            "🔥" -> EmojiBurstType.FIRE
            "💥", "🎆", "✨" -> EmojiBurstType.FIREWORKS
            "👍" -> EmojiBurstType.THUMBS_UP
            else -> return
        }

        currentType.value = type
        particles.clear()

        // Generate 40 particles with random spread
        val rnd = Random(Clock.System.now().toEpochMilliseconds())
        for (i in 0 until 40) {
            val angle = rnd.nextDouble(0.0, Math.PI * 2)
            val speed = rnd.nextFloat() * 700f + 250f
            val vx = (kotlin.math.cos(angle) * speed).toFloat()
            val vy = (kotlin.math.sin(angle) * speed).toFloat() - 250f
            val color = when (type) {
                EmojiBurstType.HEARTS -> listOf(Color(0xFFFF2D55), Color(0xFFFF375F), Color(0xFFFF6482), Color(0xFFFFB3C6)).random(rnd)
                EmojiBurstType.FIREWORKS -> listOf(AmberGold, Sky400, Emerald500, Color(0xFFA855F7), Color(0xFFF43F5E)).random(rnd)
                EmojiBurstType.CONFETTI -> listOf(AmberGold, Color(0xFF3B82F6), Emerald500, Color(0xFFEC4899), Color(0xFF8B5CF6)).random(rnd)
                EmojiBurstType.FIRE -> listOf(Color(0xFFFF4500), Color(0xFFFF8C00), Color(0xFFFFD700), Color(0xFFFF3300)).random(rnd)
                EmojiBurstType.THUMBS_UP -> listOf(Sky400, Color(0xFF0284C7), AmberGold).random(rnd)
            }
            particles.add(
                Particle(
                    velocityX = vx,
                    velocityY = vy,
                    size = rnd.nextFloat() * 12f + 8f,
                    color = color
                )
            )
        }

        isPlaying.value = true
        performHapticFeedback(SimpleUXHapticType.SUCCESS)

        scope.launch {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
            )
            isPlaying.value = false
        }
    }
}

@Composable
fun FullscreenEmojiEffectOverlay() {
    if (FullscreenEmojiEffectManager.isPlaying.value) {
        val progress = FullscreenEmojiEffectManager.animProgress.value
        val particles = FullscreenEmojiEffectManager.particles

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(9998f)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val alpha = (1f - progress).coerceIn(0f, 1f)

            particles.forEach { p ->
                val x = cx + p.velocityX * progress * (1f - progress * 0.3f)
                val gravity = 980f * progress * progress
                val y = cy + p.velocityY * progress + gravity

                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = (p.size * (1f - progress * 0.4f)).coerceAtLeast(1f),
                    center = Offset(x, y)
                )
            }
        }
    }
}
