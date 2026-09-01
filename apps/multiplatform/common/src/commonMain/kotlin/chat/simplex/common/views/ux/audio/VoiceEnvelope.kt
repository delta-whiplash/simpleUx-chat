package chat.simplex.common.views.ux.audio

import androidx.compose.runtime.Composable
import chat.simplex.common.model.CryptoFile

/** Number of amplitude buckets in a decoded voice envelope (~Telegram-style granularity). */
const val VOICE_ENVELOPE_BUCKETS = 40

/**
 * Real amplitude envelope for a voice note, decoded on-device from the local file (#91).
 *
 * SimpleX carries no per-message waveform data (MCVoice = text + duration only), so the
 * envelope can only be computed by decoding the audio where the file exists:
 * - Android: MediaExtractor + MediaCodec, seek-based sampling on a background dispatcher,
 *   cached per file (path + size + mtime) so chat scroll never blocks and repeated rows
 *   don't re-decode. Encrypted files are decrypted in memory before decoding.
 * - Desktop: skipped by design - always returns an empty envelope.
 *
 * Returns [VOICE_ENVELOPE_BUCKETS] values normalized to 0..1, or an empty list whenever no
 * real envelope is available (desktop, file missing / not downloaded yet, no audio track,
 * no decoder, decode error, fully silent audio). Callers must render the uniform-tick
 * fallback in that case - never a synthesized fake envelope.
 */
@Composable
expect fun rememberVoiceEnvelope(file: CryptoFile?, durationMs: Long): List<Float>
