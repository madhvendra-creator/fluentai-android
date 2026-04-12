package com.supereva.fluentai.ui.translation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TranslationViewModel : ViewModel() {

    private val _sourceLanguage = MutableStateFlow("English")
    val sourceLanguage: StateFlow<String> = _sourceLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow("Hindi")
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    fun swapLanguages() {
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp
    }

    fun setLanguage(isSource: Boolean, language: String) {
        if (isSource) {
            _sourceLanguage.value = language
        } else {
            _targetLanguage.value = language
        }
    }
}
