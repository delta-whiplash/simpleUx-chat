package chat.simplex.common.ui.theme

import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import chat.simplex.common.platform.desktopPlatform
import chat.simplex.res.MR

actual val PlusJakartaSans: FontFamily = FontFamily(
  Font(MR.fonts.PlusJakartaSans.regular.file),
  Font(MR.fonts.PlusJakartaSans.italic.file, style = FontStyle.Italic),
  Font(MR.fonts.PlusJakartaSans.bold.file, FontWeight.Bold),
  Font(MR.fonts.PlusJakartaSans.semibold.file, FontWeight.SemiBold),
  Font(MR.fonts.PlusJakartaSans.medium.file, FontWeight.Medium),
  Font(MR.fonts.PlusJakartaSans.light.file, FontWeight.Light)
)

actual val EmojiFont: FontFamily = if (desktopPlatform.isMac()) {
  FontFamily.Default
} else {
  FontFamily(
    Font(MR.fonts.NotoColorEmoji.regular.file),
    Font(MR.fonts.NotoColorEmoji.regular.file, style = FontStyle.Italic),
    Font(MR.fonts.NotoColorEmoji.regular.file, FontWeight.Bold),
    Font(MR.fonts.NotoColorEmoji.regular.file, FontWeight.SemiBold),
    Font(MR.fonts.NotoColorEmoji.regular.file, FontWeight.Medium),
    Font(MR.fonts.NotoColorEmoji.regular.file, FontWeight.Light)
  )
}
