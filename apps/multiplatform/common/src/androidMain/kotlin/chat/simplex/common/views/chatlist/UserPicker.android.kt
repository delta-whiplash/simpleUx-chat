package chat.simplex.common.views.chatlist

import SectionItemView
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.DrawerDefaults.ScrimOpacity
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import chat.simplex.common.model.User
import chat.simplex.common.model.UserInfo
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

private val USER_PICKER_IMAGE_SIZE = 44.dp
private val USER_PICKER_ROW_PADDING = 16.dp

@Composable
actual fun UserPickerUsersSection(
  users: List<UserInfo>,
  iconColor: Color,
  stopped: Boolean,
  onUserClicked: (user: User) -> Unit,
) {
  val scrollState = rememberScrollState()
  val screenWidthDp = windowWidth()

  if (users.isNotEmpty()) {
    SectionItemView(
      padding = PaddingValues(),
      disabled = stopped
    ) {
      Box {
        Row(
          modifier = Modifier.horizontalScroll(scrollState),
        ) {
          Spacer(Modifier.width(DEFAULT_PADDING))
          Row(horizontalArrangement = Arrangement.spacedBy(USER_PICKER_ROW_PADDING)) {
            users.forEach { u ->
              UserPickerUserBox(u, stopped, modifier = Modifier.userBoxWidth(u.user, users.size, screenWidthDp)) {
                onUserClicked(it)
                withBGApi {
                  delay(500)
                  scrollState.scrollTo(0)
                }
              }
            }
          }
          Spacer(Modifier.width(DEFAULT_PADDING))
        }
      }
    }
  }
}
@Composable
fun UserPickerUserBox(
  userInfo: UserInfo,
  stopped: Boolean,
  modifier: Modifier = Modifier,
  onClick: (user: User) -> Unit,
) {
  val isDark = isInDarkTheme()
  val user = userInfo.user
  val cardBg = if (user.activeUser) {
    if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
  } else {
    if (isDark) Color(0xFF121A26) else Color(0xFFFFFFFF)
  }

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .border(
        width = 1.dp,
        color = if (user.activeUser) {
          if (isDark) Color(0x6638BDF8) else Color(0x330284C7)
        } else {
          if (isDark) Color(0x1FFFFFFF) else Color(0x0D000000)
        },
        shape = RoundedCornerShape(18.dp)
      )
      .background(cardBg)
      .clickable(
        onClick = { onClick(userInfo.user) },
        enabled = !stopped
      )
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Box {
      ProfileImage(size = USER_PICKER_IMAGE_SIZE, image = userInfo.user.profile.image, name = user.displayName)

      if (userInfo.unreadCount > 0 && !userInfo.user.activeUser) {
        userUnreadBadge(userInfo.unreadCount, userInfo.user.showNtfs, false)
      }
    }
    NameWithBadge(
      user.displayName,
      user.profile.localBadge,
      color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
      fontWeight = if (user.activeUser) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}


@Composable
actual fun PlatformUserPicker(modifier: Modifier, pickerState: MutableStateFlow<AnimatedViewState>, content: @Composable () -> Unit) {
  val currentState by pickerState.collectAsState()
  val isDark = isInDarkTheme()
  val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

  AnimatedVisibility(
    visible = currentState.isVisible(),
    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) + slideInVertically(
      initialOffsetY = { it },
      animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 400f)
    ),
    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) + slideOutVertically(
      targetOffsetY = { it },
      animationSpec = androidx.compose.animation.core.tween(180)
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.55f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = { pickerState.value = AnimatedViewState.HIDING }
        ),
      contentAlignment = Alignment.BottomCenter
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(sheetShape)
          .background(if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF))
          .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
              listOf(
                if (isDark) Color(0x38FFFFFF) else Color(0x1F000000),
                Color.Transparent
              )
            ),
            shape = sheetShape
          )
          .then(modifier)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {}
          )
          .navigationBarsPadding()
      ) {
        Column {
          // Drag handle pill
          Box(
            modifier = Modifier
              .padding(top = 12.dp, bottom = 4.dp)
              .size(width = 38.dp, height = 4.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(if (isDark) Color(0x40FFFFFF) else Color(0x20000000))
              .align(Alignment.CenterHorizontally)
          )
          content()
        }
      }
    }
  }
}

private fun Modifier.userBoxWidth(user: User, totalUsers: Int, windowWidth: Dp): Modifier {
  return if (totalUsers == 1) {
    this.width(windowWidth - DEFAULT_PADDING * 2)
  } else if (user.activeUser) {
    this.width(windowWidth - DEFAULT_PADDING - (USER_PICKER_ROW_PADDING * 3) - USER_PICKER_IMAGE_SIZE)
  } else {
    this.widthIn(max = (windowWidth - (DEFAULT_PADDING * 2)) * 0.618f)
  }
}