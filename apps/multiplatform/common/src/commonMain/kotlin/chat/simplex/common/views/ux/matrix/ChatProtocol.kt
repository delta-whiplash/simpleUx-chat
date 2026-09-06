package chat.simplex.common.views.ux.matrix

import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo

/**
 * The transport protocol a chat runs over. SimpleX chats are native chats backed by the
 * SimpleX core; Matrix chats are synthetic chats bridged from the Matrix co-protocol
 * (frontend-only, see SPECS.md in this package).
 *
 * The ONLY way to distinguish them is the chat id: Matrix chat ids carry the
 * [MATRIX_CHAT_ID_PREFIX] prefix (e.g. "mx|!room:server"), while SimpleX ids look
 * like "<@1" (direct contacts) or numeric group ids. Behavior is pinned by ChatProtocolTest.
 */
enum class ChatProtocol { SimpleX, Matrix }

/** Id prefix marking a chat as a Matrix (co-protocol) chat. */
const val MATRIX_CHAT_ID_PREFIX = "mx|"

/** Pure classifier, usable without constructing model objects (and unit-testable on the JVM). */
fun chatProtocolFromId(id: String): ChatProtocol =
  if (id.startsWith(MATRIX_CHAT_ID_PREFIX)) ChatProtocol.Matrix else ChatProtocol.SimpleX

val ChatInfo.protocol: ChatProtocol get() = chatProtocolFromId(id)

val Chat.protocol: ChatProtocol get() = chatInfo.protocol
