package chat.simplex.common.views.helpers

import androidx.compose.runtime.Composable
import java.net.URI

/** #122: the recent-gallery grid is Android-only (MediaStore). */
@Composable
actual fun RecentGallerySection(onMediaPicked: (List<URI>) -> Unit, hide: () -> Unit) {
}
