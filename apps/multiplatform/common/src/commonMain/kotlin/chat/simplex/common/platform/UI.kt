package chat.simplex.common.platform

import androidx.compose.runtime.*
import chat.simplex.common.views.helpers.KeyboardState

expect fun showToast(text: String, timeout: Long = 2500L)

@Composable
expect fun LockToCurrentOrientationUntilDispose()

@Composable
expect fun LocalMultiplatformView(): Any?

@Composable
expect fun getKeyboardState(): State<KeyboardState>
expect fun hideKeyboard(view: Any?, clearFocus: Boolean = false)

expect fun androidIsFinishingMainActivity(): Boolean

fun registerGlobalErrorHandler() {
  Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionsHandler())
}

expect class GlobalExceptionsHandler(): Thread.UncaughtExceptionHandler {
  override fun uncaughtException(thread: Thread, e: Throwable)
}

enum class SimpleUXHapticType {
  LIGHT,
  MEDIUM,
  HEAVY,
  SUCCESS,
  CLICK
}

expect fun performHapticFeedback(type: SimpleUXHapticType = SimpleUXHapticType.LIGHT)

// SimpleUX haptics toggle (UI-layer preference, persisted outside the frozen model layer)
private const val SIMPLEUX_HAPTICS_ENABLED = "SimpleUXHapticsEnabled"
val simpleUXHapticsEnabled: MutableState<Boolean> by lazy {
  mutableStateOf(settings.getBoolean(SIMPLEUX_HAPTICS_ENABLED, true))
}
fun setSimpleUXHapticsEnabled(enabled: Boolean) {
  settings.putBoolean(SIMPLEUX_HAPTICS_ENABLED, enabled)
  simpleUXHapticsEnabled.value = enabled
}

