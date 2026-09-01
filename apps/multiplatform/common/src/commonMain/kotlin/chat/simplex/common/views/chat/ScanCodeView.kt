package chat.simplex.common.views.chat

import SectionBottomSpacer
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.ColumnWithScrollBar
import chat.simplex.common.ui.theme.Coral500
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.QRCodeScanner
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

@Composable
fun ScanCodeView(verifyCode: suspend (String?) -> Boolean, close: () -> Unit) {
  // Inline failure hint instead of an OK dialog per failed attempt (#67): the scanner
  // stays open, the hint auto-clears so a retry isn't prefaced by stale state.
  var showFailureHint by remember { mutableStateOf(false) }
  LaunchedEffect(showFailureHint) {
    if (showFailureHint) {
      delay(5000)
      showFailureHint = false
    }
  }
  ColumnWithScrollBar {
    AppBarTitle(stringResource(MR.strings.scan_code))
    QRCodeScanner { text ->
      val success  = verifyCode(text)
      if (success) {
        close()
      } else {
        showFailureHint = true
      }
      success
    }
    if (showFailureHint) {
      Text(
        stringResource(MR.strings.incorrect_code),
        Modifier.padding(horizontal = DEFAULT_PADDING),
        color = Coral500,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
      )
    }
    Text(stringResource(MR.strings.scan_code_from_contacts_app), Modifier.padding(horizontal = DEFAULT_PADDING))
    SectionBottomSpacer()
  }
}
