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
import chat.simplex.common.ui.theme.*
import chat.simplex.common.ui.theme.isInDarkTheme

@Composable
fun QuickReactionsBar(
    isVisible: Boolean,
    onReactionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    emojis: List<String>
) {
    val isDark = isInDarkTheme()
    val shape = RoundedCornerShape(24.dp)
    val scope = rememberCoroutineScope()

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
                    color = if (isDark) GlassBorderDark else Color(0x1F000000),
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
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
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
                                chat.simplex.common.platform.performHapticFeedback(chat.simplex.common.platform.SimpleUXHapticType.LIGHT)
                                FullscreenEmojiEffectManager.trigger(emoji, scope)
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

                // #120: no "+" button - the protocol only supports these 8
                // reactions, so there is no "more emojis" to open (a picker
                // would be a fake affordance)
            }
        }
    }
}
