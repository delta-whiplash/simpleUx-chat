# Matrix co-protocol P1 bridge spec (#134, #135, #136)

This document is the authoritative spec for the `matrix-bridge` module produced
in P1. It follows the swarm contract: SimpleXAPI.kt is byte-frozen, Matrix
chats are synthetic values of the SimpleX display model, and ingestion happens
only via `ChatModel.chatsContext` public methods.

## 1. Account lifecycle states (#134)

```
            login(user, password)                restore() ok
  EMPTY ─────────────────────────▶ LOGGED_IN ◀───────────────┐
    ▲                                │  │                    │
    │  logout() + wipe               │  │  start()           │
    └────────────────────────────────┘  ▼                    │
              (any state)           SYNCING ── stop() ──▶ LOGGED_IN
                                    SYNCING ── restore() failed ──▶ EMPTY
```

- **EMPTY**: no persisted session, no client. `restore()` returns false.
- **LOGGED_IN**: a `Client` exists and a session has been persisted; sync
  service not running.
- **SYNCING**: `SyncService.start()` succeeded; room list and timelines are
  live. `stop()` returns to LOGGED_IN. `logout()` wipes session file + rust-sdk
  data/cache dirs and returns to EMPTY from any state.

### Session persistence format + location

Kept from the verified spike (P0): a Java properties file at
`<filesDir>/matrix/account.properties` with keys `userId`, `deviceId`,
`accessToken`, `refreshToken?`, `homeserverUrl`. The rust-sdk crypto/state
store lives under `<filesDir>/matrix/data` + `<cacheDir>/matrix/cache`
(passed to `ClientBuilder.sessionPaths`). Restore semantics:
`ClientBuilder().homeserverUrl(stored).sessionPaths(...).build()` then
`client.restoreSession(Session(...))`; any failure deletes nothing but
reports "no restorable session" (login required). Sliding sync version is
restored as `SlidingSyncVersion.NATIVE` (the only mode the pinned SDK 26.09.3
exposes for our benches).

Rationale vs. alternatives: the SDK has no exported session-store serializer
in 26.09.3 Kotlin bindings beyond `client.session()`/`restoreSession`, so the
properties file IS the clean mechanism that survives restart (proven in P0
across process deaths).

## 2. Id scheme (#135) — the `"mx|"` contract

Every Matrix-originated chat must be identifiable by `ChatInfo.id` starting
with `"mx|"`; there is deliberately no other distinction mechanism. Mapping:

```
matrixChatId(roomId) = "mx|" + roomId          e.g. "mx|!abc:example.org"
```

**Model-layer prerequisite (documented deviation from the file-cluster
rule, approved by the human):** in `:common`'s ChatModel.kt, `Contact.id` and
`GroupInfo.id` are *computed* (`"@$contactId"`, `"#$groupId"`) and both types
are final, so no `"mx|"` id can be constructed from `matrix-bridge`. The
minimal fix (4 lines, serialization-safe with default `null`, exact-diff
budget) adds an optional `syntheticId: String? = null` constructor parameter
to both types; the `id` getter returns `syntheticId ?: "<computed>"`. All
existing call sites (which never pass it) are byte-identical in behavior.

No-collision proof obligation (tested): SimpleX chat ids are exactly the
patterns `@N` (direct), `#N` (group), `*N` (note folder), `:N` (connection),
`?N` (invalid JSON) with decimal `N`. `"mx|..."` matches none of these by
first character alone; therefore a Matrix chat id can never equal a SimpleX
chat id. Tests assert both directions.

Item ids: `ChatItem.meta.itemId` is a `Long`. Matrix event ids
(`"$event:server"`) are hashed to a positive 63-bit Long with FNV-1a-64
(`stableItemId`). Determinism (same event id → same Long across restarts) and
distinctness are tested. Cross-chat collisions are harmless because item ids
are only compared inside one chat's scope (`chatItemBelongsToScope`).

## 3. Adapter mapping table (#135)

Package `chat.simplex.matrix.adapter` defines plain input DTOs — no
`org.matrix.rustcomponents.sdk` type may appear outside `chat.simplex.matrix`
(engine) files. **DTO boundary rule: SDK types never leak past package
`chat.simplex.matrix.adapter`.**

### DTOs

```kotlin
MatrixRoomSummary(roomId, displayName, isDm, isEncrypted, memberCount, topic)
MatrixMessageDto(roomId, eventId, senderId, senderName, body, timestampMs,
                 isOwn, kind: MatrixItemKind, isEdited)
MatrixItemKind = TEXT | IMAGE | VIDEO | AUDIO | FILE | REDACTED |
                 UNABLE_TO_DECRYPT | OTHER
```

### Room → ChatInfo

| Matrix input | Display model | Notes |
|---|---|---|
| DM room (`isDm`) | `ChatInfo.Direct(Contact(...))` | `Contact.syntheticId = "mx\|<roomId>"`; `contactStatus = Active`; `activeConn = null`; profile from display name; sending not enabled yet, so `ready = false` is honest (no fake affordances) |
| non-DM room | `ChatInfo.Group(GroupInfo(...), groupChatScope = null)` | `GroupInfo.syntheticId = "mx\|<roomId>"`; `membership.memberStatus = MemComplete`, `memberRole = Member` (own membership placeholder); `groupProfile` from room name/topic |

`roomToChat(summary, previewItem?)` wraps into
`Chat(remoteHostId = null, chatInfo, chatItems = previewItem?)`. `rhId = null`
(same as SimpleX local-host chats); identity separation is the `mx|` prefix.

### Message → ChatItem

| Matrix input | Display model |
|---|---|
| kind TEXT (isOwn) | `CIContent.SndMsgContent(MsgContent.MCText(body))`, `chatDir = CIDirection.DirectSnd` (DM) / `GroupSnd` (room), `itemStatus = CIStatus.SndSent(Complete)` |
| kind TEXT (!isOwn) | `CIContent.RcvMsgContent(MCText)`, `chatDir = DirectRcv` / `GroupRcv(groupMember)` where groupMember mirrors senderId/senderName, `itemStatus = CIStatus.RcvRead` |
| kinds IMAGE..FILE | currently → `null` (ignored) until attachment wiring lands; contract: adapters return `null` for anything they cannot faithfully render — never fabricate content |
| REDACTED, UNABLE_TO_DECRYPT, OTHER | → `null` (ignored; P2 will map to placeholder contents) |
| any kind with blank body | → `null` (ignored; preview/echo noise) |

Meta: `itemTs = createdAt = updatedAt = Instant.fromEpochMilliseconds(timestampMs)`,
`itemText = body`, `itemEdited = isEdited`, `sentViaProxy = null`,
`itemDeleted = null`, `itemTimed = null`, `itemLive = false`,
`userMention = false`, `deletable = editable = false` (P1 is read-only),
`file = null`, `quotedItem = null`, `reactions = []`, `formattedText = null`,
`mentions = null`.

Read status: initial batch and live items are all marked `RcvRead` —
`addChatItem` dereferences `currentUser.value!!` on `RcvNew` items and Matrix
must not NPE a SimpleX-less session; unread counters are P2 work.

### Preview item derivation

`Chat.chatItems` carries the chat-list preview (SimpleX receiver semantics:
`addChatItem` replaces `chat.chatItems` with the latest item). The adapter
supports `roomToChat(summary, previewItem)`; in the P1 bridge the initial
room batch is applied without a preview (rooms appear with no last-message
line until their timeline is opened, because timelines are only attached on
open). Live items then flow through `addChatItem` which performs the preview
update + reorder itself.

### P1 simplifications (deliberate)

- Sender display name falls back to the MXID (`@user:server`); the
  `ProfileDetails` lookup is P2 polish.
- Received items are all `RcvRead`; unread counters are P2.
- Timeline removal diffs (`Remove/Clear/Truncate`) are no-ops; P1 is
  live-append only.

## 4. Ingestion flow + threading (#136)

Copied from the SimpleX receiver (`SimpleXAPI.kt` `processReceivedMsg`,
read-only reference): the receiver loop runs on `Dispatchers.IO` and wraps
every `chatsContext` mutation in `withContext(Dispatchers.Main)`.

`MatrixBridge.start(context)`:

1. `MatrixAccount.restore(context)` — if false, return (login flow is P1.5 UI;
   `MatrixBridge.login(...)` exists for the debug activity).
2. `MatrixEngine.start()` — SyncService with offline mode.
3. `MatrixRoomsObserver.start()`: retry window for the room list (10 × 2 s,
   same as the verified spike), then for each room: convert to
   `MatrixRoomSummary`, batch-apply via `chatsContext.addChat` on Main
   (sequential, one `withContext` hop per room — each op is O(1) list insert;
   never a blocking loop on Main).
4. For each room, attach one timeline listener (only for opened rooms in P1:
   `MatrixBridge.openRoom(roomId)`). Timeline diffs are converted on the
   engine scope (`Dispatchers.Default`) into `MatrixMessageDto`s, then each
   item is ingested on Main via `chatsContext.addChatItem(null, chatInfo, item)`
   — matching `CR.NewChatItems` handling. `Append/PushBack/PushFront/Insert/
   Set/Reset` carry items; `Remove/Clear/Truncate` are no-ops in P1 (live
   append only).

`MatrixBridge.stop()` stops the sync service and cancels the engine scope
(no thread leaks; rust-sdk `TaskHandle`s and `Timeline`s closed).
`MatrixBridge.isOpen(roomId)` reports whether a timeline listener is attached.

Nothing calls `start()` from app code yet — wiring is the orchestrator's
integration step. No `:android` edits.

## 5. Sync start/stop policy

- Sync is started only via `MatrixEngine.start()` and is idempotent
  (second call logs and returns).
- Stop is best-effort: `svc.stop()`, close state observer handle, close
  service. Failures are logged, never thrown — shutdown must not be
  blockable by a dead network.
- The engine owns one `CoroutineScope(SupervisorJob() + Dispatchers.Default)`;
  `shutdown()` cancels it and closes SDK objects in order:
  timelines → listeners → sync service → client.

## 6. Test map (TDD)

All pure adapter functions have failing-first JUnit tests in
`src/test/kotlin/chat/simplex/matrix/adapter/`:

- `MatrixAdapterIdsTest` — prefix scheme, stability, round-trip roomId
  recovery, no-collision vs SimpleX id patterns (both directions),
  `stableItemId` determinism/distinctness/positivity.
- `MatrixAdapterRoomTest` — DM → Direct with plausible Contact fields;
  group → Group; `roomToChat` shape (rhId null, id prefix, preview item).
- `MatrixAdapterMessageTest` — text → MCText + correct CIDirection from
  isOwn (DM and group), status mapping, timestamp mapping, unsupported
  kinds → null, blank body → null, edited flag.
