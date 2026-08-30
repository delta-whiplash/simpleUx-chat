package chat.simplex.common.views.chat

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.IncognitoOptionImage
import chat.simplex.common.views.usersettings.IncognitoView
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PreparedChatJoinBar(
  rhId: Long?,
  chat: Chat,
  currentUser: User,
  inProgress: Boolean,
  actionText: String,
  actionIcon: ImageResource,
  onAction: () -> Unit
) {
  val isDark = isInDarkTheme()
  val selectedUser = remember { mutableStateOf(currentUser) }
  val incognitoDefault = chatModel.controller.appPrefs.incognito.get()
  val users = chatModel.users.map { it.user }.filter { u -> u.activeUser || !u.hidden }
  val pickerExpanded = remember { mutableStateOf(false) }

  fun changeProfile(newUser: User) {
    withApi {
      if (chat.chatInfo is ChatInfo.Direct) {
        val updatedContact = chatModel.controller.apiChangePreparedContactUser(rhId, chat.chatInfo.contact.contactId, newUser.userId)
        if (updatedContact != null) {
          selectedUser.value = newUser
          chatModel.controller.appPrefs.incognito.set(false)
          pickerExpanded.value = false
          chatModel.chatsContext.updateContact(rhId, updatedContact)
        }
      } else if (chat.chatInfo is ChatInfo.Group) {
        val updatedGroup = chatModel.controller.apiChangePreparedGroupUser(rhId, chat.chatInfo.groupInfo.groupId, newUser.userId)
        if (updatedGroup != null) {
          selectedUser.value = newUser
          chatModel.controller.appPrefs.incognito.set(false)
          pickerExpanded.value = false
          chatModel.chatsContext.updateGroup(rhId, updatedGroup)
        }
      }
      chatModel.controller.changeActiveUser_(
        rhId = newUser.remoteHostId,
        toUserId = newUser.userId,
        viewPwd = null,
        keepingChatId = chat.id
      )
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    // Expandable sleek profile list popup
    AnimatedVisibility(
      visible = pickerExpanded.value && !chat.chatInfo.profileChangeProhibited,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(if (isDark) Color(0xF21E293B) else Color(0xF8FFFFFF))
          .border(1.dp, if (isDark) Color(0x3338BDF8) else Color(0x1F0284C7), RoundedCornerShape(20.dp))
          .padding(8.dp)
      ) {
        Text(
          text = generalGetString(MR.strings.profile_switcher_title),
          style = TextStyle(
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
          ),
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        users.forEach { user ->
          val isSelected = selectedUser.value.userId == user.userId && !incognitoDefault
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (isSelected) (if (isDark) Color(0x3338BDF8) else Color(0x1F0284C7)) else Color.Transparent)
              .clickable {
                changeProfile(user)
              }
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ProfileImage(size = 32.dp, image = user.image)
            Text(
              text = user.chatViewName,
              style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isDark) Color.White else Color(0xFF0F172A)
              ),
              modifier = Modifier.weight(1f)
            )
            if (isSelected) {
              Icon(
                painterResource(MR.images.ic_check),
                contentDescription = null,
                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        // Incognito option
        val isIncognito = incognitoDefault
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isIncognito) (if (isDark) Color(0x338B5CF6) else Color(0x1F8B5CF6)) else Color.Transparent)
            .clickable {
              chatModel.controller.appPrefs.incognito.set(true)
              pickerExpanded.value = false
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          IncognitoOptionImage()
          Text(
            text = stringResource(MR.strings.incognito),
            style = TextStyle(
              fontFamily = PlusJakartaSans,
              fontSize = 13.sp,
              fontWeight = if (isIncognito) FontWeight.SemiBold else FontWeight.Normal,
              color = if (isDark) Color.White else Color(0xFF0F172A)
            ),
            modifier = Modifier.weight(1f)
          )
          if (isIncognito) {
            Icon(
              painterResource(MR.images.ic_check),
              contentDescription = null,
              tint = Color(0xFF8B5CF6),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // Main Floating Toast Pop Island
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = 10.dp,
          shape = RoundedCornerShape(24.dp),
          spotColor = if (isDark) Color(0x66000000) else Color(0x22000000)
        )
        .clip(RoundedCornerShape(24.dp))
        .background(
          if (isDark) Color(0xF21E293B) else Color(0xF8FFFFFF)
        )
        .border(
          1.dp,
          if (isDark) Color(0x3338BDF8) else Color(0x1F0284C7),
          RoundedCornerShape(24.dp)
        )
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Profile Selector Chip (Left)
      Row(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .clickable(enabled = !chat.chatInfo.profileChangeProhibited && !inProgress) {
            pickerExpanded.value = !pickerExpanded.value
          }
          .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (incognitoDefault) {
          IncognitoOptionImage()
        } else {
          ProfileImage(size = 36.dp, image = selectedUser.value.image)
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(MR.strings.join_bar_join_as),
            style = TextStyle(
              fontFamily = PlusJakartaSans,
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            maxLines = 1
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = if (incognitoDefault) stringResource(MR.strings.incognito) else selectedUser.value.chatViewName,
              style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (!chat.chatInfo.profileChangeProhibited) {
              Icon(
                painterResource(if (pickerExpanded.value) MR.images.ic_chevron_up else MR.images.ic_chevron_down),
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }

      Spacer(Modifier.width(8.dp))

      // Primary Join / Connect Action Button (Right)
      Button(
        onClick = {
          performHapticFeedback(SimpleUXHapticType.MEDIUM)
          onAction()
        },
        enabled = !inProgress,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
          contentColor = Color.White,
          disabledBackgroundColor = (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)).copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
      ) {
        if (inProgress) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = Color.White,
            strokeWidth = 2.dp
          )
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              painterResource(actionIcon),
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = actionText,
              style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }
        }
      }
    }
  }
}
