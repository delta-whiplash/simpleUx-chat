package chat.simplex.common.views.ux.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

val DEFAULT_QUICK_EMOJIS = listOf("👍", "❤️", "🔥", "😂", "😮", "😢")

@Composable
fun QuickReactionsBar(
    isVisible: Boolean,
    onReactionSelected: (String) -> Unit,
    onMoreEmojisClicked: () -> Unit,
    modifier: Modifier = Modifier,
    emojis: List<String> = DEFAULT_QUICK_EMOJIS
) {
    val isDark = isInDarkTheme()
    val shape = RoundedCornerShape(24.dp)

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        exit = scaleOut()
    ) {
        Surface(
            shape = shape,
            color = if (isDark) Color(0xF01E293B) else Color(0xF8FFFFFF),
            elevation = 4.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                    shape = shape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                emojis.take(6).forEach { emoji ->
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 1.35f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "emojiScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .scale(scale)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isPressed = true
                                onReactionSelected(emoji)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 17.sp
                        )
                    }
                }

                // Plus button for full emoji picker
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0x14000000))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onMoreEmojisClicked()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(MR.images.ic_add),
                        contentDescription = "Plus d'emojis",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
