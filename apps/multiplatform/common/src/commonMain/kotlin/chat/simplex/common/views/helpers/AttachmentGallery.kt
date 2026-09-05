package chat.simplex.common.views.helpers

import androidx.compose.runtime.Composable
import java.net.URI

/**
 * #122: one recent item from the device gallery, shown as a thumbnail in the
 * attachment sheet. Android only - the desktop actual renders nothing.
 */
data class RecentMediaItem(
  val uri: String,
  val isVideo: Boolean,
  val durationSec: Int
)

/**
 * #122 phase 2: top section of the attachment sheet - a live camera tile
 * next to the recent-gallery grid. The tile only binds the camera while the
 * sheet is visible (the sheet content composes eagerly while hidden - see
 * the #99 camera-perf class); tapping it opens the existing full-screen
 * capture flow. The grid is tap-to-send via the compose preview path. When
 * media permission is missing the grid renders nothing. Desktop: nothing.
 */
@Composable
expect fun AttachmentTopSection(
  sheetVisible: Boolean,
  onCameraOpened: () -> Unit,
  onMediaPicked: (List<URI>) -> Unit,
  hide: () -> Unit
)
