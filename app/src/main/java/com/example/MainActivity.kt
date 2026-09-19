package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var inputText: TextInputEditText
    private lateinit var outputText: TextInputEditText
    private lateinit var translateBtn: MaterialButton
    private lateinit var copyBtn: ImageButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var progressContainer: View
    private lateinit var progressText: TextView
    private lateinit var selectedLanguageName: TextView
    private lateinit var selectedLanguageFlag: TextView
    private lateinit var selectedLanguageCard: View
    private lateinit var charCount: TextView

    private var currentTranslator: Translator? = null
    private var languageMenu: PopupWindow? = null
    private var selectedLanguageIndex = DEFAULT_LANGUAGE_INDEX

    private val languageOptions = listOf(
        LanguageOption("Spanish", "\uD83C\uDDEA\uD83C\uDDF8", TranslateLanguage.SPANISH),
        LanguageOption("French", "\uD83C\uDDEB\uD83C\uDDF7", TranslateLanguage.FRENCH),
        LanguageOption("German", "\uD83C\uDDE9\uD83C\uDDEA", TranslateLanguage.GERMAN),
        LanguageOption("Italian", "\uD83C\uDDEE\uD83C\uDDF9", TranslateLanguage.ITALIAN),
        LanguageOption("Chinese", "\uD83C\uDDE8\uD83C\uDDF3", TranslateLanguage.CHINESE),
        LanguageOption("Japanese", "\uD83C\uDDEF\uD83C\uDDF5", TranslateLanguage.JAPANESE),
        LanguageOption("Arabic", "\uD83C\uDDF8\uD83C\uDDE6", TranslateLanguage.ARABIC),
        LanguageOption("Hindi", "\uD83C\uDDEE\uD83C\uDDF3", TranslateLanguage.HINDI),
        LanguageOption("Korean", "\uD83C\uDDF0\uD83C\uDDF7", TranslateLanguage.KOREAN),
        LanguageOption("Russian", "\uD83C\uDDF7\uD83C\uDDFA", TranslateLanguage.RUSSIAN),
        LanguageOption("Portuguese", "\uD83C\uDDF5\uD83C\uDDF9", TranslateLanguage.PORTUGUESE),
        LanguageOption("Dutch", "\uD83C\uDDF3\uD83C\uDDF1", TranslateLanguage.DUTCH)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        configureInput()
        setupLanguagePicker()
        setupActions()

        inputText.setText(getString(R.string.default_input_text))
    }

    private fun bindViews() {
        inputText = findViewById(R.id.inputText)
        outputText = findViewById(R.id.outputText)
        translateBtn = findViewById(R.id.translateBtn)
        copyBtn = findViewById(R.id.copyBtn)
        progressBar = findViewById(R.id.progressBar)
        progressContainer = findViewById(R.id.progressContainer)
        progressText = findViewById(R.id.progressText)
        selectedLanguageName = findViewById(R.id.selectedLanguageName)
        selectedLanguageFlag = findViewById(R.id.selectedLanguageFlag)
        selectedLanguageCard = findViewById(R.id.selectedLanguageCard)
        charCount = findViewById(R.id.charCount)
    }

    private fun configureInput() {
        inputText.filters = arrayOf(InputFilter.LengthFilter(MAX_TRANSLATION_CHARS))
        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCharCount(s?.length ?: 0)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
        updateCharCount(inputText.text?.length ?: 0)
    }

    private fun setupLanguagePicker() {
        updateSelectedLanguage(languageOptions[DEFAULT_LANGUAGE_INDEX])

        selectedLanguageCard.setOnClickListener {
            showLanguageMenu()
        }
    }

    private fun setupActions() {
        copyBtn.setOnClickListener {
            copyTranslation()
        }

        translateBtn.setOnClickListener {
            performTranslation()
        }
    }

    private fun copyTranslation() {
        val translation = outputText.text?.toString()?.trim().orEmpty()
        if (translation.isBlank()) {
            Toast.makeText(this, R.string.copy_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText(getString(R.string.clipboard_label_translation), translation)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copy_success, Toast.LENGTH_SHORT).show()
    }

    private fun performTranslation() {
        val text = inputText.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) {
            Toast.makeText(this, R.string.empty_input_error, Toast.LENGTH_SHORT).show()
            return
        }

        val language = selectedLanguage()
        showLoading(getString(R.string.progress_downloading_model, language.name))
        outputText.setText("")

        currentTranslator?.close()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(language.mlKitCode)
            .build()

        val translator = Translation.getClient(options)
        currentTranslator = translator

        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                if (!canUpdateFrom(translator)) return@addOnSuccessListener

                progressText.text = getString(R.string.progress_translating, language.name)
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        if (!canUpdateFrom(translator)) return@addOnSuccessListener

                        outputText.setText(translatedText)
                        hideLoading()
                        Toast.makeText(
                            this,
                            getString(R.string.translation_success, language.name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { error ->
                        if (canUpdateFrom(translator)) {
                            handleError(getString(R.string.translation_failed, error.readableMessage()))
                        }
                    }
            }
            .addOnFailureListener { error ->
                if (canUpdateFrom(translator)) {
                    handleError(getString(R.string.download_failed, language.name, error.readableMessage()))
                }
            }
    }

    private fun selectedLanguage(): LanguageOption {
        return languageOptions.getOrElse(selectedLanguageIndex) {
            languageOptions[DEFAULT_LANGUAGE_INDEX]
        }
    }

    private fun updateSelectedLanguage(language: LanguageOption) {
        selectedLanguageName.text = language.name
        selectedLanguageFlag.text = language.flag
        selectedLanguageCard.contentDescription = getString(
            R.string.choose_target_language_with_selection,
            language.name
        )
    }

    private fun showLanguageMenu() {
        languageMenu?.dismiss()

        val listView = ListView(this).apply {
            adapter = LanguageOptionAdapter()
            clipToPadding = false
            divider = null
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setOnItemClickListener { _, _, position, _ ->
                selectedLanguageIndex = position
                updateSelectedLanguage(languageOptions[position])
                languageMenu?.dismiss()
            }
        }

        val popupWidth = selectedLanguageCard.width.takeIf { it > 0 }
            ?: (resources.displayMetrics.widthPixels - dp(40))
        val popupHeight = minOf(languageOptions.size, MAX_VISIBLE_LANGUAGE_ROWS) * dp(64) + dp(12)

        languageMenu = PopupWindow(listView, popupWidth, popupHeight, true).apply {
            setBackgroundDrawable(ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.language_menu_background
            ))
            elevation = dp(10).toFloat()
            isOutsideTouchable = true
            showAsDropDown(selectedLanguageCard, 0, dp(8))
        }
    }

    private fun showLoading(message: String) {
        progressContainer.visibility = View.VISIBLE
        progressText.text = message
        progressBar.isIndeterminate = true
        translateBtn.isEnabled = false
        copyBtn.isEnabled = false
    }

    private fun hideLoading() {
        progressContainer.visibility = View.GONE
        translateBtn.isEnabled = true
        copyBtn.isEnabled = true
    }

    private fun handleError(message: String) {
        hideLoading()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        outputText.setText(getString(R.string.error_message, message))
    }

    private fun updateCharCount(length: Int) {
        charCount.text = getString(R.string.char_count, length, MAX_TRANSLATION_CHARS)
    }

    private fun canUpdateFrom(translator: Translator): Boolean {
        return currentTranslator === translator && !isFinishing && !isDestroyed
    }

    private fun Exception.readableMessage(): String {
        return localizedMessage ?: getString(R.string.unknown_error)
    }

    override fun onDestroy() {
        languageMenu?.dismiss()
        languageMenu = null
        currentTranslator?.close()
        currentTranslator = null
        super.onDestroy()
    }

    private inner class LanguageOptionAdapter : BaseAdapter() {
        private val inflater = LayoutInflater.from(this@MainActivity)

        override fun getCount(): Int = languageOptions.size

        override fun getItem(position: Int): LanguageOption = languageOptions[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_language_option, parent, false)
            val language = getItem(position)
            val isSelected = position == selectedLanguageIndex

            view.isSelected = isSelected
            view.findViewById<TextView>(R.id.rowLanguageFlag).text = language.flag
            view.findViewById<TextView>(R.id.rowLanguageName).text = language.name
            view.findViewById<TextView>(R.id.rowLanguageDescription).text =
                getString(R.string.language_route, language.name)
            view.findViewById<ImageView>(R.id.rowLanguageSelectedIcon).visibility =
                if (isSelected) View.VISIBLE else View.INVISIBLE

            return view
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private data class LanguageOption(
        val name: String,
        val flag: String,
        val mlKitCode: String
    )

    private companion object {
        const val DEFAULT_LANGUAGE_INDEX = 0
        const val MAX_VISIBLE_LANGUAGE_ROWS = 6
        const val MAX_TRANSLATION_CHARS = 500
    }
}
