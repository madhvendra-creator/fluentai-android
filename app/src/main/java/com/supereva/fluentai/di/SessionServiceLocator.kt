package com.supereva.fluentai.di

import android.content.Context
import com.supereva.fluentai.BuildConfig
import com.supereva.fluentai.data.repository.NativeSpeechRepository
import com.supereva.fluentai.data.repository.RealAiRepository
import com.supereva.fluentai.data.repository.RealConversationRepository

import com.supereva.fluentai.data.tts.AndroidTtsEngine
import com.supereva.fluentai.domain.ai.AiConversationEngine
import com.supereva.fluentai.domain.repository.ConversationRepository
import com.supereva.fluentai.domain.repository.SpeechRepository
import com.supereva.fluentai.domain.session.DefaultSpeakingSessionCoordinator
import com.supereva.fluentai.domain.session.SpeakingSessionCoordinator
import com.supereva.fluentai.domain.tts.TtsEngine
import com.supereva.fluentai.domain.usecase.ProcessSpeechUseCase

/**
 * Manual service locator providing **app-scoped singletons**.
 *
 * Call [init] once from [android.app.Activity.onCreate] (or Application)
 * before accessing any other property.
 *
 * Replace with Hilt `@Singleton` bindings when DI is introduced.
 */
object SessionServiceLocator {

    private var _ttsEngine: AndroidTtsEngine? = null

    private var _database: com.supereva.fluentai.data.database.FluentAiDatabase? = null

    /**
     * Initialise context-dependent singletons.
     * Safe to call multiple times — only the first call has effect.
     */
    fun init(context: Context) {
        if (_ttsEngine == null) {
            val appCtx = context.applicationContext
            _ttsEngine = AndroidTtsEngine(appCtx)
            _nativeSpeechRepository = NativeSpeechRepository(appCtx)
            
            _database = androidx.room.Room.databaseBuilder(
                appCtx,
                com.supereva.fluentai.data.database.FluentAiDatabase::class.java,
                "fluent_ai_db"
            ).build()
        }
    }

    val localHistoryRepository: com.supereva.fluentai.domain.repository.LocalHistoryRepository
        get() = com.supereva.fluentai.data.repository.RoomLocalHistoryRepository(
            (_database ?: error("SessionServiceLocator.init() not called")).sessionDao()
        )

    /** Platform TTS engine. Available after [init]. */
    val ttsEngine: TtsEngine?
        get() = _ttsEngine

    private var _nativeSpeechRepository: NativeSpeechRepository? = null

    /** Android SpeechRecognizer wrapper. Available after [init]. */
    val speechRepository: SpeechRepository
        get() = _nativeSpeechRepository ?: error("SessionServiceLocator.init() must be called before accessing speechRepository")

    val coordinator: SpeakingSessionCoordinator by lazy {
        DefaultSpeakingSessionCoordinator(
            ttsEngine = _ttsEngine,
            localHistoryRepository = localHistoryRepository
        )
    }

    /**
     * Set to `null` to disable streaming and use the classic
     * [processRecordingUseCase] fallback instead.
     *
     * Re-enable streaming later by changing this to:
     *   `by lazy { StubAiConversationEngine() }`
     */
    val aiEngine: AiConversationEngine? by lazy {
        com.supereva.fluentai.data.ai.OpenAiRealtimeEngine(BuildConfig.OPENAI_API_KEY)
    }

    val processSpeechUseCase: ProcessSpeechUseCase by lazy {
        ProcessSpeechUseCase(
            aiRepository = RealAiRepository(BuildConfig.OPENAI_API_KEY)
        )
    }





    val conversationRepository: ConversationRepository by lazy {
        RealConversationRepository(BuildConfig.OPENAI_API_KEY)
    }
}
