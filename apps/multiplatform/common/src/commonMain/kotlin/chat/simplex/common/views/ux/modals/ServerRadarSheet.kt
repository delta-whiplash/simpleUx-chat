package chat.simplex.common.views.ux.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
fun ServerRadarSheet(
    isConnected: Boolean,
    onConfigureServers: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(20.dp)
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Color(0x44FFFFFF) else Color(0x22000000))
        )

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
            )
            Text(
                text = "État des Relais SMP SimpleX",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (isConnected)
                "Vos files de messages (SMP Queues) sont synchronisées en temps réel. Aucune fuite de métadonnées ni identifiant persistant."
            else
                "Connexion aux relais en cours d'établissement...",
            fontSize = 13.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )

        Spacer(Modifier.height(16.dp))

        // Diagnostic card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0x1F1E293B) else Color(0x0F0F172A))
                .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x14000000), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Protocole Réseau", fontSize = 13.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                Text("SimpleX SMP v2", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Transfert Fichiers", fontSize = 13.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                Text("XFTP Chiffré", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Isolation des Files", fontSize = 13.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                Text("100% Unidirectionnelle", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Configure servers button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2563EB))
                .clickable {
                    onConfigureServers()
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Configurer mes serveurs SMP",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        // Close button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0x1FFFFFFF) else Color(0x11000000))
                .clickable {
                    onClose()
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Fermer",
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
