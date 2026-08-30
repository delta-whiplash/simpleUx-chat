package chat.simplex.common.views.ux

import SectionDividerSpaced
import SectionItemView
import SectionView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ConnStatus
import chat.simplex.common.platform.chatModel
import chat.simplex.common.ui.theme.DEFAULT_PADDING
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
 * (see ChatListContent) and listed in this Settings section, Discord-invites style.
 * Mirrors the upstream model's own notion of a created invitation
 * (PendingContactConnection.initiated = status initiated AND not via contact URI);
 * presentation only, nothing touches the frozen model layer.
 */
fun isCreatedInvitationChat(chat: Chat): Boolean =
  chat.chatInfo is ChatInfo.ContactConnection && chat.chatInfo.contactConnection.initiated

@Composable
fun InvitationLinksSection() {
  val invites = chatModel.chats.value.filter { isCreatedInvitationChat(it) }
  if (invites.isEmpty()) return

  SectionView(stringResource(MR.strings.invites_section_title)) {
    invites.forEachIndexed { index, chat ->
      val conn = (chat.chatInfo as ChatInfo.ContactConnection).contactConnection
      if (index > 0) SectionDividerSpaced()
      SectionItemView(click = {
        ModalManager.start.showModalCloseable(cardScreen = true) { close ->
          ContactConnectionInfoView(chatModel, chat.remoteHostId, conn.connLinkInv, conn, focusAlias = false, close = close)
        }
      }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colors.secondary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Filled.Share, contentDescription = null,
              tint = MaterialTheme.colors.secondary, modifier = Modifier.size(18.dp)
            )
          }
          Column(Modifier.padding(horizontal = DEFAULT_PADDING).weight(1f)) {
            Text(
              conn.localAlias.ifEmpty { generalGetString(MR.strings.display_name_invited_to_connect) },
              maxLines = 1, overflow = TextOverflow.Ellipsis,
              fontWeight = FontWeight.Bold
            )
            Text(
              stringResource(invitationStatusRes(conn.pccConnStatus)),
              style = MaterialTheme.typography.caption,
              color = invitationStatusColor(conn.pccConnStatus)
            )
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
