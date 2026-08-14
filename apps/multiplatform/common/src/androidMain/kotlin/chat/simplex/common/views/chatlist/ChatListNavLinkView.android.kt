package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
  val shape = RoundedCornerShape(16.dp)

  val cardBg = if (selectedChat.value) {
    if (isDark) {
      Brush.linearGradient(listOf(Color(0x991E3A5F), Color(0x77152E4D)))
    } else {
      Brush.linearGradient(listOf(Color(0xEEBAE6FD), Color(0xCCE0F2FE)))
    }
  } else {
    if (isDark) {
      Brush.linearGradient(listOf(Color(0x66182232), Color(0x440F172A)))
    } else {
      Brush.linearGradient(listOf(Color(0xF5FFFFFF), Color(0xEEF8FAFC)))
    }
  }

  val borderBrush = if (selectedChat.value) {
    Brush.linearGradient(listOf(Color(0x9938BDF8), Color(0x440284C7)))
  } else {
    if (isDark) {
      Brush.linearGradient(listOf(Color(0x38FFFFFF), Color(0x10FFFFFF)))
    } else {
      Brush.linearGradient(listOf(Color(0x33000000), Color(0x11000000)))
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 3.dp)
      .clip(shape)
      .background(cardBg)
      .border(1.dp, borderBrush, shape)
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
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      chatLinkPreview()
    }
    if (dropdownMenuItems != null) {
      DefaultDropdownMenu(showMenu, dropdownMenuItems = dropdownMenuItems)
    }
  }
}
