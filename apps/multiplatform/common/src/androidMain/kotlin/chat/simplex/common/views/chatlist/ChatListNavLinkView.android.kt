package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
) {
  val isDark = isInDarkTheme()

  val rowBg = if (selectedChat.value) {
    if (isDark) Color(0x3338BDF8) else Color(0x1A0284C7)
  } else {
    Color.Transparent
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(rowBg)
      .then(
        if (!disabled) {
          Modifier
            .combinedClickable(onClick = click, onLongClick = { showMenu.value = true })
            .onRightClick { showMenu.value = true }
        } else Modifier
      )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      chatLinkPreview()
    }
    // Indented subtle divider after avatar (74dp padding start, like Telegram)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomEnd)
        .padding(start = 74.dp)
        .height(0.5.dp)
        .background(if (isDark) Color(0x1FFFFFFF) else Color(0x12000000))
    )
    if (dropdownMenuItems != null) {
      DefaultDropdownMenu(showMenu, dropdownMenuItems = dropdownMenuItems)
    }
  }
}
