package com.supereva.fluentai.data.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun startRecording() {

        audioFile = File(
            context.cacheDir,
            "audio_${System.currentTimeMillis()}.m4a"
        )

        recorder = MediaRecorder().apply {

            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile!!.absolutePath)

            prepare()
            start()
        }
    }

    fun stopRecording(): File? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return audioFile
    }
}