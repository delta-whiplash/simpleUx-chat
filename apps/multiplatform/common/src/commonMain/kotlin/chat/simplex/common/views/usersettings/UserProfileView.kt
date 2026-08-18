package chat.simplex.common.views.usersettings

import SectionBottomSpacer
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI

@Composable
fun UserProfileView(
  chatModel: ChatModel,
  close: () -> Unit,
  onNavigateToSettings: (() -> Unit)? = null
) {
  val u = remember { chatModel.currentUser }
  val user = u.value
  KeyChangeEffect(u.value?.remoteHostId, u.value?.userId) {
    close()
  }

  if (user != null) {
    var profile by remember { mutableStateOf(user.profile.toProfile()) }
    UserProfileLayout(
      profile = profile,
      close = close,
      onNavigateToSettings = onNavigateToSettings,
      saveProfile = { displayName, fullName, shortDescr, description, image ->
        withBGApi {
          val updatedProfile = profile.copy(
            displayName = displayName.trim(),
            fullName = fullName.trim(),
            shortDescr = shortDescr.trim().ifEmpty { null },
            description = description.trim().ifEmpty { null },
            image = image
          )
          val updated = chatModel.controller.apiUpdateProfile(user.remoteHostId, updatedProfile)
          if (updated != null) {
            val (newProfile, _) = updated
            chatModel.updateCurrentUser(user.remoteHostId, newProfile)
            profile = newProfile
            close()
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
  onNavigateToSettings: (() -> Unit)? = null,
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

  ModalBottomSheetLayout(
    modifier = Modifier.fillMaxSize().clipToBounds(),
    scrimColor = Color.Black.copy(alpha = 0.5F),
    sheetContent = {
      GetImageBottomSheet(
        chosenImage,
        onImageChange = { bitmap -> profileImage.value = resizeImageToStrSize(cropToSquare(bitmap), maxDataSize = 12500) },
        hideBottomSheet = {
          scope.launch { bottomSheetModalState.hide() }
        }
      )
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
        delay(200)
        descrFocusRequester.requestFocus()
      }
    }

    if (editingDescription) {
      Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background).imePadding()) {
        DefaultAppBar(
          navigationButton = { NavigationButtonBack(onButtonClicked = { editingDescription = false }) },
          fixedTitleText = generalGetString(MR.strings.profile_description__field),
          onTop = true
        )
        Column(Modifier.fillMaxSize().padding(horizontal = DEFAULT_PADDING)) {
          Box(Modifier.weight(1f, fill = false).padding(top = DEFAULT_PADDING, bottom = DEFAULT_PADDING)) {
            TextEditor(
              description,
              Modifier.heightIn(min = 140.dp),
              placeholder = stringResource(MR.strings.enter_description_optional),
              contentPadding = PaddingValues(),
              focusRequester = descrFocusRequester,
              maxLines = Int.MAX_VALUE
            )
          }
          Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
      }
    } else {
      Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        DefaultAppBar(
          navigationButton = { NavigationButtonBack(onButtonClicked = { onClose(close) }) },
          fixedTitleText = "Votre profil",
          onTop = true
        )
        ColumnWithScrollBarNoAppBar(
          Modifier
            .padding(horizontal = DEFAULT_PADDING),
        ) {
          Spacer(Modifier.height(8.dp))

          Column(Modifier.fillMaxWidth()) {
            // Avatar with Photo Badge in bottom-right corner
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
                    contentDescription = "Changer la photo",
                    tint = if (isDark) Color(0xFF0F172A) else Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }

            // Profile Text Box 1: Display Name
            ProfileTextBox(
              value = displayName,
              label = "Nom de profil",
              placeholder = "Entrez votre nom de profil",
              focusRequester = focusRequester,
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
                label = "Nom complet",
                placeholder = "Nom complet"
              )
            }

            Spacer(Modifier.height(14.dp))

            // Profile Text Box 2: Bio / Short Description
            ProfileTextBox(
              value = shortDescr,
              label = "Bio / Statut",
              placeholder = "Statut ou bio courte (ex: Dispo pour discuter)",
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

            // Profile Description Card
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0x661E293B) else Color(0xF2F1F5F9))
                .border(1.dp, if (isDark) Color(0x2AFFFFFF) else Color(0x18000000), RoundedCornerShape(14.dp))
                .clickable { editingDescription = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
              Column {
                Text(
                  text = "Description détaillée",
                  style = TextStyle(
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                  )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                  text = if (description.value.isBlank()) "Ajouter une description complète (optionnel)..." else description.value,
                  style = TextStyle(
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    color = if (description.value.isBlank()) (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)) else (if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A))
                  ),
                  maxLines = 3
                )
              }
            }

            Spacer(Modifier.height(24.dp))

            // Save and Notify Contacts Button
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
                text = if (enabled) "Enregistrer les modifications" else "Aucune modification",
                style = TextStyle(
                  fontFamily = Inter,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
              )
            }

            Spacer(Modifier.height(20.dp))

            // Connection Sharing Actions (Moved from Contacts to Profile)
            Text(
              text = "Connexions & Liens",
              style = TextStyle(
                fontFamily = Inter,
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
                    text = "Créer un lien unique",
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                      fontFamily = Inter,
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
                    text = "Scanner / Coller un lien",
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                      fontFamily = Inter,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(100.dp))
        }
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
        fontFamily = Inter,
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
            fontFamily = Inter,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
          ),
          singleLine = singleLine,
          maxLines = maxLines,
          cursorBrush = SolidColor(if (isDark) Color(0xFFE2B755) else Color(0xFFD97706)),
          decorationBox = { innerTextField ->
            if (value.value.isEmpty() && placeholder.isNotEmpty()) {
              Text(
                text = placeholder,
                style = TextStyle(
                  fontFamily = Inter,
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

@Preview
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

