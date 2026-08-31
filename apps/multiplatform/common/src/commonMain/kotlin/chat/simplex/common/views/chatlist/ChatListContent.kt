package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.planAndConnect
import chat.simplex.common.views.ux.TelegramTopHeader
import chat.simplex.common.views.ux.isCreatedInvitationChat
import chat.simplex.common.views.ux.components.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

// Extracted verbatim from ChatListView.kt (issue #4): the SimpleUX chat-list content that the
// private BoxScope.ChatList shim in ChatListView.kt delegates to. Kept in package views.chatlist
// (not views/ux) because the empty-state palette has no exact ui/theme tokens yet (issue #16) and
// the views/ux hex-lint baseline must not grow. State ownership, remember keys and call order are
// unchanged.
//
// The header (TelegramTopHeader + filter pills) is composed OUTSIDE the LazyColumn, as a sibling
// above it. It used to be a `stickyHeader` inside the list; that pinned-sticky mechanism left a
// background-colored "hole" across the middle of the viewport whenever long search results
// scrolled under it (issue #58, reproduced and root-caused 2026-08-28 on emulator-5554 - see
// issue comments). The header is always pinned anyway, so hoisting it is visually identical and
// removes the sticky machinery entirely.
@Composable
internal fun BoxScope.ChatListContent(
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
  // SimpleUX pin (FB-14): pinned chats float to the top of the list, above all
  // other chats (there are no sort headers on this screen). The sort is stable,
  // so the existing ordering within each group is untouched, and it reads the
  // snapshot-backed pinnedChatIds so toggling re-sorts immediately.
  // FB-12/13: created one-time invitations are managed in Settings (InvitationLinksSection),
  // not as chats - hidden here so each preserved link stops polluting the list.
  val chats = (if (activeFilter.value == ActiveFilter.PresetTag(PresetTagKind.FAVORITES)) {
    rawChats.filter { chatModel.starredChatIds.contains(it.id) }
  } else {
    rawChats
  }).filterNot { isCreatedInvitationChat(it) }
    .sortedByDescending { chatModel.pinnedChatIds.contains(it.id) }

  val isSearching = searchText.value.text.isNotEmpty() || searchVisible.value
  val bottomPadding = if (isSearching) 16.dp else 96.dp

  val allDirectoryGroups by SimpleUxDirectoryRepository.groups.collectAsState()
  val directoryBotDescription = stringResource(MR.strings.directory_bot_description)
  val directoryBotCategory = stringResource(MR.strings.directory_category)

  Box(Modifier.fillMaxSize().clipToBounds()) {
    Column(Modifier.fillMaxSize().imePadding()) {
      Column(
        Modifier
          .fillMaxWidth()
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
          // #83: the pill counts exactly what the Unread filter will list  - 
          // chats matching the same unreadTag predicate over the same listable
          // set. The old sumOf(unreadCount) counted unread MESSAGES across all
          // chats including the hidden invitation chats (FB-12/13), producing a
          // non-zero badge over an empty filter.
          val totalUnread = remember(allChats.value) {
            allChats.value.count { it.unreadTag && !isCreatedInvitationChat(it) }
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

      Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
        LazyColumnWithScrollBarNoAppBar(
          modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .nestedScroll(nestedScrollConnection),
          state = listState,
          contentPadding = PaddingValues(bottom = bottomPadding),
          reverseLayout = false
        ) {
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
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.secondary
                  )
                )
              }
            }
          }

          if (searchText.value.text.isNotBlank()) {
            directorySearchItems(
              query = searchText.value.text,
              groups = allDirectoryGroups,
              botDescription = directoryBotDescription,
              botCategory = directoryBotCategory,
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
                      fontFamily = PlusJakartaSans,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isInDarkTheme()) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    ),
                    textAlign = TextAlign.Center
                  )
                  Text(
                    text = stringResource(MR.strings.chat_list_empty_subtitle),
                    style = TextStyle(
                      fontFamily = PlusJakartaSans,
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
            .padding(top = (pullOffset.value * 0.4f).dp + 10.dp)
            .zIndex(10f)
        )
      }
    }
  }

  StatusBarBackground()

  LaunchedEffect(activeFilter.value) {
    searchText.value = TextFieldValue("")
  }
}
