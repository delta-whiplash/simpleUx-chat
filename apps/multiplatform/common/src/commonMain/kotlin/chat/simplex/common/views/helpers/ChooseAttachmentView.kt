package chat.simplex.common.views.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.platform.appPlatform
import chat.simplex.common.platform.chatModel
import chat.simplex.common.ui.theme.Amber600
import chat.simplex.common.ui.theme.AmberGold
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.ui.theme.HighOrLowlight
import chat.simplex.common.ui.theme.Slate300
import chat.simplex.common.ui.theme.Slate600
import chat.simplex.common.ui.theme.SurfaceContainerLowDark
import chat.simplex.common.ui.theme.SurfaceContainerLowLight
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import java.net.URI

sealed class AttachmentOption {
  object CameraPhoto: AttachmentOption()
  object GalleryImage: AttachmentOption()
  object GalleryVideo: AttachmentOption()
  object File: AttachmentOption()
}

/**
 * #122 / #126: Telegram-style attachment sheet. Android: a bento 2x4 grid on
 * top (camera spans 2 vertical cells, 6 recent-media thumbnails fill the rest,
 * tap a thumb = send immediately), then the action wells — File and Contact
 * (sends the contact's SimpleX address as a link message; only contacts with
 * a public address are offered). Gallery is no longer a separate well; it lives
 * in the bento grid. Desktop keeps the flat action row (no grid, no camera).
 * The set of sendable content types is exactly what MsgContent supports — no
 * fake entries.
 */
@Composable
fun ChooseAttachmentView(
  attachmentOption: MutableState<AttachmentOption?>,
  hide: () -> Unit,
  sheetVisible: Boolean = true,
  onMediaPicked: (List<URI>) -> Unit = {},
  onContactPicked: (String) -> Unit = {}
) {
  var showContacts by rememberSaveable { mutableStateOf(false) }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .imePadding()
      .wrapContentHeight()
  ) {
    Column(
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        .background(MaterialTheme.colors.background)
        .padding(vertical = DEFAULT_PADDING)
    ) {
      Box(
        Modifier
          .padding(bottom = 12.dp)
          .size(width = 36.dp, height = 4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(if (isInDarkTheme()) Slate600 else Slate300)
          .align(Alignment.CenterHorizontally)
      )
      if (showContacts) {
        ContactPickerList(
          onClose = { showContacts = false },
          onContactPicked = { link ->
            showContacts = false
            onContactPicked(link)
            hide()
          }
        )
      } else {
        if (appPlatform.isAndroid) {
          AttachmentTopSection(
            sheetVisible = sheetVisible,
            onCameraOpened = {
              attachmentOption.value = AttachmentOption.CameraPhoto
              hide()
            },
            onMediaPicked = onMediaPicked,
            hide = hide
          )
          Spacer(Modifier.height(12.dp))
        }
        Row(
          Modifier.fillMaxWidth().padding(horizontal = 12.dp),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          // #126: Gallery removed from the bottom row — it's now part of the
          // bento grid above. Only File and Contact remain as action wells.
          // Desktop keeps the separate Image / Video system dialogs (its
          // camera tile is Android-only).
          AttachmentAction(MR.images.ic_add_photo, MR.strings.gallery_image_button, visible = !appPlatform.isAndroid) {
            attachmentOption.value = AttachmentOption.GalleryImage
            hide()
          }
          AttachmentAction(MR.images.ic_smart_display, MR.strings.gallery_video_button, visible = !appPlatform.isAndroid) {
            attachmentOption.value = AttachmentOption.GalleryVideo
            hide()
          }
          AttachmentAction(MR.images.ic_note_add, MR.strings.choose_file) {
            attachmentOption.value = AttachmentOption.File
            hide()
          }
          AttachmentAction(MR.images.ic_person, MR.strings.send_contact_button) {
            showContacts = true
          }
        }
      }
    }
  }
}

@Composable
private fun ContactPickerList(onClose: () -> Unit, onContactPicked: (String) -> Unit) {
  val contacts = remember {
    chatModel.chats.value.mapNotNull { c ->
      val ci = c.chatInfo as? ChatInfo.Direct ?: return@mapNotNull null
      val link = ci.contact.contactLink
      if (ci.contact.active && !link.isNullOrEmpty()) c to link else null
    }
  }
  Column(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painterResource(MR.images.ic_person),
        contentDescription = stringResource(MR.strings.send_contact_button),
        tint = MaterialTheme.colors.primary,
        modifier = Modifier.size(22.dp)
      )
      Spacer(Modifier.width(10.dp))
      Text(
        stringResource(MR.strings.send_contact_button),
        fontSize = 17.sp,
        color = MaterialTheme.colors.onBackground
      )
      Spacer(Modifier.weight(1f))
      Text(
        stringResource(MR.strings.cancel_verb),
        fontSize = 15.sp,
        color = MaterialTheme.colors.primary,
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .clickable(onClick = onClose)
          .padding(6.dp)
      )
    }
    if (contacts.isEmpty()) {
      Text(
        stringResource(MR.strings.no_contacts_to_add),
        Modifier.padding(horizontal = DEFAULT_PADDING, vertical = 16.dp),
        fontSize = 14.sp,
        color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
      )
    } else {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        contacts.forEach { (chat: Chat, link: String) ->
          val ci = chat.chatInfo as ChatInfo.Direct
          Row(
            Modifier
              .fillMaxWidth()
              .clickable { onContactPicked(link) }
              .padding(horizontal = DEFAULT_PADDING, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              ci.contact.displayName,
              fontSize = 16.sp,
              color = MaterialTheme.colors.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AttachmentAction(
  icon: ImageResource,
  label: StringResource,
  visible: Boolean = true,
  onClick: () -> Unit
) {
  if (!visible) return
  val isDark = isInDarkTheme()
  Column(
    Modifier
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .widthIn(min = 78.dp)
      .padding(vertical = 10.dp, horizontal = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // #126: mineral action disc — gold-tinted with specular rim
    Box(
      Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(
          if (isDark) AmberGold.copy(alpha = 0.12f)
          else        AmberGold.copy(alpha = 0.10f)
        )
        .border(
          width = 1.dp,
          color = if (isDark) AmberGold.copy(alpha = 0.30f)
                  else        AmberGold.copy(alpha = 0.25f),
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painterResource(icon),
        contentDescription = stringResource(label),
        tint = if (isDark) AmberGold else Amber600,
        modifier = Modifier.size(24.dp)
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      stringResource(label),
      fontSize = 12.sp,
      color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
