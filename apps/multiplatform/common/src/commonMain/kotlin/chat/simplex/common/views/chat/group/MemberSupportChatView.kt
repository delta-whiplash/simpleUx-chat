package chat.simplex.common.views.chat.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.chat.*
import chat.simplex.common.views.chatlist.*
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.ux.components.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
private fun MemberSupportChatView(
  chatInfo: ChatInfo,
  memberSupportChatsCtx: ChatModel.ChatsContext,
  staleChatId: State<String?>,
  scrollToItemId: MutableState<Long?>
) {
  KeyChangeEffect(chatModel.chatId.value) {
    ModalManager.end.closeModals()
  }
  if (appPlatform.isAndroid) {
    DisposableEffect(Unit) {
      onDispose {
        val chat = chatModel.chats.value.firstOrNull { ch -> ch.id == chatInfo.id }
        if (
          memberSupportChatsCtx.isUserSupportChat
          && chat?.chatInfo?.groupInfo_?.membership?.memberPending == true
        ) {
          withBGApi {
            chatModel.chatId.value = null
          }
        }
      }
    }
  }
  ChatView(memberSupportChatsCtx, staleChatId, scrollToItemId, onComposed = {})
}

@Composable
fun MemberSupportChatAppBar(
  chatsCtx: ChatModel.ChatsContext,
  rhId: Long?,
  chat: Chat,
  scopeMember_: GroupMember?,
  scrollToItemId: MutableState<Long?>,
  close: () -> Unit,
  onSearchValueChanged: (String) -> Unit
) {
  val showSearch = rememberSaveable { mutableStateOf(false) }
  val showMenu = rememberSaveable { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val isDark = isInDarkTheme()

  val onBackClicked = {
    if (!showSearch.value) {
      close()
    } else {
      onSearchValueChanged("")
      showSearch.value = false
    }
  }
  BackHandler(onBack = onBackClicked)

  DefaultAppBar(
    navigationButton = { NavigationButtonBack(onBackClicked) },
    title = {
      if (chat.chatInfo is ChatInfo.Group && scopeMember_ != null) {
        MemberSupportChatToolbarTitle(scopeMember_)
      } else {
        AdminSupportToolbarTitle(chat.chatInfo)
      }
    },
    onTitleClick = {
      if (chat.chatInfo is ChatInfo.Group && scopeMember_ != null) {
        val groupInfo = chat.chatInfo.groupInfo
        withBGApi {
          val r = chatModel.controller.apiGroupMemberInfo(rhId, groupInfo.groupId, scopeMember_.groupMemberId)
          val stats = r?.second
          val code = if ((scopeMember_.memberActive || (groupInfo.useRelays && scopeMember_.memberCurrent)) && scopeMember_.memberRole != GroupMemberRole.Relay) {
            val memCode = chatModel.controller.apiGetGroupMemberCode(rhId, groupInfo.apiId, scopeMember_.groupMemberId)
            memCode?.second
          } else {
            null
          }
          ModalManager.end.showModalCloseable(showClose = true, cardScreen = true) { closeCurrent ->
            remember { derivedStateOf { chatModel.getGroupMember(scopeMember_.groupMemberId) } }.value?.let { mem ->
              GroupMemberInfoView(rhId, groupInfo, mem, scrollToItemId, stats, code, chatModel, openedFromSupportChat = true, close = closeCurrent) {
                closeCurrent()
                close()
              }
            }
          }
        }
      }
    },
    onTop = true,
    showSearch = showSearch.value,
    onSearchValueChanged = onSearchValueChanged,
    buttons = {
      IconButton({ showSearch.value = true }) {
        Icon(painterResource(MR.images.ic_search), stringResource(MR.strings.search_verb), tint = MaterialTheme.colors.primary)
      }
      IconButton({ showMenu.value = true }) {
        Icon(painterResource(MR.images.ic_more_vert), stringResource(MR.strings.icon_descr_settings), tint = MaterialTheme.colors.primary)
      }
    }
  )

  Box(Modifier.fillMaxWidth().wrapContentSize(Alignment.TopEnd)) {
    DefaultDropdownMenu(
      showMenu,
      offset = DpOffset(0.dp, AppBarHeight)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            showMenu.value = false
            ThemeAnimationController.trigger(
              originOffset = androidx.compose.ui.geometry.Offset(1000f, 145f),
              currentlyDark = isDark,
              scope = scope
            )
          }
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        AnimatedThemeIcon(isDark = isDark)
        Text(
          text = if (isDark) stringResource(MR.strings.theme_mode_light_descr) else stringResource(MR.strings.theme_mode_dark_descr),
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
          color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        )
      }
    }
  }
  ItemsReload(chatsCtx)
}

@Composable
fun AdminSupportToolbarTitle(
  chatInfo: ChatInfo,
  imageSize: Dp = 40.dp,
  iconColor: Color = MaterialTheme.colors.secondaryVariant.mixWith(MaterialTheme.colors.onBackground, 0.97f)
) {
  Row(
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.CenterVertically
  ) {
    ChatInfoImage(chatInfo, size = imageSize * fontSizeSqrtMultiplier, iconColor)
    Column(
      Modifier.padding(start = 10.dp),
      horizontalAlignment = Alignment.Start
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        NameWithBadge(
          chatInfo.displayName, chatInfo.nameBadge, fontWeight = FontWeight.Bold,
          maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        val currentChat = remember(chatInfo.id) { chatModel.chats.value.firstOrNull { it.chatInfo.id == chatInfo.id } }
        Spacer(Modifier.width(6.dp))
        SecurityBadge(chat = currentChat)
      }
      Text(
        stringResource(MR.strings.support_chat),
        style = MaterialTheme.typography.body2.copy(fontSize = 11.5.sp),
        color = MaterialTheme.colors.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun MemberSupportChatToolbarTitle(member: GroupMember, imageSize: Dp = 40.dp, iconColor: Color = MaterialTheme.colors.secondaryVariant.mixWith(MaterialTheme.colors.onBackground, 0.97f)) {
  Row(
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.CenterVertically
  ) {
    MemberProfileImage(size = imageSize * fontSizeSqrtMultiplier, member, iconColor)
    Column(
      Modifier.padding(start = 10.dp),
      horizontalAlignment = Alignment.Start
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (member.verified) {
          MemberVerifiedShield()
        }
        NameWithBadge(
          member.displayName, member.nameBadge, fontWeight = FontWeight.Bold,
          maxLines = 1, overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        stringResource(MR.strings.support_chat),
        style = MaterialTheme.typography.body2.copy(fontSize = 11.5.sp),
        color = MaterialTheme.colors.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

suspend fun showMemberSupportChatView(staleChatId: State<String?>, scrollToItemId: MutableState<Long?>, chatInfo: ChatInfo, scopeInfo: GroupChatScopeInfo) {
  val memberSupportChatsCtx = ChatModel.ChatsContext(secondaryContextFilter = SecondaryContextFilter.GroupChatScopeContext(scopeInfo))
  openChat(secondaryChatsCtx = memberSupportChatsCtx, chatModel.remoteHostId(), chatInfo)
  ModalManager.end.showCustomModal(true, id = ModalViewId.SECONDARY_CHAT) { close ->
    ModalView({}, showAppBar = false) {
      if (chatInfo is ChatInfo.Group && chatInfo.groupChatScope != null) {
        MemberSupportChatView(chatInfo, memberSupportChatsCtx, staleChatId, scrollToItemId)
      } else {
        LaunchedEffect(Unit) {
          close()
        }
      }
    }
  }
}
