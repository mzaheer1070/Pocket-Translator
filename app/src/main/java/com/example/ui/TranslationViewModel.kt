package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LanguageOption
import com.example.data.englishOption
import com.example.data.getLanguageByCode
import com.example.data.supportedLanguages
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val detectedLanguageCode: String? = null,
    val downloadedModelCodes: Set<String> = emptySet(),
    val downloadingModelCodes: Set<String> = emptySet(),
    val downloadProgressMap: Map<String, Float> = emptyMap(), // Language code -> float 0.0f..1.0f
    val errorMessage: String? = null,
    val userMessage: String? = null
) {
    val selectedLanguage: LanguageOption
        get() = supportedLanguages.getOrElse(selectedLanguageIndex) { supportedLanguages[0] }

    val sourceLanguage: LanguageOption
        get() = if (isReversed) selectedLanguage else englishOption

    val targetLanguage: LanguageOption
        get() = if (isReversed) englishOption else selectedLanguage

    val detectedLanguage: LanguageOption?
        get() = detectedLanguageCode?.let { getLanguageByCode(it) }

    val characterCount: Int
        get() = inputText.length

    val maxCharacters: Int = 500

    val isCurrentTargetOffline: Boolean
        get() = downloadedModelCodes.contains(targetLanguage.mlKitCode.lowercase())

    val totalLanguagesCount: Int
        get() = supportedLanguages.size

    val downloadedCount: Int
        get() = downloadedModelCodes.size

    val usedStorageMb: Int
        get() = downloadedCount * 30

    val totalStorageMb: Int
        get() = totalLanguagesCount * 30

    val storageRatio: Float
        get() = if (totalLanguagesCount > 0) downloadedCount.toFloat() / totalLanguagesCount else 0f

    val activeDownloadingLanguage: LanguageOption?
        get() = downloadingModelCodes.firstOrNull()?.let { getLanguageByCode(it) }

    val activeDownloadProgress: Float
        get() = downloadingModelCodes.firstOrNull()?.let { downloadProgressMap[it] } ?: 0f
}

class TranslationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private var currentTranslator: Translator? = null
    private val modelManager = RemoteModelManager.getInstance()
    private val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()
    private val downloadJobs = mutableMapOf<String, Job>()

    init {
        refreshDownloadedModels()
        detectLanguage(_uiState.value.inputText)
    }

    fun updateInputText(newText: String) {
        val safeText = if (newText.length > 500) newText.take(500) else newText
        _uiState.update { it.copy(inputText = safeText, errorMessage = null) }
        detectLanguage(safeText)
    }

    fun clearInput() {
        _uiState.update {
            it.copy(
                inputText = "",
                outputText = "",
                detectedLanguageCode = null,
                errorMessage = null
            )
        }
    }

    fun selectLanguage(index: Int) {
        if (index in supportedLanguages.indices) {
            _uiState.update { it.copy(selectedLanguageIndex = index, errorMessage = null) }
        }
    }

    fun toggleSwapLanguages() {
        _uiState.update { current ->
            val newReversed = !current.isReversed
            current.copy(
                isReversed = newReversed,
                inputText = current.outputText.ifBlank { current.inputText },
                outputText = if (current.outputText.isNotBlank()) current.inputText else "",
                errorMessage = null
            )
        }
        detectLanguage(_uiState.value.inputText)
    }

    fun applyDetectedLanguage() {
        val detected = _uiState.value.detectedLanguage ?: return
        val currentSource = _uiState.value.sourceLanguage

        if (detected.mlKitCode.equals(currentSource.mlKitCode, ignoreCase = true)) {
            return
        }

        if (detected.mlKitCode.equals(TranslateLanguage.ENGLISH, ignoreCase = true)) {
            // Source should be English, target can stay foreign
            if (_uiState.value.isReversed) {
                _uiState.update { it.copy(isReversed = false, userMessage = "Switched source to English") }
            }
        } else {
            // Source is foreign language (e.g. Spanish)
            val index = supportedLanguages.indexOfFirst { it.mlKitCode.equals(detected.mlKitCode, ignoreCase = true) }
            if (index >= 0) {
                _uiState.update {
                    it.copy(
                        selectedLanguageIndex = index,
                        isReversed = true,
                        userMessage = "Switched source to ${detected.name}"
                    )
                }
            }
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
        detectLanguage(_uiState.value.inputText)
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- Automatic Language Identification ---

    private fun detectLanguage(text: String) {
        val trimmed = text.trim()
        if (trimmed.length < 3) {
            _uiState.update { it.copy(detectedLanguageCode = null) }
            return
        }

        languageIdentifier.identifyLanguage(trimmed)
            .addOnSuccessListener { languageCode ->
                if (languageCode != null && languageCode != "und") {
                    _uiState.update { it.copy(detectedLanguageCode = languageCode.lowercase()) }
                } else {
                    _uiState.update { it.copy(detectedLanguageCode = null) }
                }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(detectedLanguageCode = null) }
            }
    }

    // --- Offline Model Management & Download Progress ---

    fun refreshDownloadedModels() {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val codes = models.map { it.language.lowercase() }.toSet()
                _uiState.update { it.copy(downloadedModelCodes = codes) }
            }
            .addOnFailureListener {
                // Keep existing cached state
            }
    }

    fun downloadModel(languageCode: String) {
        val normalizedCode = languageCode.lowercase()
        if (_uiState.value.downloadingModelCodes.contains(normalizedCode)) return

        _uiState.update { current ->
            current.copy(
                downloadingModelCodes = current.downloadingModelCodes + normalizedCode,
                downloadProgressMap = current.downloadProgressMap + (normalizedCode to 0.05f)
            )
        }

        // Start progressive download progress ticker
        downloadJobs[normalizedCode]?.cancel()
        downloadJobs[normalizedCode] = viewModelScope.launch {
            var progress = 0.05f
            while (progress < 0.92f) {
                delay(180)
                progress += 0.04f + (Math.random().toFloat() * 0.04f)
                if (progress > 0.92f) progress = 0.92f
                _uiState.update { current ->
                    if (current.downloadingModelCodes.contains(normalizedCode)) {
                        current.copy(downloadProgressMap = current.downloadProgressMap + (normalizedCode to progress))
                    } else {
                        current
                    }
                }
            }
        }

        val model = TranslateRemoteModel.Builder(languageCode).build()
        val conditions = DownloadConditions.Builder().build()

        modelManager.download(model, conditions)
            .addOnSuccessListener {
                val langName = getLanguageByCode(languageCode)?.name ?: languageCode
                downloadJobs[normalizedCode]?.cancel()
                downloadJobs.remove(normalizedCode)

                viewModelScope.launch {
                    // Complete progress to 100%
                    _uiState.update { current ->
                        current.copy(
                            downloadProgressMap = current.downloadProgressMap + (normalizedCode to 1.0f)
                        )
                    }
                    delay(350)
                    _uiState.update { current ->
                        current.copy(
                            downloadingModelCodes = current.downloadingModelCodes - normalizedCode,
                            downloadProgressMap = current.downloadProgressMap - normalizedCode,
                            downloadedModelCodes = current.downloadedModelCodes + normalizedCode,
                            userMessage = "$langName offline model installed (~30 MB)"
                        )
                    }
                }
            }
            .addOnFailureListener { error ->
                val langName = getLanguageByCode(languageCode)?.name ?: languageCode
                downloadJobs[normalizedCode]?.cancel()
                downloadJobs.remove(normalizedCode)
                _uiState.update { current ->
                    current.copy(
                        downloadingModelCodes = current.downloadingModelCodes - normalizedCode,
                        downloadProgressMap = current.downloadProgressMap - normalizedCode,
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
        val targetCode = targetLang.mlKitCode.lowercase()

        val isTargetOffline = currentState.downloadedModelCodes.contains(targetCode)
        val progressMsg = if (isTargetOffline) {
            "Translating to ${targetLang.name}..."
        } else {
            "Downloading & installing ${targetLang.name} model (~30 MB)..."
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                progressMessage = progressMsg,
                errorMessage = null
            )
        }

        if (!isTargetOffline) {
            downloadModel(targetLang.mlKitCode)
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

                _uiState.update { current ->
                    current.copy(
                        downloadedModelCodes = current.downloadedModelCodes + targetCode,
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
        languageIdentifier.close()
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
    }
}
