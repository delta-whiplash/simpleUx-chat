package chat.simplex.common.views.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.unit.dp
import chat.simplex.common.platform.base64ToBitmap
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.UploadContent
import chat.simplex.common.views.ux.AttachmentPreviewCard
import chat.simplex.res.MR

@Composable
fun ComposeImageView(media: ComposePreview.MediaPreview, cancelImages: () -> Unit, cancelEnabled: Boolean) {
  // FB-3: the attached-images strip is hosted in the shared Luxury Mineral
  // card (rounded glass surface + specular rim + styled remove button); the
  // thumbnails keep the same data (media.images/content) and the same
  // cancelImages callback as before.
  AttachmentPreviewCard(
    cancelEnabled = cancelEnabled,
    onCancel = cancelImages,
    cancelContentDescription = stringResource(MR.strings.icon_descr_cancel_image_preview)
  ) {
    LazyRow(
      Modifier.weight(1f).padding(horizontal = DEFAULT_PADDING_HALF),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(DEFAULT_PADDING_HALF),
    ) {
      itemsIndexed(media.images) { index, item ->
        val content = media.content[index]
        if (content is UploadContent.Video) {
          Box(contentAlignment = Alignment.Center) {
            val imageBitmap = base64ToBitmap(item)
            Image(
              imageBitmap,
              "preview video",
              modifier = Modifier.widthIn(max = 80.dp).height(64.dp).clip(RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Crop
            )
            Icon(
              painterResource(MR.images.ic_videocam_filled),
              "preview video",
              Modifier
                .size(20.dp),
              tint = Color.White
            )
          }
        } else {
          val imageBitmap = base64ToBitmap(item)
          Image(
            imageBitmap,
            "preview image",
            modifier = Modifier.widthIn(max = 80.dp).height(64.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
          )
        }
      }
    }
  }
}
