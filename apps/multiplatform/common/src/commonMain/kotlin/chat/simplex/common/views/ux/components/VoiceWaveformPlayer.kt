package chat.simplex.common.views.ux.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import kotlin.math.roundToInt

/**
 * Playback speed cycle for voice notes: 1.0x -> 1.5x -> 2.0x -> 1.0x.
 * Any unexpected value restarts the cycle at normal speed (issue #13).
 */
fun nextPlaybackSpeed(current: Float): Float = when (current) {
    1.0f -> 1.5f
    1.5f -> 2.0f
    else -> 1.0f
}

@Composable
fun VoiceWaveformPlayer(
    isPlaying: Boolean,
    progress: Float, // 0.0 to 1.0
    durationFormatted: String,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    // Hoisted: the speed belongs to the shared player session, not to this composable's
    // local state, so it survives scrolling the item out of composition (issue #13).
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    // #13: uniform ticks, not a fake amplitude envelope - SimpleX does not
    // carry per-message waveform data (MCVoice has text+duration only), so
    // varied heights would be decoration pretending to be signal. The bar is
    // still a real progress/seek control; real envelopes are tracked in the
    // follow-up issue.
    waveformData: List<Float> = remember { List(40) { 0.5f } }
) {
    val isDark = isInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Play / Pause Circle Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF2563EB))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onPlayPauseToggle()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(if (isPlaying) MR.images.ic_pause_filled else MR.images.ic_play_arrow_filled),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Interactive Waveform Bars & Time
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val seekRatio = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek(seekRatio)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    waveformData.forEachIndexed { index, heightRatio ->
                        val barProgress = index.toFloat() / waveformData.size.toFloat()
                        val isPassed = barProgress <= progress
                        val activeColor = Color(0xFF60A5FA)
                        val inactiveColor = if (isDark) Color(0x55FFFFFF) else Color(0x33000000)

                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((24.dp * heightRatio.coerceIn(0.2f, 1f)))
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(if (isPassed) activeColor else inactiveColor)
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = durationFormatted,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }

        // Speed Toggle Button (1x / 1.5x / 2x)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0x331E293B) else Color(0x140F172A))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0x22FFFFFF) else Color(0x1A000000),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onPlaybackSpeedChange(nextPlaybackSpeed(playbackSpeed))
                }
                .padding(horizontal = 7.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${playbackSpeed}x",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
            )
        }
    }
}
