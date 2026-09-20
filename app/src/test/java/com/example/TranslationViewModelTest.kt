package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.ui.TranslationViewModel
import com.google.mlkit.common.sdkinternal.MlKitContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TranslationViewModelTest {

    private lateinit var viewModel: TranslationViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        MlKitContext.initializeIfNeeded(context)
        viewModel = TranslationViewModel()
    }

    @Test
    fun defaultState_isCorrect() {
        val state = viewModel.uiState.value
        assertEquals("Hello, how are you today?", state.inputText)
        assertEquals(0, state.selectedLanguageIndex)
        assertEquals("Spanish", state.selectedLanguage.name)
        assertTrue(state.characterCount > 0)
    }

    @Test
    fun updateInputText_trimsToMaxLimit() {
        val longText = "a".repeat(600)
        viewModel.updateInputText(longText)
        assertEquals(500, viewModel.uiState.value.inputText.length)
        assertEquals(500, viewModel.uiState.value.characterCount)
    }

    @Test
    fun clearInput_resetsText() {
        viewModel.clearInput()
        assertEquals("", viewModel.uiState.value.inputText)
        assertEquals(0, viewModel.uiState.value.characterCount)
    }

    @Test
    fun selectLanguage_updatesSelection() {
        viewModel.selectLanguage(1) // French
        assertEquals("French", viewModel.uiState.value.selectedLanguage.name)
    }

    @Test
    fun toggleSwapLanguages_swapsSourceAndTarget() {
        viewModel.updateInputText("Hello")
        viewModel.selectLanguage(0) // Spanish
        viewModel.toggleSwapLanguages()

        val state = viewModel.uiState.value
        assertTrue(state.isReversed)
        assertEquals("Spanish", state.sourceLanguage.name)
        assertEquals("English", state.targetLanguage.name)
    }

    @Test
    fun storageMetrics_calculateCorrectly() {
        val state = viewModel.uiState.value
        assertEquals(12, state.totalLanguagesCount)
        assertEquals(0, state.downloadedCount)
        assertEquals(0, state.usedStorageMb)
        assertEquals(12 * 30, state.totalStorageMb)
        assertEquals(0f, state.storageRatio, 0.001f)
    }
}

