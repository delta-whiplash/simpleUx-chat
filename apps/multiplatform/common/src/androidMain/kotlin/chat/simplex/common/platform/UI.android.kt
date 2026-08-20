package chat.simplex.common.platform

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.*
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import chat.simplex.common.AppScreen
import chat.simplex.common.model.clearAndNotify
import chat.simplex.common.views.helpers.*
import androidx.compose.ui.platform.LocalContext as LocalContext1
import chat.simplex.res.MR
import kotlinx.coroutines.*

actual fun showToast(text: String, timeout: Long) = Toast.makeText(androidAppContext, text, Toast.LENGTH_SHORT).show()

@Composable
actual fun LockToCurrentOrientationUntilDispose() {
  val context = LocalContext1.current
  DisposableEffect(Unit) {
    val activity = (context as Activity?) ?: return@DisposableEffect onDispose {}
    val manager = context.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
    val rotation = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) manager.defaultDisplay.rotation else activity.display?.rotation
    activity.requestedOrientation = when (rotation) {
      Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
      else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    // Unlock orientation
    onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
  }
}

@Composable
actual fun LocalMultiplatformView(): Any? = LocalView.current

@Composable
actual fun getKeyboardState(): State<KeyboardState> {
  val density = LocalDensity.current
  val ime = WindowInsets.ime
  return remember {
    derivedStateOf {
      if (ime.getBottom(density) == 0) KeyboardState.Closed else KeyboardState.Opened
    }
  }
}

actual fun hideKeyboard(view: Any?, clearFocus: Boolean) {
  // LALAL
  //  LocalSoftwareKeyboardController.current?.hide()
  if (view is View) {
    if (clearFocus) {
      view.clearFocus()
    }
    (androidAppContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
  }
}

actual fun androidIsFinishingMainActivity(): Boolean = (mainActivity.get()?.isFinishing == true)

actual class GlobalExceptionsHandler: Thread.UncaughtExceptionHandler {
  actual override fun uncaughtException(thread: Thread, e: Throwable) {
    Log.e(TAG, "App crashed, thread name: " + thread.name + ", exception: " + e.stackTraceToString())
    includeMoreFailedComposables()
    if (ModalManager.start.hasModalsOpen()) {
      ModalManager.start.closeModal()
    } else if (chatModel.chatId.value != null) {
      withApi {
        withContext(Dispatchers.Main) {
          // Since no modals are open, the problem is probably in ChatView
          chatModel.chatId.value = null
          chatModel.chatsContext.chatItems.clearAndNotify()
        }
      }
    } else {
      // ChatList, nothing to do. Maybe to show other view except ChatList
    }
    chatModel.activeCall.value?.let {
      withBGApi {
        chatModel.callManager.endCall(it)
      }
    }
    if (thread.name == "main") {
      mainActivity.get()?.recreate()
    } else {
      mainActivity.get()?.apply {
        runOnUiThread {
          window
            ?.decorView
            ?.findViewById<ViewGroup>(android.R.id.content)
            ?.removeViewAt(0)
          setContent {
            AppScreen()
          }
        }
      }
    }
    // Wait until activity recreates to prevent showing two alerts (in case `main` was crashed)
    Handler(Looper.getMainLooper()).post {
      AlertManager.shared.showAlertMsg(
        title = generalGetString(MR.strings.app_was_crashed),
        text = e.stackTraceToString(),
        shareText = true
      )
    }
  }
}

actual fun performHapticFeedback(type: SimpleUXHapticType) {
  try {
    val activity = mainActivity.get()
    val view = activity?.window?.decorView
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && view != null) {
      val feedbackConstant = when (type) {
        SimpleUXHapticType.LIGHT -> HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        SimpleUXHapticType.MEDIUM -> HapticFeedbackConstants.CONFIRM
        SimpleUXHapticType.HEAVY -> HapticFeedbackConstants.REJECT
        SimpleUXHapticType.SUCCESS -> HapticFeedbackConstants.CONFIRM
        SimpleUXHapticType.CLICK -> HapticFeedbackConstants.KEYBOARD_TAP
      }
      view.performHapticFeedback(feedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
    } else if (view != null) {
      val feedbackConstant = when (type) {
        SimpleUXHapticType.HEAVY -> HapticFeedbackConstants.LONG_PRESS
        else -> HapticFeedbackConstants.KEYBOARD_TAP
      }
      view.performHapticFeedback(feedbackConstant)
    } else {
      val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = androidAppContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        androidAppContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
      if (vibrator != null && vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val effect = when (type) {
            SimpleUXHapticType.LIGHT -> VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
            SimpleUXHapticType.MEDIUM -> VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
            SimpleUXHapticType.HEAVY -> VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
            SimpleUXHapticType.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 15, 40, 20), -1)
            SimpleUXHapticType.CLICK -> VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE)
          }
          vibrator.vibrate(effect)
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(20)
        }
      }
    }
  } catch (_: Throwable) {
    // Ignore haptic errors gracefully
  }
}

