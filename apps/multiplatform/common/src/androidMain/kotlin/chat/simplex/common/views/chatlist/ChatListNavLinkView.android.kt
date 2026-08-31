package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chat.simplex.common.platform.onRightClick
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.*

@Composable
actual fun ChatListNavLinkLayout(
  chatLinkPreview: @Composable () -> Unit,
  click: () -> Unit,
  dropdownMenuItems: (@Composable () -> Unit)?,
  showMenu: MutableState<Boolean>,
  disabled: Boolean,
  selectedChat: State<Boolean>,
  nextChatSelected: State<Boolean>,
  selectionActive: Boolean,
  selectionChecked: Boolean,
  selectionToggle: (() -> Unit)?,
) {
  val isDark = isInDarkTheme()

  val rowBg = when {
    // #102: selected rows get the SimpleUX gold wash
    selectionChecked -> if (isDark) Color(0x33E2B755) else Color(0x1AE2B755)
    selectedChat.value -> if (isDark) Color(0x3338BDF8) else Color(0x1A0284C7)
    else -> if (isDark) Color(0xFF131A27) else Color(0xFFFFFFFF)
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(rowBg)
      .then(
        if (!disabled) {
          Modifier
            .combinedClickable(
              onClick = click,
              onLongClick = {
                // #102: long-press always enters/toggles selection (Telegram model);
                // the context menu only exists where no selection is wired
                if (selectionToggle != null) selectionToggle()
                else showMenu.value = true
              }
            )
            .onRightClick {
              if (selectionToggle != null) selectionToggle()
              else showMenu.value = true
            }
        } else Modifier
      )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      chatLinkPreview()
    }
    if (dropdownMenuItems != null) {
      DefaultDropdownMenu(showMenu, dropdownMenuItems = dropdownMenuItems)
    }
  }
}
