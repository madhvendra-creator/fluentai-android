package com.supereva.fluentai.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PracticeScreen(
    route: com.supereva.fluentai.ui.navigation.SpeakingSessionRoute,
    onBackClick: () -> Unit = {},
    viewModelStoreOwner: ViewModelStoreOwner? = null
) {
    val viewModel: SpeakingSessionViewModel = if (viewModelStoreOwner != null) {
        viewModel(
            viewModelStoreOwner = viewModelStoreOwner,
            factory = SpeakingSessionViewModel.Factory
        )
    } else {
        viewModel(factory = SpeakingSessionViewModel.Factory)
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val isRecording by viewModel.isMicHot.collectAsState()
    val lastCorrectAnswer by viewModel.lastCorrectAnswer.collectAsState()

    // Compute here — avoids 6-flow limit in ViewModel combine
    val isWaitingForNextQuestion = route.sessionMode == "TRANSLATION_PRACTICE"
            && lastCorrectAnswer != null
            && !isRecording
            && !uiState.isProcessing
            && !uiState.isAiThinking
            && !uiState.isAiSpeaking

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenLeft()
        }
    }

    LaunchedEffect(route.sessionMode) {
        if (route.sessionMode == "TRANSLATION_PRACTICE") {
            val source = route.sourceLang ?: "English"
            val target = route.targetLang ?: "Hindi"

            val starterSentences = when (source) {
                "Hindi" -> listOf(
                    "मैं बाज़ार जा रहा हूँ।",
                    "आपका नाम क्या है?",
                    "क्या आप मेरी मदद कर सकते हैं?",
                    "मुझे भूख लगी है।",
                    "यह बहुत अच्छा है।"
                )
                "Spanish" -> listOf(
                    "¿Cómo estás?", "Me llamo Juan.",
                    "¿Dónde está el baño?", "Tengo hambre.", 
                    "Hace buen tiempo."
                )
                "French" -> listOf(
                    "Comment allez-vous?", "Je m'appelle Marie.",
                    "Où est la gare?", "J'ai faim.", 
                    "Il fait beau aujourd'hui."
                )
                "German" -> listOf(
                    "Wie geht es Ihnen?", "Ich heiße Thomas.",
                    "Wo ist der Bahnhof?", "Ich habe Hunger.", 
                    "Das Wetter ist schön."
                )
                else -> listOf(
                    "How are you?", "Where are you going?",
                    "I want to eat something.", "What time is it?",
                    "Can you help me?", "I am going to sleep."
                )
            }

            val randomChallenge = starterSentences.random()
            val firstQuestion = "Please translate: '$randomChallenge'"

            viewModel.startSession(
                topicId = route.topicId,
                firstQuestion = firstQuestion,
                difficultyStr = route.difficulty,
                sessionModeStr = route.sessionMode,
                sourceLang = source,
                targetLang = target
            )
        }
    }

    SpeakingSessionScreen(
        uiState = uiState.copy(
            lastCorrectAnswer = lastCorrectAnswer,
            isWaitingForNextQuestion = isWaitingForNextQuestion
        ),
        isRecording = isRecording,
        onMicClick = {
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else if (!isRecording) {
                viewModel.onRecordingStarted()
            } else {
                viewModel.onRecordingStopped()
            }
        },
        onCancelClick = {
            viewModel.resetActiveRecording()
        },
        onStartSession = {
            viewModel.startSession(
                topicId = route.topicId,
                sessionModeStr = route.sessionMode,
                difficultyStr = route.difficulty
            )
        },
        onEndSession = {
            viewModel.endSession()
            onBackClick()
        },
        onToggleAutocorrect = { viewModel.toggleAutocorrect() },
        onWordClick = viewModel::speakWord,
        onBackClick = onBackClick,
        onNextQuestion = { viewModel.requestNextQuestion() }  // ← NEW
    )
}
