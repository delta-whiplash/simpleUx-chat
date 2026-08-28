package chat.simplex.common.views.chatlist

// Extracted verbatim from ChatListView.kt (issue #4): the chat-list tag-filter chips with the
// SimpleUX restyle. Same package, so TagsOrConnectByName / ItemPresetFilterAction in
// ChatListView.kt keep resolving them (TagsView and presetTagLabel widened private -> internal,
// signatures unchanged). Kept out of views/ux per the issue #4 hex rule: the chip palette
// (0x3300E5FF/0x333B82F6/0x331E293B...) has no exact ui/theme tokens yet (issue #16) and the
// views/ux hex-lint baseline must not grow.

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.chat.item.*
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

private val TAG_MIN_HEIGHT = 35.dp

@Composable
internal fun TagsView(searchText: MutableState<TextFieldValue>) {
  val userTags = remember { chatModel.userTags }
  val presetTags = remember { chatModel.presetTags }
  val collapsiblePresetTags = presetTags.filter { presetCanBeCollapsed(it.key) && it.value > 0 }
  val alwaysShownPresetTags = presetTags.filter { !presetCanBeCollapsed(it.key) && it.value > 0 }
  val activeFilter = remember { chatModel.activeChatTagFilter }
  val unreadTags = remember { chatModel.unreadTags }
  val rhId = chatModel.remoteHostId()

  val rowSizeModifier = Modifier.sizeIn(minHeight = TAG_MIN_HEIGHT * fontSizeSqrtMultiplier)
  val isDark = isInDarkTheme()

  TagsRow {
    // "All" Tab
    val allActive = activeFilter.value == null && searchText.value.text.isEmpty()
    val allShape = RoundedCornerShape(14.dp)
    val allBgModifier = if (allActive) {
      Modifier.background(Brush.linearGradient(listOf(Color(0x3300E5FF), Color(0x333B82F6))), shape = allShape)
    } else {
      Modifier.background(if (isDark) Color(0x331E293B) else Color(0xEEF1F5F9), shape = allShape)
    }
    val allBorderModifier = if (allActive) {
      Modifier.border(1.dp, Color(0x6600E5FF), allShape)
    } else {
      Modifier.border(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x15000000), allShape)
    }
    val allColor = if (allActive) Color(0xFF38BDF8) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
      modifier = Modifier
        .sizeIn(minHeight = TAG_MIN_HEIGHT * fontSizeSqrtMultiplier)
        .padding(horizontal = 3.dp)
        .clip(allShape)
        .then(allBgModifier)
        .then(allBorderModifier)
        .bounceClick()
        .clickable {
          chatModel.activeChatTagFilter.value = null
          searchText.value = TextFieldValue("")
        }
        .padding(horizontal = 12.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        stringResource(MR.strings.chat_list_all),
        color = allColor,
        fontWeight = if (allActive) FontWeight.Bold else FontWeight.Medium,
        fontSize = 14.sp
      )
    }

    if (collapsiblePresetTags.size > 1) {
      if (collapsiblePresetTags.size + alwaysShownPresetTags.size + userTags.value.size <= 3) {
        PresetTagKind.entries.filter { t -> (presetTags[t] ?: 0) > 0 }.forEach { tag ->
          ExpandedTagFilterView(tag)
        }
      } else {
        CollapsedTagsFilterView(searchText)
        alwaysShownPresetTags.forEach { tag ->
          ExpandedTagFilterView(tag.key)
        }
      }
    }

    userTags.value.forEach { tag ->
      val isDark = isInDarkTheme()
      val current = when (val af = activeFilter.value) {
        is ActiveFilter.UserTag -> af.tag == tag
        else -> false
      }
      val interactionSource = remember { MutableInteractionSource() }
      val showMenu = rememberSaveable { mutableStateOf(false) }
      val saving = remember { mutableStateOf(false) }
      val shape = RoundedCornerShape(14.dp)
      val bgModifier = if (current) {
        Modifier.background(Brush.linearGradient(listOf(Color(0x3300E5FF), Color(0x333B82F6))), shape = shape)
      } else {
        Modifier.background(if (isDark) Color(0x331E293B) else Color(0xEEF1F5F9), shape = shape)
      }
      val borderModifier = if (current) {
        Modifier.border(1.dp, Color(0x6600E5FF), shape)
      } else {
        Modifier.border(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x15000000), shape)
      }
      val chipColor = if (current) Color(0xFF38BDF8) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

      Box(Modifier.padding(horizontal = 3.dp)) {
        Row(
          rowSizeModifier
            .clip(shape = shape)
            .then(bgModifier)
            .then(borderModifier)
            .combinedClickable(
              onClick = {
                if (chatModel.activeChatTagFilter.value == ActiveFilter.UserTag(tag)) {
                  chatModel.activeChatTagFilter.value = null
                } else {
                  chatModel.activeChatTagFilter.value = ActiveFilter.UserTag(tag)
                }
              },
              onLongClick = { showMenu.value = true },
              interactionSource = interactionSource,
              indication = LocalIndication.current,
              enabled = !saving.value
            )
            .onRightClick { showMenu.value = true }
            .padding(horizontal = 10.dp, vertical = 5.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (tag.chatTagEmoji != null) {
            ReactionIcon(tag.chatTagEmoji, fontSize = 14.sp)
          } else {
            Icon(
              painterResource(if (current) MR.images.ic_label_filled else MR.images.ic_label),
              null,
              Modifier.size(16.sp.toDp()),
              tint = chipColor
            )
          }
          Spacer(Modifier.width(5.dp))
          val badgeText = if ((unreadTags[tag.chatTagId] ?: 0) > 0) " ●" else ""
          val visibleText = buildAnnotatedString {
            append(tag.chatTagText)
            if (badgeText.isNotEmpty()) {
              withStyle(SpanStyle(fontSize = 12.5.sp, color = Color(0xFF00E5FF))) {
                append(badgeText)
              }
            }
          }
          Text(
            text = visibleText,
            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp,
            color = chipColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        TagsDropdownMenu(rhId, tag, showMenu, saving)
      }
    }
    val plusShape = RoundedCornerShape(14.dp)
    val plusClickModifier = Modifier
      .clip(plusShape)
      .background(if (isInDarkTheme()) Color(0x221E293B) else Color(0xEEF1F5F9), plusShape)
      .border(1.dp, if (isInDarkTheme()) Color(0x26FFFFFF) else Color(0x15000000), plusShape)
      .clickable {
        ModalManager.start.showModalCloseable { close ->
          TagListEditor(rhId = rhId, close = close)
        }
      }

    if (userTags.value.isEmpty()) {
      Row(rowSizeModifier.then(plusClickModifier).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(MR.images.ic_add), stringResource(MR.strings.chat_list_add_list), Modifier.size(16.sp.toDp()), tint = MaterialTheme.colors.secondary)
        Spacer(Modifier.width(4.dp))
        Text(stringResource(MR.strings.chat_list_add_list), color = MaterialTheme.colors.secondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
      }
    } else {
      Box(rowSizeModifier.then(plusClickModifier).padding(horizontal = 8.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
        Icon(
          painterResource(MR.images.ic_add), stringResource(MR.strings.chat_list_add_list), Modifier.size(16.sp.toDp()), tint = MaterialTheme.colors.secondary
        )
      }
    }
  }
}

@Composable
private fun ExpandedTagFilterView(tag: PresetTagKind) {
  val isDark = isInDarkTheme()
  val activeFilter = remember { chatModel.activeChatTagFilter }
  val active = when (val af = activeFilter.value) {
    is ActiveFilter.PresetTag -> af.tag == tag
    else -> false
  }
  val (icon, menuIcon, text) = presetTagLabel(tag, active)
  val color = if (active) Color(0xFF38BDF8) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
  val shape = RoundedCornerShape(14.dp)
  val bgModifier = if (active) {
    Modifier.background(Brush.linearGradient(listOf(Color(0x3300E5FF), Color(0x333B82F6))), shape = shape)
  } else {
    Modifier.background(if (isDark) Color(0x331E293B) else Color(0xEEF1F5F9), shape = shape)
  }
  val borderModifier = if (active) {
    Modifier.border(1.dp, Color(0x6600E5FF), shape)
  } else {
    Modifier.border(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x15000000), shape)
  }

  Row(
    modifier = Modifier
      .sizeIn(minHeight = TAG_MIN_HEIGHT * fontSizeSqrtMultiplier)
      .padding(horizontal = 3.dp)
      .clip(shape)
      .then(bgModifier)
      .then(borderModifier)
      .bounceClick()
      .clickable {
        if (activeFilter.value == ActiveFilter.PresetTag(tag)) {
          chatModel.activeChatTagFilter.value = null
        } else {
          chatModel.activeChatTagFilter.value = ActiveFilter.PresetTag(tag)
        }
      }
      .padding(horizontal = 10.dp, vertical = 5.dp)
    ,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    Icon(
      painterResource(menuIcon ?: icon),
      stringResource(text),
      Modifier.size(16.sp.toDp()),
      tint = color
    )
    Spacer(Modifier.width(5.dp))
    Text(
      stringResource(text),
      color = color,
      fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
      fontSize = 14.sp
    )
  }
}

@Composable
private fun CollapsedTagsFilterView(searchText: MutableState<TextFieldValue>) {
  val activeFilter = remember { chatModel.activeChatTagFilter }
  val presetTags = remember { chatModel.presetTags }
  val showMenu = remember { mutableStateOf(false) }

  val selectedPresetTag = when (val af = activeFilter.value) {
    is ActiveFilter.PresetTag -> if (presetCanBeCollapsed(af.tag)) af.tag else null
    else -> null
  }

  Box(Modifier
    .clip(shape = CircleShape)
    .size(TAG_MIN_HEIGHT * fontSizeSqrtMultiplier)
    .clickable { showMenu.value = true },
    contentAlignment = Alignment.Center
  ) {
    if (selectedPresetTag != null) {
      val (icon, menuIcon, text) = presetTagLabel(selectedPresetTag, true)
      Icon(
        painterResource(menuIcon ?: icon),
        stringResource(text),
        Modifier.size(18.sp.toDp()),
        tint = MaterialTheme.colors.primary
      )
    } else {
      Icon(
        painterResource(MR.images.ic_menu),
        stringResource(MR.strings.chat_list_all),
        tint = MaterialTheme.colors.secondary
      )
    }

    val onCloseMenuAction = remember { mutableStateOf<(() -> Unit)>({}) }

    DefaultDropdownMenu(showMenu = showMenu, onClosed = onCloseMenuAction) {
      if (activeFilter.value != null || searchText.value.text.isNotBlank()) {
        ItemAction(
          stringResource(MR.strings.chat_list_all),
          painterResource(MR.images.ic_menu),
          onClick = {
            onCloseMenuAction.value = {
              searchText.value = TextFieldValue()
              chatModel.activeChatTagFilter.value = null
              onCloseMenuAction.value = {}
            }
            showMenu.value = false
          }
        )
      }
      PresetTagKind.entries.forEach { tag ->
        if ((presetTags[tag] ?: 0) > 0 && presetCanBeCollapsed(tag)) {
          ItemPresetFilterAction(tag, tag == selectedPresetTag, showMenu, onCloseMenuAction)
        }
      }
    }
  }
}

internal fun presetTagLabel(tag: PresetTagKind, active: Boolean): Triple<ImageResource, ImageResource?, StringResource> =
  when (tag) {
    PresetTagKind.GROUP_REPORTS -> Triple(if (active) MR.images.ic_flag_filled else MR.images.ic_flag, null, MR.strings.chat_list_group_reports)
    PresetTagKind.FAVORITES -> Triple(if (active) MR.images.ic_star_filled else MR.images.ic_star, null, MR.strings.chat_list_favorites)
    PresetTagKind.CONTACTS -> Triple(if (active) MR.images.ic_person_filled else MR.images.ic_person, null, MR.strings.chat_list_contacts)
    PresetTagKind.GROUPS -> Triple(if (active) MR.images.ic_group_filled else MR.images.ic_group, null, MR.strings.chat_list_groups)
    PresetTagKind.CHANNELS -> Triple(if (active) MR.images.ic_bigtop_updates_circle_filled else MR.images.ic_bigtop_updates, MR.images.ic_bigtop_updates, MR.strings.chat_list_channels)
    PresetTagKind.BUSINESS -> Triple(if (active) MR.images.ic_work_filled else MR.images.ic_work, null, MR.strings.chat_list_businesses)
    PresetTagKind.NOTES -> Triple(if (active) MR.images.ic_folder_closed_filled else MR.images.ic_folder_closed, null, MR.strings.chat_list_notes)
  }

private fun presetCanBeCollapsed(tag: PresetTagKind): Boolean = when (tag) {
  PresetTagKind.GROUP_REPORTS -> false
  else -> true
}
