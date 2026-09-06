package chat.simplex.common.views.ux.matrix

import chat.simplex.common.model.ChatInfo

// TEMP until integration merge: minimal local copy of the ChatProtocol helper that
// arrives from the badge/gating branch as views/ux/matrix/ChatProtocol.kt.
// The orchestrator dedupes at integration - do not grow this file.
enum class ChatProtocol { SimpleX, Matrix }

val ChatInfo.protocol: ChatProtocol
  get() = if (id.startsWith("mx|")) ChatProtocol.Matrix else ChatProtocol.SimpleX
