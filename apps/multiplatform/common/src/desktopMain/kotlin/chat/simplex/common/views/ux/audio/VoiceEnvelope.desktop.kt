package chat.simplex.common.views.ux.audio

import androidx.compose.runtime.Composable
import chat.simplex.common.model.CryptoFile

/**
 * Desktop actual for #91: envelope decoding is deliberately skipped (per the issue,
 * "Desktop: skip"). Always reports "no envelope" so the renderer shows the uniform-tick
 * fallback - an honest flat bar rather than a synthesized fake envelope.
 */
@Composable
actual fun rememberVoiceEnvelope(file: CryptoFile?, durationMs: Long): List<Float> = emptyList()
