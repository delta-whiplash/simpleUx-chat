package chat.simplex.common.views.ux

import SectionItemView
import itemHPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ConnStatus
import chat.simplex.common.model.getTimestampText
import chat.simplex.common.platform.InvitationLinksPrefs
import chat.simplex.common.platform.chatModel
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.ui.theme.AmberGold
import chat.simplex.common.ui.theme.GlassBorderDark
import chat.simplex.common.ui.theme.GlassBorderLight
import chat.simplex.common.ui.theme.HighOrLowlight
import chat.simplex.common.ui.theme.Slate100
import chat.simplex.common.ui.theme.Slate200
import chat.simplex.common.ui.theme.Slate800
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.AlertManager
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.common.views.helpers.withBGApi
import chat.simplex.common.views.newchat.ContactConnectionInfoView
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * FB-12/13: one-time invitations the user CREATED render as placeholder chats that
 * pile up in the chat list. They are managed here instead: hidden from the chat list
 * (see ChatListContent) and listed in this Contacts-tab section, Discord-invites style.
 * Mirrors the upstream model's own notion of a created invitation
 * (PendingContactConnection.initiated = status initiated AND not via contact URI);
 * presentation only, nothing touches the frozen model layer.
 *
 * #112: newest first, under a collapsible counted header, so accumulated invitations
 * read as a managed list instead of a stack of identical rows; the subtitle carries
 * the status and the creation date; aliases are edited from the connection sheet
 * (LocalAliasEditor). The INVITE tab re-adopts the single unshared invitation
 * instead of creating a new link per open (see NewChatView.adoptUnsharedInvitation).
 */
fun isCreatedInvitationChat(chat: Chat): Boolean =
  chat.chatInfo is ChatInfo.ContactConnection && chat.chatInfo.contactConnection.initiated

@Composable
fun InvitationLinksSection() {
  val invites = chatModel.chats.value
    .filter { isCreatedInvitationChat(it) }
    .sortedByDescending { (it.chatInfo as ChatInfo.ContactConnection).contactConnection.createdAt }
  if (invites.isEmpty()) return

  LaunchedEffect(invites.joinToString(",") { it.id }) {
    InvitationLinksPrefs.prune(invites.map { it.id }.toSet())
  }

  val expanded = rememberSaveable { mutableStateOf(true) }
  val chevron by animateFloatAsState(if (expanded.value) 180f else 0f, tween(durationMillis = 200))

  Column {
    Row(
      Modifier
        .fillMaxWidth()
        .clickable { expanded.value = !expanded.value }
        .padding(start = DEFAULT_PADDING, end = DEFAULT_PADDING, top = 4.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        stringResource(MR.strings.invites_section_title, invites.size),
        color = HighOrLowlight,
        style = MaterialTheme.typography.body2,
        fontSize = 12.sp
      )
      Spacer(Modifier.weight(1f))
      Icon(
        Icons.Filled.KeyboardArrowDown, contentDescription = null,
        tint = HighOrLowlight, modifier = Modifier.size(18.dp).rotate(chevron)
      )
    }
    if (expanded.value) {
      invites.forEachIndexed { index, chat ->
        val conn = (chat.chatInfo as ChatInfo.ContactConnection).contactConnection
        // #112: hairline inset separator - SectionDividerSpaced (section-level
        // spacing: DEFAULT_PADDING + 18dp on each side) read as a hole between rows.
        if (index > 0) {
          Divider(
            Modifier.padding(
              start = itemHPadding + 36.dp + 12.dp,
              end = itemHPadding
            ),
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.08f)
          )
        }
        SectionItemView(click = {
          ModalManager.start.showModalCloseable(cardScreen = true) { close ->
            ContactConnectionInfoView(chatModel, chat.remoteHostId, conn.connLinkInv, conn, focusAlias = false, close = close)
          }
        }) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            // #112: same avatar treatment as NewChatButton rows in this sheet
            // (36dp tinted circle + hairline rim + gold glyph) instead of a
            // bespoke secondary-alpha bubble.
            val isDark = isInDarkTheme()
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Slate800 else Slate100)
                .border(
                  BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight.copy(alpha = 0.35f)),
                  CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Filled.Share, contentDescription = null,
                tint = AmberGold, modifier = Modifier.size(18.dp)
              )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
              Text(
                conn.localAlias.ifEmpty { generalGetString(MR.strings.display_name_invited_to_connect) },
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium)
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  stringResource(invitationStatusRes(conn.pccConnStatus)),
                  style = MaterialTheme.typography.caption,
                  color = invitationStatusColor(conn.pccConnStatus)
                )
                Text(
                  " · " + getTimestampText(conn.createdAt),
                  style = MaterialTheme.typography.caption,
                  color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f)
                )
              }
            }
            if (conn.pccConnStatus is ConnStatus.New) {
              IconButton(onClick = {
                AlertManager.shared.showAlertDialog(
                  title = generalGetString(MR.strings.invites_delete_confirm_title),
                  text = generalGetString(MR.strings.invites_delete_confirm_text),
                  confirmText = generalGetString(MR.strings.delete_verb),
                  dismissText = generalGetString(MR.strings.cancel_verb),
                  destructive = true,
                  onConfirm = {
                    withBGApi {
                      chatModel.controller.deleteChat(
                        Chat(remoteHostId = chat.remoteHostId, chatInfo = ChatInfo.ContactConnection(conn), chatItems = listOf())
                      )
                    }
                  }
                )
              }) {
                Icon(
                  Icons.Filled.Delete, contentDescription = generalGetString(MR.strings.delete_verb),
                  tint = MaterialTheme.colors.error
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun invitationStatusRes(status: ConnStatus) = when (status) {
  is ConnStatus.New, is ConnStatus.Prepared -> MR.strings.invites_status_available
  is ConnStatus.Joined, is ConnStatus.Requested, is ConnStatus.Accepted, is ConnStatus.SndReady, is ConnStatus.Ready -> MR.strings.invites_status_connecting
  else -> MR.strings.invites_status_expired
}

@Composable
private fun invitationStatusColor(status: ConnStatus) = when (status) {
  is ConnStatus.New, is ConnStatus.Prepared -> MaterialTheme.colors.secondary
  is ConnStatus.Joined, is ConnStatus.Requested, is ConnStatus.Accepted, is ConnStatus.SndReady, is ConnStatus.Ready -> MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
  else -> MaterialTheme.colors.error
}
