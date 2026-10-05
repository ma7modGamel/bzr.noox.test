package noox.bzr.media

import android.content.Context
import android.media.MediaRecorder
import java.io.File

/** BR-014 recorder: AAC/.m4a, mono, 64kbps, at most 120 seconds and 5MB. */
class AndroidAudioRecorder {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    fun start(context: Context, onLimitReached: () -> Unit): File {
        check(recorder == null) { "Audio recording is already active." }
        val file = File.createTempFile("bremo-audio-", ".${AudioRecordingSpec.fileExtension}", context.cacheDir)
        val next = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(AudioRecordingSpec.outputFormat)
            setAudioEncoder(AudioRecordingSpec.encoder)
            setAudioChannels(AudioRecordingSpec.channelCount)
            setAudioEncodingBitRate(AudioRecordingSpec.bitRate)
            setMaxDuration(AudioRecordingSpec.maxDurationMillis)
            setMaxFileSize(AudioRecordingSpec.maxFileSizeBytes)
            setOutputFile(file.absolutePath)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                ) {
                    onLimitReached()
                }
            }
            prepare()
            start()
        }
        output = file
        recorder = next

        return file
    }

    fun stop(): File? {
        val file = output
        val active = recorder ?: return null
        return try {
            active.stop()
            file?.takeIf { it.isFile && it.length() in 1..AudioRecordingSpec.maxFileSizeBytes }
        } catch (_: RuntimeException) {
            file?.delete()
            null
        } finally {
            active.release()
            recorder = null
            output = null
        }
    }

    fun cancel() {
        val active = recorder ?: return
        runCatching { active.stop() }
        active.release()
        recorder = null
        output?.delete()
        output = null
    }

    val isRecording: Boolean get() = recorder != null
}
