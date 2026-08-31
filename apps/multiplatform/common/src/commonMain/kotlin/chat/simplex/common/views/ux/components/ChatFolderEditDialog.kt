package chat.simplex.common.views.ux.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.ChatFolder
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

// #98: rename / set an emoji for a preset folder. No filter-kind picker: the
// five presets have fixed predicates, creation of new folders comes later
// together with chat assignment. Empty name/emoji = back to the preset label.
@Composable
fun ChatFolderEditDialog(
  initialFolder: ChatFolder? = null,
  onDismiss: () -> Unit,
  onSave: (ChatFolder) -> Unit
) {
  var name by remember { mutableStateOf(initialFolder?.name ?: "") }
  var emoji by remember { mutableStateOf(initialFolder?.emoji ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(MR.strings.chat_folders_edit),
        style = MaterialTheme.typography.h6.copy(fontFamily = PlusJakartaSans)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
      ) {
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
      }
    },
    buttons = {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(onClick = onDismiss) {
          Text(
            text = stringResource(MR.strings.cancel_verb),
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isInDarkTheme()) Slate300 else Slate600
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
            contentColor = if (isInDarkTheme()) Slate900 else Color.White,
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
  )
}
