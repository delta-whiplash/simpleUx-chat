package chat.simplex.common.views.chatlist

import LocalCardScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import chat.simplex.common.views.usersettings.networkAndServers.NetworkAndServersView
import chat.simplex.common.views.ux.*
import chat.simplex.common.views.ux.camera.openQuickCameraSheet
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
fun ToggleChatListCard() {
  val oneHandUI = remember { appPrefs.oneHandUI.state }
  val onClose = {
    appPrefs.oneHandUICardShown.set(true)
    AlertManager.shared.showAlertMsg(
      title = generalGetString(MR.strings.one_hand_ui),
      text = generalGetString(MR.strings.one_hand_ui_change_instruction),
    )
  }
  val activeBg = MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.97f)
    .copy(alpha = appPrefs.inAppBarsAlpha.get())
  val selectedBg = MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.92f)
  Row(
    Modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
      .clip(RoundedCornerShape(percent = 50)),
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    ToolbarSegment(
      icon = MR.images.ic_mobile_3,
      text = stringResource(MR.strings.one_hand_ui_bottom_bar),
      isSelected = oneHandUI.value,
      selectedBg = selectedBg,
      activeBg = activeBg,
      modifier = Modifier.weight(1f)
    ) { appPrefs.oneHandUI.set(true) }
    Box(Modifier.weight(1f).fillMaxHeight()) {
      ToolbarSegment(
        icon = MR.images.ic_mobile_4,
        text = stringResource(MR.strings.one_hand_ui_top_bar),
        isSelected = !oneHandUI.value,
        selectedBg = selectedBg,
        activeBg = activeBg,
        modifier = Modifier.fillMaxSize()
      ) { appPrefs.oneHandUI.set(false) }
      Icon(
        painterResource(MR.images.ic_close), null,
        Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 4.dp)
          .clip(CircleShape)
          .clickable(onClick = onClose)
          .padding(8.dp)
          .size(16.dp),
        tint = MaterialTheme.colors.secondary
      )
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

val LocalSimpleUxTab = compositionLocalOf<MutableState<SimpleUxTab>> { mutableStateOf(SimpleUxTab.CHATS) }

// Spec: spec/client/chat-list.md#ChatListView
@Composable
fun ChatListView(chatModel: ChatModel, userPickerState: MutableStateFlow<AnimatedViewState>, setPerformLA: (Boolean) -> Unit, stopped: Boolean) {
  val oneHandUI = remember { appPrefs.oneHandUI.state }

  LaunchedEffect(Unit) {
    val showWhatsNew = shouldShowWhatsNew(chatModel)
    val showUpdatedConditions = chatModel.conditions.value.conditionsAction?.shouldShowNotice ?: false
    if (showWhatsNew || showUpdatedConditions) {
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
  val keyboardState by getKeyboardState()
  DisposableEffect(Unit) {
    onDispose {
      lazyListState = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
    }
  }
  val scope = rememberCoroutineScope()
  val showProfileSwitcherPopup = remember { mutableStateOf(false) }
  val quickCameraConnectFilter = remember { mutableStateOf(emptySet<String>()) }

  CompositionLocalProvider(LocalSimpleUxTab provides currentTab) {
    Box(Modifier.fillMaxSize()) {
      AnimatedContent(
        targetState = currentTab.value,
        transitionSpec = {
          fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
            scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = FastOutSlowInEasing)) togetherWith
            fadeOut(animationSpec = tween(180))
        },
        modifier = Modifier.fillMaxSize()
      ) { tab ->
        when (tab) {
          SimpleUxTab.CHATS -> {
            ChatListWithLoadingScreen(searchText, searchVisible, listState, userPickerState, setPerformLA, stopped)
          }
          SimpleUxTab.CONTACTS -> {
            if (appPlatform.isAndroid) {
              BackHandler { currentTab.value = SimpleUxTab.CHATS }
            }
            val bottomPadding = if (keyboardState == KeyboardState.Closed) 56.dp else 0.dp
            Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background).padding(bottom = bottomPadding)) {
              val modalData = remember { ModalData() }
              modalData.NewChatSheet(rh = chatModel.currentRemoteHost.value, close = { currentTab.value = SimpleUxTab.CHATS })
            }
          }
          SimpleUxTab.SETTINGS -> {
            if (appPlatform.isAndroid) {
              BackHandler { currentTab.value = SimpleUxTab.CHATS }
            }
            val bottomPadding = if (keyboardState == KeyboardState.Closed) 56.dp else 0.dp
            Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background).padding(bottom = bottomPadding)) {
              SettingsView(chatModel, setPerformLA, close = { currentTab.value = SimpleUxTab.CHATS })
            }
          }
        }
      }

    if (keyboardState == KeyboardState.Closed && searchText.value.text.isEmpty() && !searchVisible.value) {
      TelegramBottomIslandBar(
        currentTab = currentTab.value,
        onSelectTab = { tab ->
          ModalManager.start.closeModals()
          currentTab.value = tab
        },
        onProfileLongClick = {
          showProfileSwitcherPopup.value = true
        },
        userPickerState = userPickerState,
        setPerformLA = setPerformLA,
        onChatsClick = {
          if (listState.firstVisibleItemIndex != 0) {
            scope.launch { listState.animateScrollToItem(0) }
          } else {
            chatModel.activeChatTagFilter.value = null
            searchText.value = TextFieldValue("")
          }
        },
      onOpenCamera = if (appPlatform.isAndroid) {
        {
          openQuickCameraSheet(
            onPhotoCaptured = { uri ->
              // Reuses the same cross-chat hand-off as Android's system share-into-SimpleX
              // flow: setting sharedContent swaps this screen for ShareListView (App.kt's
              // StartPartOfScreen), which already knows how to pick a chat and attach media.
              // (The camera button lives on the chat-LIST screen, not inside an open
              // conversation -- ChatView slides over this screen on mobile -- so there is no
              // "currently active chat" to silently attach to; picking one is the real flow.)
              chatModel.sharedContent.value = SharedContent.Media(text = "", uris = listOf(uri))
            },
            onQrCode = { link ->
              val target = strConnectTarget(link.trim())
              if (target is ConnectTarget.Link) {
                connect(target.text, quickCameraConnectFilter) {}
                true
              } else {
                false
              }
            }
          )
        }
      } else null
    )
    }

    ProfileSwitcherOverlay(
      chatModel = chatModel,
      show = showProfileSwitcherPopup.value,
      onDismiss = { showProfileSwitcherPopup.value = false },
      onNavigateToProfile = {
        showProfileSwitcherPopup.value = false
        currentTab.value = SimpleUxTab.SETTINGS
      }
    )

    ThemeCircularRevealOverlay()
    }
  }

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
private fun ConnectBannerCard() {
  val isDark = isInDarkTheme()
  val labelBg = MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.97f)
    .copy(alpha = appPrefs.inAppBarsAlpha.get())
  val buttonSize = 30.dp * fontSizeSqrtMultiplier
  val gap = 3.dp * fontSizeSqrtMultiplier

  Column(horizontalAlignment = Alignment.End) {
    IconButton(
      onClick = { appPrefs.addressCreationCardShown.set(true) },
      modifier = Modifier.size(buttonSize)
    ) {
      Icon(
        painterResource(MR.images.ic_close),
        contentDescription = stringResource(MR.strings.icon_descr_close_button),
        modifier = Modifier
          .size(buttonSize)
          .background(MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.92f), CircleShape)
          .padding(buttonSize * 0.15f),
        tint = MaterialTheme.colors.secondary
      )
    }
    Spacer(Modifier.height(gap))
    Row(
      Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .clip(RoundedCornerShape(18.dp))
    ) {
      Column(
        Modifier.weight(1f).clickable {
          ModalManager.start.showModalCloseable { close ->
            NewChatView(chatModel.currentRemoteHost.value, NewChatOption.INVITE, close = close)
          }
        }
      ) {
        if (BuildConfigCommon.SIMPLEX_ASSETS) {
          Image(
            painterResource(if (isDark) MR.images.banner_create_link_light else MR.images.banner_create_link),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().aspectRatio(BANNER_IMAGE_RATIO)
          )
        } else {
          BannerGradientBox(isDark) {
            Icon(painterResource(MR.images.ic_add_link), contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colors.primary)
          }
        }
        Box(Modifier.fillMaxWidth().background(labelBg).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
          if (BuildConfigCommon.SIMPLEX_ASSETS) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(painterResource(MR.images.ic_add_link), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colors.primary)
              Text(stringResource(MR.strings.new_1_time_link), style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onBackground)
            }
          } else {
            Text(stringResource(MR.strings.new_1_time_link), style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onBackground)
          }
        }
      }
      Spacer(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colors.background))
      Column(
        Modifier.weight(1f).clickable {
          ModalManager.start.showModalCloseable { close ->
            NewChatView(chatModel.currentRemoteHost.value, NewChatOption.CONNECT, showQRCodeScanner = appPlatform.isAndroid, close = close)
          }
        }
      ) {
        if (BuildConfigCommon.SIMPLEX_ASSETS) {
          Image(
            painterResource(if (isDark) MR.images.banner_paste_link_light else MR.images.banner_paste_link),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().aspectRatio(BANNER_IMAGE_RATIO)
          )
        } else {
          BannerGradientBox(isDark) {
            Icon(painterResource(MR.images.ic_qr_code_scanner), contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colors.primary)
          }
        }
        Box(Modifier.fillMaxWidth().background(labelBg).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
          if (BuildConfigCommon.SIMPLEX_ASSETS) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(painterResource(MR.images.ic_qr_code_scanner), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colors.primary)
              Text(stringResource(if (appPlatform.isAndroid) MR.strings.scan_paste_link else MR.strings.paste_link), style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onBackground)
            }
          } else {
            Text(stringResource(if (appPlatform.isAndroid) MR.strings.scan_paste_link else MR.strings.paste_link), style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onBackground)
          }
        }
      }
    }
  }
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
private fun AndroidOnboardingCards() {
  val oneHandUI = remember { appPrefs.oneHandUI.state }
  val topPad = topPaddingToContent(false)
  val bottomPad = if (oneHandUI.value) {
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + AppBarHeight * fontSizeSqrtMultiplier
  } else {
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  }
  Box(Modifier.fillMaxSize().padding(top = topPad, bottom = bottomPad)) {
    ConnectOnboardingView()
  }
}

@Composable
private fun BoxScope.NewChatSheetFloatingButton(oneHandUI: State<Boolean>, stopped: Boolean) {
  FloatingActionButton(
    onClick = {
      if (!stopped) {
        showNewChatSheet(oneHandUI)
      }
    },
    Modifier
      .navigationBarsPadding()
      .padding(end = DEFAULT_PADDING, bottom = DEFAULT_PADDING)
      .align(Alignment.BottomEnd)
      .bounceClick()
      .size(AppBarHeight * fontSizeSqrtMultiplier),
    elevation = FloatingActionButtonDefaults.elevation(
      defaultElevation = 3.dp,
      pressedElevation = 1.dp,
      hoveredElevation = 5.dp,
      focusedElevation = 3.dp,
    ),
    backgroundColor = if (!stopped) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
    contentColor = Color.White
  ) {
    Icon(painterResource(MR.images.ic_edit_filled), stringResource(MR.strings.add_contact_or_create_group), Modifier.size(22.dp * fontSizeSqrtMultiplier))
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
private fun ChatListToolbar(userPickerState: MutableStateFlow<AnimatedViewState>, listState: LazyListState, stopped: Boolean, setPerformLA: (Boolean) -> Unit) {
  val serversSummary: MutableState<PresentedServersSummary?> = remember { mutableStateOf(null) }
  val barButtons = arrayListOf<@Composable RowScope.() -> Unit>()
  val updatingProgress = remember { chatModel.updatingProgress }.value
  val oneHandUI = remember { appPrefs.oneHandUI.state }

  if (updatingProgress != null) {
    barButtons.add {
      val interactionSource = remember { MutableInteractionSource() }
      val hovered = interactionSource.collectIsHoveredAsState().value
      IconButton(onClick = {
        chatModel.updatingRequest?.close()
      }, Modifier.hoverable(interactionSource)) {
        if (hovered) {
          Icon(painterResource(MR.images.ic_close), null, tint = WarningOrange)
        } else if (updatingProgress == -1f) {
          CIFileViewScope.progressIndicator()
        } else {
          CIFileViewScope.progressCircle((updatingProgress * 100).toLong(), 100)
        }
      }
    }
  }

  if (stopped) {
    barButtons.add {
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
  } else {
    if (connectProgressManager.showConnectProgress != null) {
      barButtons.add {
        Box(Modifier.padding(horizontal = DEFAULT_PADDING_HALF)) {
          CIFileViewScope.progressIndicator()
        }
      }
    }

    if (oneHandUI.value) {
      val sp16 = with(LocalDensity.current) { 16.sp.toDp() }

      if (appPlatform.isDesktop && oneHandUI.value) {
        val call = remember { chatModel.activeCall }
        if (call.value != null) {
          barButtons.add {
            val c = call.value
            if (c != null) {
              ActiveCallInteractiveArea(c)
              Spacer(Modifier.width(5.dp))
            }
          }
        }
      }

      barButtons.add {
        IconButton(
          onClick = {
            showNewChatSheet(oneHandUI)
          },
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .background(
                Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF0088FF))),
                shape = CircleShape
              )
              .border(1.dp, Color(0x66FFFFFF), CircleShape)
              .size(36.dp * fontSizeSqrtMultiplier)
          ) {
            Icon(
              painterResource(MR.images.ic_edit_filled),
              stringResource(MR.strings.add_contact_or_create_group),
              Modifier.size(sp16),
              tint = Color.White
            )
          }
        }
      }
    }
  }

  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val canScrollToZero = remember { derivedStateOf { listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0 } }
  DefaultAppBar(
    navigationButton = {
      if (chatModel.users.isEmpty() && !chatModel.desktopNoUserNoRemote) {
        NavigationButtonMenu {
          ModalManager.start.showModalCloseable(cardScreen = true) { close ->
            SettingsView(chatModel, setPerformLA, close)
          }
        }
      } else {
        val users by remember { derivedStateOf { chatModel.users.filter { u -> u.user.activeUser || !u.user.hidden } } }
        val allRead = users
          .filter { u -> !u.user.activeUser && !u.user.hidden }
          .all { u -> u.unreadCount == 0 }
        UserProfileButton(chatModel.currentUser.value?.profile?.image, allRead) {
            userPickerState.value = AnimatedViewState.VISIBLE
        }
      }
    },
    title = {
      if (!shouldShowOnboarding()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DEFAULT_SPACE_AFTER_ICON)) {
          Text(
            stringResource(MR.strings.your_chats),
            color = MaterialTheme.colors.onBackground,
            fontWeight = FontWeight.SemiBold,
          )
          SubscriptionStatusIndicator(
            click = {
              ModalManager.start.closeModals()
              val summary = serversSummary.value
              ModalManager.start.showModalCloseable(
                endButtons = {
                  if (summary != null) {
                    ShareButton {
                      val json = Json {
                        prettyPrint = true
                      }
                      val text = json.encodeToString(PresentedServersSummary.serializer(), summary)
                      clipboard.shareText(text)
                    }
                  }
                }
              ) { ServersSummaryView(chatModel.currentRemoteHost.value, serversSummary) }
            }
          )
        }
      }
    },
    onTitleClick = if (canScrollToZero.value) { { scrollToBottom(scope, listState) } } else null,
    onTop = !oneHandUI.value,
    onSearchValueChanged = {},
    buttons = { barButtons.forEach { it() } }
  )
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

@Composable
private fun ChatListSearchBar(listState: LazyListState, searchText: MutableState<TextFieldValue>, searchShowingSimplexLink: MutableState<Boolean>, searchChatFilteredBySimplexLink: MutableState<Set<String>>, connectNameCandidate: MutableState<String?>) {
  val isDark = isInDarkTheme()
  val focusRequester = remember { FocusRequester() }
  var focused by remember { mutableStateOf(false) }
  val shape = RoundedCornerShape(18.dp)

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp, vertical = 5.dp)
      .clip(shape)
      .background(if (isDark) Color(0x661E293B) else Color(0xEEF1F5F9))
      .border(
        1.dp,
        if (focused) Color(0x9938BDF8) else if (isDark) Color(0x38FFFFFF) else Color(0x1F000000),
        shape
      )
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
      Icon(
        painterResource(MR.images.ic_search),
        contentDescription = null,
        Modifier.padding(start = 4.dp, end = 6.dp).size(20.dp * fontSizeSqrtMultiplier),
        tint = if (focused) Color(0xFF38BDF8) else MaterialTheme.colors.secondary
      )
      SearchTextField(
        Modifier.weight(1f).onFocusChanged { focused = it.hasFocus }.focusRequester(focusRequester),
        placeholder = stringResource(MR.strings.search_or_paste_simplex_link),
        alwaysVisible = true,
        searchText = searchText,
        enabled = !remember { searchShowingSimplexLink }.value,
        trailingContent = null,
        // the clear button must line up with the filter icon it replaces, so no reduction here
        reducedCloseButtonPadding = 0.dp,
      ) {
        searchText.value = searchText.value.copy(it)
      }
      val hasText = remember { derivedStateOf { searchText.value.text.isNotEmpty() } }
      if (hasText.value) {
        val hideSearchOnBack: () -> Unit = { searchText.value = TextFieldValue() }
        BackHandler(onBack = hideSearchOnBack)
        KeyChangeEffect(chatModel.currentRemoteHost.value) {
          hideSearchOnBack()
        }
      } else {
        val padding = if (appPlatform.isDesktop) 0.dp else 4.dp
        if (chatModel.chats.value.isNotEmpty()) {
          ToggleFilterEnabledButton()
        }
        Spacer(Modifier.width(padding))
      }
      val focusManager = LocalFocusManager.current
      val keyboardState = getKeyboardState()
      LaunchedEffect(keyboardState.value) {
        if (keyboardState.value == KeyboardState.Closed && focused) {
          focusManager.clearFocus()
        }
      }
      val view = LocalMultiplatformView()
      LaunchedEffect(Unit) {
        snapshotFlow { searchText.value.text }
          .distinctUntilChanged()
          .collectLatest {
            val target = strConnectTarget(it.trim())
            if (target is ConnectTarget.Link) {
              hideKeyboard(view)
              searchText.value = searchText.value.copy(target.linkText, selection = TextRange.Zero)
              searchShowingSimplexLink.value = true
              searchChatFilteredBySimplexLink.value = emptySet()
              connectNameCandidate.value = null
              connect(target.text, searchChatFilteredBySimplexLink) { searchText.value = TextFieldValue() }
            } else {
              val candidate = nameSearchCandidate(it.trim())
              connectNameCandidate.value = candidate
              // clear the previous match immediately so the list falls back to text search during the debounce,
              // instead of showing a stale filtered chat while the new search runs
              searchChatFilteredBySimplexLink.value = emptySet()
              if (candidate != null) {
                // resolve the name locally on each keystroke, debounced; collectLatest cancels the in-flight
                // search when the next keystroke arrives. A bare name can be a contact or a channel, so search
                // both and filter every known chat found; drop the row only when both types are already known.
                delay(NAME_SEARCH_DEBOUNCE_MS)
                val rhId = chatModel.remoteHostId()
                val inProgress = mutableStateOf(false) // background search: no spinner, no error alerts
                val targets = if (candidate.startsWith("@") || candidate.startsWith("#")) listOf(candidate) else listOf("@$candidate", "#$candidate")
                val ids = targets.mapNotNull { name ->
                  knownChatId(rhId, chatModel.controller.apiConnectPlan(rhId, name, PlanResolveMode.PRMNever, inProgress = inProgress))
                }
                searchChatFilteredBySimplexLink.value = ids.toSet()
                if (ids.size == targets.size) connectNameCandidate.value = null
              } else if (!searchShowingSimplexLink.value || it.isEmpty()) {
                if (it.isNotEmpty()) {
                  focusRequester.requestFocus()
                } else {
                  if (!chatModel.appOpenUrlConnecting.value) {
                    connectProgressManager.cancelConnectProgress()
                  }
                  if (listState.layoutInfo.totalItemsCount > 0) {
                    listState.scrollToItem(0)
                  }
                }
                searchShowingSimplexLink.value = false
                searchChatFilteredBySimplexLink.value = emptySet()
              }
            }
          }
      }
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
  val activeFilter = remember { chatModel.activeChatTagFilter }
  val allChats = remember { chatModel.chats }
  val searchShowingSimplexLink = remember { mutableStateOf(false) }
  val searchChatFilteredBySimplexLink = remember { mutableStateOf<Set<String>>(emptySet()) }
  val connectNameCandidate = remember { mutableStateOf<String?>(null) }
  val pullOffset = remember { mutableStateOf(0f) }
  val isRefreshing = remember { mutableStateOf(false) }
  val nestedScrollConnection = remember {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y > 15f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
          searchVisible.value = true
        }
        if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
          pullOffset.value = (pullOffset.value + available.y * 0.4f).coerceAtMost(180f)
        } else if (available.y < 0 && pullOffset.value > 0) {
          pullOffset.value = (pullOffset.value + available.y).coerceAtLeast(0f)
        }
        return Offset.Zero
      }

      override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
        if (pullOffset.value > 100f) {
          isRefreshing.value = true
          chat.simplex.common.platform.performHapticFeedback(chat.simplex.common.platform.SimpleUXHapticType.MEDIUM)
          delay(800)
          isRefreshing.value = false
        }
        pullOffset.value = 0f
        return super.onPostFling(consumed, available)
      }
    }
  }
  val rawChats = filteredChats(searchShowingSimplexLink, searchChatFilteredBySimplexLink, searchText.value.text, allChats.value.toList(), activeFilter.value)
  val chats = if (activeFilter.value == ActiveFilter.PresetTag(PresetTagKind.FAVORITES)) {
    rawChats.filter { chatModel.starredChatIds.contains(it.id) }
  } else {
    rawChats
  }

  val isSearching = searchText.value.text.isNotEmpty() || searchVisible.value
  val bottomPadding = if (isSearching) 16.dp else 96.dp

  Box(Modifier.fillMaxSize().clipToBounds()) {
    LazyColumnWithScrollBarNoAppBar(
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .clipToBounds()
        .nestedScroll(nestedScrollConnection),
      state = listState,
      contentPadding = PaddingValues(bottom = bottomPadding),
      reverseLayout = false
    ) {
    stickyHeader {
      Column(
        Modifier
          .fillMaxWidth()
          .zIndex(20f)
          .background(MaterialTheme.colors.background)
          .windowInsetsPadding(WindowInsets.statusBars)
      ) {
        TelegramTopHeader(
          userPickerState = userPickerState,
          setPerformLA = setPerformLA,
          stopped = stopped,
          listState = listState,
          searchVisible = searchVisible,
          searchText = searchText,
          searchShowingSimplexLink = searchShowingSimplexLink,
          searchChatFilteredBySimplexLink = searchChatFilteredBySimplexLink,
          connectNameCandidate = connectNameCandidate
        )

        if (searchText.value.text.isEmpty() && !searchShowingSimplexLink.value) {
          // SimpleUX Fast Category Filter Pills
          val currentUxCategory = remember(activeFilter.value) {
            when (val f = activeFilter.value) {
              null -> UxFilterCategory.ALL
              is ActiveFilter.Unread -> UxFilterCategory.UNREAD
              is ActiveFilter.PresetTag -> when (f.tag) {
                PresetTagKind.CONTACTS -> UxFilterCategory.DIRECT
                PresetTagKind.GROUPS -> UxFilterCategory.GROUPS
                PresetTagKind.FAVORITES -> UxFilterCategory.FAVORITES
                else -> UxFilterCategory.ALL
              }
              else -> UxFilterCategory.ALL
            }
          }
          val totalUnread = remember(allChats.value) {
            allChats.value.sumOf { chat -> chat.chatStats.unreadCount }
          }
          FilterPillsRow(
            activeCategory = currentUxCategory,
            totalUnread = totalUnread,
            onCategorySelected = { cat ->
              chatModel.activeChatTagFilter.value = if (cat == currentUxCategory) {
                null
              } else {
                when (cat) {
                  UxFilterCategory.ALL -> null
                  UxFilterCategory.UNREAD -> ActiveFilter.Unread
                  UxFilterCategory.DIRECT -> ActiveFilter.PresetTag(PresetTagKind.CONTACTS)
                  UxFilterCategory.GROUPS -> ActiveFilter.PresetTag(PresetTagKind.GROUPS)
                  UxFilterCategory.FAVORITES -> ActiveFilter.PresetTag(PresetTagKind.FAVORITES)
                }
              }
            }
          )

          // SimpleUX Active Contacts / Favorites Rail (Collapsible on search)
          val scope = rememberCoroutineScope()
          ActiveContactsRail(
            chats = allChats.value.toList(),
            onChatClicked = { targetChat ->
              scope.launch {
                when (val info = targetChat.chatInfo) {
                  is ChatInfo.Direct -> directChatAction(targetChat.remoteHostId, info.contact, chatModel)
                  is ChatInfo.Group -> groupChatAction(targetChat.remoteHostId, info.groupInfo, chatModel, mutableStateOf(false))
                  is ChatInfo.Local -> noteFolderChatAction(targetChat.remoteHostId, info.noteFolder)
                  else -> chatModel.chatId.value = targetChat.id
                }
              }
            }
          )
        }

        if (connectNameCandidate.value != null) {
          ConnectByNameRow(connectNameCandidate.value!!, searchText, connectNameCandidate, close = null)
        }
        Spacer(Modifier.height(4.dp))
      }
    }

    itemsIndexed(chats, key = { _, chat -> chat.remoteHostId to chat.id }) { index, chat ->
      val nextChatSelected = remember(chat.id, chats) { derivedStateOf {
        chatModel.chatId.value != null && chats.getOrNull(index + 1)?.id == chatModel.chatId.value
      } }
      ChatListNavLinkView(chat, nextChatSelected)
    }

    if (searchText.value.text.isNotBlank() && chats.isEmpty()) {
      item {
        Box(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = stringResource(MR.strings.chat_list_no_local_results, searchText.value.text),
            style = TextStyle(
              fontFamily = Inter,
              fontSize = 13.sp,
              color = MaterialTheme.colors.secondary
            )
          )
        }
      }
    }

    if (searchText.value.text.isNotBlank()) {
      item {
        PublicDirectorySearchResultsSection(
          query = searchText.value.text.trim(),
          onJoinGroup = { link, onComplete ->
            val rhId = chatModel.currentRemoteHost.value?.remoteHostId
            withBGApi {
              chatModel.appOpenUrlConnecting.value = true
              planAndConnect(
                rhId,
                link,
                close = {
                  searchText.value = TextFieldValue("")
                  onComplete()
                },
                cleanup = {
                  chatModel.appOpenUrlConnecting.value = false
                  onComplete()
                },
                autoJoin = true
              )
            }
          }
        )
      }
    }

    if (chats.isEmpty() && searchText.value.text.isBlank()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isInDarkTheme()) Color(0x2238BDF8) else Color(0x1A0284C7)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                painter = painterResource(MR.images.ic_forum),
                contentDescription = null,
                tint = if (isInDarkTheme()) Color(0xFF38BDF8) else Color(0xFF0284C7),
                modifier = Modifier.size(32.dp)
              )
            }
            Text(
              text = stringResource(MR.strings.chat_list_empty_title),
              style = TextStyle(
                fontFamily = Inter,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isInDarkTheme()) Color(0xFFF1F5F9) else Color(0xFF0F172A)
              ),
              textAlign = TextAlign.Center
            )
            Text(
              text = stringResource(MR.strings.chat_list_empty_subtitle),
              style = TextStyle(
                fontFamily = Inter,
                fontSize = 13.sp,
                color = if (isInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)
              ),
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 16.dp)
            )
          }
        }
      }
    }

    if (!isSearching) {
      item {
        Spacer(Modifier.height(100.dp))
      }
    }
  }

    MineralPullToRefreshIndicator(
      pullFraction = pullOffset.value / 100f,
      isRefreshing = isRefreshing.value,
      modifier = Modifier
        .align(Alignment.TopCenter)
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(top = (pullOffset.value * 0.4f).dp + 10.dp)
        .zIndex(10f)
    )
  }



  StatusBarBackground()

  LaunchedEffect(activeFilter.value) {
    searchText.value = TextFieldValue("")
  }
}

// Default top-level part used to complete a bare name typed in the search field (search field only;
// the message parser and the wire format are unchanged).
private const val DEFAULT_NAME_TLD = "testing"
// Shortest name that offers the button, so it is discoverable but does not flash on short prefixes.
private const val MIN_NAME_LENGTH = 5
// Wait this long after the last keystroke before the local name search runs.
internal const val NAME_SEARCH_DEBOUNCE_MS = 300L

private val nameLabelRegex = Regex("[a-zA-Z0-9]+(-[a-zA-Z0-9]+)*")
private fun isNameLabel(s: String): Boolean = s.length in 1..63 && nameLabelRegex.matches(s)

// On-device candidate for connecting by SimpleX name: the string sent to the core to resolve it.
// Mirrors the domain grammar (nameLabelP/mkDomain in SimplexName.hs): an optional @/# prefix, then
// dot-separated ASCII labels; a dotless word is completed with the default top-level part. Returns
// the string to send (keeping @/# so the type is preserved), or null when the text is not a name.
internal fun nameSearchCandidate(str: String): String? {
  val text = str.trim()
  val prefix = text.firstOrNull()?.takeIf { it == '@' || it == '#' }
  val core = if (prefix != null) text.substring(1) else text
  val labels = core.split(".")
  if (core.isEmpty() || labels.any { !isNameLabel(it) }) return null
  return when {
    labels.size > 1 -> text                                            // already has a top-level part
    core.length >= MIN_NAME_LENGTH -> "${prefix ?: ""}$core.$DEFAULT_NAME_TLD"
    else -> null
  }
}

// The chat id a local (PRMNever) search resolved to — a contact, a business, or a channel — or null on a miss.
// The core returns the correct type for @ vs # (getContactToConnect / type-filtered getGroupToConnect), so no
// client-side type check is needed.
internal suspend fun knownChatId(rhId: Long?, result: ConnectionPlanResult?): String? = when (val plan = result?.connectionPlan) {
  is ConnectionPlan.ContactAddress -> (plan.contactAddressPlan as? ContactAddressPlan.Known)?.contact?.let { contact ->
    // a name-resolved chat may be prepared in the store but not yet listed, so add it (as the tap path does)
    if (chatModel.getContactChat(contact.contactId) == null) {
      chatModel.chatsContext.addChat(Chat(remoteHostId = rhId, chatInfo = ChatInfo.Direct(contact), chatItems = emptyList()))
    }
    contact.id
  }
  is ConnectionPlan.GroupLink -> (when (val g = plan.groupLinkPlan) {
    is GroupLinkPlan.Known -> g.groupInfo
    is GroupLinkPlan.OwnLink -> g.groupInfo
    else -> null
  })?.let { gInfo ->
    if (chatModel.getGroupChat(gInfo.groupId) == null) {
      chatModel.chatsContext.addChat(Chat(remoteHostId = rhId, chatInfo = ChatInfo.Group(gInfo, groupChatScope = null), chatItems = emptyList()))
    }
    gInfo.id
  }
  else -> null
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
internal fun ConnectByNameRow(name: String, searchText: MutableState<TextFieldValue>, connectNameCandidate: MutableState<String?>, close: (() -> Unit)?) {
  val view = LocalMultiplatformView()
  val isDark = isInDarkTheme()
  val isGroup = name.startsWith("#")
  val shape = RoundedCornerShape(16.dp)

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp)
      .clip(shape)
      .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
      .border(1.dp, if (isDark) Color(0x35FFFFFF) else Color(0x150F172A), shape)
      .clickable {
        hideKeyboard(view)
        performHapticFeedback(SimpleUXHapticType.LIGHT)
        withBGApi {
          planAndConnect(
            chatModel.remoteHostId(),
            name,
            close = {
              searchText.value = TextFieldValue()
              connectNameCandidate.value = null
              close?.invoke()
            },
            cleanup = {
              connectNameCandidate.value = null
            },
          )
        }
      },
    color = Color.Transparent,
    shape = shape
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(if (isGroup) (if (isDark) Color(0x2238BDF8) else Color(0x150284C7)) else (if (isDark) Color(0x22E2B755) else Color(0x15D97706))),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painter = painterResource(if (isGroup) MR.images.ic_group else MR.images.ic_person),
          contentDescription = null,
          tint = if (isGroup) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else (if (isDark) Color(0xFFE2B755) else Color(0xFFD97706)),
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text = if (isGroup) stringResource(MR.strings.connect_by_name_group, name) else stringResource(MR.strings.connect_by_name_contact, name),
          color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.5.sp
        )
        Text(
          text = stringResource(MR.strings.directory_search_hint),
          color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
          fontSize = 11.5.sp
        )
      }
      Icon(
        painter = painterResource(MR.images.ic_chevron_right),
        contentDescription = null,
        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        modifier = Modifier.size(18.dp)
      )
    }
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
private fun ChatListFeatureCards() {
  val addressCreationCardShown = remember { appPrefs.addressCreationCardShown.state }

  if (!addressCreationCardShown.value && hasConversations(chatModel.chats.value)) {
    Column(modifier = Modifier.padding(16.dp)) {
      ConnectBannerCard()
    }
  }
}

private val TAG_MIN_HEIGHT = 35.dp

@Composable
private fun TagsView(searchText: MutableState<TextFieldValue>) {
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
expect fun TagsRow(content: @Composable() (() -> Unit))

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

private fun presetTagLabel(tag: PresetTagKind, active: Boolean): Triple<ImageResource, ImageResource?, StringResource> =
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

fun scrollToBottom(scope: CoroutineScope, listState: LazyListState) {
  scope.launch { try { listState.animateScrollToItem(0) } catch (e: Exception) { Log.e(TAG, e.stackTraceToString()) } }
}

@Composable
fun PublicDirectorySearchResultsSection(
  query: String,
  onJoinGroup: (String, () -> Unit) -> Unit
) {
  val isDark = isInDarkTheme()
  val trimmed = query.trim().lowercase()
  val joiningLink = remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    SimpleUxDirectoryRepository.fetchDirectoryIfNeeded(scope)
  }

  val allDirectoryGroups by SimpleUxDirectoryRepository.groups.collectAsState()

  // The directory bot entry carries no display strings (the repository is not localized); the UI
  // localizes them here, both for rendering and for search matching.
  val botDescription = stringResource(MR.strings.directory_bot_description)
  val botCategory = stringResource(MR.strings.directory_category)

  val matchingGroups = remember(query, allDirectoryGroups, botDescription, botCategory) {
    if (trimmed.isEmpty()) emptyList()
    else allDirectoryGroups.filter {
      val description = if (it.isDirectoryBot) botDescription else it.description
      val category = if (it.isDirectoryBot) botCategory else it.category
      it.name.lowercase().contains(trimmed) ||
      description.lowercase().contains(trimmed) ||
      category.lowercase().contains(trimmed) ||
      it.link.lowercase().contains(trimmed)
    }
  }

  if (matchingGroups.isEmpty()) return

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    // Simple clean separator
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        painterResource(MR.images.ic_travel_explore),
        contentDescription = null,
        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = stringResource(MR.strings.directory_section_header),
        style = TextStyle(
          fontFamily = Inter,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
      )
      Divider(
        modifier = Modifier.weight(1f),
        color = if (isDark) Color(0x2AFFFFFF) else Color(0x1F000000)
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(if (isDark) Color(0x661E293B) else Color(0xF2F1F5F9))
        .border(1.dp, if (isDark) Color(0x2AFFFFFF) else Color(0x15000000), RoundedCornerShape(16.dp))
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      matchingGroups.forEach { group ->
        val isJoining = joiningLink.value == group.link
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    if (group.link.contains("/a#")) listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                    else listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                  )
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                painterResource(if (group.link.contains("/a#")) MR.images.ic_travel_explore else MR.images.ic_group),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = group.name,
                  style = TextStyle(
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDark) Color(0x3338BDF8) else Color(0x1F0284C7))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                  Text(
                    text = if (group.isDirectoryBot) botCategory else group.category,
                    style = TextStyle(
                      fontFamily = Inter,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Medium,
                      color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                  )
                }
              }
              Text(
                text = if (group.isDirectoryBot) botDescription else group.description,
                style = TextStyle(
                  fontFamily = Inter,
                  fontSize = 11.sp,
                  color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(Modifier.width(8.dp))

          Button(
            onClick = {
              joiningLink.value = group.link
              performHapticFeedback(SimpleUXHapticType.MEDIUM)
              onJoinGroup(group.link) {
                joiningLink.value = null
              }
            },
            enabled = !isJoining,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              backgroundColor = if (group.link.contains("/a#")) (if (isDark) Color(0xFF8B5CF6) else Color(0xFF6366F1)) else (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
              contentColor = Color.White,
              disabledBackgroundColor = (if (group.link.contains("/a#")) (if (isDark) Color(0xFF8B5CF6) else Color(0xFF6366F1)) else (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))).copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
          ) {
            if (isJoining) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = Color.White,
                strokeWidth = 2.dp
              )
            } else {
              Text(
                text = if (group.link.contains("/a#")) stringResource(MR.strings.open_verb) else stringResource(MR.strings.join_group_button),
                style = TextStyle(
                  fontFamily = Inter,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              )
            }
          }
        }
      }
    }
  }
}
