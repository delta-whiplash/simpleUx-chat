package chat.simplex.common.views.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.ChatModel
import chat.simplex.common.platform.ChatFolder
import chat.simplex.common.platform.ChatFoldersPrefs
import chat.simplex.common.ui.theme.AmberGold
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.ui.theme.Slate200
import chat.simplex.common.ui.theme.Slate400
import chat.simplex.common.ui.theme.Slate600
import chat.simplex.common.views.ux.components.ChatFolderEditDialog
import chat.simplex.common.views.ux.components.DraggableFolderRow
import chat.simplex.common.views.ux.components.UxFilterCategory
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

// #98: Chat Folders settings screen. Allows users to manage which filter
// categories are visible, reorder them, and create custom folders.
// Telegram-like UX with recommended folders, active list, and create dialog.

@Composable
fun ChatFoldersSettingsScreen(
  chatModel: ChatModel,
  onBack: () -> Unit
) {
  var folders by remember { mutableStateOf(ChatFoldersPrefs.loadFolders()) }
  var editingFolder by remember { mutableStateOf<ChatFolder?>(null) }
  var showCreateDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(MR.strings.settings_chat_folders),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colors.onBackground
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colors.onBackground
            )
          }
        },
        backgroundColor = MaterialTheme.colors.background,
        elevation = 0.dp
      )
    },
    backgroundColor = MaterialTheme.colors.background
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      // Recommended Folders section
      item {
        Text(
          text = stringResource(MR.strings.chat_folders_recommended),
          fontFamily = PlusJakartaSans,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = AmberGold,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
      }

      // Recommended: Unread
      item {
        RecommendedFolderRow(
          name = stringResource(MR.strings.chat_folders_recommended_unread),
          description = stringResource(MR.strings.chat_folders_recommended_unread_desc),
          isAdded = folders.any { it.filterKind == UxFilterCategory.UNREAD.ordinal && it.isVisible },
          onAdd = {
            folders = folders.map {
              if (it.filterKind == UxFilterCategory.UNREAD.ordinal) {
                it.copy(isVisible = true)
              } else it
            }.ifEmpty {
              listOf(ChatFolder(id = "unread", filterKind = UxFilterCategory.UNREAD.ordinal, isVisible = true, order = folders.size))
            }
            ChatFoldersPrefs.saveFolders(folders)
          }
        )
      }

      // Recommended: Personal (Direct)
      item {
        RecommendedFolderRow(
          name = stringResource(MR.strings.chat_folders_recommended_personal),
          description = stringResource(MR.strings.chat_folders_recommended_personal_desc),
          isAdded = folders.any { it.filterKind == UxFilterCategory.DIRECT.ordinal && it.isVisible },
          onAdd = {
            folders = folders.map {
              if (it.filterKind == UxFilterCategory.DIRECT.ordinal) {
                it.copy(isVisible = true)
              } else it
            }.ifEmpty {
              listOf(ChatFolder(id = "direct", filterKind = UxFilterCategory.DIRECT.ordinal, isVisible = true, order = folders.size))
            }
            ChatFoldersPrefs.saveFolders(folders)
          }
        )
      }

      item {
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Active Folders section
      item {
        Text(
          text = stringResource(MR.strings.chat_folders_active),
          fontFamily = PlusJakartaSans,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = AmberGold,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
      }

      // Active folders list (with up/down reorder buttons)
      val visibleFolders = folders.filter { it.isVisible }.sortedBy { it.order }
      itemsIndexed(visibleFolders) { index, folder ->
        Column {
          DraggableFolderRow(
            folder = folder,
            onEdit = { editingFolder = it },
            onDelete = { folderToDelete ->
              folders = folders.map {
                if (it.id == folderToDelete.id) it.copy(isVisible = false) else it
              }
              ChatFoldersPrefs.saveFolders(folders)
            }
          )

          // Reorder buttons
          if (index < visibleFolders.size - 1) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.Center
            ) {
              IconButton(
                onClick = {
                  // Move folder down (swap with next)
                  val currentOrder = folder.order
                  val nextFolder = visibleFolders.getOrNull(index + 1)
                  if (nextFolder != null) {
                    folders = folders.map {
                      when {
                        it.id == folder.id -> it.copy(order = nextFolder.order)
                        it.id == nextFolder.id -> it.copy(order = currentOrder)
                        else -> it
                      }
                    }
                    ChatFoldersPrefs.saveFolders(folders)
                  }
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.KeyboardArrowDown,
                  contentDescription = "Move down",
                  tint = Slate400,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }

      // Create New Folder button
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showCreateDialog = true }
            .background(Slate200.copy(alpha = 0.3f))
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = AmberGold,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = stringResource(MR.strings.chat_folders_create_new),
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AmberGold
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }

      // Show Folder Tags toggle
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { /* Toggle logic - for now just visual */ },
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = stringResource(MR.strings.chat_folders_show_tags),
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            color = MaterialTheme.colors.onBackground
          )

          // Disabled toggle (Premium feature placeholder)
          Switch(
            checked = false,
            onCheckedChange = null,
            enabled = false,
            colors = SwitchDefaults.colors(
              checkedThumbColor = AmberGold,
              uncheckedThumbColor = Slate400
            )
          )
        }
      }
    }
  }

  // Edit dialog
  editingFolder?.let { folder ->
    ChatFolderEditDialog(
      initialFolder = folder,
      onDismiss = { editingFolder = null },
      onSave = { updated ->
        folders = folders.map { if (it.id == updated.id) updated else it }
        ChatFoldersPrefs.saveFolders(folders)
        editingFolder = null
      }
    )
  }

  // Create dialog
  if (showCreateDialog) {
    ChatFolderEditDialog(
      onDismiss = { showCreateDialog = false },
      onSave = { newFolder ->
        folders = folders + newFolder.copy(order = folders.maxOfOrNull { it.order }?.plus(1) ?: 0)
        ChatFoldersPrefs.saveFolders(folders)
        showCreateDialog = false
      }
    )
  }
}

@Composable
private fun RecommendedFolderRow(
  name: String,
  description: String,
  isAdded: Boolean,
  onAdd: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = name,
        fontFamily = PlusJakartaSans,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = AmberGold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
        color = Slate600
      )
    }

    if (!isAdded) {
      Button(
        onClick = onAdd,
        colors = ButtonDefaults.buttonColors(
          backgroundColor = AmberGold,
          contentColor = MaterialTheme.colors.background
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Text(
          text = stringResource(MR.strings.chat_folders_add),
          fontFamily = PlusJakartaSans,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
