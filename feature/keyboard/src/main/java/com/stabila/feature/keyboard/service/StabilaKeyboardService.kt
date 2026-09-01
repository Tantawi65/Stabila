package com.stabila.feature.keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dagger.hilt.android.AndroidEntryPoint

/**
 * The core Android Input Method Service that draws the custom Stabila keyboard.
 * This class hosts a Jetpack Compose view for the keyboard UI.
 */
@AndroidEntryPoint
class StabilaKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val tremorScoreState = androidx.compose.runtime.mutableFloatStateOf(-1f)
    private val undoStack = androidx.compose.runtime.mutableStateListOf<String>()
    private lateinit var prefs: android.content.SharedPreferences

    private fun pushUndo(text: String) {
        if (text.isEmpty()) return
        undoStack.add(text)
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("stabila_ati", android.content.Context.MODE_PRIVATE)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        tremorScoreState.floatValue = prefs.getFloat("last_tremor_score", -1f)
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
                    com.stabila.feature.keyboard.ui.StabilaKeyboardLayout(
                        tremorScore = tremorScoreState.floatValue,
                        canUndo = undoStack.isNotEmpty(),
                        onKeyPress = { char ->
                            currentInputConnection?.commitText(char, 1)
                        },
                        onDelete = {
                            val ic = currentInputConnection
                            if (ic != null) {
                                val textBefore = ic.getTextBeforeCursor(1, 0)?.toString() ?: ""
                                if (textBefore.isNotEmpty()) {
                                    pushUndo(textBefore)
                                }
                                ic.deleteSurroundingText(1, 0)
                            }
                        },
                        onDeleteWord = {
                            val ic = currentInputConnection
                            if (ic != null) {
                                val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
                                if (textBefore.isNotEmpty()) {
                                    var i = textBefore.length - 1
                                    while (i >= 0 && textBefore[i].isWhitespace()) {
                                        i--
                                    }
                                    while (i >= 0 && !textBefore[i].isWhitespace()) {
                                        i--
                                    }
                                    val count = textBefore.length - (i + 1)
                                    if (count > 0) {
                                        val deleted = textBefore.substring(textBefore.length - count)
                                        pushUndo(deleted)
                                        ic.deleteSurroundingText(count, 0)
                                    }
                                }
                            }
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
                        },
                        onUndo = {
                            val ic = currentInputConnection
                            if (ic != null && undoStack.isNotEmpty()) {
                                val lastDeleted = undoStack.removeAt(undoStack.lastIndex)
                                ic.commitText(lastDeleted, 1)
                            }
                        },
                        onEnter = {
                            val info = currentInputEditorInfo
                            val inputType = info?.inputType ?: 0
                            val imeOptions = info?.imeOptions ?: 0
                            
                            val isMultiLine = (inputType and android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
                            val flagNoEnterAction = (imeOptions and android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
                            val action = imeOptions and android.view.inputmethod.EditorInfo.IME_MASK_ACTION
                            
                            val hasExplicitAction = action != android.view.inputmethod.EditorInfo.IME_ACTION_NONE && 
                                                    action != android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED
                            
                            if (hasExplicitAction && !flagNoEnterAction) {
                                // App explicitly requested an action (e.g. Search in Google App, Go, Send, Next, Done)
                                currentInputConnection?.performEditorAction(action)
                            } else if (isMultiLine || flagNoEnterAction) {
                                // Multi-line fields without explicit action (e.g. WhatsApp chat, Notes) -> insert newline
                                currentInputConnection?.commitText("\n", 1)
                            } else {
                                // Single-line fields without explicit action -> send Enter key events
                                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
                            }
                        }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
