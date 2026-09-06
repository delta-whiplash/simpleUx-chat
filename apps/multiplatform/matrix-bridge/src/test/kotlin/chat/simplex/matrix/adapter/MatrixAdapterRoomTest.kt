package chat.simplex.matrix.adapter

import chat.simplex.common.model.ChatInfo
import org.junit.Assert.*
import org.junit.Test

/** Room -> ChatInfo / Chat mapping (#135). */
class MatrixAdapterRoomTest {

    private val dm = MatrixRoomSummary(
        roomId = "!dm1:example.org",
        displayName = "Alice",
        isDm = true,
        isEncrypted = true,
    )

    private val room = MatrixRoomSummary(
        roomId = "!room1:example.org",
        displayName = "Team",
        isDm = false,
        isEncrypted = false,
        memberCount = 12,
        topic = " coordination ",
    )

    @Test
    fun `dm maps to ChatInfo Direct with plausible contact fields`() {
        val info = MatrixAdapter.roomToChatInfo(dm)
        assertTrue(info is ChatInfo.Direct)
        val contact = (info as ChatInfo.Direct).contact
        assertEquals("mx|!dm1:example.org", contact.id)
        assertEquals("Alice", contact.localDisplayName)
        assertEquals("Alice", contact.displayName)
        assertTrue(contact.active)
        assertFalse(contact.chatDeleted)
        assertFalse(contact.ready) // sending not wired yet: honest non-ready
        assertNotNull(contact.profile)
        assertEquals("Alice", contact.profile.displayName)
    }

    @Test
    fun `non-dm maps to ChatInfo Group`() {
        val info = MatrixAdapter.roomToChatInfo(room)
        assertTrue(info is ChatInfo.Group)
        val group = (info as ChatInfo.Group).groupInfo
        assertEquals("mx|!room1:example.org", group.id)
        assertEquals("Team", group.localDisplayName)
        assertEquals("Team", group.groupProfile.displayName)
        assertTrue(group.membership.memberActive)
        assertNull((info as ChatInfo.Group).groupChatScope)
    }

    @Test
    fun `roomToChat wraps with null remote host and prefixed id`() {
        val chat = MatrixAdapter.roomToChat(dm)
        assertNull(chat.remoteHostId)
        assertEquals("mx|!dm1:example.org", chat.id)
        assertTrue(chat.chatItems.isEmpty())
        assertEquals(0, chat.chatStats.unreadCount)
    }

    @Test
    fun `roomToChat carries preview item when provided`() {
        val item = MatrixAdapter.messageToChatItem(
            MatrixMessageDto(
                roomId = "!dm1:example.org",
                eventId = "\$e1:example.org",
                senderId = "@alice:example.org",
                senderName = "Alice",
                body = "last message",
                timestampMs = 1_700_000_000_000,
                isOwn = false,
                kind = MatrixItemKind.TEXT,
            ),
            dm
        )!!
        val chat = MatrixAdapter.roomToChat(dm, previewItem = item)
        assertEquals(listOf(item), chat.chatItems)
    }

    @Test
    fun `chat info types round trip via id prefix`() {
        for (summary in listOf(dm, room)) {
            val info = MatrixAdapter.roomToChatInfo(summary)
            assertTrue(info.id.startsWith("mx|"))
            assertEquals(summary.roomId, MatrixAdapter.roomIdFromChatId(info.id))
        }
    }
}
