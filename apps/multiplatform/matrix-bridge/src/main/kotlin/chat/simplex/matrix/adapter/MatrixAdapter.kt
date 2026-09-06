package chat.simplex.matrix.adapter

import chat.simplex.common.model.*
import kotlinx.datetime.Instant

/**
 * Plain input DTOs for the Matrix -> SimpleX display-model adapter (#135).
 *
 * DTO boundary rule (SPECS.md §3): these types are the ONLY shapes that cross
 * from the engine layer into the adapter. No `org.matrix.rustcomponents.sdk`
 * type may be referenced in this package.
 */

/** Everything the adapter needs to know about a room to build a ChatInfo. */
data class MatrixRoomSummary(
    val roomId: String,
    val displayName: String,
    val isDm: Boolean,
    val isEncrypted: Boolean,
    val memberCount: Long? = null,
    val topic: String? = null,
)

/** The subset of Matrix timeline item kinds the adapter can reason about. */
enum class MatrixItemKind { TEXT, IMAGE, VIDEO, AUDIO, FILE, REDACTED, UNABLE_TO_DECRYPT, OTHER }

/** One timeline event, pre-extracted from the SDK by the engine layer. */
data class MatrixMessageDto(
    val roomId: String,
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val body: String,
    val timestampMs: Long,
    val isOwn: Boolean,
    val kind: MatrixItemKind,
    val isEdited: Boolean = false,
)

/**
 * Pure, side-effect-free mapping of Matrix DTOs onto synthetic values of the
 * SimpleX display model (Chat/ChatInfo/ChatItem). All functions are
 * deterministic: identical inputs produce identical outputs (same ids, same
 * timestamps), so chat/item identity survives restarts.
 */
object MatrixAdapter {

    const val CHAT_ID_PREFIX = "mx|"

    /** "mx|<roomId>" - the single documented cross-protocol distinction mechanism. */
    fun matrixChatId(roomId: String): String = CHAT_ID_PREFIX + roomId

    /** Inverse of [matrixChatId]; null when the id is not a Matrix chat id. */
    fun roomIdFromChatId(chatId: String): String? =
        if (chatId.startsWith(CHAT_ID_PREFIX)) chatId.removePrefix(CHAT_ID_PREFIX) else null

    /**
     * Deterministic positive 63-bit id for a Matrix event id (FNV-1a-64).
     * Needed because ChatItem.meta.itemId is a Long. Stability across
     * restarts is what makes preview updates and dedupe work.
     */
    fun stableItemId(eventId: String): Long {
        var hash = 0xcbf29ce484222325UL
        for (b in eventId.encodeToByteArray()) {
            hash = hash xor b.toULong()
            hash *= 0x100000001b3UL
        }
        return (hash and Long.MAX_VALUE.toULong()).toLong()
    }

    fun roomToChatInfo(summary: MatrixRoomSummary): ChatInfo =
        if (summary.isDm) {
            ChatInfo.Direct(dmContact(summary))
        } else {
            ChatInfo.Group(groupInfo(summary), groupChatScope = null)
        }

    fun roomToChat(summary: MatrixRoomSummary, previewItem: ChatItem? = null): Chat =
        Chat(
            remoteHostId = null,
            chatInfo = roomToChatInfo(summary),
            chatItems = if (previewItem != null) arrayListOf(previewItem) else arrayListOf(),
        )

    /**
     * Message -> ChatItem. The room summary supplies the Direct-vs-Group
     * context the raw DTO cannot know. Returns null for anything the adapter
     * cannot faithfully render (unsupported kinds, blank bodies): the
     * ingestion layer must ignore nulls, never fabricate content (SPECS.md §3).
     */
    fun messageToChatItem(dto: MatrixMessageDto, room: MatrixRoomSummary): ChatItem? {
        if (dto.kind != MatrixItemKind.TEXT) return null
        val body = dto.body.trim()
        if (body.isEmpty()) return null
        val ts = Instant.fromEpochMilliseconds(dto.timestampMs)
        return if (room.isDm) directItem(dto, body, ts) else groupItem(dto, body, ts)
    }

    private fun directItem(dto: MatrixMessageDto, body: String, ts: Instant): ChatItem = ChatItem(
        chatDir = if (dto.isOwn) CIDirection.DirectSnd() else CIDirection.DirectRcv(),
        meta = meta(dto, body, ts, showGroupAsSender = false),
        reactions = emptyList(),
        content = if (dto.isOwn) {
            CIContent.SndMsgContent(MsgContent.MCText(body))
        } else {
            CIContent.RcvMsgContent(MsgContent.MCText(body))
        },
    )

    private fun groupItem(dto: MatrixMessageDto, body: String, ts: Instant): ChatItem = ChatItem(
        chatDir = if (dto.isOwn) CIDirection.GroupSnd() else CIDirection.GroupRcv(senderMember(dto)),
        meta = meta(dto, body, ts, showGroupAsSender = true),
        reactions = emptyList(),
        content = if (dto.isOwn) {
            CIContent.SndMsgContent(MsgContent.MCText(body))
        } else {
            CIContent.RcvMsgContent(MsgContent.MCText(body))
        },
    )

    private fun meta(dto: MatrixMessageDto, body: String, ts: Instant, showGroupAsSender: Boolean): CIMeta = CIMeta(
        itemId = stableItemId(dto.eventId),
        itemTs = ts,
        itemText = body,
        // RcvNew would make chatsContext.addChatItem dereference the SimpleX
        // current user; read-only P1 marks everything read (SPECS.md §3).
        itemStatus = if (dto.isOwn) CIStatus.SndSent(SndCIStatusProgress.Complete) else CIStatus.RcvRead(),
        sentViaProxy = null,
        createdAt = ts,
        updatedAt = ts,
        itemForwarded = null,
        itemDeleted = null,
        itemEdited = dto.isEdited,
        itemTimed = null,
        itemLive = false,
        userMention = false,
        deletable = false,
        editable = false,
        showGroupAsSender = showGroupAsSender,
    )

    private fun dmContact(summary: MatrixRoomSummary): Contact = Contact(
        contactId = stableItemId(summary.roomId),
        localDisplayName = summary.displayName,
        profile = LocalProfile(
            profileId = stableItemId(summary.roomId),
            displayName = summary.displayName,
            fullName = summary.displayName,
            shortDescr = summary.topic,
            localAlias = "",
        ),
        activeConn = null,
        contactUsed = false,
        contactStatus = ContactStatus.Active,
        chatSettings = ChatSettings(enableNtfs = MsgFilter.All, sendRcpts = null, favorite = false),
        userPreferences = ChatPreferences.sampleData,
        mergedPreferences = ContactUserPreferences.sampleData,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST,
        chatTs = null,
        preparedContact = null,
        contactRequestId = null,
        contactGrpInvSent = false,
        chatDeleted = false,
        chatTags = emptyList(),
        chatItemTTL = null,
        syntheticId = matrixChatId(summary.roomId),
    )

    private fun groupInfo(summary: MatrixRoomSummary): GroupInfo = GroupInfo(
        groupId = stableItemId(summary.roomId),
        useRelays = false,
        localDisplayName = summary.displayName,
        groupProfile = GroupProfile(
            displayName = summary.displayName,
            fullName = summary.displayName,
            shortDescr = summary.topic?.trim()?.takeIf { it.isNotEmpty() },
        ),
        fullGroupPreferences = FullGroupPreferences.sampleData,
        membership = GroupMember.sampleData.copy(
            groupId = stableItemId(summary.roomId),
            memberRole = GroupMemberRole.Member,
            memberStatus = GroupMemberStatus.MemComplete,
            localDisplayName = summary.displayName,
            memberProfile = LocalProfile(
                profileId = stableItemId(summary.roomId),
                displayName = summary.displayName,
                fullName = summary.displayName,
                shortDescr = null,
                localAlias = "",
            ),
            activeConn = null,
        ),
        chatSettings = ChatSettings(enableNtfs = MsgFilter.All, sendRcpts = null, favorite = false),
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST,
        chatTs = null,
        preparedGroup = null,
        groupSummary = GroupSummary(currentMembers = summary.memberCount ?: 0),
        membersRequireAttention = 0,
        chatTags = emptyList(),
        localAlias = "",
        chatItemTTL = null,
        syntheticId = matrixChatId(summary.roomId),
    )

    /** Sender-mirroring member for CIDirection.GroupRcv. */
    fun senderMember(dto: MatrixMessageDto): GroupMember = GroupMember.sampleData.copy(
        memberId = dto.senderId,
        localDisplayName = dto.senderName,
        memberProfile = LocalProfile(
            profileId = stableItemId(dto.senderId),
            displayName = dto.senderName,
            fullName = dto.senderName,
            shortDescr = null,
            localAlias = "",
        ),
        activeConn = null,
    )
}
