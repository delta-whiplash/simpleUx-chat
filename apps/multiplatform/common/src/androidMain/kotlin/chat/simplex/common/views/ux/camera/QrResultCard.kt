package chat.simplex.common.views.ux.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.newchat.ConnectTarget
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import chat.simplex.res.MR

// The confirmation card shown for EVERY detected QR code, whatever it decodes
// to (the sheet used to silently drop anything that was not a SimpleX link).
// Stateless: all actions arrive as callbacks, content arrives as QrContent.
// One tap = one action; nothing opens or shares automatically.
@Composable
fun QrResultCard(
  content: QrContent,
  connecting: Boolean,
  onConnect: () -> Unit,
  onOpenUrl: (String) -> Unit,
  onCopy: (String) -> Unit,
  onShare: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val (title, payloadText, icon) = when (content) {
    is QrContent.SimpleXTarget ->
      Triple(
        stringResource(MR.strings.quick_camera_link_detected),
        content.target.displayText(),
        MR.images.ic_link
      )
    is QrContent.Url ->
      Triple(
        stringResource(MR.strings.quick_camera_qr_detected),
        content.url,
        MR.images.ic_open_in_new
      )
    is QrContent.Text ->
      Triple(
        stringResource(MR.strings.quick_camera_qr_detected),
        content.text,
        MR.images.ic_content_copy
      )
  }
  val shape = RoundedCornerShape(16.dp)
  Column(
    Modifier
      .clip(shape)
      .background(Brush.verticalGradient(listOf(CameraChromeCardTop, CameraChromeCardBottom)))
      .border(1.dp, Brush.verticalGradient(listOf(CameraChromeRimHighlight, CameraChromeRimLowlight)), shape)
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .fillMaxWidth()
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        Modifier.size(38.dp).clip(CircleShape).background(AmberGoldWash),
        contentAlignment = Alignment.Center
      ) {
        Icon(painterResource(icon), contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
      }
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)) {
        Text(
          title,
          color = CameraChromeTextMuted,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(1.dp))
        Text(
          payloadText,
          color = CameraChromeTextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(Modifier.width(8.dp))
      Box(
        Modifier
          .size(26.dp)
          .clip(CircleShape)
          .background(CameraChromeRimHighlight)
          .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painterResource(MR.images.ic_close),
          contentDescription = stringResource(MR.strings.modal_close),
          tint = CameraChromeTextSecondary,
          modifier = Modifier.size(15.dp)
        )
      }
    }
    Spacer(Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      when (content) {
        is QrContent.SimpleXTarget -> {
          Button(
            enabled = !connecting,
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(backgroundColor = AmberGold, contentColor = CameraChromeOnGold)
          ) {
            Text(stringResource(MR.strings.quick_camera_connect), fontWeight = FontWeight.SemiBold)
          }
        }
        is QrContent.Url -> {
          Button(
            onClick = { onOpenUrl(content.url) },
            colors = ButtonDefaults.buttonColors(backgroundColor = AmberGold, contentColor = CameraChromeOnGold)
          ) {
            Text(stringResource(MR.strings.quick_camera_open_in_browser), fontWeight = FontWeight.SemiBold)
          }
        }
        is QrContent.Text -> {
          TextButton(onClick = { onCopy(content.text) }) {
            Text(
              stringResource(MR.strings.copy_verb),
              color = AmberGold,
              fontWeight = FontWeight.SemiBold
            )
          }
          Spacer(Modifier.width(6.dp))
          Button(
            onClick = { onShare(content.text) },
            colors = ButtonDefaults.buttonColors(backgroundColor = AmberGold, contentColor = CameraChromeOnGold)
          ) {
            Text(stringResource(MR.strings.share_verb), fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

// What to show for a SimpleX target on the card: the link/URI itself for
// links, the resolved @-name / #group short form for names.
private fun ConnectTarget.displayText(): String =
  when (this) {
    is ConnectTarget.Link -> text
    is ConnectTarget.Name -> text
  }
