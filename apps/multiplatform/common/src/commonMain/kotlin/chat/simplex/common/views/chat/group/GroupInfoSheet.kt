package chat.simplex.common.views.chat.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.*
import chat.simplex.common.views.chat.item.MarkdownText
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.*
import chat.simplex.common.views.ux.components.MineralEditSheet
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * #64: compact edit sheets hosted over the group/channel info modal, replacing the
 * dedicated full pages (GroupProfileView / GroupWelcomeView / ChannelWebPageView).
 * The save logic is the same as those pages carried; only the page wrapper is gone.
 */
enum class GroupInfoSheetKind { PROFILE, WELCOME_MESSAGE, WEB_PAGE }

/**
 * Dismissal guard set by the active sheet body. M2's ModalBottomSheetLayout has no dismiss
 * callback, so the host state's `confirmValueChange` vetoes user-initiated hides (scrim tap,
 * drag down) and hands the decision to the body: hide right away when nothing changed, or
 * ask about unsaved changes first.
 */
internal fun interface GroupInfoSheetDismiss {
  fun requestDismiss()
}

@Composable
fun ModalData.GroupInfoSheetHost(
  rhId: Long?,
  groupInfo: GroupInfo,
  sheetKind: MutableState<GroupInfoSheetKind?>,
  content: @Composable () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val dismissRequest = remember { mutableStateOf<GroupInfoSheetDismiss?>(null) }
  // Remember the lambda so rememberModalBottomSheetState's saveable inputs stay stable
  // (a fresh capturing lambda on every recomposition would reset the sheet state).
  val confirmValueChange = remember(dismissRequest) {
    { value: ModalBottomSheetValue ->
      if (value != ModalBottomSheetValue.Hidden) {
        true
      } else {
        val guard = dismissRequest.value
        if (guard == null) true
        else {
          guard.requestDismiss()
          false
        }
      }
    }
  }
  // skipHalfExpanded: edit sheets are tall; open fully instead of stopping half-way
  val sheetState = rememberModalBottomSheetState(
    initialValue = ModalBottomSheetValue.Hidden,
    confirmValueChange = confirmValueChange,
    skipHalfExpanded = true
  )
  // Bumped on every open so each sheet starts from the group's current values
  var session by remember { mutableStateOf(0) }

  fun closeSheet() {
    scope.launch {
      sheetState.hide()
      sheetKind.value = null
    }
  }

  LaunchedEffect(Unit) {
    snapshotFlow { sheetKind.value }
      .distinctUntilChanged()
      .collect { kind ->
        if (kind != null) {
          session++
          sheetState.show()
        }
      }
  }

  // While a sheet is active, Back dismisses the sheet instead of the whole info modal.
  // Registered after the modal's own BackHandler, so it takes precedence while enabled.
  BackHandler(enabled = sheetKind.value != null) {
    val guard = dismissRequest.value
    if (guard == null) closeSheet() else guard.requestDismiss()
  }

  ModalBottomSheetLayout(
    scrimColor = Color.Black.copy(alpha = 0.12F),
    modifier = Modifier.imePadding(),
    sheetContent = {
      when (val kind = sheetKind.value) {
        GroupInfoSheetKind.PROFILE ->
          key(session) { GroupProfileSheetBody(rhId, groupInfo, dismissRequest, ::closeSheet) }
        GroupInfoSheetKind.WELCOME_MESSAGE ->
          key(session) { GroupWelcomeSheetBody(rhId, groupInfo, dismissRequest, ::closeSheet) }
        GroupInfoSheetKind.WEB_PAGE ->
          key(session) { GroupWebPageSheetBody(rhId, groupInfo, dismissRequest, ::closeSheet) }
        null -> {}
      }
    },
    sheetState = sheetState,
    sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    sheetElevation = 0.dp
  ) {
    content()
  }
}

/** Group / channel profile: avatar, display name, (conditional) full name, short description. */
@Composable
private fun GroupProfileSheetBody(
  rhId: Long?,
  groupInfo: GroupInfo,
  dismissRequest: MutableState<GroupInfoSheetDismiss?>,
  closeSheet: () -> Unit,
) {
  val isChannel = groupInfo.useRelays
  val groupProfile = groupInfo.groupProfile
  val displayName = remember { mutableStateOf(groupProfile.displayName) }
  val fullName = remember { mutableStateOf(groupProfile.fullName) }
  val shortDescr = remember { mutableStateOf(groupProfile.shortDescr ?: "") }
  val chosenImage = remember { mutableStateOf<URI?>(null) }
  val profileImage = remember { mutableStateOf(groupProfile.image) }
  var pickingImage by remember { mutableStateOf(false) }
  val focusRequester = remember { FocusRequester() }

  fun dataUnchanged(): Boolean =
    displayName.value.trim() == groupProfile.displayName &&
        fullName.value.trim() == groupProfile.fullName &&
        shortDescr.value.trim() == (groupProfile.shortDescr ?: "") &&
        groupProfile.image == profileImage.value

  fun canUpdateProfile(): Boolean =
    displayName.value.trim().isNotEmpty() && isValidNewProfileName(displayName.value, groupProfile) && bioFitsLimit(shortDescr.value)

  fun saveProfile(afterSave: () -> Unit) {
    withBGApi {
      val p = groupProfile.copy(
        displayName = displayName.value.trim(),
        fullName = fullName.value.trim(),
        shortDescr = shortDescr.value.trim().ifEmpty { null },
        image = profileImage.value
      )
      val gInfo = chatModel.controller.apiUpdateGroup(rhId, groupInfo.groupId, p, groupInfo.useRelays)
      if (gInfo != null) {
        withContext(Dispatchers.Main) {
          chatModel.chatsContext.updateGroup(rhId, gInfo)
        }
        afterSave()
      }
    }
  }

  fun requestDismiss() {
    if (dataUnchanged() || !canUpdateProfile()) {
      closeSheet()
    } else {
      showUnsavedChangesAlert(isChannel, { saveProfile(closeSheet) }, closeSheet)
    }
  }

  DisposableEffect(Unit) {
    dismissRequest.value = GroupInfoSheetDismiss { requestDismiss() }
    onDispose { dismissRequest.value = null }
  }

  if (pickingImage) {
    // avatar picker shown as the sheet content itself (same GetImageBottomSheet as before,
    // just hosted in this sheet instead of a nested sheet of a full page)
    GetImageBottomSheet(
      chosenImage,
      onImageChange = { bitmap -> profileImage.value = resizeImageToStrSize(cropToSquare(bitmap), maxDataSize = 12500) },
      hideBottomSheet = { pickingImage = false }
    )
    return
  }

  LaunchedEffect(Unit) {
    delay(300)
    focusRequester.requestFocus()
  }

  MineralEditSheet(
    title = stringResource(if (isChannel) MR.strings.button_edit_channel_profile else MR.strings.button_edit_group_profile),
    saveTitle = stringResource(if (isChannel) MR.strings.save_channel_profile else MR.strings.save_group_profile),
    saveEnabled = !dataUnchanged() && canUpdateProfile(),
    onSave = { saveProfile(closeSheet) }
  ) {
    Text(
      stringResource(if (isChannel) MR.strings.channel_profile_is_stored_on_subscribers_devices else MR.strings.group_profile_is_stored_on_members_devices),
      fontSize = 12.sp,
      color = if (isInDarkTheme()) Slate400 else Slate500,
      modifier = Modifier.padding(bottom = 10.dp)
    )
    Row(
      Modifier.fillMaxWidth().padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.Center
    ) {
      Box(contentAlignment = Alignment.TopEnd) {
        Box(contentAlignment = Alignment.Center) {
          ProfileImage(84.dp, profileImage.value, icon = groupInfo.chatIconName, color = MaterialTheme.colors.secondary.copy(alpha = 0.1f))
          EditImageButton { pickingImage = true }
        }
        if (profileImage.value != null) {
          DeleteImageButton { profileImage.value = null }
        }
      }
    }
    Text(
      stringResource(if (isChannel) MR.strings.channel_display_name_field else MR.strings.group_display_name_field),
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      color = if (isInDarkTheme()) Slate400 else Slate500,
      modifier = Modifier.padding(bottom = 6.dp)
    )
    Box {
      ProfileNameField(displayName, "", { isValidNewProfileName(it, groupProfile) }, focusRequester)
      if (!isValidNewProfileName(displayName.value, groupProfile)) {
        IconButton(
          { showInvalidNameAlert(mkValidName(displayName.value), displayName) },
          Modifier.align(Alignment.CenterEnd).padding(end = DEFAULT_PADDING_HALF).size(20.dp)
        ) {
          Icon(painterResource(MR.images.ic_info), null, tint = MaterialTheme.colors.error)
        }
      }
    }
    if (groupProfile.fullName.trim().isNotEmpty() && groupProfile.fullName.trim() != groupProfile.displayName.trim()) {
      Spacer(Modifier.height(DEFAULT_PADDING))
      Text(
        stringResource(if (isChannel) MR.strings.channel_full_name_field else MR.strings.group_full_name_field),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isInDarkTheme()) Slate400 else Slate500,
        modifier = Modifier.padding(bottom = 6.dp)
      )
      ProfileNameField(fullName)
    }
    Spacer(Modifier.height(DEFAULT_PADDING))
    Text(
      stringResource(MR.strings.group_short_descr_field),
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      color = if (isInDarkTheme()) Slate400 else Slate500,
      modifier = Modifier.padding(bottom = 6.dp)
    )
    Box {
      ProfileNameField(shortDescr, "", isValid = { bioFitsLimit(it) })
      if (!bioFitsLimit(shortDescr.value)) {
        IconButton(
          { AlertManager.shared.showAlertMsg(title = generalGetString(MR.strings.group_descr_too_large)) },
          Modifier.align(Alignment.CenterEnd).padding(end = DEFAULT_PADDING_HALF).size(20.dp)
        ) {
          Icon(painterResource(MR.images.ic_info), null, tint = MaterialTheme.colors.error)
        }
      }
    }
  }
}

private fun isValidNewProfileName(displayName: String, groupProfile: GroupProfile): Boolean =
  displayName == groupProfile.displayName || isValidDisplayName(displayName.trim())

private fun showUnsavedChangesAlert(isChannel: Boolean, save: () -> Unit, revert: () -> Unit) {
  AlertManager.shared.showAlertDialogStacked(
    title = generalGetString(MR.strings.save_preferences_question),
    confirmText = generalGetString(if (isChannel) MR.strings.save_and_notify_channel_subscribers else MR.strings.save_and_notify_group_members),
    dismissText = generalGetString(MR.strings.exit_without_saving),
    onConfirm = save,
    onDismiss = revert,
  )
}

private const val maxWelcomeByteCount = 1200

/** Welcome message: 140dp editor with byte limit, preview/copy, explicit save. */
@Composable
private fun GroupWelcomeSheetBody(
  rhId: Long?,
  groupInfo: GroupInfo,
  dismissRequest: MutableState<GroupInfoSheetDismiss?>,
  closeSheet: () -> Unit,
) {
  val isOwner = groupInfo.isOwner && groupInfo.businessChat?.chatType == null
  val linkMode = chatModel.controller.appPrefs.simplexLinkMode.get()
  val welcomeText = remember { mutableStateOf(groupInfo.groupProfile.description ?: "") }
  var editMode by remember { mutableStateOf(true) }

  fun welcomeTextUnchanged(): Boolean =
    welcomeText.value == groupInfo.groupProfile.description || (welcomeText.value == "" && groupInfo.groupProfile.description == null)

  fun welcomeTextFitsLimit(): Boolean = chatJsonLength(welcomeText.value) <= maxWelcomeByteCount

  fun save(afterSave: () -> Unit = {}) {
    withBGApi {
      var welcome: String? = welcomeText.value.trim('\n', ' ')
      if (welcome?.length == 0) {
        welcome = null
      }
      val groupProfileUpdated = groupInfo.groupProfile.copy(description = welcome)
      val res = chatModel.controller.apiUpdateGroup(rhId, groupInfo.groupId, groupProfileUpdated, groupInfo.useRelays)
      if (res != null) {
        withContext(Dispatchers.Main) {
          chatModel.chatsContext.updateGroup(rhId, res)
        }
        welcomeText.value = welcome ?: ""
      }
      afterSave()
    }
  }

  fun requestDismiss() {
    when {
      !isOwner || welcomeTextUnchanged() -> closeSheet()
      !welcomeTextFitsLimit() -> showUnsavedChangesTooLongAlert(closeSheet)
      else -> showUnsavedChangesAlert({ save(closeSheet) }, closeSheet)
    }
  }

  DisposableEffect(Unit) {
    dismissRequest.value = GroupInfoSheetDismiss { requestDismiss() }
    onDispose { dismissRequest.value = null }
  }

  val clipboard = LocalClipboardManager.current

  MineralEditSheet(
    title = stringResource(MR.strings.group_welcome_title),
    saveTitle = stringResource(MR.strings.save_and_update_group_profile),
    saveEnabled = isOwner && !welcomeTextUnchanged() && welcomeTextFitsLimit(),
    onSave = { save(closeSheet) },
    showSaveRow = isOwner
  ) {
    if (isOwner && editMode) {
      val focusRequester = remember { FocusRequester() }
      TextEditor(
        welcomeText,
        Modifier.height(140.dp),
        stringResource(MR.strings.enter_welcome_message),
        focusRequester = focusRequester
      )
      LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
      }
    } else {
      SelectionContainer(Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
        MarkdownText(
          welcomeText.value,
          formattedText = remember(welcomeText.value) { parseToMarkdown(welcomeText.value) },
          toggleSecrets = false,
          linkMode = linkMode,
          style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground, lineHeight = 22.sp)
        )
      }
    }
    if (isOwner) {
      Text(
        if (!welcomeTextFitsLimit()) generalGetString(MR.strings.message_too_large) else "",
        fontSize = 12.sp,
        color = MaterialTheme.colors.error,
        modifier = Modifier.padding(top = 4.dp)
      )
      Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        SheetActionChip(
          icon = painterResource(if (editMode) MR.images.ic_visibility else MR.images.ic_edit),
          text = stringResource(if (editMode) MR.strings.group_welcome_preview else MR.strings.edit_verb),
          enabled = welcomeText.value.isNotEmpty()
        ) {
          editMode = !editMode
        }
        SheetActionChip(
          icon = painterResource(MR.images.ic_content_copy),
          text = stringResource(MR.strings.copy_verb),
          enabled = welcomeText.value.isNotEmpty()
        ) {
          clipboard.setText(AnnotatedString(welcomeText.value))
          showToast(generalGetString(MR.strings.copied))
        }
      }
    }
  }
}

private fun showUnsavedChangesAlert(save: () -> Unit, revert: () -> Unit) {
  AlertManager.shared.showAlertDialogStacked(
    title = generalGetString(MR.strings.save_welcome_message_question),
    confirmText = generalGetString(MR.strings.save_and_update_group_profile),
    dismissText = generalGetString(MR.strings.exit_without_saving),
    onConfirm = save,
    onDismiss = revert,
  )
}

private fun showUnsavedChangesTooLongAlert(revert: () -> Unit) {
  AlertManager.shared.showAlertDialogStacked(
    title = generalGetString(MR.strings.welcome_message_is_too_long),
    confirmText = generalGetString(MR.strings.exit_without_saving),
    onConfirm = revert,
  )
}

/** Channel web page: URL field + allow-embedding toggle + embed code. */
@Composable
private fun GroupWebPageSheetBody(
  rhId: Long?,
  groupInfo: GroupInfo,
  dismissRequest: MutableState<GroupInfoSheetDismiss?>,
  closeSheet: () -> Unit,
) {
  val isChannel = groupInfo.isChannel
  val access = groupInfo.groupProfile.publicGroup?.publicGroupAccess
  val webPage = remember { mutableStateOf(access?.groupWebPage ?: "") }
  val allowEmbedding = remember { mutableStateOf(access?.allowEmbedding ?: false) }
  val groupRelays = remember { mutableStateListOf<GroupRelay>() }

  fun dataUnchanged(): Boolean =
    webPage.value.trim() == (access?.groupWebPage ?: "") &&
        allowEmbedding.value == (access?.allowEmbedding ?: false)

  fun save() {
    withBGApi {
      val trimmedPage = webPage.value.trim()
      val newAccess = PublicGroupAccess(
        groupWebPage = trimmedPage.ifEmpty { null },
        groupDomainClaim = access?.groupDomainClaim,
        domainWebPage = access?.domainWebPage ?: false,
        allowEmbedding = allowEmbedding.value
      )
      val gp = groupInfo.groupProfile.copy(
        publicGroup = groupInfo.groupProfile.publicGroup?.copy(publicGroupAccess = newAccess)
      )
      val gInfo = chatModel.controller.apiUpdateGroup(rhId, groupInfo.groupId, gp, isChannel)
      if (gInfo != null) {
        withContext(Dispatchers.Main) {
          chatModel.chatsContext.updateGroup(rhId, gInfo)
        }
        closeSheet()
      }
    }
  }

  fun requestDismiss() {
    if (dataUnchanged()) {
      closeSheet()
    } else {
      AlertManager.shared.showAlertDialogStacked(
        title = generalGetString(MR.strings.save_preferences_question),
        confirmText = generalGetString(if (isChannel) MR.strings.save_and_notify_channel_subscribers else MR.strings.save_and_notify_group_members),
        dismissText = generalGetString(MR.strings.exit_without_saving),
        onConfirm = ::save,
        onDismiss = closeSheet,
      )
    }
  }

  DisposableEffect(Unit) {
    dismissRequest.value = GroupInfoSheetDismiss { requestDismiss() }
    onDispose { dismissRequest.value = null }
  }

  LaunchedEffect(Unit) {
    val relays = chatModel.controller.apiGetGroupRelays(rhId, groupInfo.groupId)
    groupRelays.clear()
    groupRelays.addAll(relays)
  }

  val clipboard = LocalClipboardManager.current
  val isDark = isInDarkTheme()

  MineralEditSheet(
    title = stringResource(if (isChannel) MR.strings.channel_webpage else MR.strings.group_webpage),
    saveTitle = stringResource(MR.strings.save_verb),
    saveEnabled = !dataUnchanged(),
    onSave = ::save
  ) {
    val embedCode = embedCode(groupRelays, groupInfo)
    if (embedCode != null) {
      Text(
        stringResource(MR.strings.webpage_info),
        fontSize = 12.sp,
        color = if (isDark) Slate400 else Slate500,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      Box(
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(if (isDark) Slate800 else Slate100)
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Text(
          embedCode,
          style = MaterialTheme.typography.body2.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
          maxLines = 6,
          overflow = TextOverflow.Ellipsis
        )
      }
      TextActionButton(stringResource(MR.strings.copy_code), Modifier.padding(top = 4.dp)) {
        clipboard.setText(AnnotatedString(embedCode))
        showToast(generalGetString(MR.strings.copied))
      }
    } else {
      Text(
        stringResource(MR.strings.relays_no_web_support),
        fontSize = 12.sp,
        color = if (isDark) Slate400 else Slate500,
        modifier = Modifier.padding(bottom = 8.dp)
      )
    }
    Spacer(Modifier.height(6.dp))
    Text(
      stringResource(MR.strings.enter_webpage_url),
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      color = if (isDark) Slate400 else Slate500,
      modifier = Modifier.padding(bottom = 6.dp)
    )
    Box(
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(if (isDark) Slate800 else Slate100)
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      PlainTextEditor(webPage, placeholder = stringResource(MR.strings.web_page_url_placeholder), singleLine = true)
    }
    Text(
      stringResource(MR.strings.webpage_url_footer),
      fontSize = 12.sp,
      color = if (isDark) Slate400 else Slate500,
      modifier = Modifier.padding(top = 6.dp)
    )
    PreferenceToggle(
      text = stringResource(MR.strings.allow_anyone_to_embed),
      checked = allowEmbedding.value,
      onChange = { allowEmbedding.value = it }
    )
    Text(
      stringResource(if (allowEmbedding.value) MR.strings.embed_any_webpage_can_show else MR.strings.embed_only_your_page),
      fontSize = 12.sp,
      color = if (isDark) Slate400 else Slate500,
      modifier = Modifier.padding(top = 4.dp)
    )
  }
}

private fun embedCode(groupRelays: List<GroupRelay>, groupInfo: GroupInfo): String? {
  val pg = groupInfo.groupProfile.publicGroup ?: return null
  val relayDomains = groupRelays.mapNotNull { it.relayCap.webDomain }
  if (relayDomains.isEmpty()) return null
  val domains = relayDomains.joinToString(",")
  return """<div data-simplex-channel-preview
  data-channel-link="${pg.groupLink}"
  data-channel-id="${pg.publicGroupId}"
  data-relay-domains="$domains"
  data-app-download-buttons="on"
  data-color-scheme="light"
></div>
<script src="https://simplex.chat/js/channel-preview.js"></script>"""
}

@Composable
private fun SheetActionChip(
  icon: Painter,
  text: String,
  enabled: Boolean,
  onClick: () -> Unit
) {
  val isDark = isInDarkTheme()
  Row(
    Modifier
      .clip(RoundedCornerShape(50))
      .background(if (isDark) Slate800 else Slate100)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = if (enabled) (if (isDark) Slate100 else Slate700) else (if (isDark) Slate600 else Slate400),
      modifier = Modifier.size(14.dp)
    )
    Text(
      text,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      color = if (enabled) (if (isDark) Slate100 else Slate700) else (if (isDark) Slate600 else Slate400)
    )
  }
}

@Composable
private fun TextActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Text(
    text,
    style = TextStyle(
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Start
    ),
    color = MaterialTheme.colors.primary,
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 8.dp)
  )
}
