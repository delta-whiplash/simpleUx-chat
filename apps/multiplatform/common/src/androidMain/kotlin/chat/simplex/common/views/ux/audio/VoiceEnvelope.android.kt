package chat.simplex.common.views.ux.audio

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import chat.simplex.common.model.CryptoFile
import chat.simplex.common.model.readCryptoFile
import chat.simplex.common.platform.CryptoMediaSource
import chat.simplex.common.platform.Log
import chat.simplex.common.platform.getAppFilePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "VoiceEnvelope"

/**
 * PCM window decoded per bucket. Bounding the per-bucket window bounds the whole pass:
 * total decoded audio is ~[VOICE_ENVELOPE_BUCKETS] x [SAMPLE_WINDOW_US] regardless of the
 * recording length, so a 5-minute voice note costs the same one-time (cached) decode as a
 * 10-second one. 250ms is enough PCM to expose the level of a bucket visually.
 */
private const val SAMPLE_WINDOW_US = 250_000L

/** Timeout for codec buffer dequeue calls; short so the loop stays responsive to window bounds. */
private const val DEQUEUE_TIMEOUT_US = 10_000L

/**
 * Hard iteration cap per bucket (feed + drain cycles). Normally the loop exits via the
 * "feeding done + codec idle" condition; the cap only guards against pathological decoders.
 */
private const val MAX_ITERATIONS_PER_BUCKET = 512

/** Consecutive idle dequeues (after feeding is done) before a bucket is considered drained. */
private const val IDLE_CYCLES_BEFORE_NEXT_BUCKET = 3

/** Decoded envelopes kept in memory; each is ~40 floats, so this is trivially small. */
private const val CACHE_CAPACITY = 32

/** Cache key: file identity including content stats, so a re-downloaded/changed file re-decodes. */
private data class EnvelopeCacheKey(val path: String, val length: Long, val lastModified: Long)

private val envelopeCacheLock = Any()

private val envelopeCache = object : LinkedHashMap<EnvelopeCacheKey, List<Float>>(16, 0.75f, true) {
  override fun removeEldestEntry(eldest: MutableMap.MutableEntry<EnvelopeCacheKey, List<Float>>): Boolean =
    size > CACHE_CAPACITY
}

@Composable
actual fun rememberVoiceEnvelope(file: CryptoFile?, durationMs: Long): List<Float> {
  // Two File stat calls (size + mtime) - cheap enough for composition, and they make the
  // cache honest about the file content (path alone would survive an in-place overwrite).
  val key = remember(file) { file?.toEnvelopeCacheKey() }
  var envelope by remember(key) {
    val cached = key?.let { k -> synchronized(envelopeCacheLock) { envelopeCache[k] } }
    mutableStateOf(cached ?: emptyList())
  }
  LaunchedEffect(key) {
    val k = key ?: return@LaunchedEffect
    val f = file ?: return@LaunchedEffect
    if (envelope.isNotEmpty()) return@LaunchedEffect
    // Decode off the main thread; composition keeps running with the uniform fallback
    // (empty list) until real buckets arrive.
    val decoded = withContext(Dispatchers.Default) { decodeEnvelope(f, durationMs) }
    if (decoded.isNotEmpty()) {
      synchronized(envelopeCacheLock) { envelopeCache[k] = decoded }
      envelope = decoded
    }
    // Empty result stays uncached so a retry is possible (e.g. file still downloading);
    // decode failures are rare and cheap relative to wrongly freezing a fallback.
  }
  return envelope
}

private fun CryptoFile.toEnvelopeCacheKey(): EnvelopeCacheKey? = try {
  val absolutePath = if (isAbsolutePath) filePath else getAppFilePath(filePath)
  val f = File(absolutePath)
  if (f.isFile) EnvelopeCacheKey(f.absolutePath, f.length(), f.lastModified()) else null
} catch (e: Exception) {
  null
}

/** Any failure means "no envelope" - the UI falls back to uniform ticks (#91). */
private fun decodeEnvelope(file: CryptoFile, durationMs: Long): List<Float> {
  return try {
  val absolutePath = if (file.isAbsolutePath) file.filePath else getAppFilePath(file.filePath)
  if (!File(absolutePath).isFile) return emptyList()
  val extractor = MediaExtractor()
  try {
    if (file.cryptoArgs != null) {
      // Voice notes are encrypted at rest; decrypt in memory and decode from the byte array.
      extractor.setDataSource(CryptoMediaSource(readCryptoFile(absolutePath, file.cryptoArgs)))
    } else {
      extractor.setDataSource(absolutePath)
    }
    decodeEnvelopeBuckets(extractor, durationMs)
  } finally {
    runCatching { extractor.release() }
  }
} catch (e: Exception) {
    Log.d(TAG, "envelope decode failed for ${file.filePath}: ${e.message}")
    emptyList()
  }
}

/**
 * Seek-based sampling: for each of the [VOICE_ENVELOPE_BUCKETS] buckets, seek the extractor to
 * the bucket start (closest preceding sync frame), decode at most [SAMPLE_WINDOW_US] of PCM and
 * keep the peak absolute sample. Total decode work is bounded no matter how long the recording
 * is (chosen over a full sequential pass, which would cost the whole file's duration).
 */
private fun decodeEnvelopeBuckets(extractor: MediaExtractor, fallbackDurationMs: Long): List<Float> {
  var trackIndex = -1
  var trackFormat: MediaFormat? = null
  for (i in 0 until extractor.trackCount) {
    val format = extractor.getTrackFormat(i)
    if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
      trackIndex = i
      trackFormat = format
      break
    }
  }
  if (trackIndex < 0 || trackFormat == null) return emptyList()
  val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return emptyList()
  val containerDurationUs =
    if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) trackFormat.getLong(MediaFormat.KEY_DURATION) else -1L
  // Duration from the container is authoritative; MCVoice's duration is only a fallback.
  val durationUs = if (containerDurationUs > 0) containerDurationUs else fallbackDurationMs * 1000L
  if (durationUs <= 0) return emptyList()

  extractor.selectTrack(trackIndex)
  val codec = try {
    MediaCodec.createDecoderByType(mime)
  } catch (e: Exception) {
    Log.d(TAG, "no decoder for $mime: ${e.message}")
    return emptyList()
  }

  val peaks = FloatArray(VOICE_ENVELOPE_BUCKETS)
  try {
    codec.configure(trackFormat, null, null, 0)
    codec.start()
    val bucketUs = durationUs / VOICE_ENVELOPE_BUCKETS
    val windowUs = minOf(bucketUs, SAMPLE_WINDOW_US)
    val info = MediaCodec.BufferInfo()
    for (bucket in 0 until VOICE_ENVELOPE_BUCKETS) {
      val windowStartUs = bucket * bucketUs
      val windowEndUs = windowStartUs + windowUs
      extractor.seekTo(windowStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
      codec.flush()
      var feedingDone = false
      var idleCycles = 0
      var iterations = 0
      while (iterations++ < MAX_ITERATIONS_PER_BUCKET) {
        if (!feedingDone) {
          val inputIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
          if (inputIndex >= 0) {
            val input = codec.getInputBuffer(inputIndex)!!
            val sampleSize = extractor.readSampleData(input, 0)
            if (sampleSize < 0 || extractor.sampleTime > windowEndUs) {
              // Past this bucket's window (or EOF): stop feeding. The dequeued-but-unqueued
              // input slot is reclaimed by the flush() at the start of the next bucket.
              // No EOS flag is ever queued, so the codec stays usable across buckets.
              feedingDone = true
            } else {
              codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
              extractor.advance()
            }
          }
        }
        val outputIndex = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
        if (outputIndex >= 0) {
          if (info.size > 0) {
            val output = codec.getOutputBuffer(outputIndex)!!
            val outputFormat = codec.outputFormat
            val isFloatPcm = outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
              outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
            val peak = if (isFloatPcm) floatPcmPeak(output, info) else int16PcmPeak(output, info)
            if (peak > peaks[bucket]) peaks[bucket] = peak
          }
          codec.releaseOutputBuffer(outputIndex, false)
          idleCycles = 0
        } else {
          idleCycles++
          if (feedingDone && idleCycles >= IDLE_CYCLES_BEFORE_NEXT_BUCKET) break
        }
      }
    }
  } finally {
    runCatching { codec.stop() }
    runCatching { codec.release() }
  }
  return normalizePeaks(peaks)
}

private fun int16PcmPeak(buffer: ByteBuffer, info: MediaCodec.BufferInfo): Float {
  buffer.position(info.offset)
  buffer.limit(info.offset + info.size)
  val samples = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
  var peak = 0
  while (samples.hasRemaining()) {
    val v = kotlin.math.abs(samples.get().toInt())
    if (v > peak) peak = v
  }
  return peak / 32768f
}

private fun floatPcmPeak(buffer: ByteBuffer, info: MediaCodec.BufferInfo): Float {
  buffer.position(info.offset)
  buffer.limit(info.offset + info.size)
  val samples = buffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
  var peak = 0f
  while (samples.hasRemaining()) {
    val v = kotlin.math.abs(samples.get())
    if (v > peak) peak = v
  }
  return peak.coerceIn(0f, 1f)
}

/**
 * Scale bucket peaks so the loudest bucket is 1.0. Silent audio (all-zero peaks) yields an
 * empty envelope - a flat line of zeros carries no visual information, uniform ticks are the
 * honest rendering there.
 */
private fun normalizePeaks(peaks: FloatArray): List<Float> {
  val maxPeak = peaks.maxOrNull() ?: 0f
  if (maxPeak <= 0f) return emptyList()
  return peaks.map { (it / maxPeak).coerceIn(0f, 1f) }
}
