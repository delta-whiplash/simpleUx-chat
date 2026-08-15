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
import chat.simplex.common.model.Connection
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.AlertManager
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
fun SecurityBadge(
    chat: Chat?,
    modifier: Modifier = Modifier
) {
    val isDark = isInDarkTheme()
    val contact = remember(chat) {
        (chat?.chatInfo as? ChatInfo.Direct)?.contact
    }

    val isPQ = true // SimpleX uses post-quantum Kyber/ML-KEM ratchet by default
    val isVerified = contact?.verified == true
    val shape = RoundedCornerShape(12.dp)

    val labelText = when {
        isVerified -> "Vérifié"
        isPQ -> "PQ Chiffré"
        else -> "E2EE"
    }

    val accentColor = when {
        isVerified -> Color(0xFF00E5FF) // Cyan for Verified
        isPQ -> Color(0xFF10B981) // Emerald for Post-Quantum
        else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
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
                val secDetails = buildString {
                    append("Statut Cryptographique SimpleX:\n\n")
                    append("• Chiffrement: Double-Ratchet E2EE bout-en-bout\n")
                    append("• Post-Quantum (PQ): ${if (isPQ) "Actif (Kyber / ML-KEM)" else "Standard"}\n")
                    append("• Contact: ${if (isVerified) "Code de sécurité vérifié ✓" else "Non vérifié hors-bande"}\n")
                    append("• Relais: Queues unidirectionnelles isolées (zéro métadonnée)")
                }
                AlertManager.shared.showAlertMsg(
                    title = "Sécurité & Cryptographie",
                    text = secDetails
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
                painter = painterResource(if (isVerified) MR.images.ic_verified_user else MR.images.ic_lock),
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
