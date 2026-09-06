package chat.simplex.matrix.adapter

import chat.simplex.common.model.CIDirection
import chat.simplex.common.model.CIStatus
import chat.simplex.common.model.MsgContent
import org.junit.Assert.*
import org.junit.Test

/** Message -> ChatItem mapping (#135). */
class MatrixAdapterMessageTest {

    private val dmRoom = MatrixRoomSummary(roomId = "!dm1:example.org", displayName = "Alice", isDm = true, isEncrypted = true)
    private val groupRoom = MatrixRoomSummary(roomId = "!room1:example.org", displayName = "Team", isDm = false, isEncrypted = false)

    private fun textDto(isOwn: Boolean, body: String = "hello", roomId: String = "!dm1:example.org") = MatrixMessageDto(
        roomId = roomId,
        eventId = "\$evt-$isOwn-$body:example.org",
        senderId = if (isOwn) "@me:example.org" else "@alice:example.org",
        senderName = if (isOwn) "Me" else "Alice",
        body = body,
        timestampMs = 1_700_000_123_456,
        isOwn = isOwn,
        kind = MatrixItemKind.TEXT,
    )

    @Test
    fun `own dm text maps to MCText with DirectSnd and sent status`() {
        val item = MatrixAdapter.messageToChatItem(textDto(isOwn = true), dmRoom)!!
        val content = item.content
        assertTrue(content is chat.simplex.common.model.CIContent.SndMsgContent)
        assertEquals("hello", (content as chat.simplex.common.model.CIContent.SndMsgContent).msgContent.text)
        assertTrue((content.msgContent as MsgContent) is MsgContent.MCText)
        assertTrue(item.chatDir is CIDirection.DirectSnd)
        assertTrue(item.meta.itemStatus is CIStatus.SndSent)
    }

    @Test
    fun `received dm text maps to MCText with DirectRcv`() {
        val item = MatrixAdapter.messageToChatItem(textDto(isOwn = false), dmRoom)!!
        assertTrue(item.content is chat.simplex.common.model.CIContent.RcvMsgContent)
        assertTrue(item.chatDir is CIDirection.DirectRcv)
        assertEquals("hello", item.content.msgContent!!.text)
    }

    @Test
    fun `own group text maps to GroupSnd`() {
        val item = MatrixAdapter.messageToChatItem(textDto(isOwn = true, roomId = "!room1:example.org"), groupRoom)!!
        assertTrue(item.chatDir is CIDirection.GroupSnd)
    }

    @Test
    fun `received group text maps to GroupRcv carrying sender member`() {
        val item = MatrixAdapter.messageToChatItem(textDto(isOwn = false, roomId = "!room1:example.org"), groupRoom)!!
        val dir = item.chatDir
        assertTrue(dir is CIDirection.GroupRcv)
        assertEquals("@alice:example.org", (dir as CIDirection.GroupRcv).groupMember.memberId)
        assertEquals("Alice", dir.groupMember.localDisplayName)
    }

    @Test
    fun `timestamps map deterministically`() {
        val item = MatrixAdapter.messageToChatItem(textDto(isOwn = false), dmRoom)!!
        assertEquals(1_700_000_123_456, item.meta.itemTs.toEpochMilliseconds())
        assertEquals(item.meta.itemTs, item.meta.createdAt)
        assertEquals(item.meta.itemTs, item.meta.updatedAt)
    }

    @Test
    fun `item id is stable for the same event`() {
        val a = MatrixAdapter.messageToChatItem(textDto(isOwn = true), dmRoom)!!
        val b = MatrixAdapter.messageToChatItem(textDto(isOwn = true), dmRoom)!!
        assertEquals(a.id, b.id)
        assertTrue(a.id > 0)
    }

    @Test
    fun `item text equals body`() {
        val item = MatrixAdapter.messageToChatItem(textDto(isOwn = false, body = "bonjour"), dmRoom)!!
        assertEquals("bonjour", item.meta.itemText)
    }

    @Test
    fun `unsupported kinds map to null`() {
        for (kind in listOf(
            MatrixItemKind.IMAGE, MatrixItemKind.VIDEO, MatrixItemKind.AUDIO,
            MatrixItemKind.FILE, MatrixItemKind.REDACTED,
            MatrixItemKind.UNABLE_TO_DECRYPT, MatrixItemKind.OTHER,
        )) {
            val dto = textDto(isOwn = false).copy(kind = kind)
            assertNull("kind $kind should be ignored", MatrixAdapter.messageToChatItem(dto, dmRoom))
        }
    }

    @Test
    fun `blank body maps to null`() {
        assertNull(MatrixAdapter.messageToChatItem(textDto(isOwn = false, body = ""), dmRoom))
        assertNull(MatrixAdapter.messageToChatItem(textDto(isOwn = false, body = "   "), dmRoom))
    }

    @Test
    fun `edited flag maps to itemEdited`() {
        val item = MatrixAdapter.messageToChatItem(textDto(isOwn = true).copy(isEdited = true), dmRoom)!!
        assertTrue(item.meta.itemEdited)
        val item2 = MatrixAdapter.messageToChatItem(textDto(isOwn = true).copy(isEdited = false), dmRoom)!!
        assertFalse(item2.meta.itemEdited)
    }

    @Test
    fun `no file, quote or reactions on synthetic items`() {
        val item = MatrixAdapter.messageToChatItem(textDto(isOwn = false), dmRoom)!!
        assertNull(item.file)
        assertNull(item.quotedItem)
        assertTrue(item.reactions.isEmpty())
    }
}
