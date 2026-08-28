package chat.simplex.common.views.ux.camera

import java.net.URI

// No camera-first quick-capture UI on desktop yet; the central button is
// hidden there (see ChatListView.kt's onOpenCamera = ... else null), so this
// is never actually invoked. It only exists to satisfy the expect/actual
// contract for :common:compileKotlinDesktop.
actual fun openQuickCameraSheet(
  onPhotoCaptured: (URI) -> Unit,
  onQrCode: suspend (String) -> Boolean
) {
}
