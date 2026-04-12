package com.supereva.fluentai.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var audioFile: File? = null
    private var fileOutputStream: FileOutputStream? = null
    private val isRecording = AtomicBoolean(false)

    val minBufferSize: Int
        get() = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    companion object {
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun startRecording() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Microphone permission is required to record audio.")
        }

        val bufferSize = minBufferSize
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IOException("AudioRecord configuration is not supported on this device.")
        }

        audioFile = File(
            context.cacheDir,
            "audio_${System.currentTimeMillis()}.pcm"
        )
        fileOutputStream = FileOutputStream(audioFile)

        val localRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (localRecorder.state != AudioRecord.STATE_INITIALIZED) {
            localRecorder.release()
            throw IOException("AudioRecord failed to initialize.")
        }

        audioRecord = localRecorder
        audioRecord?.startRecording()
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
}