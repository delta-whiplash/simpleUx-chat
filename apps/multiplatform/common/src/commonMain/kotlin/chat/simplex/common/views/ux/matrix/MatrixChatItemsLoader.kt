package chat.simplex.common.views.ux.matrix

import chat.simplex.common.model.*
import chat.simplex.common.platform.chatModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * #139 loader seam for Matrix conversations. Matrix chats are synthetic Chat values
 * (ChatInfo.id starts with "mx|") living in the regular ChatModel state; the SimpleX
 * core (ChatController.api*) must NEVER be called for them. This interface is the
 * single place the :matrix-bridge engine will implement at integration to serve a
 * decrypted Matrix timeline; until then [EmptyMatrixTimelineSource] serves nothing
 * and Matrix conversations open as an empty read-only timeline.
 *
 * Coordinates with spec: views/ux/matrix/SPECS-LIST.md
 */
interface MatrixTimelineSource {
  /** Latest timeline slice for the chat, oldest-first like core-loaded chat items. */
  suspend fun loadLatest(chatInfo: ChatInfo): List<ChatItem>

  /** Next older slice (empty list = timeline exhausted / nothing more to load). */
  suspend fun paginateOlder(chatInfo: ChatInfo): List<ChatItem>
}

/** Default no-op source: serves from nothing until the engine bridge implements the interface. */
object EmptyMatrixTimelineSource : MatrixTimelineSource {
  override suspend fun loadLatest(chatInfo: ChatInfo): List<ChatItem> = emptyList()
  override suspend fun paginateOlder(chatInfo: ChatInfo): List<ChatItem> = emptyList()
}

/**
 * Service-locator hook for the engine bridge. This is wiring, not UI state (ChatModel
 * itself cannot be edited in this wave), so it stays a plain top-level var; the engine
 * registers its source once at startup. Deviation from the "no global mutable
 * singletons" ux rule is documented in SPECS-LIST.md.
 */
var matrixTimelineSource: MatrixTimelineSource = EmptyMatrixTimelineSource

/**
 * Opens a Matrix conversation (row tap in the unified chat list). Mirrors openLoadedChat()
 * in ChatListNavLinkView.kt: swap items into the primary context, set chatId, clear state -
 * but the items come from the Matrix seam (falling back to the synthetic chat's own
 * preview chatItems carried in ChatModel), never from ChatController.apiGetChat.
 */
suspend fun openMatrixChat(chat: Chat, chatModel: ChatModel) {
  val loaded = matrixTimelineSource.loadLatest(chat.chatInfo)
  val items = if (loaded.isNotEmpty()) loaded else chat.chatItems
  withContext(Dispatchers.Main) {
    chatModel.chatsContext.chatItems.replaceAll(items)
    chatModel.chatId.value = chat.chatInfo.id
    chatModel.chatsContext.chatState.clear()
  }
}

/**
 * Prepends an older slice into the open Matrix timeline (LazyColumn scrolled to the
 * top). Returns true when items were added, so ChatView's pagination caller can keep
 * its "loading more" semantics. No splits/unread bookkeeping: Matrix items are read-only
 * this wave and carry no unread split state.
 */
suspend fun paginateMatrixChat(chat: Chat, chatsCtx: ChatModel.ChatsContext): Boolean {
  val older = matrixTimelineSource.paginateOlder(chat.chatInfo)
  if (older.isEmpty()) return false
  withContext(Dispatchers.Main) {
    chatsCtx.chatItems.value.addAll(0, older)
  }
  return true
}
