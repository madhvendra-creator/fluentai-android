package com.supereva.fluentai.di

import android.content.Context
import com.supereva.fluentai.data.audio.AudioRecorder
import com.supereva.fluentai.data.database.FluentAiDatabase
import com.supereva.fluentai.data.repository.NativeSpeechRepository
import com.supereva.fluentai.data.repository.RealAiRepository
import com.supereva.fluentai.data.repository.RealConversationRepository
import com.supereva.fluentai.data.repository.RoomLocalHistoryRepository
import com.supereva.fluentai.data.tts.AndroidTtsEngine
import com.supereva.fluentai.domain.ai.AiConversationEngine
import com.supereva.fluentai.domain.repository.ConversationRepository
import com.supereva.fluentai.domain.repository.SpeechRepository
import com.supereva.fluentai.domain.session.DefaultSpeakingSessionCoordinator
import com.supereva.fluentai.domain.session.SpeakingSessionCoordinator
import com.supereva.fluentai.domain.tts.TtsEngine
import com.supereva.fluentai.domain.usecase.ProcessSpeechUseCase

object SessionServiceLocator {

    private var _audioRecorder: AudioRecorder? = null
    private var _ttsEngine: AndroidTtsEngine? = null
    private var _database: FluentAiDatabase? = null
    private var _nativeSpeechRepository: NativeSpeechRepository? = null

    private var _authManager: com.supereva.fluentai.domain.auth.AuthManager? = null

    fun init(context: Context) {
        if (_ttsEngine == null) {
            val appCtx = context.applicationContext
            _ttsEngine = AndroidTtsEngine(appCtx)
            _nativeSpeechRepository = NativeSpeechRepository(appCtx)
            _audioRecorder = AudioRecorder(appCtx)
            _authManager = com.supereva.fluentai.data.auth.RealAuthManager(appCtx)

            _database = androidx.room.Room.databaseBuilder(
                appCtx,
                FluentAiDatabase::class.java,
                "fluent_ai_db"
            ).build()
        }
    }

    val localHistoryRepository: com.supereva.fluentai.domain.repository.LocalHistoryRepository
        get() = RoomLocalHistoryRepository(
            (_database ?: error("SessionServiceLocator.init() not called")).sessionDao()
        )

    val authManager: com.supereva.fluentai.domain.auth.AuthManager
        get() = _authManager ?: error("SessionServiceLocator.init() not called")

    val ttsEngine: TtsEngine?
        get() = _ttsEngine

    val speechRepository: SpeechRepository
        get() = _nativeSpeechRepository ?: error("SessionServiceLocator.init() not called")

    val audioRecorder: AudioRecorder
        get() = _audioRecorder ?: error("SessionServiceLocator.init() not called")

    val okHttpClient: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    val coordinator: SpeakingSessionCoordinator by lazy {
        DefaultSpeakingSessionCoordinator(
            ttsEngine = _ttsEngine,
            localHistoryRepository = localHistoryRepository
        )
    }

    val aiEngine: AiConversationEngine? = null

    val processSpeechUseCase: ProcessSpeechUseCase by lazy {
        ProcessSpeechUseCase(
            aiRepository = RealAiRepository(authManager = authManager)
        )
    }

    val conversationRepository: ConversationRepository by lazy {
        RealConversationRepository(authManager = authManager)
    }
}