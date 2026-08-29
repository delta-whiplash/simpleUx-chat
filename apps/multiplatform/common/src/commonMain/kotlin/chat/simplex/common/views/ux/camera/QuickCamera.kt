package chat.simplex.common.views.ux.camera

import java.net.URI

// Opens the SimpleUX quick-camera sheet (see the Android actual + QuickCameraSheet.kt).
// Not composable itself: it just triggers a ModalManager modal, so it can be called
// directly from a plain callback (e.g. the central bottom-bar button's onClick).
expect fun openQuickCameraSheet(
  onPhotoCaptured: (URI) -> Unit,
  onQrCode: suspend (String) -> Boolean,
  onTextShared: (String) -> Unit = {}
)
