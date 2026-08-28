package chat.simplex.common.views.ux.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.ChatModel
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun QuickProfileSwitcher(
    users: List<chat.simplex.common.model.UserInfo>,
    onProfileSelected: (Long) -> Unit,
    onNewProfileClicked: () -> Unit,
    onIncognitoInviteClicked: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isInDarkTheme()
    val visibleUsers = remember(users) {
        users.filter { !it.user.hidden }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF))
            .padding(20.dp)
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Color(0x44FFFFFF) else Color(0x22000000))
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(MR.strings.profile_switcher_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        )

        Spacer(Modifier.height(12.dp))

        // Profiles list
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleUsers) { userItem ->
                val isActive = userItem.user.activeUser
                val shape = RoundedCornerShape(16.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            when {
                                isActive && isDark -> Color(0x3300E5FF)
                                isActive && !isDark -> Color(0x220284C7)
                                isDark -> Color(0x1F1E293B)
                                else -> Color(0x0F0F172A)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) (if (isDark) Color(0x8000E5FF) else Color(0x660284C7))
                            else (if (isDark) Color(0x22FFFFFF) else Color(0x14000000)),
                            shape = shape
                        )
                        .clickable {
                            onProfileSelected(userItem.user.userId)
                            onClose()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileImage(
                        image = userItem.user.profile?.image,
                        size = 42.dp,
                        color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userItem.user.profile?.displayName ?: stringResource(MR.strings.profile_default_name),
                            fontSize = 15.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        if (isActive) {
                            Text(
                                text = stringResource(MR.strings.profile_switcher_active),
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        }
                    }

                    if (userItem.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${userItem.unreadCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Incognito Link Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                        )
                    )
                    .clickable {
                        onIncognitoInviteClicked()
                        onClose()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(MR.images.ic_theater_comedy_filled),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(MR.strings.profile_switcher_incognito_link),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // New Profile Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0x331E293B) else Color(0x140F172A))
                    .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000), RoundedCornerShape(14.dp))
                    .clickable {
                        onNewProfileClicked()
                        onClose()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(MR.images.ic_add),
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(MR.strings.profile_switcher_new_profile),
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
