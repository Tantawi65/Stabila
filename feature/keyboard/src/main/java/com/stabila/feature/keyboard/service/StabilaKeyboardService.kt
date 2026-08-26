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

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
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

        val prefs = getSharedPreferences("stabila_ati", android.content.Context.MODE_PRIVATE)
        
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            setContent {
                val tremorScore = prefs.getFloat("last_tremor_score", -1f)
                
                StabilaTheme {
                    com.stabila.feature.keyboard.ui.StabilaKeyboardLayout(
                        tremorScore = tremorScore,
                        onKeyPress = { char ->
                            currentInputConnection?.commitText(char, 1)
                        },
                        onDelete = {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                        },
                        onEnter = {
                            val info = currentInputEditorInfo
                            val action = info?.imeOptions?.and(android.view.inputmethod.EditorInfo.IME_MASK_ACTION) ?: 0
                            if (action != android.view.inputmethod.EditorInfo.IME_ACTION_NONE && action != android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED) {
                                currentInputConnection?.performEditorAction(action)
                            } else {
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
