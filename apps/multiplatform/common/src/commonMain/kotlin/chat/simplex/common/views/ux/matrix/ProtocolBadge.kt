package chat.simplex.common.views.ux.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.simplex.common.ui.theme.GlassBorderDark
import chat.simplex.common.ui.theme.GlassBorderLight
import chat.simplex.common.ui.theme.Sky400
import chat.simplex.common.ui.theme.Slate800
import chat.simplex.common.ui.theme.isInDarkTheme
import androidx.compose.desktop.ui.tooling.preview.Preview

/**
 * Protocol identity markers for co-protocol (Matrix) chats. See SPECS.md in this package.
 *
 * Design language: Luxury Mineral. The badge is a mineral disc in the "Links / Connection
 * Azure" accent token ([Sky400]) carrying a hub glyph (network/protocol, no brand asset),
 * with the system's hairline specular rim to match the bevel language of other discs.
 * No raw color literals: every color comes from theme tokens. Stateless and parameter-free
 * by design - visibility is the caller's gate (`chat.protocol == ChatProtocol.Matrix`).
 */

/** Avatar-corner overlay: 14.dp azure disc + hub glyph, hairline rim, sits on the avatar edge. */
@Composable
fun ProtocolBadgeOverlay(modifier: Modifier = Modifier) {
  MineralProtocolDisc(size = 14.dp, glyphSize = 9.dp, modifier = modifier)
}

/** Name-row inline badge: compact capsule version of the same mineral disc. */
@Composable
fun ProtocolBadgeInline(modifier: Modifier = Modifier) {
  MineralProtocolDisc(size = 11.dp, glyphSize = 7.dp, modifier = modifier)
}

@Composable
private fun MineralProtocolDisc(size: Dp, glyphSize: Dp, modifier: Modifier) {
  val rim = if (isInDarkTheme()) GlassBorderDark else GlassBorderLight
  Box(
    modifier
      .size(size)
      .clip(CircleShape)
      .background(Sky400)
      .border(1.dp, rim, CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      Icons.Filled.Hub,
      contentDescription = null,
      tint = Slate800,
      modifier = Modifier.size(glyphSize)
    )
  }
}

@Preview
@Composable
fun ProtocolBadgeOverlayPreview() {
  Row(verticalAlignment = Alignment.CenterVertically) {
    ProtocolBadgeOverlay(Modifier.padding(8.dp))
    ProtocolBadgeInline(Modifier.padding(8.dp))
  }
}
