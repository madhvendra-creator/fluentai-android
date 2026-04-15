package com.supereva.fluentai.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.supereva.fluentai.data.repository.LearnRepository
import com.supereva.fluentai.domain.learn.Lesson
import com.supereva.fluentai.domain.learn.LearnLevel
import com.supereva.fluentai.domain.learn.LearnUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LearnUiState(
    val units: List<LearnUnit> = emptyList(),
    val selectedLesson: Lesson? = null,
    val selectedLessonUnitId: String? = null,
    val selectedLessonLevelId: String? = null
)

class LearnViewModel(
    private val repository: LearnRepository
) : ViewModel() {

    private val completedLessonIds = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    init {
        loadUnits()
    }

    private fun loadUnits() {
        _uiState.update { it.copy(units = buildUnitsWithState(repository.getUnits())) }
    }

    fun completeLesson(lessonId: String) {
        completedLessonIds.add(lessonId)
        _uiState.update { it.copy(units = buildUnitsWithState(repository.getUnits()), selectedLesson = null) }
    }

    fun selectLesson(lesson: Lesson, unitId: String, levelId: String) {
        _uiState.update { it.copy(selectedLesson = lesson, selectedLessonUnitId = unitId, selectedLessonLevelId = levelId) }
    }

    fun dismissLesson() {
        _uiState.update { it.copy(selectedLesson = null, selectedLessonUnitId = null, selectedLessonLevelId = null) }
    }

    private fun buildUnitsWithState(rawUnits: List<LearnUnit>): List<LearnUnit> {
        return rawUnits.map { unit ->
            // For unit 1 level 1 only: first uncompleted lesson is unlocked, rest locked until previous done
            val updatedLevels = unit.levels.mapIndexed { levelIndex, level ->
                val isLevelUnlocked = if (unit.id == "unit_interview" && levelIndex == 0) {
                    true
                } else {
                    // A level unlocks when all lessons of the previous level are complete
                    val prevLevel = unit.levels.getOrNull(levelIndex - 1)
                    prevLevel?.lessons?.all { it.id in completedLessonIds } == true
                }

                val updatedLessons = level.lessons.mapIndexed { lessonIndex, lesson ->
                    val isCompleted = lesson.id in completedLessonIds
                    lesson.copy(isCompleted = isCompleted)
                }

                val completedCount = updatedLessons.count { it.isCompleted }

                level.copy(
                    isUnlocked = isLevelUnlocked,
                    completedLessons = completedCount,
                    lessons = updatedLessons
                )
            }
            unit.copy(levels = updatedLevels)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LearnViewModel(LearnRepository()) as T
        }
    }
}
