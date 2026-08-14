package chat.simplex.common.views.chat.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.GlassTokens
import chat.simplex.common.ui.theme.glassSurface

/**
 * Compact, centered security pill badge indicating E2EE status.
 *
 * Displays a lock icon + "End-to-end encryption active" text inside
 * a blue-tinted glass surface. Tapping opens detailed encryption info.
 *
 * Matches the HTML mockup's "Premium Security Badge Pill" component.
 */
@Composable
fun SecurityPillBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .glassSurface(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = GlassTokens.SecurityPillBg.copy(alpha = 0.12f),
                    borderColor = GlassTokens.SecurityPillBorder.copy(alpha = 0.25f)
                )
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = GlassTokens.SecurityPillIcon,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "End-to-end encryption active",
                color = GlassTokens.SecurityPillText,
                fontSize = 11.5.sp
            )
        }
    }
}
