package chat.simplex.common.views.helpers

import androidx.compose.runtime.Composable
import java.net.URI

/** #122: the camera tile + recent-gallery grid are Android-only (MediaStore). */
@Composable
actual fun AttachmentTopSection(
  sheetVisible: Boolean,
  onCameraOpened: () -> Unit,
  onMediaPicked: (List<URI>) -> Unit,
  hide: () -> Unit
) {
}
