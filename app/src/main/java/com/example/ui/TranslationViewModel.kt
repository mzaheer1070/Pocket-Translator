package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LanguageOption
import com.example.data.englishOption
import com.example.data.getLanguageByCode
import com.example.data.supportedLanguages
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslationUiState(
    val inputText: String = "Hello, how are you today?",
    val outputText: String = "",
    val selectedLanguageIndex: Int = 0,
    val isReversed: Boolean = false, // If true: Translates Target -> English
    val isLoading: Boolean = false,
    val progressMessage: String = "",
    val isListeningSpeech: Boolean = false,
    val isModelManagerOpen: Boolean = false,
    val downloadedModelCodes: Set<String> = emptySet(),
    val downloadingModelCodes: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val userMessage: String? = null
) {
    val selectedLanguage: LanguageOption
        get() = supportedLanguages.getOrElse(selectedLanguageIndex) { supportedLanguages[0] }

    val sourceLanguage: LanguageOption
        get() = if (isReversed) selectedLanguage else englishOption

    val targetLanguage: LanguageOption
        get() = if (isReversed) englishOption else selectedLanguage

    val characterCount: Int
        get() = inputText.length

    val maxCharacters: Int = 500

    val isCurrentTargetOffline: Boolean
        get() = downloadedModelCodes.contains(selectedLanguage.mlKitCode)
}

class TranslationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private var currentTranslator: Translator? = null
    private val modelManager = RemoteModelManager.getInstance()

    init {
        refreshDownloadedModels()
    }

    fun updateInputText(newText: String) {
        val safeText = if (newText.length > 500) newText.take(500) else newText
        _uiState.update { it.copy(inputText = safeText, errorMessage = null) }
    }

    fun clearInput() {
        _uiState.update { it.copy(inputText = "", outputText = "", errorMessage = null) }
    }

    fun selectLanguage(index: Int) {
        if (index in supportedLanguages.indices) {
            _uiState.update { it.copy(selectedLanguageIndex = index, errorMessage = null) }
        }
    }

    fun toggleSwapLanguages() {
        _uiState.update { current ->
            current.copy(
                isReversed = !current.isReversed,
                inputText = current.outputText.ifBlank { current.inputText },
                outputText = if (current.outputText.isNotBlank()) current.inputText else "",
                errorMessage = null
            )
        }
    }

    fun setModelManagerOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isModelManagerOpen = isOpen) }
        if (isOpen) {
            refreshDownloadedModels()
        }
    }

    fun setSpeechListening(isListening: Boolean) {
        _uiState.update { it.copy(isListeningSpeech = isListening) }
    }

    fun appendTranscribedText(transcription: String) {
        if (transcription.isBlank()) return
        _uiState.update { current ->
            val updated = if (current.inputText.isBlank()) transcription else "${current.inputText} $transcription"
            val safe = if (updated.length > 500) updated.take(500) else updated
            current.copy(
                inputText = safe,
                isListeningSpeech = false,
                userMessage = "Speech transcribed"
            )
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- Offline Model Management ---

    fun refreshDownloadedModels() {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val codes = models.map { it.language.lowercase() }.toSet()
                _uiState.update { it.copy(downloadedModelCodes = codes) }
            }
            .addOnFailureListener {
                // Keep existing cached state if lookup fails
            }
    }

    fun downloadModel(languageCode: String) {
        val normalizedCode = languageCode.lowercase()
        _uiState.update { it.copy(downloadingModelCodes = it.downloadingModelCodes + normalizedCode) }

        val model = TranslateRemoteModel.Builder(languageCode).build()
        val conditions = DownloadConditions.Builder().build()

        modelManager.download(model, conditions)
            .addOnSuccessListener {
                val langName = getLanguageByCode(languageCode)?.name ?: languageCode
                _uiState.update { current ->
                    current.copy(
                        downloadingModelCodes = current.downloadingModelCodes - normalizedCode,
                        downloadedModelCodes = current.downloadedModelCodes + normalizedCode,
                        userMessage = "$langName offline model downloaded"
                    )
                }
            }
            .addOnFailureListener { error ->
                val langName = getLanguageByCode(languageCode)?.name ?: languageCode
                _uiState.update { current ->
                    current.copy(
                        downloadingModelCodes = current.downloadingModelCodes - normalizedCode,
                        errorMessage = "Failed to download $langName model: ${error.localizedMessage ?: "Network error"}"
                    )
                }
            }
    }

    fun deleteModel(languageCode: String) {
        val normalizedCode = languageCode.lowercase()
        val model = TranslateRemoteModel.Builder(languageCode).build()

        modelManager.deleteDownloadedModel(model)
            .addOnSuccessListener {
                val langName = getLanguageByCode(languageCode)?.name ?: languageCode
                _uiState.update { current ->
                    current.copy(
                        downloadedModelCodes = current.downloadedModelCodes - normalizedCode,
                        userMessage = "Removed $langName offline model"
                    )
                }
            }
            .addOnFailureListener { error ->
                val langName = getLanguageByCode(languageCode)?.name ?: languageCode
                _uiState.update { current ->
                    current.copy(
                        errorMessage = "Could not delete $langName model: ${error.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
    }

    // --- Translation Logic ---

    fun translate() {
        val currentState = _uiState.value
        val textToTranslate = currentState.inputText.trim()

        if (textToTranslate.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter text to translate.") }
            return
        }

        val sourceLang = currentState.sourceLanguage
        val targetLang = currentState.targetLanguage

        val isTargetOffline = currentState.downloadedModelCodes.contains(targetLang.mlKitCode.lowercase())
        val progressMsg = if (isTargetOffline) {
            "Translating to ${targetLang.name}..."
        } else {
            "Downloading ${targetLang.name} model..."
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                progressMessage = progressMsg,
                errorMessage = null
            )
        }

        currentTranslator?.close()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang.mlKitCode)
            .setTargetLanguage(targetLang.mlKitCode)
            .build()

        val translator = Translation.getClient(options)
        currentTranslator = translator

        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                if (currentTranslator !== translator) return@addOnSuccessListener

                // Mark model as downloaded in our state
                _uiState.update { current ->
                    current.copy(
                        downloadedModelCodes = current.downloadedModelCodes + targetLang.mlKitCode.lowercase(),
                        progressMessage = "Translating to ${targetLang.name}..."
                    )
                }

                translator.translate(textToTranslate)
                    .addOnSuccessListener { translatedResult ->
                        if (currentTranslator !== translator) return@addOnSuccessListener

                        _uiState.update {
                            it.copy(
                                outputText = translatedResult,
                                isLoading = false,
                                progressMessage = "",
                                userMessage = "Translated to ${targetLang.name}"
                            )
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (currentTranslator !== translator) return@addOnFailureListener
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                progressMessage = "",
                                errorMessage = "Translation error: ${exception.localizedMessage ?: "Unknown error"}"
                            )
                        }
                    }
            }
            .addOnFailureListener { exception ->
                if (currentTranslator !== translator) return@addOnFailureListener
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        progressMessage = "",
                        errorMessage = "Failed to download model for ${targetLang.name}: ${exception.localizedMessage ?: "Network error"}"
                    )
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        currentTranslator?.close()
        currentTranslator = null
    }
}
