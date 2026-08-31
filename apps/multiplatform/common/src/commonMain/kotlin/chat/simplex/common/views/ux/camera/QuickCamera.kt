package chat.simplex.common.views.ux.camera

import androidx.compose.runtime.Composable

// #84: the camera as an in-shell pane - rendered INSIDE the Chats tab
// content area (top bar and island bar stay put), so Scan feels like the other
// tabs instead of a dedicated fullscreen page. [onClose] runs on the sheet's
// close affordance and its back handler; hosts typically route it back to CHATS.
@Composable
expect fun QuickCameraPane(onClose: () -> Unit)
