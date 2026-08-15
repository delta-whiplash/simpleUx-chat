package chat.simplex.common.views.ux.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.AlertManager
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun SecurityBadge(
    chat: Chat?,
    modifier: Modifier = Modifier
) {
    val encryption = chat.securityBadgeEncryption() ?: return
    val isDark = isInDarkTheme()
    val contact = remember(chat) {
        (chat?.chatInfo as? ChatInfo.Direct)?.contact
    }
    val isVerified = contact?.verified == true
    val shape = RoundedCornerShape(12.dp)

    val labelText = when {
        isVerified -> stringResource(MR.strings.security_badge_verified)
        encryption == SecurityBadgeEncryption.POST_QUANTUM -> stringResource(MR.strings.security_badge_pq_encrypted)
        encryption == SecurityBadgeEncryption.STANDARD_E2EE -> stringResource(MR.strings.security_badge_e2ee)
        else -> stringResource(MR.strings.security_badge_not_encrypted)
    }

    val accentColor = when {
        isVerified -> Color(0xFF00E5FF) // Cyan for Verified
        encryption == SecurityBadgeEncryption.POST_QUANTUM -> Color(0xFF10B981) // Emerald for Post-Quantum
        encryption == SecurityBadgeEncryption.STANDARD_E2EE -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        else -> Color(0xFFEF4444) // Red for Not Encrypted
    }

    val alertTitle = stringResource(MR.strings.security_badge_alert_title)
    val encryptionLine = when (encryption) {
        SecurityBadgeEncryption.POST_QUANTUM -> stringResource(MR.strings.e2ee_info_pq_short)
        SecurityBadgeEncryption.STANDARD_E2EE -> stringResource(MR.strings.e2ee_info_no_pq_short)
        SecurityBadgeEncryption.NOT_ENCRYPTED -> stringResource(MR.strings.security_badge_no_e2ee_line)
    }
    val contactLine = if (isVerified) {
        stringResource(MR.strings.security_badge_contact_verified_line)
    } else {
        stringResource(MR.strings.security_badge_contact_unverified_line)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (isDark) accentColor.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                AlertManager.shared.showAlertMsg(
                    title = alertTitle,
                    text = "$encryptionLine\n$contactLine"
                )
            }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(
                    when {
                        isVerified -> MR.images.ic_verified_user
                        encryption == SecurityBadgeEncryption.NOT_ENCRYPTED -> MR.images.ic_lock_open_right
                        else -> MR.images.ic_lock
                    }
                ),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = labelText,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
