package com.supereva.fluentai.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.supereva.fluentai.data.database.entity.SessionEntity
import com.supereva.fluentai.domain.repository.LocalHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit

class ProgressViewModel(
    repository: LocalHistoryRepository
) : ViewModel() {

    val sessions: StateFlow<List<SessionEntity>> = repository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val weeklyAverageScore: StateFlow<Double> = sessions.map { list ->
        val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val recentSessions = list.filter { it.timestamp >= oneWeekAgo }
        if (recentSessions.isNotEmpty()) {
            recentSessions.map { it.averageScore }.average()
        } else {
            0.0
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    data class DailyScore(val label: String, val score: Double)

    val weeklyChartData: StateFlow<List<DailyScore>> = sessions.map { list ->
        val calendar = java.util.Calendar.getInstance()
        val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6) // Include today
        
        // Group by day key (e.g., "Mon", "Tue") or better yet, day of year to handle sorting
        // For localized labels, we can use simple formatter
        val dateFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        
        // Map 7 days ending today
        (0..6).map { offset ->
            val time = System.currentTimeMillis() - TimeUnit.DAYS.toMillis((6 - offset).toLong())
            val dayStart = getStartOfDay(time)
            val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)
            
            val sessionsToday = list.filter { it.timestamp in dayStart until dayEnd }
            val average = if (sessionsToday.isNotEmpty()) {
                sessionsToday.map { it.averageScore }.average()
            } else {
                0.0
            }
            
            DailyScore(dateFormat.format(java.util.Date(time)), average)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun getStartOfDay(time: Long): Long {
         val calendar = java.util.Calendar.getInstance()
         calendar.timeInMillis = time
         calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
         calendar.set(java.util.Calendar.MINUTE, 0)
         calendar.set(java.util.Calendar.SECOND, 0)
         calendar.set(java.util.Calendar.MILLISECOND, 0)
         return calendar.timeInMillis
    }

    companion object {
        fun provideFactory(
            repository: LocalHistoryRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgressViewModel(repository) as T
            }
        }
    }
}
