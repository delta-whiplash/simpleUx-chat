package chat.simplex.common.views.ux

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.ui.theme.Slate400
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

// #102: Telegram-style transformed top bar for chat-list selection mode:
// X + selected count on the left, batch actions on the right. All actions are
// real (pin / mark read-unread / add to folder / delete) - no decorative icons.
// Action glyphs share one style: filled Material vectors tinted Slate400 -
// the gold circle-folder badge is an avatar asset, not an action glyph.
@Composable
fun ChatSelectionTopBar(
  count: Int,
  anyUnpinned: Boolean,
  anyUnread: Boolean,
  deleteEnabled: Boolean,
  onClose: () -> Unit,
  onPin: () -> Unit,
  onToggleRead: () -> Unit,
  onAddToFolder: () -> Unit,
  onDelete: () -> Unit
) {
  Row(
    // #102: without this the bar draws under the system status bar and its
    // action icons are untouchable (found on emulator-5554)
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(onClick = onClose) {
      Icon(
        painter = painterResource(MR.images.ic_close),
        contentDescription = generalGetString(MR.strings.cancel_verb),
        tint = MaterialTheme.colors.onBackground,
        modifier = Modifier.size(22.dp)
      )
    }
    Text(
      text = count.toString(),
      fontFamily = PlusJakartaSans,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colors.onBackground
    )
    Spacer(Modifier.weight(1f))

    IconButton(onClick = onPin) {
      Icon(
        painter = painterResource(MR.images.ic_pin),
        contentDescription = generalGetString(if (anyUnpinned) MR.strings.pin_chat else MR.strings.unpin_chat),
        tint = Slate400,
        modifier = Modifier.size(20.dp)
      )
    }
    IconButton(onClick = onToggleRead) {
      Icon(
        painter = painterResource(if (anyUnread) MR.images.ic_check else MR.images.ic_mark_chat_unread),
        contentDescription = generalGetString(if (anyUnread) MR.strings.mark_read else MR.strings.mark_unread),
        tint = Slate400,
        modifier = Modifier.size(20.dp)
      )
    }
    IconButton(onClick = onAddToFolder) {
      Icon(
        imageVector = Icons.Filled.DriveFileMove,
        contentDescription = generalGetString(MR.strings.add_to_folder),
        tint = Slate400,
        modifier = Modifier.size(20.dp)
      )
    }
    IconButton(onClick = onDelete, enabled = deleteEnabled) {
      Icon(
        imageVector = Icons.Filled.Delete,
        contentDescription = generalGetString(MR.strings.delete_verb),
        // explicit tint bypasses IconButton's disabled dimming (it rides on
        // LocalContentAlpha), so the disabled state is applied here
        tint = Slate400.copy(alpha = if (deleteEnabled) ContentAlpha.high else ContentAlpha.disabled),
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
