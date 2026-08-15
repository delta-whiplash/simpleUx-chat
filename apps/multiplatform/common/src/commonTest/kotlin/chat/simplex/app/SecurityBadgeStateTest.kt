package chat.simplex.app

import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.Connection
import chat.simplex.common.model.Contact
import chat.simplex.common.views.ux.components.SecurityBadgeEncryption.POST_QUANTUM
import chat.simplex.common.views.ux.components.SecurityBadgeEncryption.STANDARD_E2EE
import chat.simplex.common.views.ux.components.SecurityBadgeEncryption.NOT_ENCRYPTED
import chat.simplex.common.views.ux.components.securityBadgeEncryption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecurityBadgeStateTest {

  private fun connection(pqSndEnabled: Boolean?, pqRcvEnabled: Boolean?): Connection =
    Connection.sampleData.copy(pqSndEnabled = pqSndEnabled, pqRcvEnabled = pqRcvEnabled)

  private fun directChat(conn: Connection?): Chat =
    Chat(remoteHostId = null, chatInfo = ChatInfo.Direct(Contact.sampleData.copy(activeConn = conn)), chatItems = emptyList())

  @Test
  fun directChatWithFullyNegotiatedPQConnectionIsPostQuantum() {
    assertEquals(POST_QUANTUM, directChat(connection(pqSndEnabled = true, pqRcvEnabled = true)).securityBadgeEncryption())
  }

  @Test
  fun directChatWithoutPQConnectionIsStandardE2EE() {
    assertEquals(STANDARD_E2EE, directChat(connection(pqSndEnabled = null, pqRcvEnabled = null)).securityBadgeEncryption())
    assertEquals(STANDARD_E2EE, directChat(connection(pqSndEnabled = false, pqRcvEnabled = false)).securityBadgeEncryption())
  }

  @Test
  fun directChatRequiresBothPQDirections() {
    assertEquals(STANDARD_E2EE, directChat(connection(pqSndEnabled = true, pqRcvEnabled = null)).securityBadgeEncryption())
    assertEquals(STANDARD_E2EE, directChat(connection(pqSndEnabled = null, pqRcvEnabled = true)).securityBadgeEncryption())
  }

  @Test
  fun directChatWithoutActiveConnectionIsNotEncrypted() {
    assertEquals(NOT_ENCRYPTED, directChat(conn = null).securityBadgeEncryption())
  }

  @Test
  fun absentChatHidesBadge() {
    val chat: Chat? = null
    assertNull(chat.securityBadgeEncryption())
  }

  @Test
  fun groupChatIsStandardE2EE() {
    val group = Chat(remoteHostId = null, chatInfo = ChatInfo.Group.sampleData, chatItems = emptyList())
    assertEquals(STANDARD_E2EE, group.securityBadgeEncryption())
  }

  @Test
  fun pendingContactRequestHidesBadge() {
    val request = Chat(remoteHostId = null, chatInfo = ChatInfo.ContactRequest.sampleData, chatItems = emptyList())
    assertNull(request.securityBadgeEncryption())
  }
}
