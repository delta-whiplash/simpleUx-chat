package chat.simplex.common.views.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.chat.item.*
import chat.simplex.common.model.*
import chat.simplex.common.platform.getLoadedFilePath
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import kotlinx.datetime.Clock

@Composable
fun ContextItemView(
  contextItems: List<ChatItem>,
  contextIcon: Painter,
  showSender: Boolean = true,
  chatInfo: ChatInfo,
  contextIconColor: Color? = null,
  cancelContextItem: () -> Unit,
) {
  val isDark = isInDarkTheme()

  @Composable
  fun MessageText(contextItem: ChatItem, attachment: ImageResource?, lines: Int, prefix: AnnotatedString? = null, stripLink: String? = null) {
    val inlineContent: Pair<AnnotatedString.Builder.() -> Unit, Map<String, InlineTextContent>>? = if (attachment != null) {
      remember(contextItem.id) {
        val inlineContentBuilder: AnnotatedString.Builder.() -> Unit = {
          appendInlineContent(id = "attachmentIcon")
          append(" ")
        }
        val inlineContent = mapOf(
          "attachmentIcon" to InlineTextContent(
            Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter)
          ) {
            Icon(painterResource(attachment), null, tint = if (isDark) Color(0xFFE2B755) else Color(0xFFD97706))
          }
        )
        inlineContentBuilder to inlineContent
      }
    } else null
    MarkdownText(
      contextItem.text, contextItem.formattedText,
      sender = null,
      toggleSecrets = false,
      maxLines = lines,
      inlineContent = inlineContent,
      linkMode = SimplexLinkMode.DESCRIPTION,
      modifier = Modifier.fillMaxWidth(),
      mentions = contextItem.mentions,
      userMemberId = when {
        chatInfo is ChatInfo.Group -> chatInfo.groupInfo.membership.memberId
        else -> null
      },
      prefix = prefix,
      stripLink = stripLink,
    )
  }

  fun attachment(contextItem: ChatItem): ImageResource? {
    val fileIsLoaded = getLoadedFilePath(contextItem.file) != null

    val mc = contextItem.content.msgContent
    return when (mc) {
      is MsgContent.MCFile -> if (fileIsLoaded) MR.images.ic_draft_filled else null
      is MsgContent.MCImage -> MR.images.ic_image
      is MsgContent.MCVoice ->  if (fileIsLoaded) MR.images.ic_play_arrow_filled else null
      is MsgContent.MCChat -> mc.chatLink.smallIconRes
      else -> null
    }
  }

  @Composable
  fun ContextMsgPreview(contextItem: ChatItem, lines: Int) {
    val mc = contextItem.content.msgContent
    if (mc is MsgContent.MCChat) {
      val hasText = contextItem.text != mc.chatLink.connLinkStr
      val prefix = buildAnnotatedString { append(mc.chatLink.displayName + if (hasText) " - " else "") }
      MessageText(contextItem, remember(contextItem.id) { mc.chatLink.smallIconRes }, lines, prefix = prefix, stripLink = mc.chatLink.connLinkStr)
    } else {
      MessageText(contextItem, remember(contextItem.id) { attachment(contextItem) }, lines)
    }
  }

  val shape = RoundedCornerShape(16.dp)
  val effectiveIconColor = contextIconColor ?: (if (isDark) Color(0xFFE2B755) else Color(0xFFD97706))

  Row(
    Modifier
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .clip(shape)
      .background(
        if (isDark) Brush.verticalGradient(listOf(Color(0xFF1E2533), Color(0xFF131720)))
        else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)))
      )
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          if (isDark) listOf(Color(0x38FFFFFF), Color(0x0EFFFFFF))
          else listOf(Color(0x250F172A), Color(0x0C0F172A))
        ),
        shape = shape
      )
      .padding(start = 4.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left Accent Bar with rounded caps
    Box(
      modifier = Modifier
        .padding(vertical = 8.dp, horizontal = 6.dp)
        .width(3.5.dp)
        .height(36.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(
          Brush.verticalGradient(
            if (isDark) listOf(Color(0xFFE2B755), Color(0xFFD97706))
            else listOf(Color(0xFFD97706), Color(0xFFB45309))
          )
        )
    )

    // Context Icon inside subtle disc
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(if (isDark) Color(0x22E2B755) else Color(0x18D97706)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        contextIcon,
        modifier = Modifier.size(15.dp),
        contentDescription = stringResource(MR.strings.icon_descr_context),
        tint = effectiveIconColor,
      )
    }

    Spacer(Modifier.width(8.dp))

    Row(
      Modifier
        .padding(vertical = 8.dp)
        .fillMaxWidth()
        .weight(1F),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (contextItems.count() == 1) {
        val contextItem = contextItems[0]
        val sender = contextItem.memberDisplayName

        if (showSender && sender != null) {
          Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              sender,
              style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFE2B755) else Color(0xFFD97706)
              )
            )
            ContextMsgPreview(contextItem, lines = 1)
          }
        } else {
          ContextMsgPreview(contextItem, lines = 2)
        }
      } else if (contextItems.isNotEmpty()) {
        Text(
          String.format(generalGetString(if (chatInfo.chatType == ChatType.Local) MR.strings.compose_save_messages_n else MR.strings.compose_forward_messages_n), contextItems.count()),
          fontStyle = FontStyle.Italic,
          color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
          fontSize = 13.sp
        )
      }
    }

    // Polished Close Button
    IconButton(
      onClick = cancelContextItem,
      modifier = Modifier.size(32.dp)
    ) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(if (isDark) Color(0x2A94A3B8) else Color(0x1F64748B)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painterResource(MR.images.ic_close),
          contentDescription = stringResource(MR.strings.cancel_verb),
          tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
          modifier = Modifier.size(13.dp)
        )
      }
    }
  }
}

@Preview
@Composable
fun PreviewContextItemView() {
  SimpleXTheme {
    ContextItemView(
      contextItems = listOf(ChatItem.getSampleData(1, CIDirection.DirectRcv(), Clock.System.now(), "hello")),
      contextIcon = painterResource(MR.images.ic_edit_filled),
      chatInfo = Chat.sampleData.chatInfo
    ) {}
  }
}
