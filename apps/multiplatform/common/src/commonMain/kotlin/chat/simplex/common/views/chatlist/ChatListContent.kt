package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import chat.simplex.common.views.ux.ChatsTopBar
import chat.simplex.common.views.ux.ChatFoldersSettingsScreen
import chat.simplex.common.views.ux.ChatSelectionTopBar
import chat.simplex.common.views.ux.showAddToFolderModal
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
// The header (ChatsTopBar + filter pills) is composed OUTSIDE the LazyColumn, as a sibling
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
  // #102: Telegram-style selection mode - long-press a chat to enter, taps
  // toggle rows, the top bar morphs into X + count + batch actions.
  val selectionMode = remember { mutableStateOf(false) }
  val selectedChatIds = remember { mutableStateListOf<String>() }
  val searchShowingSimplexLink = remember { mutableStateOf(false) }
  val searchChatFilteredBySimplexLink = remember { mutableStateOf<Set<String>>(emptySet()) }
  val connectNameCandidate = remember { mutableStateOf<String?>(null) }
  // #98/#101: bumped when the Chat Folders settings modal closes so both the
  // pills row and custom-folder filtering re-read ChatFoldersPrefs.
  var foldersVersion by remember { mutableStateOf(0) }
  // #99: chatModel.chats is a SnapshotStateList mutated in place (its .value
  // identity never changes), so a composition-time copy is taken here - the
  // tracked read invalidates this scope on any chat mutation, and the copy's
  // structural equality lets the remember blocks below cache across unrelated
  // recompositions (pull frames, typing, other model ticks).
  val allChatsSnapshot = allChats.value.toList()
  // #101: a custom folder lists exactly its included chats minus the excluded
  // ones - membership lives in ChatFoldersPrefs.
  val customFolderFilter = (activeFilter.value as? ActiveFilter.CustomFolder)?.let { cf ->
    remember(cf.folderId, foldersVersion) { ChatFoldersPrefs.loadFolders().find { it.id == cf.folderId } }
  }
  // SimpleUX pin (FB-14): pinned chats float to the top of the list, above all
  // other chats (there are no sort headers on this screen). The sort is stable,
  // so the existing ordering within each group is untouched, and it reads the
  // snapshot-backed pinnedChatIds so toggling re-sorts immediately.
  // FB-12/13: created one-time invitations are managed in Settings (InvitationLinksSection),
  // not as chats - hidden here so each preserved link stops polluting the list.
  // #99: the whole filter + sort pipeline runs once per input change instead of
  // on every recomposition; pinned/starred ids are captured as Sets so the
  // comparator stops doing contains() over the SnapshotStateLists (O(n*m)).
  // chatModel.chatId is a key because filteredChats() always includes the open
  // chat even when it fails the filter.
  val pinnedIds = chatModel.pinnedChatIds.toSet()
  val starredIds = chatModel.starredChatIds.toSet()
  val chats = remember(
    allChatsSnapshot, searchText.value.text, activeFilter.value,
    searchShowingSimplexLink.value, searchChatFilteredBySimplexLink.value,
    customFolderFilter, pinnedIds, starredIds, chatModel.chatId.value
  ) {
    val rawChats = filteredChats(searchShowingSimplexLink, searchChatFilteredBySimplexLink, searchText.value.text, allChatsSnapshot, activeFilter.value)
    (when {
      activeFilter.value is ActiveFilter.CustomFolder ->
        rawChats.filter { customFolderFilter?.matchesChat(it.id) == true }
      activeFilter.value == ActiveFilter.PresetTag(PresetTagKind.FAVORITES) ->
        rawChats.filter { starredIds.contains(it.id) }
      else -> rawChats
    }).filterNot { isCreatedInvitationChat(it) }
      .sortedByDescending { pinnedIds.contains(it.id) }
  }

  val isSearching = searchText.value.text.isNotEmpty() || searchVisible.value
  val bottomPadding = if (isSearching) 16.dp else 96.dp

  // #98: visible folders re-read whenever the settings modal closes
  // (foldersVersion bump in its onDispose). "All" is always on regardless of
  // stored state - it has no toggle in settings. Hoisted above the header so
  // the pills row and the list swipe (#111) share one folder order.
  val visibleFolders = remember(foldersVersion) {
    ChatFoldersPrefs.loadFolders()
      .map { if (it.id == "all") it.copy(isVisible = true) else it }
      .filter { it.isVisible }
      .sortedBy { it.order }
  }

  // #111: single source of truth for folder -> filter, shared by the pills
  // row and the horizontal swipe so both always agree on the target view.
  fun folderToFilter(folder: ChatFolder): ActiveFilter? = when {
    folder.id == "all" -> null
    folder.id == "unread" -> ActiveFilter.Unread
    folder.isCustom -> ActiveFilter.CustomFolder(folder.id)
    folder.id == "direct" -> ActiveFilter.PresetTag(PresetTagKind.CONTACTS)
    folder.id == "groups" -> ActiveFilter.PresetTag(PresetTagKind.GROUPS)
    folder.id == "favorites" -> ActiveFilter.PresetTag(PresetTagKind.FAVORITES)
    else -> null
  }
  val orderedFilters = remember(foldersVersion) { visibleFolders.mapNotNull(::folderToFilter) }

  // #111: Telegram-style folder switching - a horizontal drag on the list
  // moves to the previous/next visible folder in pill order. Enabled under
  // the same conditions as the pills row, instant on commit (#58: no
  // AnimatedContent, no retained raster layers), rubber-banded at the ends.
  val folderDragX = remember { mutableStateOf(0f) }
  val listContentWidth = remember { mutableStateOf(1f) }
  val folderSwipeEnabled = !selectionMode.value && searchText.value.text.isEmpty() &&
    !searchShowingSimplexLink.value && orderedFilters.size > 1

  val allDirectoryGroups by SimpleUxDirectoryRepository.groups.collectAsState()
  val directoryBotDescription = stringResource(MR.strings.directory_bot_description)
  val directoryBotCategory = stringResource(MR.strings.directory_category)

  Box(Modifier.fillMaxSize().clipToBounds()) {
    Column(Modifier.fillMaxSize().imePadding()) {
      Column(
        Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colors.background)
      ) {
        if (selectionMode.value) {
          val selectedChats = allChats.value.filter { selectedChatIds.contains(it.id) }
          // #102: note-folder chats (Private notes) cannot be deleted - the core
          // rejects ApiDeleteChat for Local chats ("not supported", verified on
          // emulator-5554), same as Telegram's Saved Messages. Delete only
          // offers the remaining selected chats; the action dims when that
          // set is empty.
          val deletableChats = selectedChats.filter { it.chatInfo !is ChatInfo.Local }
          ChatSelectionTopBar(
            count = selectedChatIds.size,
            anyUnpinned = selectedChats.any { !chatModel.pinnedChatIds.contains(it.id) },
            anyUnread = selectedChats.any { it.unreadTag || it.chatStats.unreadCount > 0 },
            deleteEnabled = deletableChats.isNotEmpty(),
            onClose = {
              selectionMode.value = false
              selectedChatIds.clear()
            },
            onPin = {
              val pinAll = selectedChats.any { !chatModel.pinnedChatIds.contains(it.id) }
              selectedChats.forEach { chat ->
                if (chatModel.pinnedChatIds.contains(chat.id) != pinAll) chatModel.togglePinnedChat(chat.id)
              }
            },
            onToggleRead = {
              val readAll = selectedChats.any { it.unreadTag || it.chatStats.unreadCount > 0 }
              selectedChats.forEach { chat ->
                if (readAll) markChatRead(chat) else markChatUnread(chat, chatModel)
              }
            },
            onAddToFolder = { showAddToFolderModal(selectedChatIds) },
            onDelete = {
              // #102: batch delete - one confirmation naming the chats, then the
              // same SimpleXAPI.deleteChat path the single-chat delete uses
              if (deletableChats.isNotEmpty()) {
                AlertManager.shared.showAlertDialog(
                  title = generalGetString(MR.strings.delete_chat_question),
                  text = deletableChats.joinToString(", ") { it.chatInfo.displayName },
                  parseHtml = false,
                  confirmText = generalGetString(MR.strings.delete_verb),
                  onConfirm = {
                    withBGApi {
                      deletableChats.forEach { chatModel.controller.deleteChat(it) }
                      withContext(Dispatchers.Main) {
                        if (chatModel.chatId.value != null && deletableChats.any { it.id == chatModel.chatId.value }) {
                          chatModel.chatId.value = null
                        }
                        selectionMode.value = false
                        selectedChatIds.clear()
                      }
                    }
                  },
                  destructive = true,
                  dismissText = generalGetString(MR.strings.cancel_verb)
                )
              }
            }
          )
        } else {
          ChatsTopBar(
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
        }

        if (!selectionMode.value && searchText.value.text.isEmpty() && !searchShowingSimplexLink.value) {
          // #98: Auto-hide rule - if only 1 folder visible, hide the entire row
          if (visibleFolders.size > 1) {
            // Active pill follows the domain filter; custom folders (#101)
            // resolve by their folder id.
            val activeFolderId = when (val f = activeFilter.value) {
              null -> "all"
              is ActiveFilter.Unread -> "unread"
              is ActiveFilter.CustomFolder -> f.folderId
              is ActiveFilter.PresetTag -> when (f.tag) {
                PresetTagKind.CONTACTS -> "direct"
                PresetTagKind.GROUPS -> "groups"
                PresetTagKind.FAVORITES -> "favorites"
                else -> "all"
              }
              else -> "all"
            }

            // #83: the pill counts exactly what the Unread filter will list  -
            // chats matching the same unreadTag predicate over the same listable
            // set. The old sumOf(unreadCount) counted unread MESSAGES across all
            // chats including the hidden invitation chats (FB-12/13), producing a
            // non-zero badge over an empty filter.
            // #99: keyed on the snapshot copy, not allChats.value - the
            // SnapshotStateList is mutated in place so its identity never
            // changes and a key on it would freeze the count after first
            // composition.
            val totalUnread = remember(allChatsSnapshot) {
              allChatsSnapshot.count { it.unreadTag && !isCreatedInvitationChat(it) }
            }

            FilterPillsRow(
              visibleFolders = visibleFolders,
              activeFolderId = activeFolderId,
              totalUnread = totalUnread,
              onFolderSelected = { folder ->
                chatModel.activeChatTagFilter.value = folderToFilter(folder)
              },
              onManageClick = {
                ModalManager.start.showModal(cardScreen = true) {
                  DisposableEffect(Unit) {
                    onDispose { foldersVersion++ }
                  }
                  ChatFoldersSettingsScreen(
                    chatModel = chatModel,
                    onBack = { ModalManager.start.closeModals() }
                  )
                }
              }
            )
          }

          // SimpleUX Active Contacts / Favorites Rail (Collapsible on search)
          val scope = rememberCoroutineScope()
          // #99: the shared snapshot (stable identity between unrelated
          // recompositions) keeps the rail's remember(chats) filter cached.
          ActiveContactsRail(
            chats = allChatsSnapshot,
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
            .graphicsLayer { translationX = folderDragX.value }
            .onSizeChanged { listContentWidth.value = it.width.toFloat() }
            .then(
              if (folderSwipeEnabled) Modifier.pointerInput(orderedFilters) {
                detectHorizontalDragGestures(
                  onDragEnd = {
                    val threshold = (listContentWidth.value * 0.25f).coerceAtLeast(200f)
                    val dx = folderDragX.value
                    val index = orderedFilters.indexOfFirst { it == activeFilter.value }
                    val target = when {
                      dx <= -threshold && index < orderedFilters.lastIndex -> index + 1
                      dx >= threshold && index > 0 -> index - 1
                      else -> null
                    }
                    folderDragX.value = 0f
                    if (target != null) {
                      performHapticFeedback(SimpleUXHapticType.MEDIUM)
                      activeFilter.value = orderedFilters[target]
                    }
                  },
                  onDragCancel = { folderDragX.value = 0f }
                ) { _, dragAmount ->
                  val index = orderedFilters.indexOfFirst { it == activeFilter.value }
                  val atEdge = (index <= 0 && dragAmount > 0) ||
                    (index >= orderedFilters.lastIndex && dragAmount < 0)
                  // No neighbor in the drag direction: rubber-band instead of travel
                  folderDragX.value = if (atEdge) {
                    (folderDragX.value + dragAmount * 0.3f).coerceIn(-160f, 160f)
                  } else {
                    (folderDragX.value + dragAmount).coerceIn(-listContentWidth.value / 3f, listContentWidth.value / 3f)
                  }
                }
              } else Modifier
            ),
          state = listState,
          contentPadding = PaddingValues(bottom = bottomPadding),
          reverseLayout = false
        ) {
          itemsIndexed(chats, key = { _, chat -> chat.remoteHostId to chat.id }) { index, chat ->
            val nextChatSelected = remember(chat.id, chats) { derivedStateOf {
              chatModel.chatId.value != null && chats.getOrNull(index + 1)?.id == chatModel.chatId.value
            } }
            ChatListNavLinkView(
              chat,
              nextChatSelected,
              selectionActive = selectionMode.value,
              selectionChecked = selectedChatIds.contains(chat.id),
              onEnterSelection = {
                selectionMode.value = true
                selectedChatIds.clear()
                selectedChatIds.add(chat.id)
              },
              onToggleSelection = {
                if (selectedChatIds.contains(chat.id)) {
                  selectedChatIds.remove(chat.id)
                  if (selectedChatIds.isEmpty()) selectionMode.value = false
                } else {
                  selectedChatIds.add(chat.id)
                }
              }
            )
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
      }
    }
  }

  LaunchedEffect(activeFilter.value) {
    searchText.value = TextFieldValue("")
  }
}
