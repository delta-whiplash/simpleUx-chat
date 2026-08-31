package chat.simplex.common.views.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import chat.simplex.common.model.ChatModel
import chat.simplex.common.platform.ChatFolder
import chat.simplex.common.platform.ChatFoldersPrefs
import chat.simplex.common.ui.theme.AmberGold
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.ui.theme.Slate200
import chat.simplex.common.ui.theme.Slate400
import chat.simplex.common.ui.theme.Slate600
import chat.simplex.common.views.helpers.DefaultAppBar
import chat.simplex.common.views.helpers.NavigationButtonBack
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.common.views.ux.components.ChatFolderEditDialog
import chat.simplex.common.views.ux.components.localizedLabel
import chat.simplex.common.views.ux.components.UxFilterCategory
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

// #98: Chat Folders manager. One list, the five preset folders: toggle
// visibility, rename / add an emoji, reorder by long-press drag.
// Custom folder creation is deliberately absent until chats can actually be
// assigned to folders - a folder that filters nothing is a fake affordance.
@Composable
fun ChatFoldersSettingsScreen(
  chatModel: ChatModel,
  onBack: () -> Unit
) {
  var folders by remember {
    mutableStateOf(ChatFoldersPrefs.loadFolders().sortedBy { it.order })
  }
  var editingFolder by remember { mutableStateOf<ChatFolder?>(null) }

  Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
    DefaultAppBar(
      navigationButton = { NavigationButtonBack(onButtonClicked = onBack) },
      fixedTitleText = generalGetString(MR.strings.settings_chat_folders),
      buttons = {},
      onTop = true,
      solidBackground = true
    )

    FolderList(
      folders = folders,
      onReorder = { list ->
        folders = list
        ChatFoldersPrefs.saveFolders(list)
      },
      onEdit = { editingFolder = it }
    )
  }

  editingFolder?.let { folder ->
    ChatFolderEditDialog(
      initialFolder = folder,
      onDismiss = { editingFolder = null },
      onSave = { updated ->
        val list = folders.map { if (it.id == updated.id) updated.copy(order = it.order) else it }
        folders = list
        ChatFoldersPrefs.saveFolders(list)
        editingFolder = null
      }
    )
  }
}

@Composable
private fun FolderList(
  folders: List<ChatFolder>,
  onReorder: (List<ChatFolder>) -> Unit,
  onEdit: (ChatFolder) -> Unit
) {
  // Long-press drag to reorder: the dragged row follows the finger and rows
  // swap once it crosses ~60% of the row height. Order is persisted live.
  val current by rememberUpdatedState(folders)
  var dragIndex by remember { mutableStateOf(-1) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  fun move(from: Int, to: Int) {
    val list = current.toMutableList()
    val item = list.removeAt(from)
    list.add(to, item)
    onReorder(list.mapIndexed { i, f -> f.copy(order = i) })
  }

  Column(Modifier.fillMaxSize()) {
    Text(
      text = generalGetString(MR.strings.settings_chat_folders),
      fontFamily = PlusJakartaSans,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = Slate600,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )

    folders.forEachIndexed { index, folder ->
      val dragging = index == dragIndex
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .zIndex(if (dragging) 1f else 0f)
          .graphicsLayer { translationY = if (dragging) offsetY else 0f }
          .background(if (dragging) MaterialTheme.colors.surface else MaterialTheme.colors.background)
          .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
              onDragStart = { dragIndex = index; offsetY = 0f },
              onDrag = { change, amount ->
                change.consume()
                if (dragIndex == -1) dragIndex = index
                offsetY += amount.y
                val rowH = size.height.toFloat().coerceAtLeast(1f)
                while (dragIndex < current.size - 1 && offsetY > rowH * 0.6f) {
                  move(dragIndex, dragIndex + 1); dragIndex++; offsetY -= rowH
                }
                while (dragIndex > 0 && offsetY < -rowH * 0.6f) {
                  move(dragIndex, dragIndex - 1); dragIndex--; offsetY += rowH
                }
              },
              onDragEnd = { dragIndex = -1; offsetY = 0f },
              onDragCancel = { dragIndex = -1; offsetY = 0f }
            )
          }
          .clickable { onEdit(folder) }
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Filled.DragHandle,
          contentDescription = null,
          tint = Slate400,
          modifier = Modifier.size(24.dp)
        )
        folder.emoji?.let { emoji ->
          Text(
            text = emoji,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 16.dp)
          )
        }
        val label = folder.name
          ?: UxFilterCategory.entries.getOrNull(folder.filterKind)?.localizedLabel()
          ?: generalGetString(MR.strings.chat_list_all)
        Text(
          text = label,
          fontFamily = PlusJakartaSans,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colors.onBackground,
          modifier = Modifier
            .weight(1f)
            .padding(start = if (folder.emoji != null) 8.dp else 16.dp)
        )
        Switch(
          checked = folder.isVisible,
          onCheckedChange = { enabled ->
            val list = current.map { if (it.id == folder.id) it.copy(isVisible = enabled) else it }
            onReorder(list)
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = AmberGold,
            checkedTrackColor = AmberGold.copy(alpha = 0.3f),
            uncheckedThumbColor = Slate400,
            uncheckedTrackColor = Slate200
          )
        )
      }
    }
  }
}
