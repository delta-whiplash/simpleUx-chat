package chat.simplex.common.views.chat.item

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.*

@Composable
fun CIEventView(text: AnnotatedString) {
  Box(
    Modifier
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    Box(
      Modifier
        .clip(CornerPill)
        .background(if (isInDarkTheme()) Color(0xCC141B28) else Color(0xEEF1F5F9))
        .border(
          width = 1.dp,
          color = if (isInDarkTheme()) Color(0x33FFFFFF) else Color(0x1A000000),
          shape = CornerPill
        )
        .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
      Text(
        text,
        style = MaterialTheme.typography.body2.copy(
          fontSize = 12.sp,
          lineHeight = 16.sp,
          textAlign = TextAlign.Center
        ),
        color = if (isInDarkTheme()) Color(0xFFCBD5E1) else Color(0xFF475569),
        maxLines = 4
      )
    }
  }
}
@Preview/*(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  name = "Dark Mode"
)*/
@Composable
fun CIEventViewPreview() {
  SimpleXTheme {
    CIEventView(buildAnnotatedString { append("event happened") })
  }
}
