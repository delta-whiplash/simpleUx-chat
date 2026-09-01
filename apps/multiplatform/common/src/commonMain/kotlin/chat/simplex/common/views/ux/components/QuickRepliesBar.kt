package chat.simplex.common.views.ux.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.SimpleUXHapticType
import chat.simplex.common.platform.performHapticFeedback
import chat.simplex.common.ui.theme.*
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.bounceClick
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
private fun defaultQuickReplies(): List<String> = listOf(
    stringResource(MR.strings.quick_reply_agree),
    stringResource(MR.strings.quick_reply_thanks),
    stringResource(MR.strings.quick_reply_perfect),
    stringResource(MR.strings.quick_reply_on_my_way),
    stringResource(MR.strings.quick_reply_talk_later),
    stringResource(MR.strings.quick_reply_looking_into_it)
)

@Composable
fun QuickRepliesBar(
    isVisible: Boolean,
    onQuickReplySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isInDarkTheme()
    val scrollState = rememberScrollState()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            defaultQuickReplies().forEach { reply ->
                val pillShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(if (isDark) Color(0x331E293B) else Color(0xEEF1F5F9))
                        .border(
                            1.dp,
                            if (isDark) GlassBorderDark else Color(0x1F000000),
                            pillShape
                        )
                        .bounceClick(scaleDown = 0.94f)
                        .clickable {
                            performHapticFeedback(SimpleUXHapticType.LIGHT)
                            onQuickReplySelected(reply)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reply,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Slate400 else Slate600
                    )
                }
            }
        }
    }
}
