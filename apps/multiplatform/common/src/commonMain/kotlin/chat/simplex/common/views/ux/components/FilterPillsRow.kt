package chat.simplex.common.views.ux.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.ChatFolder
import chat.simplex.common.platform.SimpleUXHapticType
import chat.simplex.common.platform.performHapticFeedback
import chat.simplex.common.ui.theme.*
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.bounceClick
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

enum class UxFilterCategory {
  ALL,
  UNREAD,
  DIRECT,
  GROUPS,
  FAVORITES
}

@Composable
fun UxFilterCategory.localizedLabel(): String = when (this) {
  UxFilterCategory.ALL -> stringResource(MR.strings.chat_list_all)
  UxFilterCategory.UNREAD -> stringResource(MR.strings.filter_pill_unread)
  UxFilterCategory.DIRECT -> stringResource(MR.strings.filter_pill_direct)
  UxFilterCategory.GROUPS -> stringResource(MR.strings.chat_list_groups)
  UxFilterCategory.FAVORITES -> stringResource(MR.strings.chat_list_favorites)
}

// #98: Filter pills driven by the user's Chat Folders config. Each folder can
// carry a custom name and/or emoji; folders without them fall back to the
// preset label. The "+" pill opens the folder manager in Settings.

@Composable
fun FilterPillsRow(
  visibleFolders: List<ChatFolder>,
  activeFolderId: String?,
  onFolderSelected: (ChatFolder) -> Unit,
  onManageClick: () -> Unit,
  modifier: Modifier = Modifier,
  totalUnread: Int = 0
) {
  val isDark = isInDarkTheme()
  val scrollState = rememberScrollState()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      // #98 (feedback): breathing room below the top bar card
      .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    visibleFolders.sortedBy { it.order }.forEach { folder ->
      val isSelected = folder.id == activeFolderId
      val shape = RoundedCornerShape(20.dp)

      val badgeCount: Int? = when {
        folder.filterKind == UxFilterCategory.UNREAD.ordinal && totalUnread > 0 -> totalUnread
        else -> null
      }

      val bgColor = animateColorAsState(
        targetValue = when {
          isSelected && isDark -> Color(0x33E2B755)
          isSelected && !isDark -> Color(0xFFFEF3C7)
          isDark -> Color(0x1F1E293B)
          else -> Slate50
        },
        animationSpec = spring()
      )

      val borderColor = animateColorAsState(
        targetValue = when {
          isSelected && isDark -> Color(0x80E2B755)
          isSelected && !isDark -> AmberGold
          isDark -> Color(0x2EFFFFFF)
          else -> Slate200
        },
        animationSpec = spring()
      )

      val textColor = animateColorAsState(
        targetValue = when {
          isSelected && isDark -> AmberGold
          isSelected && !isDark -> Color(0xFFB45309)
          isDark -> Slate400
          else -> Slate600
        },
        animationSpec = spring()
      )

      Box(
        modifier = Modifier
          .clip(shape)
          .background(bgColor.value)
          .border(width = 1.dp, color = borderColor.value, shape = shape)
          .bounceClick(scaleDown = 0.95f)
          .clickable(
            role = androidx.compose.ui.semantics.Role.Tab,
            onClick = {
              performHapticFeedback(SimpleUXHapticType.LIGHT)
              onFolderSelected(folder)
            }
          )
          .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          folder.emoji?.let { emoji ->
            Text(text = emoji, fontSize = 14.sp)
          }

          val displayName = folder.name
            ?: UxFilterCategory.entries.getOrNull(folder.filterKind)?.localizedLabel()
            ?: stringResource(MR.strings.chat_list_all)

          Text(
            text = displayName,
            color = textColor.value,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
          )

          if (badgeCount != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                  Brush.linearGradient(
                    if (isDark) listOf(AmberGold, Amber600)
                    else listOf(Amber600, Color(0xFFB45309))
                  )
                )
                .padding(horizontal = 6.dp, vertical = 1.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                color = if (isDark) Slate900 else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // "+" pill opens the folder manager. Identical metrics to the other pills
    // so the row stays on one visual baseline (feedback 2026-08-31).
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(if (isDark) Color(0x1F1E293B) else Slate50)
        .border(width = 1.dp, color = if (isDark) Color(0x2EFFFFFF) else Slate200, shape = RoundedCornerShape(20.dp))
        .bounceClick(scaleDown = 0.95f)
        .clickable(
          onClick = {
            performHapticFeedback(SimpleUXHapticType.LIGHT)
            onManageClick()
          }
        )
        .padding(horizontal = 14.dp, vertical = 7.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "+",
        color = if (isDark) Slate400 else Slate600,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
