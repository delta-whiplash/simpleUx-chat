package chat.simplex.common.views.ux

import LocalCardScreen
import androidx.compose.animation.*
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
import chat.simplex.common.views.ux.components.*
import chat.simplex.common.views.ux.modals.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import chat.simplex.common.views.chatlist.*

@Composable
fun TelegramTopHeader(
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
  val oneHandUI = remember { appPrefs.oneHandUI.state }
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  if (searchVisible.value || searchText.value.text.isNotEmpty()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      NavigationButtonBack(
        onButtonClicked = {
          searchText.value = TextFieldValue("")
          searchVisible.value = false
          focusManager.clearFocus()
        },
        tintColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
      )

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(if (isDark) Color(0x661E293B) else Color(0xEEF1F5F9))
          .border(
            1.dp,
            if (isDark) Color(0x38FFFFFF) else Color(0x1F000000),
            RoundedCornerShape(16.dp)
          )
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        SearchTextField(
          modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
          placeholder = stringResource(MR.strings.search_or_paste_simplex_link),
          alwaysVisible = true,
          searchText = searchText,
          enabled = !remember { searchShowingSimplexLink }.value,
          trailingContent = null,
          reducedCloseButtonPadding = 0.dp,
        ) {
          searchText.value = searchText.value.copy(it)
        }
      }

      LaunchedEffect(Unit) {
        focusRequester.requestFocus()
      }

      val hideSearchOnBack: () -> Unit = {
        searchText.value = TextFieldValue("")
        searchVisible.value = false
        focusManager.clearFocus()
      }
      BackHandler(onBack = hideSearchOnBack)
      KeyChangeEffect(chatModel.currentRemoteHost.value) {
        hideSearchOnBack()
      }
    }
  } else {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable {
            ModalManager.start.showCustomModal { close ->
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
          .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "SimpleUX",
          color = if (isDark) Color(0xFFE2B755) else Color(0xFFD97706),
          style = TextStyle(
            fontFamily = Inter,
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
              .background(Color(0xFFEF4444))
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        // Search Button - Natural & Borderless
        IconButton(onClick = { searchVisible.value = true }) {
          Icon(
            painterResource(MR.images.ic_search),
            contentDescription = "Recherche",
            tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
            modifier = Modifier.size(22.dp)
          )
        }

        val headerScope = rememberCoroutineScope()
        Box {
          // Options Menu Trigger - Natural & Borderless
          IconButton(onClick = { showMenu.value = true }) {
            Icon(
              painterResource(MR.images.ic_more_vert),
              contentDescription = "Options",
              tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
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
                    originOffset = Offset(950f, 145f),
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
                text = if (isDark) "Mode Clair" else "Mode Sombre",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
              )
            }

            Divider(color = if (isDark) Color(0x15FFFFFF) else Color(0x10000000), thickness = 0.5.dp)

            val tabState = LocalSimpleUxTab.current

            // Profiles & Identities
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  showMenu.value = false
                  userPickerState.value = AnimatedViewState.VISIBLE
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Icon(
                painterResource(MR.images.ic_supervised_user_circle_filled),
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Profils & Identités",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
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
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Nouveau contact",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
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
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Paramètres",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
              )
            }
          }
        }
      }
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
  CHATS, CONTACTS, SETTINGS
}

@Composable
fun BoxScope.TelegramBottomIslandBar(
  currentTab: SimpleUxTab,
  onSelectTab: (SimpleUxTab) -> Unit,
  onProfileLongClick: () -> Unit = {},
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  onChatsClick: () -> Unit,
  onOpenCamera: (() -> Unit)? = null
) {
  val isDark = isInDarkTheme()
  val shape = RoundedCornerShape(32.dp)

  Box(
    modifier = Modifier
      .zIndex(10f)
      .align(Alignment.BottomCenter)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(start = 20.dp, end = 20.dp, bottom = 2.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = shape,
      color = if (isDark) Color(0xEE121A26) else Color(0xFAFFFFFF),
      elevation = 12.dp,
      modifier = Modifier
        .wrapContentWidth()
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(
            listOf(
              if (isDark) Color(0x4DFFFFFF) else Color(0x220F172A),
              if (isDark) Color(0x1AFFFFFF) else Color(0x0A0F172A)
            )
          ),
          shape = shape
        )
    ) {
      Row(
        modifier = Modifier
          .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Tab 1: Chats
        IslandTabItem(
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

        Spacer(Modifier.width(10.dp))

        // Tab 2: Contacts
        IslandTabItem(
          label = stringResource(MR.strings.settings_section_title_contact),
          icon = MR.images.ic_supervised_user_circle_filled,
          isActive = (currentTab == SimpleUxTab.CONTACTS),
          onClick = {
            onSelectTab(SimpleUxTab.CONTACTS)
          }
        )

        if (onOpenCamera != null) {
          Spacer(Modifier.width(10.dp))

          // Central quick-access camera button (see views/ux/camera/QuickCameraSheet.kt,
          // Android-only for now). v1 visual treatment only — a raised disc overlapping
          // the capsule's top edge (the Instagram/WeChat look this was modeled on) is a
          // follow-up polish pass once this can be screenshotted on a real device.
          IconButton(onClick = onOpenCamera) {
            Icon(
              painterResource(MR.images.ic_photo_camera),
              contentDescription = stringResource(MR.strings.quick_camera_open),
              tint = MaterialTheme.colors.primary,
              modifier = Modifier.size(26.dp)
            )
          }
        }

        Spacer(Modifier.width(10.dp))

        // Tab 3: Settings
        IslandTabItem(
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
  onLongClick: (() -> Unit)? = null
) {
  val isDark = isInDarkTheme()
  val activeShape = RoundedCornerShape(20.dp)
  val activeBg = if (isActive) {
    if (isDark) Brush.linearGradient(listOf(Color(0x33E2B755), Color(0x22D97706)))
    else Brush.linearGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)))
  } else {
    SolidColor(Color.Transparent)
  }

  val inactiveColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

  Box(
    modifier = Modifier
      .clip(activeShape)
      .background(activeBg)
      .then(if (isActive) Modifier.border(1.dp, if (isDark) Color(0x66E2B755) else Color(0xFFF59E0B), activeShape) else Modifier)
      .then(
        if (onLongClick != null) {
          Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        } else {
          Modifier.clickable(onClick = onClick)
        }
      )
      .padding(horizontal = 16.dp, vertical = 6.dp),
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
        tint = if (isActive) (if (isDark) Color(0xFFE2B755) else Color(0xFFB45309)) else inactiveColor
      )
      Spacer(Modifier.height(2.dp))
      Text(
        label,
        fontSize = 11.sp,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        color = if (isActive) (if (isDark) Color(0xFFE2B755) else Color(0xFFB45309)) else inactiveColor
      )
    }
  }
}

