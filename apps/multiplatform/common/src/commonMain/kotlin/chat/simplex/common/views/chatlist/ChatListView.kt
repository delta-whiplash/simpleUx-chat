package chat.simplex.common.views.chatlist

import LocalCardScreen
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import chat.simplex.common.AppLock
import chat.simplex.common.BuildConfigCommon
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.model.ChatController.stopRemoteHostAndReloadHosts
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.*
import chat.simplex.common.platform.*
import chat.simplex.common.views.call.Call
import chat.simplex.common.views.chat.item.*
import chat.simplex.common.views.chat.topPaddingToContent
import chat.simplex.common.views.newchat.*
import chat.simplex.common.views.onboarding.*
import chat.simplex.common.views.usersettings.*
import chat.simplex.common.views.ux.*
import chat.simplex.common.views.ux.components.*
import chat.simplex.common.views.ux.modals.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

enum class PresetTagKind { GROUP_REPORTS, FAVORITES, CONTACTS, GROUPS, CHANNELS, BUSINESS, NOTES }

sealed class ActiveFilter {
  data class PresetTag(val tag: PresetTagKind) : ActiveFilter()
  data class UserTag(val tag: ChatTag) : ActiveFilter()
  data object Unread: ActiveFilter()
}

private fun showNewChatSheet(oneHandUI: State<Boolean>) {
  connectProgressManager.cancelConnectProgress()
  ModalManager.start.closeModals()
  ModalManager.end.closeModals()
  chatModel.newChatSheetVisible.value = true
  ModalManager.start.showCustomModal { close ->
    val close = {
      // It will set it faster than in onDispose. It's important to catch the actual state before
      // closing modal for reacting with status bar changes in [App]
      chatModel.newChatSheetVisible.value = false
      close()
    }
    ModalView(close, showAppBar = !oneHandUI.value) {
      if (appPlatform.isAndroid) {
        BackHandler {
          close()
        }
      }
      NewChatSheet(rh = chatModel.currentRemoteHost.value, close)
      DisposableEffect(Unit) {
        onDispose {
          chatModel.newChatSheetVisible.value = false
        }
      }
    }
  }
}



@Composable
private fun ToolbarSegment(
  icon: ImageResource,
  text: String,
  isSelected: Boolean,
  selectedBg: Color,
  activeBg: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Row(
    modifier
      .fillMaxHeight()
      .background(if (isSelected) selectedBg else activeBg)
      .then(if (!isSelected) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      painterResource(icon), null, Modifier.size(20.dp),
      tint = if (isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.primary
    )
    Spacer(Modifier.width(8.dp))
    Text(
      text,
      color = if (isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.onBackground,
      style = MaterialTheme.typography.body1
    )
  }
}

// Spec: spec/client/chat-list.md#ChatListView
@Composable
fun ChatListView(chatModel: ChatModel, userPickerState: MutableStateFlow<AnimatedViewState>, setPerformLA: (Boolean) -> Unit, stopped: Boolean) {
  val oneHandUI = remember { appPrefs.oneHandUI.state }

  LaunchedEffect(Unit) {
    // #65: "What's new" no longer auto-opens full-screen on every update  - 
    // it stays reachable from Settings (with a badge when unread). Only the
    // updated-conditions notice (legal) still surfaces automatically.
    val showUpdatedConditions = chatModel.conditions.value.conditionsAction?.shouldShowNotice ?: false
    if (showUpdatedConditions) {
      // Requested here, so that the country is known by the time the modal opens
      platform.androidLoadPlayStoreCountry()
      delay(1000L)
      ModalManager.center.showCustomModal { close -> WhatsNewView(close = close, updatedConditions = showUpdatedConditions) }
    }
  }

  if (appPlatform.isDesktop) {
    KeyChangeEffect(chatModel.chatId.value) {
      if (chatModel.chatId.value != null && !ModalManager.end.isLastModalOpen(ModalViewId.SECONDARY_CHAT)) {
        ModalManager.end.closeModalsExceptFirst()
      }
      AudioPlayer.stop()
      VideoPlayerHolder.stopAll()
    }
  }
  val currentTab = rememberSaveable { mutableStateOf(SimpleUxTab.CHATS) }
  val searchText = rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
  val searchVisible = rememberSaveable { mutableStateOf(false) }
  val listState = rememberLazyListState(lazyListState.first, lazyListState.second)
  DisposableEffect(Unit) {
    onDispose {
      lazyListState = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
    }
  }
  SimpleUxTabHost(
    chatModel = chatModel,
    currentTab = currentTab,
    searchText = searchText,
    searchVisible = searchVisible,
    listState = listState,
    userPickerState = userPickerState,
    setPerformLA = setPerformLA,
    chatsTab = {
      ChatListWithLoadingScreen(searchText, searchVisible, listState, userPickerState, setPerformLA, stopped)
    }
  )

  if (searchText.value.text.isEmpty()) {
    if (appPlatform.isDesktop && !oneHandUI.value) {
      val call = remember { chatModel.activeCall }.value
      if (call != null) {
        ActiveCallInteractiveArea(call)
      }
    }
  }
  if (appPlatform.isAndroid) {
    val wasAllowedToSetupNotifications = rememberSaveable { mutableStateOf(false) }
    val canEnableNotifications = remember { derivedStateOf { chatModel.chatRunning.value == true } }
    if (wasAllowedToSetupNotifications.value || canEnableNotifications.value) {
      SetNotificationsModeAdditions()
      LaunchedEffect(Unit) { wasAllowedToSetupNotifications.value = true }
    }
  }
}

@Composable
private fun ChatListCard(
  close: () -> Unit,
  onCardClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
) {
  Column(
    modifier = Modifier.clip(RoundedCornerShape(18.dp))
  ) {
    Box(
      modifier = Modifier
        .background(MaterialTheme.appColors.sentMessage)
        .clickable {
          onCardClick?.invoke()
        }
    ) {
      Box(
        modifier = Modifier.fillMaxWidth().matchParentSize().padding(5.dp),
        contentAlignment = Alignment.TopEnd
      ) {
        IconButton(
          onClick = {
            close()
          }
        ) {
          Icon(
            painterResource(MR.images.ic_close), stringResource(MR.strings.back), tint = MaterialTheme.colors.secondary
          )
        }
      }
      content()
    }
  }
}

private const val BANNER_IMAGE_RATIO = 800f / 505f

@Composable
private fun BannerGradientBox(isDark: Boolean, content: @Composable () -> Unit) {
  val stops = if (isDark) darkStops else lightStops
  val scale = if (isDark) 1.5f else 1.2f
  val gp = gradientPoints(1f / BANNER_IMAGE_RATIO, scale)
  var size by remember { mutableStateOf(IntSize.Zero) }
  val brush = remember(size, isDark) {
    if (size.width > 0 && size.height > 0) {
      Brush.linearGradient(
        colorStops = stops,
        start = Offset(gp.startX * size.width, gp.startY * size.height),
        end = Offset(gp.endX * size.width, gp.endY * size.height)
      )
    } else {
      Brush.linearGradient(colorStops = stops)
    }
  }
  Box(
    Modifier.fillMaxWidth().aspectRatio(BANNER_IMAGE_RATIO).background(brush).onSizeChanged { size = it },
    contentAlignment = Alignment.Center
  ) { content() }
}



@Composable
private fun BoxScope.ChatListWithLoadingScreen(
  searchText: MutableState<TextFieldValue>,
  searchVisible: MutableState<Boolean>,
  listState: LazyListState,
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  stopped: Boolean
) {
  if (chatModel.chatRunning.value == null) {
    Text(stringResource(MR.strings.loading_chats), Modifier.align(Alignment.Center), color = MaterialTheme.colors.secondary)
  } else {
    if (!chatModel.desktopNoUserNoRemote) {
      ChatList(searchText = searchText, searchVisible = searchVisible, listState = listState, userPickerState = userPickerState, setPerformLA = setPerformLA, stopped = stopped)
    }
  }
}


@Composable
private fun ConnectButton(text: String, onClick: () -> Unit) {
  Button(
    onClick,
    shape = RoundedCornerShape(21.dp),
    colors = ButtonDefaults.textButtonColors(
      backgroundColor = MaterialTheme.colors.primaryVariant
    ),
    elevation = null,
    contentPadding = PaddingValues(horizontal = DEFAULT_PADDING, vertical = DEFAULT_PADDING_HALF),
    modifier = Modifier.height(42.dp)
  ) {
    Text(text, color = Color.White)
  }
}



@Composable
fun SubscriptionStatusIndicator(click: (() -> Unit)) {
  var subs by remember { mutableStateOf(SMPServerSubs.newSMPServerSubs) }
  var hasSess by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  suspend fun setSubsTotal() {
    if (chatModel.currentUser.value != null && chatModel.controller.hasChatCtrl() && chatModel.chatRunning.value == true) {
      val r = chatModel.controller.getAgentSubsTotal(chatModel.remoteHostId())
      if (r != null) {
        subs = r.first
        hasSess = r.second
      }
    }
  }

  LaunchedEffect(Unit) {
    setSubsTotal()
    scope.launch {
      while (isActive) {
        delay(1.seconds)
        if ((appPlatform.isDesktop || chatModel.chatId.value == null) && !ModalManager.start.hasModalsOpen() && !ModalManager.fullscreen.hasModalsOpen() && isAppVisibleAndFocused()) {
          setSubsTotal()
        }
      }
    }
  }

  SimpleButtonFrame(
    click = click,
    disabled = chatModel.chatRunning.value != true
  ) {
    SubscriptionStatusIndicatorView(subs = subs, hasSess = hasSess)
  }
}

@Composable
fun UserProfileButton(image: String?, allRead: Boolean, onButtonClicked: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = onButtonClicked) {
      Box {
        ProfileImage(
          image = image,
          size = 37.dp * fontSizeSqrtMultiplier,
          color = MaterialTheme.colors.secondaryVariant.mixWith(MaterialTheme.colors.onBackground, 0.97f)
        )
        if (!allRead) {
          unreadBadge()
        }
      }
    }
    if (appPlatform.isDesktop) {
      val h by remember { chatModel.currentRemoteHost }
      if (h != null) {
        Spacer(Modifier.width(12.dp))
        HostDisconnectButton {
          stopRemoteHostAndReloadHosts(h!!, true)
        }
      }
    }
  }
}


@Composable
private fun BoxScope.unreadBadge(text: String? = "") {
  Text(
    text ?: "",
    color = MaterialTheme.colors.onPrimary,
    fontSize = 6.sp,
    modifier = Modifier
      .background(MaterialTheme.colors.primary, shape = CircleShape)
      .badgeLayout()
      .padding(horizontal = 3.dp)
      .padding(vertical = 1.dp)
      .align(Alignment.TopEnd)
  )
}

@Composable
private fun ToggleFilterEnabledButton() {
  val showUnread = remember { chatModel.activeChatTagFilter }.value == ActiveFilter.Unread

  IconButton(onClick = {
    if (showUnread) {
      chatModel.activeChatTagFilter.value = null
    } else {
      chatModel.activeChatTagFilter.value = ActiveFilter.Unread
    }
  }) {
    val sp16 = with(LocalDensity.current) { 16.sp.toDp() }
    Icon(
      painterResource(MR.images.ic_filter_list),
      null,
      tint = if (showUnread) MaterialTheme.colors.background else MaterialTheme.colors.secondary,
      modifier = Modifier
        .padding(3.dp)
        .background(color = if (showUnread) MaterialTheme.colors.primary else Color.Unspecified, shape = RoundedCornerShape(50))
        .border(width = 1.dp, color = if (showUnread) MaterialTheme.colors.primary else Color.Unspecified, shape = RoundedCornerShape(50))
        .padding(3.dp)
        .size(sp16)
    )
  }
}

@Composable
expect fun ActiveCallInteractiveArea(call: Call)

fun connectIfOpenedViaUri(rhId: Long?, uri: String, chatModel: ChatModel) {
  Log.d(TAG, "connectIfOpenedViaUri: opened via link")
  if (chatModel.currentUser.value == null) {
    chatModel.appOpenUrl.value = rhId to uri
  } else {
    withBGApi {
      chatModel.appOpenUrlConnecting.value = true
      planAndConnect(rhId, uri, close = null, cleanup = { chatModel.appOpenUrlConnecting.value = false })
    }
  }
}



internal fun connect(link: String, searchChatFilteredBySimplexLink: MutableState<Set<String>>, cleanup: (() -> Unit)?) {
  withBGApi {
    planAndConnect(
      chatModel.remoteHostId(),
      link,
      filterKnownContact = { searchChatFilteredBySimplexLink.value = setOf(it.id) },
      filterKnownGroup = { searchChatFilteredBySimplexLink.value = setOf(it.id) },
      close = null,
      cleanup = cleanup,
    )
  }
}

@Composable
private fun ErrorSettingsView() {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(generalGetString(MR.strings.error_showing_content), color = MaterialTheme.colors.error, fontStyle = FontStyle.Italic)
  }
}

private var lazyListState = 0 to 0

enum class ScrollDirection {
  Up, Down, Idle
}

@Composable
fun BoxScope.StatusBarBackground() {
  if (appPlatform.isAndroid) {
    val bg = if (LocalCardScreen.current) canvasColorForCurrentTheme() else MaterialTheme.colors.background
    Box(
      Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .zIndex(50f)
        .background(bg)
    )
  }
}

@Composable
fun BoxScope.NavigationBarBackground(appBarOnBottom: Boolean = false, mixedColor: Boolean, noAlpha: Boolean = false) {
  val keyboardState = getKeyboardState()
  if (appPlatform.isAndroid && keyboardState.value == KeyboardState.Closed) {
    val barPadding = WindowInsets.navigationBars.asPaddingValues()
    val paddingBottom = barPadding.calculateBottomPadding()
    val color = if (mixedColor) MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.97f) else MaterialTheme.colors.background
    val finalColor = color.copy(if (noAlpha) 1f else if (appBarOnBottom) remember { appPrefs.inAppBarsAlpha.state }.value else 0.6f)
    Box(Modifier.align(Alignment.BottomStart).height(paddingBottom).fillMaxWidth().background(finalColor))
  }
}

@Composable
fun BoxScope.NavigationBarBackground(modifier: Modifier, color: Color = MaterialTheme.colors.background) {
  val keyboardState = getKeyboardState()
  if (appPlatform.isAndroid && keyboardState.value == KeyboardState.Closed) {
    val barPadding = WindowInsets.navigationBars.asPaddingValues()
    val paddingBottom = barPadding.calculateBottomPadding()
    val finalColor = color.copy(0.6f)
    Box(modifier.align(Alignment.BottomStart).height(paddingBottom).fillMaxWidth().background(finalColor))
  }
}

@Composable
private fun BoxScope.ChatList(
  searchText: MutableState<TextFieldValue>,
  searchVisible: MutableState<Boolean>,
  listState: LazyListState,
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  stopped: Boolean
) {
  ChatListContent(searchText, searchVisible, listState, userPickerState, setPerformLA, stopped)
}

// The list tags and the connect-by-name row share one slot. When there is no name, the tags show; on
// mobile the row replaces the tags while shown. On desktop both show, arranged by the caller (which
// knows whether the search bar is above or below), passed as desktopView.
@Composable
private fun TagsOrConnectByName(
  searchText: MutableState<TextFieldValue>,
  connectNameCandidate: MutableState<String?>,
  desktopView: @Composable (candidate: String) -> Unit,
) {
  val candidate = connectNameCandidate.value
  when {
    candidate == null -> TagsView(searchText)
    !appPlatform.isDesktop -> ConnectByNameRow(candidate, searchText, connectNameCandidate, close = null)
    else -> desktopView(candidate)
  }
}


@Composable
private fun NoChatsView(searchText: MutableState<TextFieldValue>) {
  val activeFilter = remember { chatModel.activeChatTagFilter }.value

  if (searchText.value.text.isBlank()) {
    when (activeFilter) {
      is ActiveFilter.PresetTag -> Text(generalGetString(MR.strings.no_filtered_chats), color = MaterialTheme.colors.secondary, textAlign = TextAlign.Center) // this should not happen
      is ActiveFilter.UserTag -> Text(String.format(generalGetString(MR.strings.no_chats_in_list), activeFilter.tag.chatTagText), color = MaterialTheme.colors.secondary, textAlign = TextAlign.Center)
      is ActiveFilter.Unread -> {
          Row(
            Modifier.clip(shape = CircleShape).clickable { chatModel.activeChatTagFilter.value = null }.padding(DEFAULT_PADDING_HALF),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              painterResource(MR.images.ic_filter_list),
              null,
              tint = MaterialTheme.colors.secondary
            )
            Text(generalGetString(MR.strings.no_unread_chats), color = MaterialTheme.colors.secondary, textAlign = TextAlign.Center)
          }
      }
      null -> {
        Text(generalGetString(MR.strings.no_chats), color = MaterialTheme.colors.secondary, textAlign = TextAlign.Center)
      }
    }
  } else {
    Text(generalGetString(MR.strings.no_chats_found), color = MaterialTheme.colors.secondary, textAlign = TextAlign.Center)
  }
}



@Composable
expect fun TagsRow(content: @Composable() (() -> Unit))


@Composable
fun ItemPresetFilterAction(
  presetTag: PresetTagKind,
  active: Boolean,
  showMenu: MutableState<Boolean>,
  onCloseMenuAction: MutableState<(() -> Unit)>
) {
  val (icon, _, text) = presetTagLabel(presetTag, active)
  ItemAction(
    stringResource(text),
    painterResource(icon),
    color = if (active) MaterialTheme.colors.primary else Color.Unspecified,
    onClick = {
      onCloseMenuAction.value = {
        chatModel.activeChatTagFilter.value = ActiveFilter.PresetTag(presetTag)
        onCloseMenuAction.value = {}
      }
      showMenu.value = false
    }
  )
}

fun filteredChats(
  searchShowingSimplexLink: State<Boolean>,
  searchChatFilteredBySimplexLink: State<Set<String>>,
  searchText: String,
  chats: List<Chat>,
  activeFilter: ActiveFilter? = null,
): List<Chat> {
  val linkChatIds = searchChatFilteredBySimplexLink.value
  return if (linkChatIds.isNotEmpty()) {
    chats.filter { it.id in linkChatIds }
  } else {
    val s = if (searchShowingSimplexLink.value) "" else searchText.trim().lowercase()
    if (s.isEmpty())
      chats.filter { chat -> chat.id == chatModel.chatId.value || (!chat.chatInfo.chatDeleted && !chat.chatInfo.contactCard && filtered(chat, activeFilter)) }
    else {
      chats.filter { chat ->
        chat.id == chatModel.chatId.value ||
          when (val cInfo = chat.chatInfo) {
            is ChatInfo.Direct -> !cInfo.contact.chatDeleted && !chat.chatInfo.contactCard && cInfo.anyNameContains(s)
            is ChatInfo.Group -> cInfo.anyNameContains(s)
            is ChatInfo.Local -> cInfo.anyNameContains(s)
            is ChatInfo.ContactRequest -> cInfo.anyNameContains(s)
            is ChatInfo.ContactConnection -> cInfo.contactConnection.localAlias.lowercase().contains(s)
            is ChatInfo.InvalidJSON -> false
          }
      }
    }
  }
}

private fun filtered(chat: Chat, activeFilter: ActiveFilter?): Boolean =
  when (activeFilter) {
    is ActiveFilter.PresetTag -> presetTagMatchesChat(activeFilter.tag, chat.chatInfo, chat.chatStats)
    is ActiveFilter.UserTag -> chat.chatInfo.chatTags?.contains(activeFilter.tag.chatTagId) ?: false
    is ActiveFilter.Unread -> chat.unreadTag
    else -> true
  }

fun presetTagMatchesChat(tag: PresetTagKind, chatInfo: ChatInfo, chatStats: Chat.ChatStats): Boolean =
  when (tag) {
    PresetTagKind.GROUP_REPORTS -> chatStats.reportsCount > 0
    PresetTagKind.FAVORITES -> chatInfo.chatSettings?.favorite == true
    PresetTagKind.CONTACTS -> when (chatInfo) {
      is ChatInfo.Direct -> !chatInfo.contact.isContactCard && !chatInfo.contact.chatDeleted
      is ChatInfo.ContactRequest -> true
      is ChatInfo.ContactConnection -> true
      is ChatInfo.Group -> chatInfo.groupInfo.businessChat?.chatType == BusinessChatType.Customer
      else -> false
    }
    PresetTagKind.GROUPS -> when (chatInfo) {
      is ChatInfo.Group -> chatInfo.groupInfo.businessChat == null && !chatInfo.groupInfo.isChannel
      else -> false
    }
    PresetTagKind.CHANNELS -> when (chatInfo) {
      is ChatInfo.Group -> chatInfo.groupInfo.isChannel
      else -> false
    }
    PresetTagKind.BUSINESS -> when (chatInfo) {
      is ChatInfo.Group -> chatInfo.groupInfo.businessChat?.chatType == BusinessChatType.Business
      else -> false
    }
    PresetTagKind.NOTES -> when (chatInfo) {
      is ChatInfo.Local -> !chatInfo.noteFolder.chatDeleted
      else -> false
    }
  }

fun scrollToBottom(scope: CoroutineScope, listState: LazyListState) {
  scope.launch { try { listState.animateScrollToItem(0) } catch (e: Exception) { Log.e(TAG, e.stackTraceToString()) } }
}
