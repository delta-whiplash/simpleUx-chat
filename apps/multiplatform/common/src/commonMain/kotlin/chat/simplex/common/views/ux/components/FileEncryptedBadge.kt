package chat.simplex.common.views.ux.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import chat.simplex.common.views.helpers.toDp
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

/**
 * Small scrim-circle badge marking a file/attachment as encrypted at rest
 * (issue #116). Replaces the per-message lock glyph the meta line used to
 * carry: the badge is an overlay on the attachment's own thumbnail, so it
 * describes the file, not the message. Align it inside the thumbnail's Box
 * (e.g. `Modifier.align(Alignment.TopEnd).padding(...)`).
 *
 * Colors follow the existing media-overlay pattern (PlayButton in
 * CIVideoView): black scrim + white glyph, no new accent tokens.
 */
@Composable
fun FileEncryptedBadge(modifier: Modifier = Modifier) {
  Surface(
    modifier,
    color = Color.Black.copy(alpha = 0.25f),
    shape = RoundedCornerShape(percent = 50),
    contentColor = LocalContentColor.current
  ) {
    Box(
      Modifier.size(20.sp.toDp()),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painterResource(MR.images.ic_lock),
        contentDescription = null,
        Modifier.size(12.sp.toDp()),
        tint = Color.White
      )
    }
  }
}
