package chat.simplex.common.views.chat.glass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.GlassTokens
import chat.simplex.common.ui.theme.glassSurface

/**
 * Glassmorphism-styled message bubble for text messages.
 *
 * Features:
 * - Asymmetric rounded corners (tail corner = 4dp, others = 20dp)
 * - Sent: blue-purple gradient glass
 * - Received: white-tinted glass
 * - Sender name in accent color (for group chats)
 * - Timestamp with delivery status checkmarks
 *
 * This wraps message text content; it does NOT replace the full ChatItemView
 * but provides the visual container used when glass mode is active.
 */
@Composable
fun GlassMessageBubble(
    isOutgoing: Boolean,
    senderName: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .glassSurface(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isOutgoing) 20.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 20.dp
                    ),
                    backgroundColor = if (isOutgoing) {
                        GlassTokens.SentBubblePrimary.copy(alpha = GlassTokens.SentBubbleAlpha)
                    } else {
                        GlassTokens.ReceivedBubbleColor.copy(alpha = GlassTokens.ReceivedBubbleAlpha)
                    },
                    borderColor = if (isOutgoing) {
                        GlassTokens.SentBubbleAccent.copy(alpha = GlassTokens.SentBorderAlpha)
                    } else {
                        Color.White.copy(alpha = GlassTokens.ReceivedBorderAlpha)
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                // Sender name for group incoming messages
                if (!isOutgoing && senderName != null) {
                    Text(
                        text = senderName,
                        color = GlassTokens.SenderNameAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                content()
            }
        }
    }
}

/**
 * Timestamp + delivery status row for glass bubbles.
 */
@Composable
fun GlassMessageMeta(
    timestamp: String,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = timestamp,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.5.sp
        )
        if (isOutgoing) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "✓✓",
                color = GlassTokens.ReadReceipt,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
