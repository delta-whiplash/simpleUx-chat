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
import chat.simplex.common.platform.SimpleUXHapticType
import chat.simplex.common.platform.performHapticFeedback
import chat.simplex.common.ui.theme.*
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.bounceClick
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

enum class UxFilterCategory {
    ALL,
    UNREAD,
    DIRECT,
    GROUPS,
    FAVORITES
}

@Composable
private fun UxFilterCategory.localizedLabel(): String = when (this) {
    UxFilterCategory.ALL -> stringResource(MR.strings.chat_list_all)
    UxFilterCategory.UNREAD -> stringResource(MR.strings.filter_pill_unread)
    UxFilterCategory.DIRECT -> stringResource(MR.strings.filter_pill_direct)
    UxFilterCategory.GROUPS -> stringResource(MR.strings.chat_list_groups)
    UxFilterCategory.FAVORITES -> stringResource(MR.strings.chat_list_favorites)
}

@Composable
fun FilterPillsRow(
    activeCategory: UxFilterCategory,
    onCategorySelected: (UxFilterCategory) -> Unit,
    modifier: Modifier = Modifier,
    totalUnread: Int = 0
) {
    val isDark = isInDarkTheme()
    val scrollState = rememberScrollState()

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
                    isSelected && !isDark -> Color(0xFFFEF3C7)
                    isDark -> Color(0x1F1E293B)
                    else -> Slate50
                },
                animationSpec = spring()
            )

            val borderColor = animateColorAsState(
                targetValue = when {
                    isSelected && isDark -> Color(0x80E2B755)
                    isSelected && !isDark -> AmberGold
                    isDark -> Color(0x2EFFFFFF)
                    else -> Slate200
                },
                animationSpec = spring()
            )

            val textColor = animateColorAsState(
                targetValue = when {
                    isSelected && isDark -> AmberGold
                    isSelected && !isDark -> Color(0xFFB45309)
                    isDark -> Slate400
                    else -> Slate600
                },
                animationSpec = spring()
            )

            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bgColor.value)
                    .border(width = 1.dp, color = borderColor.value, shape = shape)
                    .bounceClick(scaleDown = 0.95f)
                    .clickable(
                        role = androidx.compose.ui.semantics.Role.Tab,
                        onClick = {
                            performHapticFeedback(SimpleUXHapticType.LIGHT)
                            onCategorySelected(category)
                        }
                    )
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
                            tint = if (isSelected) (if (isDark) AmberGold else Amber600) else textColor.value,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = category.localizedLabel(),
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
                                        if (isDark) listOf(AmberGold, Amber600)
                                        else listOf(Amber600, Color(0xFFB45309))
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                color = if (isDark) Slate900 else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
