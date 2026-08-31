package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import chat.simplex.common.platform.onRightClick
import chat.simplex.common.views.helpers.*

object NoIndication : IndicationNodeFactory {
  // Should be as a class, not an object. Otherwise, crash
  private class NoIndicationInstance : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() { drawContent() }
  }
  override fun create(interactionSource: InteractionSource): DelegatableNode = NoIndicationInstance()
  override fun hashCode(): Int = -1
  override fun equals(other: Any?) = other === this
}

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
  var modifier = Modifier.fillMaxWidth()
  if (!disabled) modifier = modifier
    .combinedClickable(
      onClick = click,
      onLongClick = {
        // #102: long-press always enters/toggles selection (Telegram model);
        // the context menu only remains where no selection is wired
        if (selectionToggle != null) selectionToggle()
        else showMenu.value = true
      }
    )
    .onRightClick {
      if (selectionToggle != null) selectionToggle()
      else showMenu.value = true
    }
  CompositionLocalProvider(
    LocalIndication provides if (selectedChat.value && !disabled) NoIndication else LocalIndication.current
  ) {
    Box(modifier) {
      if (selectionChecked) {
        Box(Modifier.matchParentSize().background(MaterialTheme.colors.primary.copy(alpha = 0.12f)))
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top
      ) {
        chatLinkPreview()
      }
      if (selectedChat.value) {
        Box(Modifier.matchParentSize().background(MaterialTheme.colors.onBackground.copy(0.05f)))
      }
      if (dropdownMenuItems != null) {
        DefaultDropdownMenu(showMenu, dropdownMenuItems = dropdownMenuItems)
      }
    }
  }
}
