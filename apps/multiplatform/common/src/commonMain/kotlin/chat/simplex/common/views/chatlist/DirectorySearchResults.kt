package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.SimpleUXHapticType
import chat.simplex.common.platform.performHapticFeedback
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

// Extracted from ChatListView.kt (issue #4). Kept in package views.chatlist (not views/ux)
// on purpose: this file is a SimpleUX fork satellite of the chat-list screen, its colors have no
// exact ui/theme tokens yet (tracked by the #16 token consolidation), and the public symbol is
// imported by NewChatSheet.kt.
//
// The section is emitted as ONE LAZY ITEM PER GROUP (plus a header item), NOT as a single item
// wrapping a Column of rows: a single item taller than the viewport used to render with a
// background-colored hole clipped across the middle of the screen (issue #58, reproduced
// 2026-08-28 on emulator-5554). Per-group items keep every row inside the lazy viewport logic.

fun LazyListScope.directorySearchItems(
  query: String,
  groups: List<SimpleUxDirectoryGroup>,
  botDescription: String,
  botCategory: String,
  onJoinGroup: (String, () -> Unit) -> Unit
) {
  val trimmed = query.trim().lowercase()
  val matchingGroups = if (trimmed.isEmpty()) emptyList() else groups.filter {
    val description = if (it.isDirectoryBot) botDescription else it.description
    val category = if (it.isDirectoryBot) botCategory else it.category
    it.name.lowercase().contains(trimmed) ||
    description.lowercase().contains(trimmed) ||
    category.lowercase().contains(trimmed) ||
    it.link.lowercase().contains(trimmed)
  }
  if (matchingGroups.isEmpty()) return

  item(key = "directory_header") {
    DirectorySectionHeader()
  }

  itemsIndexed(matchingGroups, key = { i, g -> "directory_${i}_${g.link}" }) { index, group ->
    DirectoryGroupRow(
      group = group,
      botDescription = botDescription,
      botCategory = botCategory,
      first = index == 0,
      last = index == matchingGroups.lastIndex,
      onJoinGroup = onJoinGroup
    )
  }
}

@Composable
private fun DirectorySectionHeader() {
  val isDark = isInDarkTheme()
  val scope = rememberCoroutineScope()
  LaunchedEffect(Unit) {
    SimpleUxDirectoryRepository.fetchDirectoryIfNeeded(scope)
  }
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(
      painterResource(MR.images.ic_travel_explore),
      contentDescription = null,
      tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
      modifier = Modifier.size(16.dp)
    )
    Text(
      text = stringResource(MR.strings.directory_section_header),
      style = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
      )
    )
    Divider(
      modifier = Modifier.weight(1f),
      color = if (isDark) Color(0x2AFFFFFF) else Color(0x1F000000)
    )
  }
}

@Composable
private fun DirectoryGroupRow(
  group: SimpleUxDirectoryGroup,
  botDescription: String,
  botCategory: String,
  first: Boolean,
  last: Boolean,
  onJoinGroup: (String, () -> Unit) -> Unit
) {
  val isDark = isInDarkTheme()
  val joiningLink = remember { mutableStateOf<String?>(null) }
  val isJoining = joiningLink.value == group.link
  // One tinted card per group. Corners: the first/last rows round their outer edge so a
  // contiguous list still reads as a single panel (spacing 2.dp between cards).
  val shape = when {
    first && last -> RoundedCornerShape(16.dp)
    first -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    last -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    else -> RoundedCornerShape(6.dp)
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = if (first || last) 2.dp else 1.dp)
      .clip(shape)
      .background(if (isDark) Color(0x661E293B) else Color(0xF2F1F5F9))
      .border(1.dp, if (isDark) Color(0x2AFFFFFF) else Color(0x15000000), shape)
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              if (group.link.contains("/a#")) listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
              else listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painterResource(if (group.link.contains("/a#")) MR.images.ic_travel_explore else MR.images.ic_group),
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(20.dp)
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = group.name,
            style = TextStyle(
              fontFamily = PlusJakartaSans,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (isDark) Color.White else Color(0xFF0F172A)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(if (isDark) Color(0x3338BDF8) else Color(0x1F0284C7))
              .padding(horizontal = 5.dp, vertical = 1.dp)
          ) {
            Text(
              text = if (group.isDirectoryBot) botCategory else group.category,
              style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
              )
            )
          }
        }
        Text(
          text = if (group.isDirectoryBot) botDescription else group.description,
          style = TextStyle(
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Spacer(Modifier.width(8.dp))

    Button(
      onClick = {
        joiningLink.value = group.link
        performHapticFeedback(SimpleUXHapticType.MEDIUM)
        onJoinGroup(group.link) {
          joiningLink.value = null
        }
      },
      enabled = !isJoining,
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = if (group.link.contains("/a#")) (if (isDark) Color(0xFF8B5CF6) else Color(0xFF6366F1)) else (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
        contentColor = Color.White,
        disabledBackgroundColor = (if (group.link.contains("/a#")) (if (isDark) Color(0xFF8B5CF6) else Color(0xFF6366F1)) else (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))).copy(alpha = 0.5f)
      ),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
    ) {
      if (isJoining) {
        CircularProgressIndicator(
          modifier = Modifier.size(14.dp),
          color = Color.White,
          strokeWidth = 2.dp
        )
      } else {
        Text(
          text = if (group.link.contains("/a#")) stringResource(MR.strings.open_verb) else stringResource(MR.strings.join_group_button),
          style = TextStyle(
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        )
      }
    }
  }
}
