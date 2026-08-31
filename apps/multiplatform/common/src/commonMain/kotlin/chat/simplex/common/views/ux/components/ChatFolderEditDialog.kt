package chat.simplex.common.views.ux.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.ChatFolder
import chat.simplex.common.ui.theme.AmberGold
import chat.simplex.common.ui.theme.PlusJakartaSans
import chat.simplex.common.ui.theme.Slate600
import chat.simplex.common.views.ux.components.UxFilterCategory
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

// #98: Dialog for creating/editing a chat folder. Supports text name, emoji,
// and filter kind selection. All strings via MR.strings, colors via tokens.

@Composable
fun ChatFolderEditDialog(
  initialFolder: ChatFolder? = null,
  onDismiss: () -> Unit,
  onSave: (ChatFolder) -> Unit
) {
  var name by remember { mutableStateOf(initialFolder?.name ?: "") }
  var emoji by remember { mutableStateOf(initialFolder?.emoji ?: "") }
  var selectedFilterKind by remember {
    mutableIntStateOf(initialFolder?.filterKind ?: UxFilterCategory.ALL.ordinal)
  }
  var expanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initialFolder == null) {
          stringResource(MR.strings.chat_folders_create_new)
        } else {
          stringResource(MR.strings.chat_folders_edit)
        },
        style = MaterialTheme.typography.h6.copy(fontFamily = PlusJakartaSans)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Name input
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = {
            Text(
              text = stringResource(MR.strings.chat_folders_name_hint),
              fontFamily = PlusJakartaSans,
              fontSize = 13.sp
            )
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        // Emoji input
        OutlinedTextField(
          value = emoji,
          onValueChange = { emoji = it },
          label = {
            Text(
              text = stringResource(MR.strings.chat_folders_emoji_hint),
              fontFamily = PlusJakartaSans,
              fontSize = 13.sp
            )
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
        )

        // Filter kind dropdown
        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = !expanded }
        ) {
          OutlinedTextField(
            value = UxFilterCategory.entries[selectedFilterKind].localizedLabel(),
            onValueChange = {},
            readOnly = true,
            label = {
              Text(
                text = stringResource(MR.strings.chat_folders_filter_kind),
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp
              )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
          )

          ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
          ) {
            UxFilterCategory.entries.forEach { category ->
              DropdownMenuItem(
                onClick = {
                  selectedFilterKind = category.ordinal
                  expanded = false
                }
              ) {
                Text(
                  text = category.localizedLabel(),
                  fontFamily = PlusJakartaSans,
                  fontSize = 13.sp,
                  color = if (category.ordinal == selectedFilterKind) AmberGold else Slate600
                )
              }
            }
          }
        }
      }
    },
    buttons = {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = onDismiss) {
          Text(
            text = "Cancel",
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
            color = Slate600
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(
          onClick = {
            val folder = ChatFolder(
              id = initialFolder?.id ?: "folder_${System.currentTimeMillis()}",
              name = name.ifEmpty { null },
              emoji = emoji.ifEmpty { null },
              filterKind = selectedFilterKind,
              isVisible = initialFolder?.isVisible ?: true,
              order = initialFolder?.order ?: 0
            )
            onSave(folder)
          },
          enabled = name.isNotEmpty() || emoji.isNotEmpty()
        ) {
          Text(
            text = "Save",
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
            color = AmberGold,
            fontWeight = MaterialTheme.typography.button.fontWeight
          )
        }
      }
    }
  )
}
