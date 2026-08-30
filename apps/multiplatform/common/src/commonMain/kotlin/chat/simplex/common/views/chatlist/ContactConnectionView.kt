package chat.simplex.common.views.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.*
import chat.simplex.common.model.PendingContactConnection
import chat.simplex.common.model.getTimestampText
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR

@Composable
fun ContactConnectionView(contactConnection: PendingContactConnection) {
  val isDark = isInDarkTheme()
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier
        .size(54.dp * fontSizeSqrtMultiplier)
        .clip(CircleShape)
        .border(1.dp, if (isDark) Color(0x38FFFFFF) else Color(0x1F000000), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      ProfileImage(size = 54.dp * fontSizeSqrtMultiplier, null, if (contactConnection.initiated) MR.images.ic_add_link else MR.images.ic_link)
    }
    Spacer(Modifier.width(10.dp))
    Column(
      modifier = Modifier.weight(1F)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          contactConnection.displayName,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.h3,
          fontWeight = FontWeight.Bold,
          color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
          modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        val ts = getTimestampText(contactConnection.updatedAt)
        ChatListTimestampView(ts)
      }
      Row(Modifier.heightIn(min = 34.sp.toDp()).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
          contactConnection.description,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            lineHeight = 19.sp
          ),
          modifier = Modifier.weight(1f)
        )
        if (contactConnection.incognito) {
          Spacer(Modifier.width(6.dp))
          IncognitoIcon(contactConnection.incognito)
        }
      }
    }
  }
}
