package chat.simplex.common.views.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.appPlatform
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.ui.theme.HighOrLowlight
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

sealed class AttachmentOption {
  object CameraPhoto: AttachmentOption()
  object GalleryImage: AttachmentOption()
  object GalleryVideo: AttachmentOption()
  object File: AttachmentOption()
}

/**
 * #56: bottom attachment panel in the Luxury Mineral language - rounded
 * sheet, circular tinted icon wells with labels underneath, instead of the
 * old flat icon row whose width fractions came from a removed column layout
 * and overflowed. One implementation for both platforms (the camera action
 * is Android-only; platform differences start at the system picker, behind
 * AttachmentSelection).
 */
@Composable
fun ChooseAttachmentView(attachmentOption: MutableState<AttachmentOption?>, hide: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .imePadding()
      .wrapContentHeight()
  ) {
    Column(
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        .background(MaterialTheme.colors.background)
        .padding(vertical = DEFAULT_PADDING)
    ) {
      Box(
        Modifier
          .padding(bottom = 12.dp)
          .size(width = 40.dp, height = 4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(HighOrLowlight)
          .align(Alignment.CenterHorizontally)
      )
      Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        AttachmentAction(MR.images.ic_camera_enhance, MR.strings.use_camera_button, visible = appPlatform.isAndroid) {
          attachmentOption.value = AttachmentOption.CameraPhoto
          hide()
        }
        AttachmentAction(MR.images.ic_add_photo, MR.strings.gallery_image_button) {
          attachmentOption.value = AttachmentOption.GalleryImage
          hide()
        }
        AttachmentAction(MR.images.ic_smart_display, MR.strings.gallery_video_button) {
          attachmentOption.value = AttachmentOption.GalleryVideo
          hide()
        }
        AttachmentAction(MR.images.ic_note_add, MR.strings.choose_file) {
          attachmentOption.value = AttachmentOption.File
          hide()
        }
      }
    }
  }
}

@Composable
private fun AttachmentAction(
  icon: ImageResource,
  label: StringResource,
  visible: Boolean = true,
  onClick: () -> Unit
) {
  if (!visible) return
  Column(
    Modifier
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .widthIn(min = 78.dp)
      .padding(vertical = 10.dp, horizontal = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colors.primary.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painterResource(icon),
        contentDescription = stringResource(label),
        tint = MaterialTheme.colors.primary,
        modifier = Modifier.size(24.dp)
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      stringResource(label),
      fontSize = 12.sp,
      color = MaterialTheme.colors.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
