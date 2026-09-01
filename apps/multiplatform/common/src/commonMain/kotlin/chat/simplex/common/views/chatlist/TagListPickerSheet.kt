package chat.simplex.common.views.chatlist

import SectionTextFooter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.Chat
import chat.simplex.common.platform.chatModel
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.ui.theme.Slate50
import chat.simplex.common.ui.theme.Slate900
import chat.simplex.common.ui.theme.Slate600
import chat.simplex.common.ui.theme.Slate300
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

/**
 * #63: chat → tag assignment as a bottom sheet opened from the chat-row menus
 * (was a full-screen picker page for what is usually 0-5 rows). The reorder-mode
 * TagListView page remains for tag management. Menus trigger it through
 * [LocalTagListPicker]; null local (previews/isolated tests) falls back to the page.
 */
val LocalTagListPicker = compositionLocalOf<((Chat) -> Unit)?> { null }

@Composable
fun TagListPickerSheetContent(chat: Chat, onDismiss: () -> Unit) {
  val isDark = isInDarkTheme()
  val userTags = remember { chatModel.userTags }
  val saving = remember { mutableStateOf(false) }
  val rhId = chat.remoteHostId
  val chatTagIds = remember { derivedStateOf { chat.chatInfo.chatTags ?: emptyList() } }

  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
      .background(if (isDark) Slate900 else Slate50)
      .navigationBarsPadding()
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Box(
      Modifier
        .align(Alignment.CenterHorizontally)
        .width(36.dp)
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(if (isDark) Slate600 else Slate300)
    )
    Spacer(Modifier.height(14.dp))

    // Single-choice, Telegram-style: tapping the selected tag removes it
    userTags.value.forEach { tag ->
      val selected = chatTagIds.value.contains(tag.chatTagId)
      Row(
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable(enabled = !saving.value) {
            saving.value = true
            setTag(rhId = rhId, tagId = if (selected) null else tag.chatTagId, chat = chat) {
              saving.value = false
              onDismiss()
            }
          }
          .padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (tag.chatTagEmoji != null) {
          Text(tag.chatTagEmoji!!, fontSize = 18.sp)
        }
        if (tag.chatTagEmoji != null) {
          Spacer(Modifier.width(12.dp))
        }
        Text(
          tag.chatTagText,
          Modifier.weight(1f),
          fontSize = 16.sp,
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
          color = MaterialTheme.colors.onBackground
        )
        if (selected) {
          Icon(painterResource(MR.images.ic_check), null, tint = MaterialTheme.colors.primary)
        }
      }
    }

    Spacer(Modifier.height(6.dp))
    Text(
      stringResource(MR.strings.create_list),
      Modifier
        .clip(RoundedCornerShape(12.dp))
        .clickable {
          onDismiss()
          ModalManager.start.showModalCloseable { close ->
            TagListEditor(rhId = rhId, close = close, chat = chat)
          }
        }
        .padding(horizontal = 8.dp, vertical = 13.dp),
      fontSize = 16.sp,
      color = MaterialTheme.colors.primary
    )
  }
}
