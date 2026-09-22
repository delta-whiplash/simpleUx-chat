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

  // #175: flat mineral rows - no card-per-row. Rows sit on the canvas and
  // only light up for state: gold wash when checked in selection mode, quiet
  // neutral wash for the chat currently open (Telegram model).
  val rowBg = when {
    selectionChecked -> if (isDark) AmberGoldWash else AmberGold.copy(alpha = 0.10f)
    selectedChat.value -> if (isDark) MineralRowOpenDark else MineralRowOpenLight
    else -> Color.Transparent
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 2.dp)
      .clip(RoundedCornerShape(14.dp))
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
