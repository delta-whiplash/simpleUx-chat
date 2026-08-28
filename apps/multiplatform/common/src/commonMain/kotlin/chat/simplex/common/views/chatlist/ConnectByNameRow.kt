package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.planAndConnect
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

// Extracted verbatim from ChatListView.kt (issue #4). Kept in package views.chatlist (not views/ux)
// on purpose: the restyled surface colors have no exact ui/theme tokens yet (issue #16) and the
// views/ux hex-lint baseline must not grow. Internal signature unchanged (used by ChatListView and
// NewChatSheet).
@Composable
internal fun ConnectByNameRow(name: String, searchText: MutableState<TextFieldValue>, connectNameCandidate: MutableState<String?>, close: (() -> Unit)?) {
  val view = LocalMultiplatformView()
  val isDark = isInDarkTheme()
  val isGroup = name.startsWith("#")
  val shape = RoundedCornerShape(16.dp)

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp)
      .clip(shape)
      .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
      .border(1.dp, if (isDark) Color(0x35FFFFFF) else Color(0x150F172A), shape)
      .clickable {
        hideKeyboard(view)
        performHapticFeedback(SimpleUXHapticType.LIGHT)
        withBGApi {
          planAndConnect(
            chatModel.remoteHostId(),
            name,
            close = {
              searchText.value = TextFieldValue()
              connectNameCandidate.value = null
              close?.invoke()
            },
            cleanup = {
              connectNameCandidate.value = null
            },
          )
        }
      },
    color = Color.Transparent,
    shape = shape
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(if (isGroup) (if (isDark) Color(0x2238BDF8) else Color(0x150284C7)) else (if (isDark) Color(0x22E2B755) else Color(0x15D97706))),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painter = painterResource(if (isGroup) MR.images.ic_group else MR.images.ic_person),
          contentDescription = null,
          tint = if (isGroup) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else (if (isDark) Color(0xFFE2B755) else Color(0xFFD97706)),
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text = if (isGroup) stringResource(MR.strings.connect_by_name_group, name) else stringResource(MR.strings.connect_by_name_contact, name),
          color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.5.sp
        )
        Text(
          text = stringResource(MR.strings.directory_search_hint),
          color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
          fontSize = 11.5.sp
        )
      }
      Icon(
        painter = painterResource(MR.images.ic_chevron_right),
        contentDescription = null,
        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
