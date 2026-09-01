package chat.simplex.common.views.ux

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.ChatFolder
import chat.simplex.common.platform.ChatFoldersPrefs
import chat.simplex.common.ui.theme.AmberGold
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.MR

// #101: modal listing custom folders with a checkbox per folder; checking adds
// the chat ids to the folder's includes, unchecking removes them. Shared by
// the chat context menu (single chat) and the selection mode (batch, #102).
fun showAddToFolderModal(chatIds: Collection<String>) {
  ModalManager.start.showModalCloseable { close ->
    AddToFolderView(chatIds = chatIds, close = close)
  }
}

@Composable
fun AddToFolderView(chatIds: Collection<String>, close: () -> Unit) {
  var folders by remember { mutableStateOf(ChatFoldersPrefs.loadFolders()) }
  val customFolders = folders.filter { it.isCustom }

  Column(
    Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
  ) {
    Text(
      text = generalGetString(MR.strings.add_to_folder),
      fontFamily = PlusJakartaSans,
      fontSize = 17.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colors.onBackground,
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )

    if (customFolders.isEmpty()) {
      Text(
        text = generalGetString(MR.strings.chat_folders_none_yet),
        fontFamily = PlusJakartaSans,
        fontSize = 14.sp,
        color = MaterialTheme.colors.secondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
      )
    }

    customFolders.forEach { folder ->
      val allIn = chatIds.all { folder.includedChatIds.contains(it) }
      fun toggleFolder() {
        val updated = if (allIn) {
          folder.copy(includedChatIds = folder.includedChatIds - chatIds.toSet())
        } else {
          folder.copy(includedChatIds = folder.includedChatIds + chatIds)
        }
        ChatFoldersPrefs.saveFolder(updated)
        folders = ChatFoldersPrefs.loadFolders()
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { toggleFolder() }
          .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Filled.Folder,
          contentDescription = null,
          tint = AmberGold,
          modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        val label = folder.name ?: folder.emoji ?: folder.id
        Text(
          text = label,
          fontFamily = PlusJakartaSans,
          fontSize = 15.sp,
          color = MaterialTheme.colors.onBackground,
          modifier = Modifier.weight(1f)
        )
        Checkbox(
          checked = allIn,
          onCheckedChange = { _ -> toggleFolder() },
          colors = CheckboxDefaults.colors(checkedColor = AmberGold)
        )
      }
    }
  }
}
