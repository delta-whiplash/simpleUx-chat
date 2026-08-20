package chat.simplex.common.views.ux.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.DefaultDropdownMenu
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

data class DisappearingPreset(val seconds: Int?, val label: String)

val DISAPPEARING_PRESETS = listOf(
    DisappearingPreset(null, "Désactivé (Off)"),
    DisappearingPreset(30, "30 secondes"),
    DisappearingPreset(300, "5 minutes"),
    DisappearingPreset(3600, "1 heure"),
    DisappearingPreset(86400, "24 heures"),
    DisappearingPreset(604800, "7 jours")
)

fun formatTTL(seconds: Int?): String {
    return when (seconds) {
        null -> "Off"
        in 1..59 -> "${seconds}s"
        in 60..3599 -> "${seconds / 60}m"
        in 3600..86399 -> "${seconds / 3600}h"
        else -> "${seconds / 86400}j"
    }
}

@Composable
fun DisappearingTimerBar(
    currentTTL: Int?,
    onTTLSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isInDarkTheme()
    val showMenu = remember { mutableStateOf(false) }
    val isTimerActive = currentTTL != null
    val shape = RoundedCornerShape(12.dp)

    val activeColor = if (isTimerActive) Color(0xFFF59E0B) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    if (isTimerActive) activeColor.copy(alpha = if (isDark) 0.15f else 0.10f)
                    else (if (isDark) Color(0x1F1E293B) else Color(0x140F172A))
                )
                .border(
                    width = 1.dp,
                    color = if (isTimerActive) activeColor.copy(alpha = 0.4f) else (if (isDark) Color(0x22FFFFFF) else Color(0x1A000000)),
                    shape = shape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showMenu.value = true
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(if (isTimerActive) MR.images.ic_timer_filled else MR.images.ic_timer),
                    contentDescription = "Messages éphémères",
                    tint = activeColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isTimerActive) formatTTL(currentTTL) else "Éphémère",
                    color = activeColor,
                    fontSize = 11.sp,
                    fontWeight = if (isTimerActive) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        DefaultDropdownMenu(showMenu) {
            DISAPPEARING_PRESETS.forEach { preset ->
                DropdownMenuItem(
                    onClick = {
                        showMenu.value = false
                        onTTLSelected(preset.seconds)
                    },
                    modifier = Modifier.height(38.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(if (preset.seconds == currentTTL) MR.images.ic_check else MR.images.ic_timer),
                            contentDescription = preset.label,
                            tint = if (preset.seconds == currentTTL) MaterialTheme.colors.primary else if (isInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = preset.label,
                            color = if (preset.seconds == currentTTL) MaterialTheme.colors.primary else if (isInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = if (preset.seconds == currentTTL) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
