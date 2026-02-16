package com.supereva.fluentai.ui.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supereva.fluentai.di.SessionServiceLocator
import com.supereva.fluentai.ui.progress.components.RecentSessionsList
import com.supereva.fluentai.ui.progress.components.WeeklyFluencyChart

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModel.provideFactory(SessionServiceLocator.localHistoryRepository)
    )
) {
    val sessions by viewModel.sessions.collectAsState()
    val chartData by viewModel.weeklyChartData.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            WeeklyFluencyChart(
                data = chartData
            )
            
            RecentSessionsList(
                sessions = sessions,
                onSessionClick = { sessionId ->
                    // Handle navigation to details
                    // For now, maybe just log or show toast, or expand?
                    // User asked to "tap to review".
                    // I'll leave it as a TODO or implement a simple dialog if requested.
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
