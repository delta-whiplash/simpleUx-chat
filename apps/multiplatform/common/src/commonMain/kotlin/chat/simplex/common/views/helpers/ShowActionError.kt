package chat.simplex.common.views.helpers

import chat.simplex.common.platform.Log
import chat.simplex.common.platform.TAG
import chat.simplex.res.MR

/**
 * #66: user-facing error alerts never show raw stack traces. The user gets
 * the action's title plus a short first-line detail when the exception
 * message is itself human-readable; the full stack always goes to the log.
 */
fun showActionError(title: String, e: Throwable, context: String = title) {
  Log.e(TAG, "$context: ${e.stackTraceToString()}")
  val detail = e.message
    ?.lineSequence()
    ?.firstOrNull()
    ?.takeIf { it.isNotBlank() && it.length <= 160 && !it.contains(" at ") && !it.contains("\t") }
  AlertManager.shared.showAlertMsg(
    title = title,
    text = detail ?: generalGetString(MR.strings.error_something_went_wrong)
  )
}
