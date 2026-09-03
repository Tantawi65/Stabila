package com.stabila.feature.keyboard.service

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.stabila.core.ui.StabilaTheme
import com.stabila.feature.keyboard.suggestion.ContextExtractor
import com.stabila.feature.keyboard.suggestion.KeyboardLanguage
import com.stabila.feature.keyboard.suggestion.SuggestionEngine
import com.stabila.feature.keyboard.suggestion.UserFrequencyStore
import com.stabila.feature.keyboard.ui.StabilaKeyboardLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * The core Android Input Method Service that draws the custom Stabila keyboard.
 * This class hosts a Jetpack Compose view for the keyboard UI and coordinates
 * word suggestion and next-word prediction for both English and Arabic.
 */
@AndroidEntryPoint
class StabilaKeyboardService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // ── Lifecycle / DI plumbing ──────────────────────────────────────────────

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    /** Injected by Hilt — singleton, shared across all input sessions. */
    @Inject
    lateinit var suggestionEngine: SuggestionEngine

    @Inject
    lateinit var userFrequencyStore: UserFrequencyStore

    /**
     * Coroutine scope tied to the service lifetime.
     * Uses [SupervisorJob] so a failed suggestion coroutine does not cancel
     * dictionary loading or other pending work.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ── Compose state ────────────────────────────────────────────────────────

    private val tremorScoreState = androidx.compose.runtime.mutableFloatStateOf(-1f)
    private val undoStack = mutableStateListOf<String>()

    /**
     * Current suggestion results exposed to the Compose UI.
     * Always mutated on the main thread.
     */
    private val suggestions = mutableStateListOf<String>()

    /**
     * Whether the current input field allows suggestions.
     * Determined once per [onStartInputView] from [EditorInfo].
     */
    private val suggestionsEnabled = mutableStateOf(false)

    /**
     * Current keyboard language (true = Arabic, false = English).
     */
    private val isArabicState = mutableStateOf(false)

    // ── Stale-result guard ───────────────────────────────────────────────────

    /**
     * Incremented before every asynchronous suggestion lookup.
     * A result is applied only if its captured sequence number matches the
     * current counter — discarding stale responses from earlier keystrokes.
     */
    private val querySequence = AtomicLong(0L)

    // ── Shared preferences ───────────────────────────────────────────────────

    private lateinit var prefs: android.content.SharedPreferences

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("stabila_ati", android.content.Context.MODE_PRIVATE)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // Load dictionaries and bigrams in background for both English and Arabic
        loadDictionary()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        tremorScoreState.floatValue = prefs.getFloat("last_tremor_score", -1f)

        // Evaluate the target field's input type to decide whether to show suggestions.
        suggestionsEnabled.value = info != null && shouldShowSuggestions(info)
        suggestions.clear()

        // Seed suggestions for any text already in the field.
        if (suggestionsEnabled.value) {
            updateSuggestions()
        }
    }

    override fun onCreateInputView(): View {
        val container = object : FrameLayout(this) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                val root = rootView
                root.setViewTreeLifecycleOwner(this@StabilaKeyboardService)
                root.setViewTreeViewModelStoreOwner(this@StabilaKeyboardService)
                root.setViewTreeSavedStateRegistryOwner(this@StabilaKeyboardService)
            }
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                StabilaTheme {
                    StabilaKeyboardLayout(
                        tremorScore = tremorScoreState.floatValue,
                        canUndo = undoStack.isNotEmpty(),
                        suggestions = suggestions.toList(),
                        suggestionsEnabled = suggestionsEnabled.value,
                        onSuggestionSelected = { word -> selectSuggestion(word) },
                        onLanguageChange = { isArabic ->
                            isArabicState.value = isArabic
                            updateSuggestions()
                        },
                        onKeyPress = { char ->
                            currentInputConnection?.commitText(char, 1)
                            updateSuggestions()
                        },
                        onDelete = {
                            val ic = currentInputConnection
                            if (ic != null) {
                                val textBefore = ic.getTextBeforeCursor(1, 0)?.toString() ?: ""
                                if (textBefore.isNotEmpty()) pushUndo(textBefore)
                                ic.deleteSurroundingText(1, 0)
                            }
                            updateSuggestions()
                        },
                        onDeleteWord = {
                            val ic = currentInputConnection
                            if (ic != null) {
                                val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
                                if (textBefore.isNotEmpty()) {
                                    var i = textBefore.length - 1
                                    while (i >= 0 && textBefore[i].isWhitespace()) i--
                                    while (i >= 0 && !textBefore[i].isWhitespace()) i--
                                    val count = textBefore.length - (i + 1)
                                    if (count > 0) {
                                        pushUndo(textBefore.substring(textBefore.length - count))
                                        ic.deleteSurroundingText(count, 0)
                                    }
                                }
                            }
                            updateSuggestions()
                        },
                        onClearAll = {
                            val ic = currentInputConnection
                            if (ic != null) {
                                val textBefore = ic.getTextBeforeCursor(2000, 0)?.toString() ?: ""
                                val textAfter = ic.getTextAfterCursor(2000, 0)?.toString() ?: ""
                                val fullText = textBefore + textAfter
                                if (fullText.isNotEmpty()) {
                                    pushUndo(fullText)
                                    ic.deleteSurroundingText(textBefore.length, textAfter.length)
                                }
                            }
                            suggestions.clear()
                        },
                        onUndo = {
                            val ic = currentInputConnection
                            if (ic != null && undoStack.isNotEmpty()) {
                                ic.commitText(undoStack.removeAt(undoStack.lastIndex), 1)
                            }
                            updateSuggestions()
                        },
                        onEnter = {
                            val info = currentInputEditorInfo
                            val inputType = info?.inputType ?: 0
                            val imeOptions = info?.imeOptions ?: 0

                            val isMultiLine = (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
                            val flagNoEnterAction =
                                (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
                            val action = imeOptions and EditorInfo.IME_MASK_ACTION

                            val hasExplicitAction = action != EditorInfo.IME_ACTION_NONE &&
                                    action != EditorInfo.IME_ACTION_UNSPECIFIED

                            if (hasExplicitAction && !flagNoEnterAction) {
                                // App explicitly requested an action (Search, Go, Send, Next, Done)
                                currentInputConnection?.performEditorAction(action)
                            } else if (isMultiLine || flagNoEnterAction) {
                                // Multi-line fields → insert newline
                                currentInputConnection?.commitText("\n", 1)
                            } else {
                                // Single-line fields without explicit action → send Enter key events
                                currentInputConnection?.sendKeyEvent(
                                    android.view.KeyEvent(
                                        android.view.KeyEvent.ACTION_DOWN,
                                        android.view.KeyEvent.KEYCODE_ENTER,
                                    )
                                )
                                currentInputConnection?.sendKeyEvent(
                                    android.view.KeyEvent(
                                        android.view.KeyEvent.ACTION_UP,
                                        android.view.KeyEvent.KEYCODE_ENTER,
                                    )
                                )
                            }
                            // After submitting / line-break, clear suggestions — new context.
                            suggestions.clear()
                        },
                    )
                }
            }
        }

        container.addView(composeView)
        return container
    }

    override fun onWindowShown() {
        super.onWindowShown()
        tremorScoreState.floatValue = prefs.getFloat("last_tremor_score", -1f)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        saveUserFrequencyData()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        saveUserFrequencyData()
        store.clear()
        serviceScope.cancel()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Undo stack
    // ─────────────────────────────────────────────────────────────────────────

    private fun pushUndo(text: String) {
        if (text.isEmpty()) return
        undoStack.add(text)
        if (undoStack.size > 30) undoStack.removeAt(0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dictionary & Model loading
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reads wordlists and bigrams for both English and Arabic from assets on an IO thread.
     * Also restores stored user frequency learning data.
     */
    private fun loadDictionary() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // 1. Restore local user frequency store
                val savedUserData = prefs.getString("user_frequency_data", null)
                if (!savedUserData.isNullOrBlank()) {
                    userFrequencyStore.loadFromString(savedUserData)
                }

                // 2. Load English assets
                try {
                    val enWords = assets.open("en_wordlist.txt")
                        .bufferedReader(Charsets.UTF_8)
                        .readLines()
                    suggestionEngine.load(enWords, KeyboardLanguage.ENGLISH)

                    val enBigrams = assets.open("en_bigrams.txt")
                        .bufferedReader(Charsets.UTF_8)
                        .readLines()
                    suggestionEngine.loadBigrams(enBigrams, KeyboardLanguage.ENGLISH)
                } catch (e: Exception) {
                    // English fallback
                }

                // 3. Load Arabic assets
                try {
                    val arWords = assets.open("ar_wordlist.txt")
                        .bufferedReader(Charsets.UTF_8)
                        .readLines()
                    suggestionEngine.load(arWords, KeyboardLanguage.ARABIC)

                    val arBigrams = assets.open("ar_bigrams.txt")
                        .bufferedReader(Charsets.UTF_8)
                        .readLines()
                    suggestionEngine.loadBigrams(arBigrams, KeyboardLanguage.ARABIC)
                } catch (e: Exception) {
                    // Arabic fallback
                }
            } catch (e: Exception) {
                // Suggestions unavailable — not a fatal error.
            }
        }
    }

    private fun saveUserFrequencyData() {
        try {
            val exported = userFrequencyStore.exportToString()
            prefs.edit().putString("user_frequency_data", exported).apply()
        } catch (e: Exception) {
            // Ignore persistence errors
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Suggestion logic
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Determines whether the current field should receive suggestions.
     */
    private fun shouldShowSuggestions(info: EditorInfo): Boolean {
        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS

        if (inputClass != InputType.TYPE_CLASS_TEXT) return false

        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        ) return false

        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_URI
        ) return false

        if (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0) return false

        return true
    }

    /**
     * Extracts the word currently being typed from [textBefore] for backwards-compatibility.
     */
    internal fun extractCurrentWord(textBefore: String): String {
        return ContextExtractor.extract(textBefore).currentWord
    }

    /**
     * Reads text before the cursor, parses context, and schedules prediction lookup on [Dispatchers.Default].
     */
    private fun updateSuggestions() {
        if (!suggestionsEnabled.value) return

        val ic = currentInputConnection ?: run {
            suggestions.clear()
            return
        }

        val textBefore = ic.getTextBeforeCursor(200, 0)?.toString() ?: ""
        if (textBefore.isEmpty()) {
            suggestions.clear()
            return
        }

        val context = ContextExtractor.extract(textBefore)
        val currentLang = if (isArabicState.value) KeyboardLanguage.ARABIC else KeyboardLanguage.ENGLISH

        val seq = querySequence.incrementAndGet()

        serviceScope.launch(Dispatchers.Default) {
            val results = suggestionEngine.suggest(context, currentLang)

            withContext(Dispatchers.Main) {
                // Discard stale results — a newer keystroke superseded this one.
                if (querySequence.get() != seq) return@withContext

                suggestions.clear()
                suggestions.addAll(results)
            }
        }
    }

    /**
     * Replaces the current partial word with [word], appends a trailing space,
     * and records the selection in the on-device user frequency model.
     */
    private fun selectSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(200, 0)?.toString() ?: ""
        val context = ContextExtractor.extract(textBefore)

        if (context.currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(context.currentWord.length, 0)
        }
        ic.commitText("$word ", 1)

        // Learn user word and bigram selection
        userFrequencyStore.recordWord(word, context.previousWord)

        // Clear stale suggestions and immediately update for next-word prediction
        suggestions.clear()
        updateSuggestions()
    }
}
