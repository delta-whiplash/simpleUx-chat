package chat.simplex.common.views.ux.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.AppPlatform
import chat.simplex.common.platform.ChatFolder
import chat.simplex.common.platform.appPlatform
import chat.simplex.common.ui.theme.AmberGold
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.ui.theme.Slate300
import chat.simplex.common.ui.theme.Slate400
import chat.simplex.common.ui.theme.Slate500
import chat.simplex.common.ui.theme.Slate600
import chat.simplex.common.ui.theme.Slate900
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

// #98: rename / set an emoji for a preset folder. Custom overlay card instead
// of AlertDialog: identical airy spacing whether the keyboard is open or not
// (Delta feedback 2026-08-31 - AlertDialog compacted when idle and reflowed
// under the IME). Empty name/emoji = back to the preset label.
@Composable
fun ChatFolderEditDialog(
  initialFolder: ChatFolder? = null,
  onDismiss: () -> Unit,
  onSave: (ChatFolder) -> Unit
) {
  var name by remember { mutableStateOf(initialFolder?.name ?: "") }
  var emoji by remember { mutableStateOf(initialFolder?.emoji ?: "") }
  val isDark = isInDarkTheme()

  if (appPlatform == AppPlatform.ANDROID) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.5f))
      .imePadding()
      .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colors.surface,
      border = BorderStroke(1.dp, if (isDark) Slate400.copy(alpha = 0.2f) else Slate300),
      elevation = 8.dp,
      modifier = Modifier
        .padding(horizontal = 24.dp)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { /* consume taps so the backdrop doesn't dismiss */ }
    ) {
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
      ) {
        Text(
          text = stringResource(MR.strings.chat_folders_edit),
          fontFamily = PlusJakartaSans,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colors.onSurface
        )

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = {
            Text(
              text = stringResource(MR.strings.chat_folders_name_hint),
              fontFamily = PlusJakartaSans,
              fontSize = 14.sp
            )
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        OutlinedTextField(
          value = emoji,
          onValueChange = { emoji = it },
          label = {
            Text(
              text = stringResource(MR.strings.chat_folders_emoji_hint),
              fontFamily = PlusJakartaSans,
              fontSize = 14.sp
            )
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text(
              text = stringResource(MR.strings.cancel_verb),
              fontFamily = PlusJakartaSans,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              color = if (isDark) Slate300 else Slate600
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Button(
            onClick = {
              onSave(
                (initialFolder ?: ChatFolder(id = "all", filterKind = 0)).copy(
                  name = name.ifEmpty { null },
                  emoji = emoji.ifEmpty { null }
                )
              )
            },
            colors = ButtonDefaults.buttonColors(
              backgroundColor = AmberGold,
              contentColor = if (isDark) Slate900 else Color.White,
              disabledBackgroundColor = Slate400.copy(alpha = 0.3f),
              disabledContentColor = Slate500
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
          ) {
            Text(
              text = stringResource(MR.strings.save_verb),
              fontFamily = PlusJakartaSans,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
