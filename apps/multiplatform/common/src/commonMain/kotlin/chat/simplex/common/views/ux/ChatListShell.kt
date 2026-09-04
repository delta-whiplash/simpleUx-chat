package chat.simplex.common.views.ux

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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import chat.simplex.common.views.ux.camera.QuickCameraPane
import chat.simplex.common.views.ux.components.*
import chat.simplex.common.views.ux.modals.*
import chat.simplex.common.views.ux.update.AppUpdater
import chat.simplex.common.views.ux.update.UpdateNoticeBanner
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import chat.simplex.common.views.chatlist.*

@Composable
fun ChatsTopBar(
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  stopped: Boolean,
  listState: LazyListState,
  searchVisible: MutableState<Boolean>,
  searchText: MutableState<TextFieldValue>,
  searchShowingSimplexLink: MutableState<Boolean>,
  searchChatFilteredBySimplexLink: MutableState<Set<String>>,
  connectNameCandidate: MutableState<String?>
) {
  val isDark = isInDarkTheme()
  val showMenu = remember { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  val tabState = LocalSimpleUxTab.current
  val headerScope = rememberCoroutineScope()
  var kebabCenter by remember { mutableStateOf<Offset?>(null) }

  // #99: derived so per-keystroke search writes don't recompose the header
  // wrapper - it only flips when search mode is entered/left.
  val searchModeActive by remember { derivedStateOf { searchVisible.value || searchText.value.text.isNotEmpty() } }

  if (searchModeActive) {
    // Search mode: use DefaultAppBar with search
    DefaultAppBar(
      navigationButton = {
        NavigationButtonBack(
          onButtonClicked = {
            searchText.value = TextFieldValue("")
            searchVisible.value = false
            focusManager.clearFocus()
          },
          tintColor = if (isDark) Sky400 else Blue600
        )
      },
      onTop = true,
      showSearch = true,
      searchAlwaysVisible = true,
      searchPlaceholder = stringResource(MR.strings.search_or_paste_simplex_link),
      onSearchValueChanged = { searchText.value = searchText.value.copy(it) },
      searchTrailingContent = null,
      solidBackground = true
    )
  } else {
    // Normal mode: title with menu
    // #63: radar sheet trigger, hoisted for the click lambda (null outside the host)
    val openRadar = LocalServerRadarSheet.current
    DefaultAppBar(
      onTop = true,
      solidBackground = true,
      title = {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable {
              // openRadar is hoisted above (CompositionLocals read in composition);
              // #63: the radar sheet lives at the SimpleUxTabHost root (see the host
              // comment for why it can never wrap this bar again); legacy modal push
              // only where the local is absent
              if (openRadar != null) {
                openRadar()
              } else if (!ModalManager.start.hasModalOpen(ModalViewId.CONNECTION_STATUS)) {
                ModalManager.start.showCustomModal(id = ModalViewId.CONNECTION_STATUS) { close ->
                  ServerRadarSheet(
                    isConnected = chatModel.chatRunning.value == true,
                    onConfigureServers = {
                      close()
                      ModalManager.start.showCustomModal { closeServers ->
                        NetworkAndServersView(closeServers)
                      }
                    },
                    onClose = close
                  )
                }
              }
            }
            .padding(vertical = 4.dp, horizontal = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "SimpleUX",
            color = if (isDark) AmberGold else Amber600,
            style = TextStyle(
              fontFamily = PlusJakartaSans,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
          )
          val isConnected = chatModel.chatRunning.value == true
          if (!isConnected) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Coral500)
            )
          }
        }
      },
      buttons = {
        // Search Button
        IconButton(onClick = { searchVisible.value = true }) {
          Icon(
            painterResource(MR.images.ic_search),
            contentDescription = stringResource(MR.strings.search_verb),
            tint = if (isDark) Slate300 else Slate600,
            modifier = Modifier.size(22.dp)
          )
        }

        // Options Menu
        Box {
          IconButton(
            onClick = { showMenu.value = true },
            modifier = Modifier.onGloballyPositioned { kebabCenter = it.boundsInWindow().center }
          ) {
            Icon(
              painterResource(MR.images.ic_more_vert),
              contentDescription = stringResource(MR.strings.icon_descr_options),
              tint = if (isDark) Slate300 else Slate600,
              modifier = Modifier.size(22.dp)
            )
          }

          DefaultDropdownMenu(
            showMenu = showMenu,
            offset = DpOffset(0.dp, 4.dp)
          ) {
            // Theme Switch Item
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  showMenu.value = false
                  ThemeAnimationController.trigger(
                    originOffset = kebabCenter,
                    currentlyDark = isDark,
                    scope = headerScope
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
                fontWeight = FontWeight.Normal,
                color = if (isDark) Slate50 else Slate900
              )
            }

            Divider(color = if (isDark) Color(0x15FFFFFF) else Color(0x10000000), thickness = 0.5.dp)

            // Profiles & Identities
            val openProfileSwitcher = LocalOpenProfileSwitcher.current
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  showMenu.value = false
                  if (openProfileSwitcher != null) {
                    openProfileSwitcher()
                  } else {
                    userPickerState.value = AnimatedViewState.VISIBLE
                  }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Icon(
                painterResource(MR.images.ic_supervised_user_circle_filled),
                contentDescription = null,
                tint = if (isDark) Slate400 else Slate500,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = stringResource(MR.strings.profile_switcher_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Slate50 else Slate900
              )
            }

            // New Contact
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  showMenu.value = false
                  ModalManager.start.closeModals()
                  tabState.value = SimpleUxTab.CONTACTS
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Icon(
                painterResource(MR.images.ic_person_add),
                contentDescription = null,
                tint = if (isDark) Slate400 else Slate500,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = stringResource(MR.strings.chat_list_new_contact),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Slate50 else Slate900
              )
            }

            // Settings
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  showMenu.value = false
                  ModalManager.start.closeModals()
                  tabState.value = SimpleUxTab.SETTINGS
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Icon(
                painterResource(MR.images.ic_settings),
                contentDescription = null,
                tint = if (isDark) Slate400 else Slate500,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = stringResource(MR.strings.toolbar_settings),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Slate50 else Slate900
              )
            }
          }
        }
      }
    )
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
          searchChatFilteredBySimplexLink.value = emptySet()
          if (candidate != null) {
            delay(NAME_SEARCH_DEBOUNCE_MS)
            val rhId = chatModel.remoteHostId()
            val inProgress = mutableStateOf(false)
            val targets = if (candidate.startsWith("@") || candidate.startsWith("#")) listOf(candidate) else listOf("@$candidate", "#$candidate")
            val ids = targets.mapNotNull { name ->
              knownChatId(rhId, chatModel.controller.apiConnectPlan(rhId, name, PlanResolveMode.PRMNever, inProgress = inProgress))
            }
            searchChatFilteredBySimplexLink.value = ids.toSet()
            if (ids.size == targets.size) connectNameCandidate.value = null
          } else if (!searchShowingSimplexLink.value || it.isEmpty()) {
            if (it.isEmpty()) {
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

enum class SimpleUxTab {
  CHATS, CONTACTS, SCAN, SETTINGS
}

val LocalSimpleUxTab = compositionLocalOf<MutableState<SimpleUxTab>> { mutableStateOf(SimpleUxTab.CHATS) }

/**
 * Opened by the header kebab's "Profiles & Identities" entry (FB-5): shows the
 * [ProfileSwitcherOverlay] hosted by [SimpleUxTabHost]. Null outside the host
 * (call sites then fall back to the desktop-only UserPicker).
 */
val LocalOpenProfileSwitcher = compositionLocalOf<(() -> Unit)?> { null }

/**
 * Opened by tapping the "SimpleUX" brand text in the chat-list header (server
 * connectivity at a glance). Provided by [SimpleUxSheetsHost]; null outside it (call
 * sites then fall back to the legacy modal push).
 */
val LocalServerRadarSheet = compositionLocalOf<(() -> Unit)?> { null }

/**
 * #109: the launch update notice ([AppUpdater] instance), provided by MainScreen and
 * docked under the island bar by [SimpleUxTabHost]; null outside it (no notice).
 */
val LocalUpdateNotice = compositionLocalOf<AppUpdater?> { null }

// Extracted verbatim from ChatListView.kt (issue #4): the tab-switch host of the chat-list screen.
// The fork-owned state (current tab, search visibility, profile-switcher popup, quick-camera filter)
// stays owned by the caller in ChatListView.kt and is passed in; only the CHATS tab content differs
// per call site, so it arrives as the chatsTab slot.
@Composable
fun SimpleUxTabHost(
  chatModel: ChatModel,
  currentTab: MutableState<SimpleUxTab>,
  searchText: MutableState<TextFieldValue>,
  searchVisible: MutableState<Boolean>,
  listState: LazyListState,
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  chatsTab: @Composable BoxScope.() -> Unit
) {
  val keyboardState by getKeyboardState()
  val scope = rememberCoroutineScope()
  val showProfileSwitcherPopup = remember { mutableStateOf(false) }

  CompositionLocalProvider(
    LocalSimpleUxTab provides currentTab,
    LocalOpenProfileSwitcher provides { showProfileSwitcherPopup.value = true }
  ) {
    Box(Modifier.fillMaxSize()) {
      // No animated tab transitions here (issue #58): the transition's retained layer
      // rasterized a background-colored hole across the middle of the viewport that ate
      // list rows (reproduced on emulator-5554 with both hardware and software rendering,
      // light and dark themes). Tab switches are instant, which matches the Telegram
      // benchmark anyway. The CI lint guard enforces this.
      Box(Modifier.fillMaxSize()) {
        when (val tab = currentTab.value) {
          SimpleUxTab.CHATS -> {
            chatsTab()
          }
          SimpleUxTab.CONTACTS -> {
            if (appPlatform.isAndroid) {
              BackHandler { currentTab.value = SimpleUxTab.CHATS }
            }
            val bottomPadding = if (keyboardState == KeyboardState.Closed) 56.dp else 0.dp
            Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background).padding(bottom = bottomPadding)) {
              Column(Modifier.fillMaxSize()) {
                // #85: created one-time invitations live in the Contacts tab -
                // the context where they are created and consumed - not at the
                // bottom of Settings. #112: rendered inside the sheet's list
                // flow (below the invite actions) instead of pinned above it.
                // Zero-state: renders nothing at all.
                val modalData = remember { ModalData() }
                modalData.NewChatSheet(
                  rh = chatModel.currentRemoteHost.value,
                  invitationsSection = { InvitationLinksSection() },
                  close = { currentTab.value = SimpleUxTab.CHATS }
                )
              }
            }
          }
          // #84: Scan is a tab, not a page - the top bar and island bar stay
          // in place and only the content area swaps to the camera. The pane's
          // own BackHandler routes back to CHATS (FB-16 lesson: back must
          // always have an in-app way out).
          SimpleUxTab.SCAN -> {
            QuickCameraPane(onClose = { currentTab.value = SimpleUxTab.CHATS })
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

      // #99: derived - reading searchText here directly recomposed the whole
      // tab-host shell on every keystroke.
      val showIslandBar by remember { derivedStateOf { keyboardState == KeyboardState.Closed && searchText.value.text.isEmpty() && !searchVisible.value } }
      // #109: the update notice is docked UNDER the island bar in the same bottom
      // stack - its presence lifts the bar instead of floating over content
      val updateNotice = LocalUpdateNotice.current
      if (showIslandBar || updateNotice != null) {
        Column(
          Modifier
            .zIndex(10f)
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp)
        ) {
          if (showIslandBar) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                // #84: Scan is a tab now - the camera renders as QuickCameraPane in
                // the tab host; the island item only needs to know it's available.
                scanAvailable = appPlatform.isAndroid
              )
            }
          }
          if (updateNotice != null) {
            // under the bar: its presence lifts the bar, nothing floats over content
            UpdateNoticeBanner(updateNotice, Modifier.padding(top = 8.dp))
          }
        }
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
}

/**
 * #63: the screen-root sheet layer for the whole chat surface (list AND the sliding
 * conversation panel): server radar + chat→tag assignment.
 *
 * M2 ModalBottomSheetLayout always fills max size - the scrim needs the whole screen -
 * so wrapping slotted content with it (the top bar in ChatsTopBar, or the tab box
 * slots) makes that slot consume the entire column and starves the chat list: that is
 * the ux.40/41 empty-list regression, reverted in de0f9cdac. Hosted HERE, at the
 * AndroidScreen root around already-full-size children, full-size is exactly right.
 * The trigger locals let any depth open a sheet; surfaces outside this host (desktop
 * panel, previews) see null locals and fall back to the legacy page/modal paths.
 */
sealed interface ChatListSheet {
  data class TagPicker(val chat: Chat) : ChatListSheet
  data object ServerRadar : ChatListSheet
}

@Composable
fun SimpleUxSheetsHost(chatModel: ChatModel, content: @Composable () -> Unit) {
  val scope = rememberCoroutineScope()
  var chatListSheet by remember { mutableStateOf<ChatListSheet?>(null) }
  val chatListSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
  LaunchedEffect(chatListSheet) {
    if (chatListSheet != null) chatListSheetState.show() else chatListSheetState.hide()
  }
  LaunchedEffect(chatListSheetState.currentValue, chatListSheetState.targetValue) {
    if (chatListSheetState.currentValue == ModalBottomSheetValue.Hidden && chatListSheetState.targetValue == ModalBottomSheetValue.Hidden && chatListSheet != null) {
      chatListSheet = null
    }
  }
  BackHandler(enabled = chatListSheet != null) {
    scope.launch { chatListSheetState.hide() }
  }

  ModalBottomSheetLayout(
    sheetState = chatListSheetState,
    sheetContent = {
      when (val sheet = chatListSheet) {
        is ChatListSheet.TagPicker ->
          TagListPickerSheetContent(sheet.chat) { chatListSheet = null }
        ChatListSheet.ServerRadar ->
          ServerRadarSheet(
            isConnected = chatModel.chatRunning.value == true,
            onConfigureServers = {
              scope.launch { chatListSheetState.hide() }
              ModalManager.start.showCustomModal { closeServers ->
                NetworkAndServersView(closeServers)
              }
            },
            onClose = { scope.launch { chatListSheetState.hide() } }
          )
        null -> {}
      }
    }
  ) {
    CompositionLocalProvider(
      LocalTagListPicker provides { c: Chat -> chatListSheet = ChatListSheet.TagPicker(c) },
      LocalServerRadarSheet provides { chatListSheet = ChatListSheet.ServerRadar }
    ) {
      content()
    }
  }
}

// Vertical space the island bottom bar occupies above the navigation bars:
// its 2.dp bottom offset + Row vertical padding (2 x 6.dp) + a tab item's
// icon-over-label content (~50.dp). In-shell panes that draw their own bottom
// chrome (the Scan camera, #95) must lift it above this line or it lands
// behind the bar, which is composed last at zIndex(10f) over the tab content.
val BottomIslandBarClearance = 64.dp

@Composable
fun BoxScope.TelegramBottomIslandBar(
  currentTab: SimpleUxTab,
  onSelectTab: (SimpleUxTab) -> Unit,
  onProfileLongClick: () -> Unit = {},
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  onChatsClick: () -> Unit,
  // #84: whether the device offers the Scan pane (Android); when false the
  // Scan item is not composed at all.
  scanAvailable: Boolean = false
) {
  val isDark = isInDarkTheme()
  val shape = RoundedCornerShape(32.dp)

  // #109: bottom placement (nav inset, z-order, horizontal margin) is owned by the
  // bottom stack in SimpleUxTabHost so the update notice can dock under the bar.
  Box(
    modifier = Modifier,
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = shape,
      color = if (isDark) Color(0xEE121A26) else Color(0xFAFFFFFF),
      elevation = 12.dp,
      // FB-1: tabs share the width equally, so the pill's width is bounded
      // (full width up to a phone-friendly cap) instead of hugging uneven
      // label lengths. On desktop the 400.dp cap keeps the island compact.
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 400.dp)
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(
            listOf(
              if (isDark) Color(0x4DFFFFFF) else Color(0x220F172A),
              if (isDark) GlassBorderDark else Color(0x0A0F172A)
            )
          ),
          shape = shape
        )
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Tab 1: Chats
        IslandTabItem(
          modifier = Modifier.weight(1f),
          label = stringResource(MR.strings.settings_section_title_chats),
          icon = MR.images.ic_forum,
          isActive = (currentTab == SimpleUxTab.CHATS),
          onClick = {
            if (currentTab == SimpleUxTab.CHATS) {
              onChatsClick()
            } else {
              onSelectTab(SimpleUxTab.CHATS)
            }
          }
        )

        // Tab 2: Contacts
        IslandTabItem(
          modifier = Modifier.weight(1f),
          label = stringResource(MR.strings.settings_section_title_contact),
          icon = MR.images.ic_supervised_user_circle_filled,
          isActive = (currentTab == SimpleUxTab.CONTACTS),
          onClick = {
            onSelectTab(SimpleUxTab.CONTACTS)
          }
        )

        if (scanAvailable) {
          // Scan (see views/ux/camera/QuickCameraPane, Android-only): a real
          // tab since #84 - active while the camera pane is shown, re-tap
          // returns to CHATS. Same labeled-item layout as the other tabs.
          IslandTabItem(
            modifier = Modifier.weight(1f),
            label = stringResource(MR.strings.island_scan),
            icon = MR.images.ic_photo_camera,
            isActive = (currentTab == SimpleUxTab.SCAN),
            onClick = {
              if (currentTab == SimpleUxTab.SCAN) {
                onSelectTab(SimpleUxTab.CHATS)
              } else {
                onSelectTab(SimpleUxTab.SCAN)
              }
            }
          )
        }

        // Tab 3: Settings
        IslandTabItem(
          modifier = Modifier.weight(1f),
          label = stringResource(MR.strings.settings_section_title_settings),
          icon = MR.images.ic_settings,
          isActive = (currentTab == SimpleUxTab.SETTINGS),
          onClick = {
            onSelectTab(SimpleUxTab.SETTINGS)
          },
          onLongClick = onProfileLongClick
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IslandTabItem(
  label: String,
  icon: ImageResource,
  isActive: Boolean,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val isDark = isInDarkTheme()
  val activeShape = RoundedCornerShape(20.dp)
  val activeBg = if (isActive) {
    if (isDark) Brush.linearGradient(listOf(Color(0x33E2B755), Color(0x22D97706)))
    else Brush.linearGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)))
  } else {
    SolidColor(Color.Transparent)
  }

  val inactiveColor = if (isDark) Slate300 else Slate600

  Box(
    modifier = modifier
      .clip(activeShape)
      .background(activeBg)
      .then(if (isActive) Modifier.border(1.dp, if (isDark) Color(0x66E2B755) else AmberGold, activeShape) else Modifier)
      .then(
        if (onLongClick != null) {
          Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        } else {
          Modifier.clickable(onClick = onClick)
        }
      )
      .padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        painterResource(icon),
        contentDescription = label,
        modifier = Modifier.size(20.dp),
        tint = if (isActive) (if (isDark) AmberGold else Color(0xFFB45309)) else inactiveColor
      )
      Spacer(Modifier.height(2.dp))
      // FB-11: labels are single-line; instead of wrapping or ellipsizing the four
      // known short labels, the font shrinks slightly to fit (keeps the tab grid
      // intact under large font scales / narrow screens).
      var labelFontSize by remember(label) { mutableStateOf(11.sp) }
      Text(
        label,
        fontSize = labelFontSize,
        maxLines = 1,
        softWrap = false,
        onTextLayout = {
          if (it.hasVisualOverflow && labelFontSize > 8.sp) labelFontSize *= 0.92f
        },
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        color = if (isActive) (if (isDark) AmberGold else Color(0xFFB45309)) else inactiveColor
      )
    }
  }
}

