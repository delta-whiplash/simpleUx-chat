package chat.simplex.common.views.chat.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.GlassTokens
import chat.simplex.common.ui.theme.glassSurface

/**
 * Glassmorphism-styled top app bar for the chat screen.
 *
 * Features:
 * - Frosted glass pill container with shimmer border
 * - Gradient avatar circle showing group/contact initials
 * - Online status indicator (green pulsing dot)
 * - Back, call, and more action buttons
 *
 * This bar is rendered instead of the default [ChatInfoToolbar]
 * when glass mode is active.
 */
@Composable
fun GlassTopAppBar(
    displayName: String,
    statusText: String,
    onBackClick: () -> Unit,
    onTitleClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .glassSurface(
                shape = RoundedCornerShape(28.dp),
                backgroundColor = Color.White.copy(alpha = 0.07f),
                borderColor = Color.White.copy(alpha = 0.15f)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Avatar + Name + Status (clickable for info)
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onTitleClick() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gradient avatar with initials
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(GlassTokens.OrbIndigo, GlassTokens.OrbViolet)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName
                        .split(" ", limit = 2)
                        .joinToString("") { it.take(1) }
                        .uppercase()
                        .take(2),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Online indicator dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GlassTokens.OnlineGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Call button (only if callable)
        if (onCallClick != null) {
            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Secure audio call",
                    tint = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // More options
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
