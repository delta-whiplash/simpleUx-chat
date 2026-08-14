package chat.simplex.common.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.ui.unit.dp

val CornerExtraSmall = RoundedCornerShape(6.dp)
val Corner8 = RoundedCornerShape(8.dp)
val CornerSmall = RoundedCornerShape(10.dp)
val Corner12 = RoundedCornerShape(12.dp)
val Corner16 = RoundedCornerShape(16.dp)
val Corner18 = RoundedCornerShape(18.dp)
val CornerMedium = RoundedCornerShape(16.dp)
val CornerLarge = RoundedCornerShape(22.dp)
val Corner24 = RoundedCornerShape(24.dp)
val CornerExtraLarge = RoundedCornerShape(28.dp)
val CornerPill = RoundedCornerShape(percent = 50)

// Telegram-style continuous asymmetric bubble shapes
val SentBubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
val ReceivedBubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp)
val GroupedCardShape = RoundedCornerShape(18.dp)
val FloatingDockShape = RoundedCornerShape(32.dp)

val Shapes = Shapes(
  small = CornerSmall,
  medium = CornerMedium,
  large = CornerLarge
)
