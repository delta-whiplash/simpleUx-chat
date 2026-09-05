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
 * Recent-gallery section of the attachment sheet: a grid of the most recent
 * photos/videos, tap sends it straight into the compose preview. Android
 * actual handles the media permission and the MediaStore query; when the
 * permission is missing it renders nothing (the Gallery action below still
 * opens the system picker, which needs no permission). Desktop: nothing.
 */
@Composable
expect fun RecentGallerySection(onMediaPicked: (List<URI>) -> Unit, hide: () -> Unit)
