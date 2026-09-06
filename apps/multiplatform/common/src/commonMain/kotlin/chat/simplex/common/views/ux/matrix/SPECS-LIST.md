# SPECS-LIST — Unified chat list + read-only Matrix conversation (#138, #139)

Branch `matrix/p1-list`. Agent C file cluster only. All file:line references verified
on this branch before editing.

## What "unified" means

A Matrix chat is a synthetic `Chat` value inserted into the same `chatModel.chats`
state as SimpleX chats. The ONLY distinction is `ChatInfo.id` starting with `"mx|"`
(helper: `ChatInfo.protocol`, see ChatProtocolLocal.kt — TEMP copy until the
integration merge brings `views/ux/matrix/ChatProtocol.kt`).

A Matrix chat row and conversation must be INDISTINGUISHABLE from SimpleX except
for the badge another agent adds. Verified code paths:

- List membership + snapshot: `ChatListContent.kt:65,81` — `chatModel.chats`
  SnapshotStateList, per-frame `toList()` copy. Synthetic chats are ordinary
  elements; no id-format assumptions.
- Search/folder filter: `ChatListView.kt:576-612` (`filteredChats`, `filtered`,
  `presetTagMatchesChat`) switches on `chatInfo` variants only — synthetic
  Direct/Group rows render and filter identically (name search via
  `anyNameContains`, unread folder via `chat.unreadTag`, favorites via
  `chatSettings.favorite`). No id parsing, no createdAt parsing.
- Pinned sort: `ChatListContent.kt:98,113` — `pinnedChatIds.toSet()` +
  `sortedByDescending`, id-agnostic; pinning Matrix chats works (local prefs).
- Unread pill count: `ChatListContent.kt:261-263` — `unreadTag` predicate over
  the same snapshot; id-agnostic.
- Custom folders: `ChatListContent.kt:84-86,107-108` — `ChatFoldersPrefs`
  membership by `chat.id` string; Matrix ids participate naturally.
- Row rendering: `ChatListNavLinkView.kt:100-144` — Direct/Group branches render
  `ChatPreviewView(chat, ...)` from `chat.chatItems` carried in the model; the
  preview pipeline is untouched (badge agent owns ChatPreviewView.kt).
- List item key: `ChatListContent.kt:337` — `chat.remoteHostId to chat.id`;
  unique for synthetic ids (`mx|...`).
- Base ordering: `ChatsContext` pop/reorder in `ChatModel.kt` (~line 876) —
  engine bridge will mutate the same list the same way.

Verified NON-assumptions that could have broken: none of the list paths parse the
id format; unread counting is field-based (`chatStats.unreadCount`, `unreadTag`);
previews come from the Chat value, not a core query.

## Loader seam design (#139)

`views/ux/matrix/MatrixChatItemsLoader.kt`:

- `interface MatrixTimelineSource` — `loadLatest(chatInfo): List<ChatItem>`,
  `paginateOlder(chatInfo): List<ChatItem>` (empty = exhausted). The contract's
  Boolean `paginateOlder` was refined to return the slice so the UI merge stays
  in one place.
- `EmptyMatrixTimelineSource` — default no-op; serves from nothing until the
  :matrix-bridge engine registers an implementation.
- `var matrixTimelineSource` — service-locator hook for the engine. Deviation
  from the "no global mutable singletons" ux rule: this is engine wiring, not
  composable state, and ChatModel.kt is frozen in this wave. Documented here.
- `openMatrixChat(chat, chatModel)` — mirrors `openLoadedChat()`
  (ChatListNavLinkView.kt:289-295): `loadLatest` (falling back to the synthetic
  chat's own preview `chatItems`), then `chatItems.replaceAll`, `chatId.value`,
  `chatState.clear()` on Main. Selection: `chatModel.chatId.value = chat.id`
  drives ChatView exactly as for SimpleX.
- `paginateMatrixChat(chat, chatsCtx)` — prepends the older slice at index 0 on
  Main; returns whether anything was added.

Invariants: for a Matrix chat, no `ChatController.api*` call is reachable — see
diff list below for every gated call site.

## Upstream diffs (file:line on this branch, after edits)

| File | Change | Lines (+/-) | Reason |
|---|---|---|---|
| ChatListContent.kt:171-173 | `deletableChats` excludes Matrix chats | +3 | batch delete calls `controller.deleteChat` → core with synthetic id |
| ChatListNavLinkView.kt:79-82 | Matrix branch in `defaultClickAction` → `openMatrixChat` | +4 | row tap must not reach `apiLoadMessages`/`apiGetChat` |
| ChatListNavLinkView.kt:113-120 | Direct branch `dropdownMenuItems` = null for Matrix | +5 | ContactMenuItems actions are core calls |
| ChatListNavLinkView.kt:133-140 | Group branch `dropdownMenuItems` = null for Matrix | +5 | GroupMenuItems actions are core calls |
| ChatListNavLinkView.kt:752-754 | `markChatRead` early return | +3 | `apiChatRead`/`apiChatUnread` |
| ChatListNavLinkView.kt:787-789 | `markChatUnread` early return | +3 | `apiChatUnread` |
| ChatView.kt:206 | skip `apiContactInfo` for Matrix Direct | +1 (modified line) | core call |
| ChatView.kt:286-288 | `onSearchValueChanged` early return | +3 | search → `apiFindMessages` → core |
| ChatView.kt:300-305 | `composeView`: Matrix → no composer host (empty branch) | +6 | #139 read-only; hide, never dead input |
| ChatView.kt:531-536 | `loadMessages` callback → `paginateMatrixChat` for Matrix | +6 | pagination → `apiLoadMessages` → core |
| ChatView.kt:845-846 | `updateAvailableContent` early return | +2 | `apiGetChatContentTypes` |
| ChatView.kt:3163-3164 | `scrollToItem` search-jump early return | +2 | `apiLoadMessages` Around pagination |
| ChatView.kt:3501-3502 | `markUnreadChatAsRead` early return | +2 | `apiChatUnread` |
| ChatItemsLoader.kt | none | 0 | seam branches at call sites; loader file itself untouched |

Net upstream (non-`views/ux/matrix`) diff: ~43 added / 6 replaced lines across 3
files, each 1-6 lines, no logic moved. New files (not upstream diff):
`views/ux/matrix/ChatProtocolLocal.kt` (TEMP, deduped at integration),
`views/ux/matrix/MatrixChatItemsLoader.kt`, this spec.

## Not supported this wave (hidden, not faked)

- Sending / composing in Matrix chats (composer host hidden entirely).
- Row context menu for Matrix chats (menu hidden).
- In-chat message search and jump-to-item for Matrix chats (search input no-ops).
- Mark read/unread for Matrix chats (actions unreachable via hidden menu; batch
  toggle in selection mode no-ops for Matrix rows).

Each becomes real when the engine bridge implements `MatrixTimelineSource` and
the write-side seams (later waves).
