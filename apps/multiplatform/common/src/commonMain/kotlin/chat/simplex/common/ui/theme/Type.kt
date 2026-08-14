package chat.simplex.common.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
  h1 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    letterSpacing = (-0.5).sp
  ),
  h2 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    letterSpacing = (-0.3).sp
  ),
  h3 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    letterSpacing = (-0.2).sp
  ),
  h4 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Medium,
    fontSize = 16.5.sp,
    letterSpacing = (-0.1).sp
  ),
  body1 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 15.5.sp,
    letterSpacing = (-0.1).sp
  ),
  body2 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp,
    letterSpacing = (-0.05).sp
  ),
  button = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    letterSpacing = 0.1.sp
  ),
  caption = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 12.5.sp
  )
)
