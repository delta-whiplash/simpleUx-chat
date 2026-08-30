package chat.simplex.common.views.chat

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.ux.AttachmentPreviewCard
import chat.simplex.res.MR

@Composable
fun ComposeFileView(fileName: String, cancelFile: () -> Unit, cancelEnabled: Boolean) {
  // FB-3: the attached-file row is hosted in the shared Luxury Mineral card
  // (rounded glass surface + specular rim + styled remove button); the file
  // jewel follows the gold-wash disc pattern of ContextItemView's context icon
  // (SKILL 3.4). Same fileName data and same cancelFile callback as before.
  AttachmentPreviewCard(
    cancelEnabled = cancelEnabled,
    onCancel = cancelFile,
    cancelContentDescription = stringResource(MR.strings.icon_descr_cancel_file_preview)
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(CircleShape)
        .background(AmberGoldWash),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painterResource(MR.images.ic_draft_filled),
        stringResource(MR.strings.icon_descr_file),
        Modifier.size(20.dp),
        tint = AmberGold
      )
    }
    Spacer(Modifier.width(10.dp))
    Text(
      fileName,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
      style = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.onBackground
      )
    )
  }
}

@Preview
@Composable
fun PreviewComposeFileView() {
  SimpleXTheme {
    ComposeFileView(
      "test.txt",
      cancelFile = {},
      cancelEnabled = true
    )
  }
}
