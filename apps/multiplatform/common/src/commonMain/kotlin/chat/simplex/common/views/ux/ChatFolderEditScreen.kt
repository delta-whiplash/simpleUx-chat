package chat.simplex.common.views.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatModel
import chat.simplex.common.platform.ChatFolder
import chat.simplex.common.platform.ChatFoldersPrefs
import chat.simplex.common.ui.theme.AmberGold
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.ui.theme.Slate200
import chat.simplex.common.ui.theme.Slate400
import chat.simplex.common.ui.theme.Slate500
import chat.simplex.common.ui.theme.Slate600
import chat.simplex.common.ui.theme.Slate900
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.AlertManager
import chat.simplex.common.views.helpers.DefaultAppBar
import chat.simplex.common.views.helpers.NavigationButtonBack
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

// #101: full-screen editor for custom chat folders (Telegram "Edit Folder"
// model): name + emoji, Included Chats with an Add Chats picker, Excluded
// Chats, and folder deletion. A custom folder lists exactly the chats it
// includes minus the excluded ones - no membership, no filter (no fakes).
@Composable
fun ChatFolderEditScreen(
  chatModel: ChatModel,
  initialFolder: ChatFolder?,
  onDone: (ChatFolder?) -> Unit
) {
  val isDark = isInDarkTheme()
  val isNew = initialFolder == null
  var name by remember { mutableStateOf(initialFolder?.name ?: "") }
  var emoji by remember { mutableStateOf(initialFolder?.emoji ?: "") }
  var included by remember { mutableStateOf(initialFolder?.includedChatIds ?: emptySet()) }
  var excluded by remember { mutableStateOf(initialFolder?.excludedChatIds ?: emptySet()) }
  var pickingFor by remember { mutableStateOf<String?>(null) }

  // chatModel.chats is a Compose State<List<Chat>>
  val chats by chatModel.chats
  val chatsById = remember(chats) { chats.associateBy { it.id } }

  fun buildFolder(): ChatFolder = (initialFolder ?: ChatFolder(
    id = "custom_${System.currentTimeMillis()}",
    filterKind = -1,
    order = Int.MAX_VALUE
  )).copy(
    name = name.ifEmpty { null },
    emoji = emoji.ifEmpty { null },
    includedChatIds = included,
    excludedChatIds = excluded
  )

  if (pickingFor != null) {
    ChatPickerView(
      chats = chats.toList(),
      title = if (pickingFor == "included") {
        generalGetString(MR.strings.chat_folders_add_chats)
      } else {
        generalGetString(MR.strings.chat_folders_add_chats_exclude)
      },
      selectedIds = (if (pickingFor == "included") included else excluded).toSet(),
      onToggle = { chatId, add ->
        if (pickingFor == "included") {
          included = if (add) included + chatId else included - chatId
        } else {
          excluded = if (add) excluded + chatId else excluded - chatId
          // a chat cannot be both included and excluded - excludes win anyway,
          // but keeping the sets disjoint keeps the editor honest
          if (add) included = included - chatId
        }
      },
      onClose = { pickingFor = null }
    )
    return
  }

  Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
    DefaultAppBar(
      navigationButton = { NavigationButtonBack(onButtonClicked = { onDone(null) }) },
      fixedTitleText = if (isNew) generalGetString(MR.strings.chat_folders_new_folder_title) else generalGetString(MR.strings.chat_folders_edit_folder_title),
      buttons = {},
      onTop = true,
      solidBackground = true
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = {
          Text(
            text = generalGetString(MR.strings.chat_folders_name_hint),
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp
          )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
      )

      OutlinedTextField(
        value = emoji,
        onValueChange = { emoji = it },
        label = {
          Text(
            text = generalGetString(MR.strings.chat_folders_emoji_hint),
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp
          )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )

      // Included Chats
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = generalGetString(MR.strings.chat_folders_included_chats),
          fontFamily = PlusJakartaSans,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = AmberGold
        )
        included.forEach { chatId ->
          val chat = chatsById[chatId]
          ChatMemberRow(
            label = chat?.chatInfo?.chatViewName ?: chatId,
            emoji = null,
            onRemove = { included = included - chatId }
          )
        }
        AddChatsRow(generalGetString(MR.strings.chat_folders_add_chats)) { pickingFor = "included" }
      }

      // Excluded Chats
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = generalGetString(MR.strings.chat_folders_excluded_chats),
          fontFamily = PlusJakartaSans,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = AmberGold
        )
        excluded.forEach { chatId ->
          val chat = chatsById[chatId]
          ChatMemberRow(
            label = chat?.chatInfo?.chatViewName ?: chatId,
            emoji = null,
            onRemove = { excluded = excluded - chatId }
          )
        }
        AddChatsRow(generalGetString(MR.strings.chat_folders_add_chats_exclude)) { pickingFor = "excluded" }
      }

      Text(
        text = generalGetString(MR.strings.chat_folders_helper_text),
        fontFamily = PlusJakartaSans,
        fontSize = 13.sp,
        color = Slate600
      )

      // Save - the editor owns persistence; the manager reloads from prefs
      androidx.compose.material.Button(
        onClick = {
          val folder = buildFolder()
          ChatFoldersPrefs.saveFolder(folder)
          onDone(folder)
        },
        enabled = name.isNotEmpty() || emoji.isNotEmpty(),
        colors = androidx.compose.material.ButtonDefaults.buttonColors(
          backgroundColor = AmberGold,
          contentColor = if (isDark) Slate900 else Color.White,
          disabledBackgroundColor = Slate400.copy(alpha = 0.3f),
          disabledContentColor = Slate500
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = generalGetString(MR.strings.save_verb),
          fontFamily = PlusJakartaSans,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Delete (existing custom folders only) - quiet destructive text action,
      // no container: a filled/outline Button shell around it read as clutter
      if (!isNew) {
        androidx.compose.material.TextButton(
          onClick = {
            val folder = initialFolder
            if (folder != null) {
              AlertManager.shared.showAlertDialog(
                title = generalGetString(MR.strings.chat_folders_delete_confirm_title),
                text = generalGetString(MR.strings.chat_folders_delete_confirm_text),
                confirmText = generalGetString(MR.strings.delete_verb),
                onConfirm = {
                  ChatFoldersPrefs.deleteFolder(folder.id)
                  onDone(null)
                },
                destructive = true,
                dismissText = generalGetString(MR.strings.cancel_verb)
              )
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = generalGetString(MR.strings.chat_folders_delete_folder),
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.error
          )
        }
      }
    }
  }
}

@Composable
private fun ChatMemberRow(label: String, emoji: String?, onRemove: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(MaterialTheme.colors.surface)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    emoji?.let {
      Text(text = it, fontSize = 16.sp)
      Spacer(Modifier.width(8.dp))
    }
    Text(
      text = label,
      fontFamily = PlusJakartaSans,
      fontSize = 14.sp,
      color = MaterialTheme.colors.onBackground,
      modifier = Modifier.weight(1f)
    )
    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
      Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = null,
        tint = Slate400,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
private fun AddChatsRow(label: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Filled.Add,
      contentDescription = null,
      tint = AmberGold,
      modifier = Modifier.size(20.dp)
    )
    Spacer(Modifier.width(10.dp))
    Text(
      text = label,
      fontFamily = PlusJakartaSans,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      color = AmberGold
    )
  }
}

// Full-screen chat multi-select used by the Included/Excluded sections.
@Composable
private fun ChatPickerView(
  chats: List<Chat>,
  title: String,
  selectedIds: Set<String>,
  onToggle: (String, Boolean) -> Unit,
  onClose: () -> Unit
) {
  val isDark = isInDarkTheme()
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = title,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colors.onBackground
          )
        },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = null,
              tint = MaterialTheme.colors.onBackground
            )
          }
        },
        actions = {
          Text(
            text = selectedIds.size.toString(),
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AmberGold,
            modifier = Modifier.padding(end = 16.dp)
          )
        },
        backgroundColor = MaterialTheme.colors.background,
        elevation = 0.dp
      )
    },
    backgroundColor = MaterialTheme.colors.background
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
      chats.forEach { chat ->
        val checked = selectedIds.contains(chat.id)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(chat.id, !checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = chat.chatInfo.chatViewName,
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.weight(1f)
          )
          Checkbox(
            checked = checked,
            onCheckedChange = { onToggle(chat.id, it) },
            colors = CheckboxDefaults.colors(
              checkedColor = AmberGold,
              checkmarkColor = if (isDark) Slate900 else Color.White
            )
          )
        }
      }
    }
  }
}
