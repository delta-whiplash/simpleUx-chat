package chat.simplex.common.views.chatlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.views.helpers.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.themedBackground
import chat.simplex.common.views.chat.topPaddingToContent
import chat.simplex.common.views.newchat.ActiveProfilePicker
import chat.simplex.common.views.ux.share.ShareMediaScreen
import chat.simplex.common.views.ux.share.ShareTarget
import chat.simplex.res.MR
import kotlinx.coroutines.launch

/**
 * FB-2: the share-into-SimpleX surface now renders ShareMediaScreen (views/ux/share)  - 
 * proper header, incoming-content preview, and the chat picker as mineral cards. This file
 * stays the thin adapter: it computes per-chat enablement from the shared-content flags and
 * performs the business actions on selection. The legacy ShareList / ShareListToolbar /
 * EmptyList composables below are kept uncalled as an upstream merge buffer.
 */
@Composable
fun ShareListView(chatModel: ChatModel, stopped: Boolean) {
  val sharedContent = chatModel.sharedContent.value
  var isMediaOrFileAttachment = false
  var isVoice = false
  var hasSimplexLink = false
  when (sharedContent) {
    is SharedContent.Text ->
      hasSimplexLink = hasSimplexLink(sharedContent.text)
    is SharedContent.Media -> {
      isMediaOrFileAttachment = true
      hasSimplexLink = hasSimplexLink(sharedContent.text)
    }
    is SharedContent.File -> {
      isMediaOrFileAttachment = true
      hasSimplexLink = hasSimplexLink(sharedContent.text)
    }
    is SharedContent.Forward -> {
      sharedContent.chatItems.forEach { ci ->
        val mc = ci.content.msgContent
        if (mc != null) {
          isMediaOrFileAttachment = isMediaOrFileAttachment || mc.isMediaOrFileAttachment
          isVoice = isVoice || mc.isVoice
          hasSimplexLink = hasSimplexLink || hasSimplexLink(mc.text)
        }
      }
    }
    is SharedContent.ChatLink, is SharedContent.MyAddress -> {
      hasSimplexLink = true
    }
    null -> {}
  }

  val scope = rememberCoroutineScope()
  val targets by remember(sharedContent, isMediaOrFileAttachment, isVoice, hasSimplexLink) {
    derivedStateOf {
      chatModel.chats.value.toList()
        .filter {
          it.chatInfo.ready && it.chatInfo.sendMsgEnabled &&
            !((sharedContent is SharedContent.ChatLink || sharedContent is SharedContent.MyAddress) && it.chatInfo is ChatInfo.Local)
        }
        .sortedByDescending { it.chatInfo is ChatInfo.Local }
        .map { chat -> ShareTarget(chat, shareTargetEnabled(chat, isMediaOrFileAttachment, isVoice, hasSimplexLink)) }
    }
  }

  Box(Modifier.fillMaxSize().themedBackground(bgLayerSize = LocalAppBarHandler.current?.backgroundGraphicsLayerSize, bgLayer = LocalAppBarHandler.current?.backgroundGraphicsLayer)) {
    ShareMediaScreen(
      sharedContent = sharedContent,
      targets = targets,
      stopped = stopped,
      onTargetSelected = { chat ->
        scope.launch {
          if (chatModel.chatRunning.value == false) {
            AlertManager.shared.showAlertMsg(
              generalGetString(MR.strings.chat_is_stopped_indication),
              generalGetString(MR.strings.you_can_start_chat_via_setting_or_by_restarting_the_app)
            )
          } else {
            selectShareTarget(chatModel, chat, isMediaOrFileAttachment, isVoice, hasSimplexLink)
          }
        }
      },
      onDismiss = {
        // Drop shared content
        chatModel.sharedContent.value = null
        if (sharedContent is SharedContent.Forward) {
          chatModel.chatId.value = sharedContent.fromChatInfo.id
        } else if (sharedContent is SharedContent.ChatLink) {
          chatModel.chatId.value = sharedContent.groupInfo.id
        }
      }
    )
  }
}

/** Same restriction checks the legacy share rows applied per chat type. */
private fun shareTargetEnabled(chat: Chat, isMediaOrFileAttachment: Boolean, isVoice: Boolean, hasSimplexLink: Boolean): Boolean =
  when (val info = chat.chatInfo) {
    is ChatInfo.Direct ->
      !(isVoice && !info.featureEnabled(ChatFeature.Voice))
    is ChatInfo.Group ->
      !(isMediaOrFileAttachment && !chat.groupFeatureEnabled(GroupFeature.Files)) &&
        !(isVoice && !info.featureEnabled(ChatFeature.Voice)) &&
        !(hasSimplexLink && !chat.groupFeatureEnabled(GroupFeature.SimplexLinks))
    is ChatInfo.Local -> true
    else -> false
  }

/** Same open actions as the legacy share rows, including the disabled-tap alert. */
private suspend fun selectShareTarget(
  chatModel: ChatModel,
  chat: Chat,
  isMediaOrFileAttachment: Boolean,
  isVoice: Boolean,
  hasSimplexLink: Boolean
) {
  when (val info = chat.chatInfo) {
    is ChatInfo.Direct -> {
      val voiceProhibited = isVoice && !info.featureEnabled(ChatFeature.Voice)
      if (voiceProhibited) {
        showShareProhibitedByPrefAlert()
      } else {
        directChatAction(chat.remoteHostId, info.contact, chatModel)
      }
    }
    is ChatInfo.Group -> {
      val simplexLinkProhibited = hasSimplexLink && !chat.groupFeatureEnabled(GroupFeature.SimplexLinks)
      val fileProhibited = isMediaOrFileAttachment && !chat.groupFeatureEnabled(GroupFeature.Files)
      val voiceProhibited = isVoice && !info.featureEnabled(ChatFeature.Voice)
      if (simplexLinkProhibited || fileProhibited || voiceProhibited) {
        showShareProhibitedByPrefAlert()
      } else {
        groupChatAction(chat.remoteHostId, info.groupInfo, chatModel)
      }
    }
    is ChatInfo.Local -> noteFolderChatAction(chat.remoteHostId, info.noteFolder)
    else -> {}
  }
}

private fun showShareProhibitedByPrefAlert() {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(MR.strings.cannot_share_message_alert_title),
    text = generalGetString(MR.strings.cannot_share_message_alert_text),
  )
}

private fun hasSimplexLink(msg: String): Boolean {
  val parsedMsg = parseToMarkdown(msg) ?: return false
  return parsedMsg.any { ft -> ft.format is Format.SimplexLink }
}

@Composable
private fun EmptyList() {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(stringResource(MR.strings.you_have_no_chats), color = MaterialTheme.colors.secondary)
  }
}

@Composable
private fun ShareListToolbar(chatModel: ChatModel, stopped: Boolean, onSearchValueChanged: (String) -> Unit) {
  var showSearch by rememberSaveable { mutableStateOf(false) }
  val hideSearchOnBack = { onSearchValueChanged(""); showSearch = false }
  if (showSearch) {
    BackHandler(onBack = hideSearchOnBack)
  }
  val users by remember { derivedStateOf { chatModel.users.filter { u -> u.user.activeUser || !u.user.hidden } } }
  val navButton: @Composable RowScope.() -> Unit = {
    val sharedContent = remember { chatModel.sharedContent }.value
    when {
      showSearch -> NavigationButtonBack(hideSearchOnBack)
      (users.size > 1 || chatModel.remoteHosts.isNotEmpty()) && sharedContent !is SharedContent.Forward && sharedContent !is SharedContent.ChatLink && sharedContent !is SharedContent.MyAddress -> {
        val allRead = users
          .filter { u -> !u.user.activeUser && !u.user.hidden }
          .all { u -> u.unreadCount == 0 }
        UserProfileButton(chatModel.currentUser.value?.profile?.image, allRead) {
          ModalManager.start.showCustomModal(keyboardCoversBar = false) { close ->
            val search = rememberSaveable { mutableStateOf("") }
            ModalView(
              { close() },
              showSearch = true,
              searchAlwaysVisible = true,
              onSearchValueChanged = { search.value = it },
              content = {
                ActiveProfilePicker(
                  search = search,
                  rhId = chatModel.remoteHostId,
                  close = close,
                  contactConnection = null,
                  showIncognito = false
                )
              }
            )
          }
        }
      }
      else -> NavigationButtonBack(onButtonClicked = {
        // Drop shared content
        chatModel.sharedContent.value = null
        if (sharedContent is SharedContent.Forward) {
          chatModel.chatId.value = sharedContent.fromChatInfo.id
        } else if (sharedContent is SharedContent.ChatLink) {
          chatModel.chatId.value = sharedContent.groupInfo.id
        }
      })
    }
  }

  DefaultAppBar(
    navigationButton = navButton,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          when (val v = chatModel.sharedContent.value) {
            is SharedContent.Text -> stringResource(MR.strings.share_message)
            is SharedContent.Media -> stringResource(MR.strings.share_image)
            is SharedContent.File -> stringResource(MR.strings.share_file)
            is SharedContent.Forward -> if (v.chatItems.size > 1) stringResource(MR.strings.forward_multiple) else stringResource(MR.strings.forward_message)
            is SharedContent.ChatLink -> stringResource(MR.strings.share_channel)
            is SharedContent.MyAddress -> stringResource(MR.strings.share_address)
            null -> stringResource(MR.strings.share_message)
          },
          color = MaterialTheme.colors.onBackground,
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    onTitleClick = null,
    showSearch = showSearch,
    onTop = !remember { appPrefs.oneHandUI.state }.value,
    onSearchValueChanged = onSearchValueChanged,
    buttons = {
      if (chatModel.chats.value.size >= 8) {
        IconButton({ showSearch = true }) {
          Icon(painterResource(MR.images.ic_search_500), stringResource(MR.strings.search_verb), tint = MaterialTheme.colors.primary)
        }
      }
      if (stopped) {
        IconButton(onClick = {
          AlertManager.shared.showAlertMsg(
            generalGetString(MR.strings.chat_is_stopped_indication),
            generalGetString(MR.strings.you_can_start_chat_via_setting_or_by_restarting_the_app)
          )
        }) {
          Icon(
            painterResource(MR.images.ic_report_filled),
            generalGetString(MR.strings.chat_is_stopped_indication),
            tint = Color.Red,
          )
        }
      }
    }
  )
}

@Composable
private fun ShareList(
  chatModel: ChatModel,
  search: String,
  isMediaOrFileAttachment: Boolean,
  isVoice: Boolean,
  hasSimplexLink: Boolean,
) {
  val oneHandUI = remember { appPrefs.oneHandUI.state }
  val chats by remember(search) {
    derivedStateOf {
      val sorted = chatModel.chats.value.toList().filter { it.chatInfo.ready && it.chatInfo.sendMsgEnabled && !((chatModel.sharedContent.value is SharedContent.ChatLink || chatModel.sharedContent.value is SharedContent.MyAddress) && it.chatInfo is ChatInfo.Local) }.sortedByDescending { it.chatInfo is ChatInfo.Local }
      filteredChats(mutableStateOf(false), mutableStateOf<Set<String>>(emptySet()), search, sorted)
    }
  }
  val topPaddingToContent = topPaddingToContent(false)
  LazyColumnWithScrollBar(
    modifier = Modifier.then(if (oneHandUI.value) Modifier.consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Vertical)) else Modifier).imePadding(),
    contentPadding = PaddingValues(
      top = if (oneHandUI.value) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() else topPaddingToContent,
      bottom = if (oneHandUI.value) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + AppBarHeight * fontSizeSqrtMultiplier else 0.dp
    ),
    reverseLayout = oneHandUI.value
  ) {
    items(chats) { chat ->
      ShareListNavLinkView(
        chat,
        chatModel,
        isMediaOrFileAttachment = isMediaOrFileAttachment,
        isVoice = isVoice,
        hasSimplexLink = hasSimplexLink,
      )
    }
  }
}
