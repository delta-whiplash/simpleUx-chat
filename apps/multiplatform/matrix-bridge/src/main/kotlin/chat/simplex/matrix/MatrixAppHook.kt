package chat.simplex.matrix

import android.content.Context
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatItem
import chat.simplex.common.views.ux.matrix.MatrixTimelineSource
import chat.simplex.common.views.ux.matrix.matrixTimelineSource

/**
 * App-start wiring for the Matrix co-protocol (#128, P1 integration step).
 *
 * MainActivity invokes this via reflection guarded by runCatching: in release
 * builds the matrix-bridge module is absent (debugImplementation only) and the
 * call is a silent no-op, so release behavior is bit-identical.
 */
object MatrixAppHook {

    @JvmStatic
    fun onAppStart(context: Context) {
        matrixTimelineSource = BridgeTimelineSource
        // Starts restore + sync + ingestion only when a stored Matrix session exists.
        MatrixBridge.start(context)
    }
}

/**
 * P1 timeline source: serves the current ingested state from ChatModel; live
 * updates flow through ChatsContext via the bridge's listeners. History
 * back-pagination is deliberately empty in P1 (documented on #139) - the UI
 * treats it as "no more pages".
 */
object BridgeTimelineSource : MatrixTimelineSource {
    override suspend fun loadLatest(chatInfo: ChatInfo): List<ChatItem> =
        chat.simplex.common.platform.chatModel.getChat(chatInfo.id)?.chatItems ?: emptyList()

    override suspend fun paginateOlder(chatInfo: ChatInfo): List<ChatItem> = emptyList()
}
