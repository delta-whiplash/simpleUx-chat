package chat.simplex.common.views.ux.camera

import chat.simplex.common.helpers.toURI
import chat.simplex.common.views.helpers.ModalManager
import java.net.URI

actual fun openQuickCameraSheet(
  onPhotoCaptured: (URI) -> Unit,
  onQrCode: suspend (String) -> Boolean,
  onTextShared: (String) -> Unit
) {
  ModalManager.start.showCustomModal { close ->
    QuickCameraSheet(
      onClose = close,
      onPhotoCaptured = { uri -> onPhotoCaptured(uri.toURI()) },
      onQrCode = onQrCode,
      onTextShared = onTextShared
    )
  }
}
