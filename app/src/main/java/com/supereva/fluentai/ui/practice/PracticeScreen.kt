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
import com.supereva.fluentai.data.audio.AudioRecorder

/**
 * Thin **stateful wrapper** that owns the ViewModel, AudioRecorder,
 * and permission state. All rendering is delegated to the stateless
 * [SpeakingSessionScreen].
 *
 * @param viewModelStoreOwner  When provided, the [SpeakingSessionViewModel]
 *   is scoped to this owner (typically the nav-graph backstack entry) so
 *   the session survives recomposition and bottom-nav switches.
 */
@Composable
fun PracticeScreen(
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
    // Native speech is handled by ViewModel/Repository internally, so no AudioRecorder here

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // We keep local state for the button toggle visual, or we could derive it
    // But since startListening is async, local immediate toggle feels more responsive
    var isRecording by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Clean up the session when the user leaves this screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenLeft()
        }
    }

    SpeakingSessionScreen(
        uiState = uiState,
        isRecording = isRecording,
        onMicClick = {
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else if (!isRecording) {
                viewModel.onRecordingStarted()
                isRecording = true
            } else {
                viewModel.onRecordingStopped()
                isRecording = false
            }
        },
        onStartSession = { viewModel.startSession() },
        onEndSession = { viewModel.endSession() },
        onWordClick = viewModel::speakWord,
        onBackClick = onBackClick
    )
}
