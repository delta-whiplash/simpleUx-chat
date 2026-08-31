package chat.simplex.common.views.ux.camera

import androidx.compose.runtime.Composable
import java.net.URI

// No camera-first quick-capture UI on desktop yet; the central button is
// hidden there (see ChatListView.kt's onOpenCamera = ... else null), so this
// is never actually invoked. It only exists to satisfy the expect/actual
// contract for :common:compileKotlinDesktop.
actual fun openQuickCameraSheet(
  onPhotoCaptured: (URI) -> Unit,
  onQrCode: suspend (String) -> Boolean,
  onTextShared: (String) -> Unit
) {
}

// #84: the Scan tab is Android-only (no camera preview pipeline on desktop);
// the island Scan item is not shown there, so this pane is never composed.
@Composable
actual fun QuickCameraPane(onClose: () -> Unit) {
}
