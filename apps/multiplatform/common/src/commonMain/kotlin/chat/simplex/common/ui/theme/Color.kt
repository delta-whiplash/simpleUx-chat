package chat.simplex.common.ui.theme

import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.cos
import kotlin.math.sin

fun oklch(L: Float, C: Float, H: Float, alpha: Float = 1f): Color {
  val hRad = H * (Math.PI.toFloat() / 180f)
  return Color(L, C * cos(hRad), C * sin(hRad), alpha, ColorSpaces.Oklab)
}

val Indigo = Color(0xFF9966FF)
val SimplexBlue = Color(0, 136, 255, 255)  // If this value changes also need to update #0088ff in string resource files
val SimplexGreen = Color(77, 218, 103, 255)
val SecretColor = Color(0x40808080)
val LightGray = Color(241, 242, 246, 255)
val DarkGray = Color(43, 44, 46, 255)
val HighOrLowlight = Color(139, 135, 134, 255)
val MessagePreviewDark = Color(179, 175, 174, 255)
val MessagePreviewLight = Color(49, 45, 44, 255)
val ToolbarLight = Color(220, 220, 220, 12)
val ToolbarDark = Color(80, 80, 80, 12)
val SettingsSecondaryLight = Color(200, 196, 195, 90)
val GroupDark = Color(80, 80, 80, 60)
val IncomingCallLight = Color(239, 237, 236, 255)
val WarningOrange = Color(255, 127, 0, 255)
val WarningYellow = Color(255, 192, 0, 255)
val FileLight = Color(191, 194, 199, 255)
val FileDark = Color(94, 94, 98, 255)

val MenuTextColor: Color @Composable get () = if (isInDarkTheme()) LocalContentColor.current.copy(alpha = 0.8f) else Color.Black
val NoteFolderIconColor: Color @Composable get() = MaterialTheme.appColors.primaryVariant2

// Material 3 & Apple Liquid Glass Design Tokens
val GlassFrostedDark = Color(0xCC121622)
val GlassFrostedLight = Color(0xD9FFFFFF)
val GlassBorderDark = Color(0x33FFFFFF)
val GlassBorderLight = Color(0x24000000)
val GlassSpecularHighlight = Color(0x55FFFFFF)

// Accent Vibrancy Tokens
val TelegramBlue = Color(0xFF2AABEE)
val ElectricIndigo = Color(0xFF6366F1)
val EmeraldGreen = Color(0xFF10B981)
val CoralRed = Color(0xFFEF4444)
val AmberGold = Color(0xFFF59E0B)
val VioletPurple = Color(0xFF8B5CF6)

// Surface Container Tokens (Dark Mode)
val SurfaceContainerLowestDark = Color(0xFF090C12)
val SurfaceContainerLowDark = Color(0xFF0F141F)
val SurfaceContainerDark = Color(0xFF161C2A)
val SurfaceContainerHighDark = Color(0xFF1E2536)
val SurfaceContainerHighestDark = Color(0xFF283144)

// Surface Container Tokens (Light Mode)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF7F9FC)
val SurfaceContainerLight = Color(0xFFEFF2F8)
val SurfaceContainerHighLight = Color(0xFFE5EAF3)
val SurfaceContainerHighestLight = Color(0xFFD9E1ED)
