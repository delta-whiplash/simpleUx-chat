package chat.simplex.common.views.ux.camera

import androidx.compose.runtime.Composable
import java.net.URI

// Opens the SimpleUX quick-camera sheet (see the Android actual + QuickCameraSheet.kt).
// Not composable itself: it just triggers a ModalManager modal, so it can be called
// directly from a plain callback (e.g. the central bottom-bar button's onClick).
expect fun openQuickCameraSheet(
  onPhotoCaptured: (URI) -> Unit,
  onQrCode: suspend (String) -> Boolean,
  onTextShared: (String) -> Unit = {}
)

// #84: the same camera as an in-shell pane - rendered INSIDE the Chats tab
// content area (top bar and island bar stay put), so Scan feels like the other
// tabs instead of a dedicated fullscreen page. [onClose] runs on the sheet's
// close affordance and its back handler; hosts typically route it back to CHATS.
@Composable
expect fun QuickCameraPane(onClose: () -> Unit)
