package chat.simplex.common.views.ux.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

enum class UxFilterCategory(val label: String) {
    ALL("Tous"),
    UNREAD("Non lus"),
    DIRECT("Directs"),
    GROUPS("Groupes"),
    FAVORITES("Favoris")
}

@Composable
fun FilterPillsRow(
    activeCategory: UxFilterCategory,
    onCategorySelected: (UxFilterCategory) -> Unit,
    modifier: Modifier = Modifier,
    chatModelInstance: ChatModel = ChatModel
) {
    val isDark = isInDarkTheme()
    val scrollState = rememberScrollState()

    // Calculate unread count
    val totalUnread: Int = remember(chatModelInstance.chats.value) {
        chatModelInstance.chats.value.sumOf { chat -> chat.chatStats.unreadCount }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UxFilterCategory.entries.forEach { category ->
            val isSelected = category == activeCategory
            val shape = RoundedCornerShape(20.dp)

            val badgeCount: Int? = when (category) {
                UxFilterCategory.UNREAD -> if (totalUnread > 0) totalUnread else null
                else -> null
            }

            val bgColor = animateColorAsState(
                targetValue = when {
                    isSelected && isDark -> Color(0x33E2B755)
                    isSelected && !isDark -> Color(0x22D97706)
                    isDark -> Color(0x1F1E293B)
                    else -> Color(0x140F172A)
                },
                animationSpec = spring(),
                label = "pillBg"
            )

            val borderColor = animateColorAsState(
                targetValue = when {
                    isSelected && isDark -> Color(0x80E2B755)
                    isSelected && !isDark -> Color(0x66D97706)
                    isDark -> Color(0x2EFFFFFF)
                    else -> Color(0x1F000000)
                },
                animationSpec = spring(),
                label = "pillBorder"
            )

            val textColor = animateColorAsState(
                targetValue = when {
                    isSelected && isDark -> Color(0xFFE2B755)
                    isSelected && !isDark -> Color(0xFFD97706)
                    isDark -> Color(0xFF94A3B8)
                    else -> Color(0xFF64748B)
                },
                animationSpec = spring(),
                label = "pillText"
            )

            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bgColor.value)
                    .border(width = 1.dp, color = borderColor.value, shape = shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onCategorySelected(category)
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (category == UxFilterCategory.FAVORITES) {
                        Icon(
                            painter = painterResource(MR.images.ic_star_filled),
                            contentDescription = null,
                            tint = if (isSelected) (if (isDark) Color(0xFFE2B755) else Color(0xFFD97706)) else textColor.value,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = category.label,
                        color = textColor.value,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    if (badgeCount != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        if (isDark) listOf(Color(0xFFE2B755), Color(0xFFD97706))
                                        else listOf(Color(0xFFD97706), Color(0xFFB45309))
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                color = if (isDark) Color(0xFF0F172A) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
