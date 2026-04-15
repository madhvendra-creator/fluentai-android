package com.supereva.fluentai.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var audioFile: File? = null
    private var fileOutputStream: FileOutputStream? = null
    private val isRecording = AtomicBoolean(false)

    val minBufferSize: Int
        get() = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    companion object {
        const val SAMPLE_RATE = 24000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val PCM_16BIT_MAX = 32768f
    }

    data class VadConfig(
        val frameDurationMs: Int = 20,
        val speechThresholdDbOffset: Float = 10f,
        val minThresholdDb: Float = -42f
    )

    data class VadFrame(
        val pcm: ByteArray,
        val bytesRead: Int,
        val rmsDb: Float,
        val isSpeech: Boolean,
        val timestampMs: Long
    )

    data class SegmentFile(
        val file: File,
        val durationMs: Long,
        val bytes: Int
    )

    val isHot: Boolean
        get() = isRecording.get()

    private fun ensureRecorder(bufferSize: Int): AudioRecord {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Microphone permission is required to record audio.")
        }

        val existing = audioRecord
        if (existing != null && existing.state == AudioRecord.STATE_INITIALIZED) {
            return existing
        }

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IOException("AudioRecord failed to initialize.")
        }
        audioRecord = recorder
        return recorder
    }

    fun startRecording() {
        val bufferSize = minBufferSize
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IOException("AudioRecord configuration is not supported on this device.")
        }

        audioFile = File(
            context.cacheDir,
            "audio_${System.currentTimeMillis()}.pcm"
        )
        fileOutputStream = FileOutputStream(audioFile)

        val localRecorder = ensureRecorder(bufferSize)
        localRecorder.startRecording()
        isRecording.set(true)
    }

    fun startHotMic() {
        if (isRecording.get()) return
        val bufferSize = minBufferSize
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IOException("AudioRecord configuration is not supported on this device.")
        }
        val recorder = ensureRecorder(bufferSize)
        recorder.startRecording()
        isRecording.set(true)
    }

    /**
     * Reads audio data from the recorder into [buffer].
     * Also writes the read data to the local backup file.
     *
     * @return The number of bytes read, or 0 if error/stopped.
     */
    fun read(buffer: ByteArray, size: Int): Int {
        if (!isRecording.get() || audioRecord == null) return 0

        val readResult = audioRecord?.read(buffer, 0, size) ?: 0
        if (readResult > 0) {
            try {
                fileOutputStream?.write(buffer, 0, readResult)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return readResult
    }

    fun streamVadFrames(config: VadConfig = VadConfig()): Flow<VadFrame> = flow {
        startHotMic()

        val bytesPerSample = 2
        val frameBytes = ((SAMPLE_RATE * config.frameDurationMs) / 1000) * bytesPerSample
        val readSize = maxOf(frameBytes, minBufferSize.coerceAtLeast(1024))
        val buffer = ByteArray(readSize)
        var noiseFloorDb = -55f

        while (isRecording.get()) {
            val bytesRead = audioRecord?.read(buffer, 0, readSize) ?: 0
            if (bytesRead <= 0) continue

            val chunk = buffer.copyOf(bytesRead)
            val rmsDb = computeRmsDb(chunk, bytesRead)

            if (rmsDb < noiseFloorDb + 3f) {
                noiseFloorDb = (noiseFloorDb * 0.95f) + (rmsDb * 0.05f)
            }

            val threshold = maxOf(config.minThresholdDb, noiseFloorDb + config.speechThresholdDbOffset)
            val isSpeech = rmsDb > threshold

            emit(
                VadFrame(
                    pcm = chunk,
                    bytesRead = bytesRead,
                    rmsDb = rmsDb,
                    isSpeech = isSpeech,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    fun writePcmToWav(
        pcmData: ByteArray,
        fileNamePrefix: String = "utterance"
    ): SegmentFile {
        val wavFile = File(
            context.cacheDir,
            "${fileNamePrefix}_${System.currentTimeMillis()}.wav"
        )
        FileOutputStream(wavFile).use { fos ->
            val channels = 1
            val bitsPerSample = 16
            val byteRate = SAMPLE_RATE * channels * bitsPerSample / 8
            val dataLen = pcmData.size
            val totalDataLen = dataLen + 36

            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray())
                putInt(totalDataLen)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1)
                putShort(channels.toShort())
                putInt(SAMPLE_RATE)
                putInt(byteRate)
                putShort((channels * bitsPerSample / 8).toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(dataLen)
            }.array()

            fos.write(header)
            fos.write(pcmData)
        }

        val durationMs = (pcmData.size / 2L) * 1000L / SAMPLE_RATE
        return SegmentFile(file = wavFile, durationMs = durationMs, bytes = pcmData.size)
    }

    private fun computeRmsDb(bytes: ByteArray, bytesRead: Int): Float {
        var sumSquares = 0.0
        val sampleCount = bytesRead / 2
        if (sampleCount <= 0) return -90f

        var i = 0
        while (i + 1 < bytesRead) {
            val sample = ((bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xFF)).toShort()
            val normalized = sample / PCM_16BIT_MAX
            sumSquares += (normalized * normalized)
            i += 2
        }

        val meanSquare = sumSquares / sampleCount
        val rms = kotlin.math.sqrt(meanSquare).toFloat().coerceAtLeast(1e-6f)
        return (20f * kotlin.math.log10(rms))
    }

    fun stopRecording(): File? {
        isRecording.set(false)
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            fileOutputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        
        audioRecord = null
        fileOutputStream = null

        return audioFile
    }

    fun stopHotMic(releaseRecorder: Boolean = false) {
        isRecording.set(false)
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        if (releaseRecorder) {
            try {
                audioRecord?.release()
            } catch (_: Exception) {
            }
            audioRecord = null
        }
    }

    fun release() {
        stopHotMic(releaseRecorder = true)
    }
}