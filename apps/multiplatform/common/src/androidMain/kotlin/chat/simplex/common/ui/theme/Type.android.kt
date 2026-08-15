package chat.simplex.common.ui.theme

import androidx.compose.ui.text.font.*
import chat.simplex.res.MR

actual val Inter: FontFamily = FontFamily(
  Font(MR.fonts.PlusJakartaSans.regular.fontResourceId),
  Font(MR.fonts.PlusJakartaSans.italic.fontResourceId, style = FontStyle.Italic),
  Font(MR.fonts.PlusJakartaSans.bold.fontResourceId, FontWeight.Bold),
  Font(MR.fonts.PlusJakartaSans.semibold.fontResourceId, FontWeight.SemiBold),
  Font(MR.fonts.PlusJakartaSans.medium.fontResourceId, FontWeight.Medium),
  Font(MR.fonts.PlusJakartaSans.light.fontResourceId, FontWeight.Light)
)

actual val EmojiFont: FontFamily = FontFamily.Default

