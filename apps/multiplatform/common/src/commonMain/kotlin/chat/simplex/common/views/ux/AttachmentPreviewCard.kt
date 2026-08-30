package chat.simplex.common.views.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import chat.simplex.common.ui.theme.CameraChromeCardBottom
import chat.simplex.common.ui.theme.CameraChromeCardTop
import chat.simplex.common.ui.theme.CameraChromeRimHighlight
import chat.simplex.common.ui.theme.CameraChromeRimLowlight
import chat.simplex.common.ui.theme.CameraChromeTextSecondary
import chat.simplex.common.ui.theme.GlassTokens
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

/**
 * FB-3: Luxury Mineral card chrome for the composer's attachment previews
 * (previously a flat sent-color strip with a bare close icon).
 *
 * Owns only the card surface: vertical mineral-card gradient (SKILL 2.1),
 * 1.dp specular rim hairline (SKILL 2.2), and a polished circular remove
 * button with a proper 40.dp touch target (SKILL 3.4 close-button pattern).
 * The attached content (thumbnails, file icon + name) is passed via the
 * content slot by [chat.simplex.common.views.chat.ComposeImageView] /
 * [chat.simplex.common.views.chat.ComposeFileView], which keep the same
 * cancel callbacks as before — remove/send logic is unchanged.
 *
 * Parameterized and side-effect-free: constructible in isolation (AGENTS §5).
 */
@Composable
fun AttachmentPreviewCard(
  cancelEnabled: Boolean,
  onCancel: () -> Unit,
  cancelContentDescription: String,
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit
) {
  val isDark = isInDarkTheme()
  val shape = RoundedCornerShape(16.dp)
  Row(
    modifier
      .padding(top = 8.dp)
      .clip(shape)
      .background(
        if (isDark) Brush.verticalGradient(listOf(CameraChromeCardTop, CameraChromeCardBottom))
        else Brush.verticalGradient(listOf(Color.White, GlassTokens.SearchBarBgLight))
      )
      .border(
        width = 1.dp,
        brush = if (isDark) Brush.verticalGradient(listOf(CameraChromeRimHighlight, CameraChromeRimLowlight))
                else SolidColor(GlassTokens.ChatListCardBorderLight),
        shape = shape
      )
      .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    content()
    if (cancelEnabled) {
      // Styled remove button: 40.dp touch target, mineral disc, hairline glyph.
      IconButton(onClick = onCancel, modifier = Modifier.size(40.dp)) {
        Box(
          modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            painterResource(MR.images.ic_close),
            cancelContentDescription,
            tint = if (isDark) CameraChromeTextSecondary else GlassTokens.FilterChipInactiveTextLight,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }
  }
}
