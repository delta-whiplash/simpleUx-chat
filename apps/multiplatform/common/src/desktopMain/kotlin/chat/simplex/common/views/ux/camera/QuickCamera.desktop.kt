package chat.simplex.common.views.ux.camera

import androidx.compose.runtime.Composable

// #84: the Scan tab is Android-only (no camera preview pipeline on desktop);
// the island Scan item is not shown there, so this pane is never composed.
@Composable
actual fun QuickCameraPane(onClose: () -> Unit) {
}
