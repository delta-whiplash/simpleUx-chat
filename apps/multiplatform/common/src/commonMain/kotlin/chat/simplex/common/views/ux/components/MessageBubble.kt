package chat.simplex.common.views.ux.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MessageBubble(
    isSentByMe: Boolean,
    timestampFormatted: String,
    senderName: String? = null,
    quotedSender: String? = null,
    quotedText: String? = null,
    deliveryStatus: String? = null, // "sent", "delivered", "read"
    onReply: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isInDarkTheme()
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val replyThreshold = 60f

    // Asymmetric bubble shape
    val bubbleShape = if (isSentByMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = if (isSentByMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // Swipe-to-Reply indicator behind bubble
        if (offsetX.value > 10f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF3B82F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(MR.images.ic_reply),
                    contentDescription = stringResource(MR.strings.reply_verb),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (delta > 0 || offsetX.value > 0) {
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + delta).coerceIn(0f, 90f)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    },
                    onDragStopped = {
                        if (offsetX.value > replyThreshold) {
                            onReply?.invoke()
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
                .widthIn(max = 320.dp)
                .clip(bubbleShape)
                .background(
                    if (isSentByMe) {
                        Brush.linearGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                        )
                    } else {
                        if (isDark) Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                        else Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF)))
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isSentByMe) Color(0x33FFFFFF) else (if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                    shape = bubbleShape
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                // Sender name (in group chats)
                if (!isSentByMe && !senderName.isNullOrBlank()) {
                    Text(
                        text = senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Quoted message box (Reply preview)
                if (!quotedText.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSentByMe) Color(0x33000000) else (if (isDark) Color(0x44000000) else Color(0x1F000000)))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(if (isSentByMe) Color(0xFF93C5FD) else Color(0xFF38BDF8))
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            if (!quotedSender.isNullOrBlank()) {
                                Text(
                                    text = quotedSender,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSentByMe) Color(0xFFDBEAFE) else (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = quotedText,
                                fontSize = 12.sp,
                                color = if (isSentByMe) Color(0xCCFFFFFF) else (if (isDark) Color(0xCCF8FAFC) else Color(0xCC0F172A)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Message Body
                content()

                // Metadata: Timestamp & Status
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timestampFormatted,
                        fontSize = 11.sp,
                        color = if (isSentByMe) Color(0xAAFFFFFF) else (if (isDark) Color(0xAA94A3B8) else Color(0xAA64748B))
                    )
                    if (isSentByMe && deliveryStatus != null) {
                        Icon(
                            painter = painterResource(
                                when (deliveryStatus) {
                                    "read" -> MR.images.ic_check_filled
                                    "delivered" -> MR.images.ic_check_filled
                                    else -> MR.images.ic_check
                                }
                            ),
                            contentDescription = deliveryStatus,
                            tint = if (deliveryStatus == "read") Color(0xFF93C5FD) else Color(0xAAFFFFFF),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
