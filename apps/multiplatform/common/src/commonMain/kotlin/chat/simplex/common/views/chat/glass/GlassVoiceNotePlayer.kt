package chat.simplex.common.views.chat.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.GlassTokens
import chat.simplex.common.ui.theme.glassSurface
import kotlin.math.sin

/**
 * Glassmorphism-styled voice note player with animated spectrum visualizer.
 *
 * Features:
 * - Glass pill container matching sent/received bubble styling
 * - Circular play/pause button with indigo fill
 * - 18-bar animated frequency spectrum (Canvas)
 * - Playback speed selector cycling 1.0x → 1.5x → 2.0x
 * - Duration display
 *
 * This component renders the *visual* player. It accepts playback state
 * from external callers that integrate with the existing [AudioPlayer].
 */
@Composable
fun GlassVoiceNotePlayer(
    durationText: String,
    isOutgoing: Boolean,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    currentSpeed: String,
    onToggleSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .width(280.dp)
                .glassSurface(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = if (isOutgoing) {
                        GlassTokens.SentBubblePrimary.copy(alpha = 0.30f)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    },
                    borderColor = if (isOutgoing) {
                        GlassTokens.SentBubbleAccent.copy(alpha = 0.35f)
                    } else {
                        Color.White.copy(alpha = 0.15f)
                    }
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassTokens.VoicePlayButtonBg)
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Animated frequency spectrum (18 bars)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
            ) {
                val barCount = 18
                val barWidth = 3.dp.toPx()
                val gap = (size.width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

                // Pre-defined bar height factors to give a natural waveform shape
                val defaultHeights = listOf(
                    0.3f, 0.6f, 0.9f, 0.4f, 0.7f, 1.0f, 0.5f, 0.8f,
                    0.4f, 0.65f, 0.95f, 0.35f, 0.75f, 0.5f, 0.85f, 0.3f, 0.6f, 0.4f
                )

                for (i in 0 until barCount) {
                    val factor = defaultHeights.getOrElse(i) { 0.5f }
                    val animatedFactor = if (isPlaying) {
                        (factor + 0.3f * sin(wavePhase * 2 * Math.PI.toFloat() + i)).coerceIn(0.2f, 1.0f)
                    } else {
                        factor
                    }
                    val barHeight = size.height * animatedFactor
                    val x = i * (barWidth + gap)
                    val y = (size.height - barHeight) / 2

                    drawRoundRect(
                        color = if (i < 8) GlassTokens.VoiceBarActive else GlassTokens.VoiceBarInactive.copy(alpha = GlassTokens.VoiceBarInactiveAlpha),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Speed selector + duration
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleSpeed() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = currentSpeed,
                        color = GlassTokens.VoiceSpeedText,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
