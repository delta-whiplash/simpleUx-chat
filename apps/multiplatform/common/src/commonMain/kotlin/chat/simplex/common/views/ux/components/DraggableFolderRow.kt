package chat.simplex.common.views.ux.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.ChatFolder
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.ui.theme.Slate400
import chat.simplex.common.ui.theme.Slate600
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

// #98: Draggable row for chat folder management. Shows drag handle (≡),
// folder name/emoji, and menu button (⋮) for edit/delete actions.

@Composable
fun DraggableFolderRow(
  folder: ChatFolder,
  onEdit: (ChatFolder) -> Unit,
  onDelete: (ChatFolder) -> Unit,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colors.background)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Drag handle
    Icon(
      imageVector = Icons.Filled.DragHandle,
      contentDescription = null,
      tint = Slate400,
      modifier = Modifier.size(24.dp)
    )

    // Folder name/emoji display
    Row(
      modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Show emoji if present
      folder.emoji?.let { emoji ->
        Text(
          text = emoji,
          fontSize = 20.sp,
          modifier = Modifier.padding(end = 8.dp)
        )
      }

      // Show name or fallback to filter kind label
      val displayName = folder.name
        ?: UxFilterCategory.entries.getOrNull(folder.filterKind)?.localizedLabel()
        ?: stringResource(MR.strings.chat_folders_all_chats)

      Text(
        text = displayName,
        fontFamily = PlusJakartaSans,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.onBackground
      )
    }

    // Menu button
    Box {
      IconButton(
        onClick = { showMenu = true },
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Filled.MoreVert,
          contentDescription = null,
          tint = Slate400,
          modifier = Modifier.size(20.dp)
        )
      }

      DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
      ) {
        DropdownMenuItem(
          onClick = {
            showMenu = false
            onEdit(folder)
          }
        ) {
          Text(
            text = stringResource(MR.strings.chat_folders_edit),
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
            color = Slate600
          )
        }

        DropdownMenuItem(
          onClick = {
            showMenu = false
            onDelete(folder)
          }
        ) {
          Text(
            text = stringResource(MR.strings.chat_folders_delete),
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
            color = MaterialTheme.colors.error
          )
        }
      }
    }
  }
}
