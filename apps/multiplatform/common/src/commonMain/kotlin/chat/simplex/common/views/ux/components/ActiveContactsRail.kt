package chat.simplex.common.views.ux.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.ChatInfoImage
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
fun ActiveContactsRail(
    chats: List<Chat>,
    onChatClicked: (Chat) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val isDark = isInDarkTheme()
    val scrollState = rememberScrollState()

    // Filter to get direct contacts or favorite chats
    val recentContacts = remember(chats) {
        chats.filter { chat ->
            chat.chatInfo is ChatInfo.Direct || chat.chatInfo.chatSettings?.favorite == true
        }.take(12)
    }

    AnimatedVisibility(
        visible = isVisible && recentContacts.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                recentContacts.forEach { chat ->
                    val info = chat.chatInfo
                    val name = info.displayName
                    val isDirect = info is ChatInfo.Direct
                    val isFavorite = info.chatSettings?.favorite == true
                    val hasUnread = chat.unreadTag
                    val unreadCnt = chat.chatStats.unreadCount

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(62.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onChatClicked(chat)
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(54.dp)
                        ) {
                            // Status / Gradient Ring
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            hasUnread -> Brush.linearGradient(
                                                listOf(Color(0xFF00E5FF), Color(0xFF0088FF))
                                            )
                                            isFavorite -> Brush.linearGradient(
                                                listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                            )
                                            isDark -> Brush.linearGradient(
                                                listOf(Color(0x6638BDF8), Color(0x221E293B))
                                            )
                                            else -> Brush.linearGradient(
                                                listOf(Color(0x440284C7), Color(0x11E2E8F0))
                                            )
                                        }
                                    )
                                    .padding(2.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ChatInfoImage(
                                        chatInfo = info,
                                        size = 46.dp
                                    )
                                }
                            }

                            // Favorite star icon badge
                            if (isFavorite) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B))
                                        .border(1.5.dp, if (isDark) Color(0xFF0F172A) else Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(MR.images.ic_star_filled),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            } else if (hasUnread) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00E5FF))
                                        .border(1.5.dp, if (isDark) Color(0xFF0F172A) else Color.White, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                    Text(
                                        text = if (unreadCnt > 9) "9+" else if (unreadCnt > 0) "$unreadCnt" else "•",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
