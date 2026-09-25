package noox.bzr.media

import android.media.MediaRecorder

/** BR-014 — contract shared with iOS and enforced again by the server. */
object AudioRecordingSpec {
    const val fileExtension = "m4a"
    const val mimeType = "audio/mp4"
    const val channelCount = 1
    const val bitRate = 64_000
    const val maxDurationMillis = 120_000
    const val maxFileSizeBytes = 5L * 1024 * 1024
    const val outputFormat = MediaRecorder.OutputFormat.MPEG_4
    const val encoder = MediaRecorder.AudioEncoder.AAC
}
