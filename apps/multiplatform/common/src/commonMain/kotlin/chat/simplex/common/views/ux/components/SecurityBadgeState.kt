package chat.simplex.common.views.ux.components

import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo

/** Encryption state rendered by [SecurityBadge], derived from the chat's real connection state. */
enum class SecurityBadgeEncryption { POST_QUANTUM, STANDARD_E2EE, NOT_ENCRYPTED }

/**
 * Derives the encryption state displayed by [SecurityBadge] from the chat's actual connection state.
 *
 * - Direct chats: post-quantum when the connection negotiated PQ in both directions
 *   (`connPQEnabled`), standard E2EE when a connection is established without PQ,
 *   not encrypted when there is no active connection.
 * - Groups: standard E2EE (every member connection is E2EE by protocol).
 * - Returns `null` when there is nothing verifiable to display (no chat, pending contact
 *   requests/connections, local notes) — the badge is hidden rather than guessing.
 */
fun Chat?.securityBadgeEncryption(): SecurityBadgeEncryption? =
  when (val info = this?.chatInfo) {
    is ChatInfo.Direct -> when {
      info.contact.activeConn?.connPQEnabled == true -> SecurityBadgeEncryption.POST_QUANTUM
      info.contact.activeConn != null -> SecurityBadgeEncryption.STANDARD_E2EE
      else -> SecurityBadgeEncryption.NOT_ENCRYPTED
    }
    is ChatInfo.Group -> SecurityBadgeEncryption.STANDARD_E2EE
    else -> null
  }
