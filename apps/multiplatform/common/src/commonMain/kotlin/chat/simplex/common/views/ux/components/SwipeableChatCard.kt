package chat.simplex.common.views.ux.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.Chat
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeableChatCard(
    chat: Chat,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onToggleRead: ((Chat) -> Unit)? = null,
    onToggleFavorite: ((Chat) -> Unit)? = null,
    isStarred: Boolean = false,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val isDark = isInDarkTheme()
    val isUnread = chat.unreadTag || chat.chatStats.unreadCount > 0
    // Real starred state is passed in by the caller (ChatModel.starredChatIds), never local fake state
    val isFavorite = isStarred

    val threshold = 90f

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Background Actions Bar (Revealed on swipe)
        val currentOffset = offsetX.value
        if (abs(currentOffset) > 10f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        when {
                            currentOffset > 0 -> if (isUnread) Color(0xFF10B981) else Color(0xFF3B82F6) // Right swipe: Read/Unread
                            else -> if (isFavorite) Color(0xFFF59E0B) else Color(0xFF8B5CF6) // Left swipe: Favorite
                        }
                    )
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (currentOffset > 0) Arrangement.Start else Arrangement.End
            ) {
                if (currentOffset > 0) {
                    // Right swipe action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isUnread) MR.images.ic_check else MR.images.ic_mark_chat_unread),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(if (isUnread) MR.strings.mark_read else MR.strings.mark_unread),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // Left swipe action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isFavorite) MR.images.ic_star_off else MR.images.ic_star),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(if (isFavorite) MR.strings.unfavorite_chat else MR.strings.favorite_chat),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Foreground Content
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(if (abs(offsetX.value) > 1f) (if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)) else Color.Transparent)
                .clickable(
                    enabled = onClick != null,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (abs(offsetX.value) < 5f) {
                        onClick?.invoke()
                    }
                }
                .draggable(
                    enabled = onToggleRead != null || onToggleFavorite != null,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val newOffset = (offsetX.value + delta).coerceIn(-130f, 130f)
                            offsetX.snapTo(newOffset)
                        }
                    },
                    onDragStopped = {
                        val endOffset = offsetX.value
                        if (endOffset > threshold) {
                            performHapticFeedback(SimpleUXHapticType.MEDIUM)
                            // Right action: Toggle read/unread
                            onToggleRead?.invoke(chat)
                        } else if (endOffset < -threshold) {
                            performHapticFeedback(SimpleUXHapticType.MEDIUM)
                            // Left action: Toggle favorite
                            onToggleFavorite?.invoke(chat)
                        }
                        coroutineScope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    }
                )
        ) {
            content()
        }
    }
}
