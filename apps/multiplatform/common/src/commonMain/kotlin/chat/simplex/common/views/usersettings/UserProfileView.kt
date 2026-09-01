package chat.simplex.common.views.usersettings

import SectionBottomSpacer
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.onboarding.ReadableText
import chat.simplex.common.platform.*
import chat.simplex.common.views.*
import chat.simplex.common.views.newchat.*
import chat.simplex.res.MR
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI

@Composable
fun UserProfileView(chatModel: ChatModel, close: () -> Unit) {
  val u = remember {chatModel.currentUser}
  val user = u.value
  KeyChangeEffect(u.value?.remoteHostId, u.value?.userId) {
    close()
  }

  if (user != null) {
    var profile by remember { mutableStateOf(user.profile.toProfile()) }
    UserProfileLayout(
      profile = profile,
      close = close,
      saveProfile = { displayName, fullName, shortDescr, description, image ->
        withBGApi {
          val updatedProfile = profile.copy(displayName = displayName.trim(), fullName = fullName.trim(), shortDescr = shortDescr.trim().ifEmpty { null }, description = description.trim().ifEmpty { null }, image = image)
          val updated = chatModel.controller.apiUpdateProfile(user.remoteHostId, updatedProfile)
          if (updated != null) {
            val (newProfile, _) = updated
            chatModel.updateCurrentUser(user.remoteHostId, newProfile)
            // FB-6: stay on the profile page after a successful save (local state already
            // reflects the saved values, so the save button disables and back navigation
            // won't prompt about unsaved changes).
            profile = newProfile
          }
        }
      }
    )
  }
}

@Composable
fun UserProfileLayout(
  profile: Profile,
  close: () -> Unit,
  saveProfile: (String, String, String, String, String?) -> Unit,
) {
  val isDark = isInDarkTheme()
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val displayName = remember { mutableStateOf(profile.displayName) }
  val fullName = remember { mutableStateOf(profile.fullName) }
  val shortDescr = remember { mutableStateOf(profile.shortDescr ?: "") }
  val description = remember { mutableStateOf(profile.description ?: "") }
  val chosenImage = rememberSaveable { mutableStateOf<URI?>(null) }
  val profileImage = rememberSaveable { mutableStateOf(profile.image) }
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  val keyboardState by getKeyboardState()
  var savedKeyboardState by remember { mutableStateOf(keyboardState) }
  val focusRequester = remember { FocusRequester() }
  val descrFocusRequester = remember { FocusRequester() }
  var editingDescription by remember { mutableStateOf(false) }
  var descrHadFocus by remember { mutableStateOf(false) }
    ModalBottomSheetLayout(
      scrimColor = Color.Black.copy(alpha = 0.12F),
      sheetContent = {
        GetImageBottomSheet(
          chosenImage,
          onImageChange = { bitmap -> profileImage.value = resizeImageToStrSize(cropToSquare(bitmap), maxDataSize = 12500) },
          hideBottomSheet = {
            scope.launch { bottomSheetModalState.hide() }
          })
      },
      sheetState = bottomSheetModalState,
      sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
      fun dataUnchanged(): Boolean =
        displayName.value.trim() == profile.displayName &&
            fullName.value.trim() == profile.fullName &&
            shortDescr.value.trim() == (profile.shortDescr ?: "") &&
            description.value.trim() == (profile.description ?: "") &&
            profile.image == profileImage.value
      fun onClose(close: () -> Unit): Boolean = if (dataUnchanged() || !canSaveProfile(displayName.value, shortDescr.value, profile)) {
        chatModel.centerPanelBackgroundClickHandler = null
        close()
        false
      } else {
        showUnsavedChangesAlert(
          {
            chatModel.centerPanelBackgroundClickHandler = null
            saveProfile(displayName.value, fullName.value, shortDescr.value, description.value, profileImage.value)
          },
          {
            chatModel.centerPanelBackgroundClickHandler = null
            close()
          }
        )
        true
      }
      DisposableEffect(Unit) {
        onDispose { chatModel.centerPanelBackgroundClickHandler = null }
      }
      LaunchedEffect(Unit) {
        chatModel.centerPanelBackgroundClickHandler = {
          onClose(close = { ModalManager.start.closeModals() })
        }
      }
      LaunchedEffect(editingDescription) {
        if (editingDescription) {
          descrHadFocus = false
          delay(200)
          descrFocusRequester.requestFocus()
        }
      }
      ModalView(close = { onClose(close) }) {
        ColumnWithScrollBar(
          Modifier
            .padding(horizontal = DEFAULT_PADDING),
        ) {
          // FB-15: SimpleUX title copy ("Your profile") instead of upstream's your_current_profile
          AppBarTitle(stringResource(MR.strings.context_user_picker_your_profile), withPadding = false)
          ReadableText(generalGetString(MR.strings.your_profile_is_stored_on_device_and_shared_only_with_contacts_simplex_cannot_see_it), TextAlign.Center)
          Column(
            Modifier
              .fillMaxWidth()
          ) {
            // Avatar with Photo Badge in bottom-right corner (SimpleUX restyle)
            Box(
              Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier.size(108.dp),
                contentAlignment = Alignment.Center
              ) {
                // Main Avatar Circle
                Box(
                  modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .clickable { scope.launch { bottomSheetModalState.show() } }
                    .border(
                      width = 2.dp,
                      brush = Brush.linearGradient(
                        if (isDark) listOf(Color(0xFFE2B755), Color(0xFFD97706))
                        else listOf(Color(0xFFD97706), Color(0xFFB45309))
                      ),
                      shape = CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  ProfileImage(
                    size = 104.dp,
                    image = profileImage.value,
                    name = displayName.value
                  )
                }

                // Delete Photo Button in top-right corner if image present
                if (profileImage.value != null) {
                  Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    DeleteImageButton { profileImage.value = null }
                  }
                }

                // Camera Badge in bottom-right corner
                Box(
                  modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFFE2B755) else Color(0xFFD97706))
                    .border(2.dp, if (isDark) Color(0xFF0F172A) else Color.White, CircleShape)
                    .clickable { scope.launch { bottomSheetModalState.show() } },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    painter = painterResource(MR.images.ic_photo_camera),
                    contentDescription = stringResource(MR.strings.icon_descr_change_photo),
                    tint = if (isDark) Color(0xFF0F172A) else Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }

            // Profile Text Box 1: Display Name
            // keyboardOptions restores the IME behavior upstream's ProfileNameField carried
            // (no autocorrect / no capitalization for name and bio fields)
            ProfileTextBox(
              value = displayName,
              label = stringResource(MR.strings.profile_name_label),
              placeholder = stringResource(MR.strings.profile_name_placeholder),
              focusRequester = focusRequester,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrect = false),
              isValid = { isValidNewProfileName(it, profile) },
              trailingIcon = if (!isValidNewProfileName(displayName.value, profile)) {
                {
                  IconButton({ showInvalidNameAlert(mkValidName(displayName.value), displayName) }, Modifier.size(20.dp)) {
                    Icon(painterResource(MR.images.ic_info), null, tint = MaterialTheme.colors.error)
                  }
                }
              } else null
            )

            if (showFullName(profile)) {
              Spacer(Modifier.height(14.dp))
              ProfileTextBox(
                value = fullName,
                label = stringResource(MR.strings.profile_full_name_label),
                placeholder = stringResource(MR.strings.profile_full_name_label),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrect = false)
              )
            }

            Spacer(Modifier.height(14.dp))

            // Profile Text Box 2: Bio / Short Description
            ProfileTextBox(
              value = shortDescr,
              label = stringResource(MR.strings.profile_bio_label),
              placeholder = stringResource(MR.strings.profile_bio_placeholder),
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrect = false),
              isValid = { bioFitsLimit(it) },
              trailingIcon = if (!bioFitsLimit(shortDescr.value)) {
                {
                  IconButton(
                    onClick = { AlertManager.shared.showAlertMsg(title = generalGetString(MR.strings.bio_too_large)) },
                    Modifier.size(20.dp)
                  ) {
                    Icon(painterResource(MR.images.ic_info), null, tint = MaterialTheme.colors.error)
                  }
                }
              } else null
            )

            Spacer(Modifier.height(14.dp))

            // Profile Description Card (SimpleUX restyle of upstream's add/edit description link).
            // #64: expands IN PLACE into the editor instead of swapping the modal for a full editor
            // page; collapse on the chevron or on focus loss (value stays in `description`, saved
            // via the Save button / unsaved-changes alert like the other fields).
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0x661E293B) else Color(0xF2F1F5F9))
                .border(1.dp, if (isDark) Color(0x2AFFFFFF) else Color(0x18000000), RoundedCornerShape(14.dp))
                .then(if (editingDescription) Modifier else Modifier.clickable { editingDescription = true })
                .onFocusChanged { focusState ->
                  if (focusState.hasFocus) {
                    descrHadFocus = true
                  } else if (descrHadFocus && editingDescription) {
                    editingDescription = false
                  }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = stringResource(MR.strings.profile_detailed_description),
                    style = TextStyle(
                      fontFamily = PlusJakartaSans,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    ),
                    modifier = Modifier.weight(1f)
                  )
                  if (editingDescription) {
                    IconButton(onClick = { editingDescription = false }, modifier = Modifier.size(24.dp)) {
                      Icon(
                        painter = painterResource(MR.images.ic_arrow_upward),
                        contentDescription = stringResource(MR.strings.profile_detailed_description),
                        tint = if (isDark) Color(0xFFE2B755) else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                }
                Spacer(Modifier.height(4.dp))
                if (editingDescription) {
                  TextEditor(
                    description,
                    Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    placeholder = stringResource(MR.strings.enter_description_optional),
                    contentPadding = PaddingValues(),
                    focusRequester = descrFocusRequester,
                    maxLines = Int.MAX_VALUE
                  )
                } else {
                  Text(
                    text = if (description.value.isBlank()) stringResource(MR.strings.profile_detailed_description_hint) else description.value,
                    style = TextStyle(
                      fontFamily = PlusJakartaSans,
                      fontSize = 14.sp,
                      color = if (description.value.isBlank()) (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)) else (if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A))
                    ),
                    maxLines = 3
                  )
                }
              }
            }

            Spacer(Modifier.height(24.dp))

            // Save and Notify Contacts Button (SimpleUX button row; upstream uses a text link)
            val enabled = !dataUnchanged() && canSaveProfile(displayName.value, shortDescr.value, profile)
            Button(
              onClick = {
                if (enabled) {
                  saveProfile(displayName.value, fullName.value, shortDescr.value, description.value, profileImage.value)
                }
              },
              enabled = enabled,
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isDark) Color(0xFFE2B755) else Color(0xFFD97706),
                disabledBackgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                contentColor = if (isDark) Color(0xFF0F172A) else Color.White,
                disabledContentColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
              ),
              modifier = Modifier.fillMaxWidth().height(48.dp),
              elevation = ButtonDefaults.elevation(defaultElevation = if (enabled) 4.dp else 0.dp)
            ) {
              Text(
                text = if (enabled) stringResource(MR.strings.profile_save_changes) else stringResource(MR.strings.profile_no_changes),
                style = TextStyle(
                  fontFamily = PlusJakartaSans,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
              )
            }

            Spacer(Modifier.height(20.dp))

            // Connection Sharing Actions (SimpleUX addition; moved from Contacts to Profile)
            Text(
              text = stringResource(MR.strings.profile_connections_links),
              style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
              ),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Card 1: Create 1-time link
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0x331E293B) else Color(0xF0F1F5F9),
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    val closeAll = { ModalManager.start.closeModals() }
                    ModalManager.start.showModalCloseable(endButtons = { AddContactLearnMoreButton() }) { _ ->
                      NewChatView(chatModel.currentRemoteHost.value, NewChatOption.INVITE, close = closeAll)
                    }
                  }
                  .border(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x14000000), RoundedCornerShape(16.dp))
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    painterResource(MR.images.ic_add_link),
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFE2B755) else Color(0xFFD97706),
                    modifier = Modifier.size(26.dp)
                  )
                  Spacer(Modifier.height(8.dp))
                  Text(
                    text = stringResource(MR.strings.profile_create_one_time_link),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                      fontFamily = PlusJakartaSans,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                  )
                }
              }

              // Card 2: Scan / Paste link
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0x331E293B) else Color(0xF0F1F5F9),
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    val closeAll = { ModalManager.start.closeModals() }
                    ModalManager.start.showModalCloseable(endButtons = { AddContactLearnMoreButton() }) { _ ->
                      NewChatView(chatModel.currentRemoteHost.value, NewChatOption.CONNECT, showQRCodeScanner = appPlatform.isAndroid, close = closeAll)
                    }
                  }
                  .border(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x14000000), RoundedCornerShape(16.dp))
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    painterResource(MR.images.ic_qr_code),
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFE2B755) else Color(0xFFD97706),
                    modifier = Modifier.size(26.dp)
                  )
                  Spacer(Modifier.height(8.dp))
                  Text(
                    text = stringResource(MR.strings.profile_scan_paste_link),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                      fontFamily = PlusJakartaSans,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                  )
                }
              }
            }
          }
          Spacer(Modifier.height(DEFAULT_BOTTOM_BUTTON_PADDING))
          if (savedKeyboardState != keyboardState) {
            LaunchedEffect(keyboardState) {
              scope.launch {
                savedKeyboardState = keyboardState
                scrollState.animateScrollTo(scrollState.maxValue)
              }
            }
          }
          SectionBottomSpacer()
        }
      }
    }
}

@Composable
fun ProfileTextBox(
  value: MutableState<String>,
  placeholder: String = "",
  label: String,
  singleLine: Boolean = true,
  maxLines: Int = 1,
  focusRequester: FocusRequester? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  isValid: (String) -> Boolean = { true },
  trailingIcon: (@Composable () -> Unit)? = null
) {
  val isDark = isInDarkTheme()
  var focused by remember { mutableStateOf(false) }
  val valid = isValid(value.value)
  val shape = RoundedCornerShape(14.dp)

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = label,
      style = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (focused) (if (isDark) Color(0xFFE2B755) else Color(0xFFD97706)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
      ),
      modifier = Modifier.padding(bottom = 6.dp)
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(if (isDark) Color(0x661E293B) else Color(0xF2F1F5F9))
        .border(
          width = if (focused) 1.5.dp else 1.dp,
          color = if (!valid) MaterialTheme.colors.error
                  else if (focused) (if (isDark) Color(0xFFE2B755) else Color(0xFFD97706))
                  else (if (isDark) Color(0x2AFFFFFF) else Color(0x18000000)),
          shape = shape
        )
        .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        BasicTextField(
          value = value.value,
          onValueChange = { value.value = it },
          modifier = Modifier
            .weight(1f)
            .onFocusChanged { focused = it.isFocused }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
          textStyle = TextStyle(
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
          ),
          keyboardOptions = keyboardOptions,
          singleLine = singleLine,
          maxLines = maxLines,
          cursorBrush = SolidColor(if (isDark) Color(0xFFE2B755) else Color(0xFFD97706)),
          decorationBox = { innerTextField ->
            if (value.value.isEmpty() && placeholder.isNotEmpty()) {
              Text(
                text = placeholder,
                style = TextStyle(
                  fontFamily = PlusJakartaSans,
                  fontSize = 15.sp,
                  color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
              )
            }
            innerTextField()
          }
        )

        if (trailingIcon != null) {
          Spacer(Modifier.width(8.dp))
          trailingIcon()
        }
      }
    }
  }
}

@Composable
fun EditImageButton(click: () -> Unit) {
  IconButton(
    onClick = click,
    modifier = Modifier.size(30.dp)
  ) {
    Icon(
      painterResource(MR.images.ic_photo_camera),
      contentDescription = stringResource(MR.strings.edit_image),
      tint = MaterialTheme.colors.primary,
      modifier = Modifier.size(30.dp)
    )
  }
}

@Composable
fun DeleteImageButton(click: () -> Unit) {
  IconButton(onClick = click) {
    Icon(
      painterResource(MR.images.ic_close),
      contentDescription = stringResource(MR.strings.delete_image),
      tint = MaterialTheme.colors.primary,
    )
  }
}

private fun showUnsavedChangesAlert(save: () -> Unit, revert: () -> Unit) {
  AlertManager.shared.showAlertDialogStacked(
    title = generalGetString(MR.strings.save_preferences_question),
    confirmText = generalGetString(MR.strings.save_and_notify_contacts),
    dismissText = generalGetString(MR.strings.exit_without_saving),
    onConfirm = save,
    onDismiss = revert,
  )
}

private fun isValidNewProfileName(displayName: String, profile: Profile): Boolean =
  displayName == profile.displayName || isValidDisplayName(displayName.trim())

private fun showFullName(profile: Profile): Boolean =
  profile.fullName.trim().isNotEmpty() && profile.fullName.trim() != profile.displayName.trim()

private fun canSaveProfile(displayName: String, shortDescr: String, profile: Profile): Boolean =
  displayName.trim().isNotEmpty() && isValidNewProfileName(displayName, profile) && bioFitsLimit(shortDescr)

@Preview/*(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)*/
@Composable
fun PreviewUserProfileLayoutEditOff() {
  SimpleXTheme {
    UserProfileLayout(
      profile = Profile.sampleData,
      close = {},
      saveProfile = { _, _, _, _, _ -> }
    )
  }
}

@Preview/*(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)*/
@Composable
fun PreviewUserProfileLayoutEditOn() {
  SimpleXTheme {
    UserProfileLayout(
      profile = Profile.sampleData,
      close = {},
      saveProfile = { _, _, _, _, _ -> }
    )
  }
}
