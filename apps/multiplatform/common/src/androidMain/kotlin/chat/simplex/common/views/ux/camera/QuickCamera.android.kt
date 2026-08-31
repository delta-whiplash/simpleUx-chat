package chat.simplex.common.views.ux.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import chat.simplex.common.helpers.toURI
import chat.simplex.common.model.*
import chat.simplex.common.platform.chatModel
import chat.simplex.common.views.chatlist.connect
import chat.simplex.common.views.helpers.SharedContent
import chat.simplex.common.views.newchat.ConnectTarget
import chat.simplex.common.views.newchat.strConnectTarget

// #84: the camera as an in-shell pane (Scan tab). Same routing semantics the
// modal version had: photos and shared text use the proven ShareListView
// hand-off via sharedContent; SimpleX links connect immediately; other QR
// content is surfaced by the sheet's universal QR routing.
@Composable
actual fun QuickCameraPane(onClose: () -> Unit) {
  val quickCameraConnectFilter = remember { mutableStateOf(emptySet<String>()) }
  QuickCameraSheet(
    onClose = onClose,
    onPhotoCaptured = { uri ->
      // Reuses the same cross-chat hand-off as Android's system share-into-SimpleX
      // flow: setting sharedContent swaps this screen for ShareListView (App.kt's
      // StartPartOfScreen), which already knows how to pick a chat and attach media.
      chatModel.sharedContent.value = SharedContent.Media(text = "", uris = listOf(uri.toURI()))
    },
    onQrCode = { link ->
      val target = strConnectTarget(link.trim())
      if (target is ConnectTarget.Link) {
        connect(target.text, quickCameraConnectFilter) {}
        true
      } else {
        false
      }
    },
    onTextShared = { text ->
      chatModel.sharedContent.value = SharedContent.Text(text)
    }
  )
}
