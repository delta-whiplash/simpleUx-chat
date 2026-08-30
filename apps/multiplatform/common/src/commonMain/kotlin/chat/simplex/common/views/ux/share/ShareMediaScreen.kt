package chat.simplex.common.views.ux.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.NavigationButtonBack
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.common.views.helpers.SearchTextField
import chat.simplex.common.views.helpers.SharedContent
import chat.simplex.common.views.helpers.getBitmapFromUri
import chat.simplex.common.platform.isImage
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * One selectable target row of [ShareMediaScreen]. The chat and whether it may receive the
 * shared content (feature preferences, chat type) are decided by the adapter, not here.
 */
data class ShareTarget(val chat: Chat, val enabled: Boolean)

private data class SharedMediaPreview(val uri: URI, val bitmap: ImageBitmap?, val isVideo: Boolean)

/**
 * FB-2 redesign of the system share-into-SimpleX surface ("Share media…"): a proper top
 * header, a real preview card of the incoming content, and the target-chat picker as
 * grouped mineral cards. Fully parameterized (no ChatModel defaults, no side effects):
 * selection and dismissal are callbacks implemented by the adapter in ShareListView.kt.
 * Tokens only; strings via MR.
 */
@Composable
fun ShareMediaScreen(
  sharedContent: SharedContent?,
  targets: List<ShareTarget>,
  stopped: Boolean,
  onTargetSelected: (Chat) -> Unit,
  onDismiss: () -> Unit
) {
  val search = rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
  val showSearch = targets.size >= 8
  val visibleTargets = remember(targets, search.value.text) {
    if (search.value.text.isEmpty()) targets
    else targets.filter { it.chat.chatInfo.chatViewName.contains(search.value.text, ignoreCase = true) }
  }

  Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
    ShareMediaHeader(sharedContent, stopped, onDismiss)

    Box(Modifier.weight(1f)) {
      Column(
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
      ) {
        SharedContentPreview(sharedContent)
        SectionTitle(stringResource(MR.strings.share_screen_pick_chat_title))

        if (showSearch) {
          SearchField(search)
        }

        if (visibleTargets.isEmpty()) {
          ShareEmptyState()
        } else {
          TargetPickerCard(visibleTargets, onTargetSelected)
        }

        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun ShareMediaHeader(sharedContent: SharedContent?, stopped: Boolean, onDismiss: () -> Unit) {
  Column(Modifier.fillMaxWidth().statusBarsPadding()) {
    Row(
      Modifier.fillMaxWidth().padding(end = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      NavigationButtonBack(onDismiss)
      Spacer(Modifier.width(4.dp))
      Text(
        text = shareTitle(sharedContent),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (stopped) {
      StoppedBanner()
    }
  }
}

/** Same per-type titles the share flow always used; no new strings needed for them. */
@Composable
private fun shareTitle(sharedContent: SharedContent?): String =
  when (sharedContent) {
    is SharedContent.Text -> stringResource(MR.strings.share_message)
    is SharedContent.Media -> stringResource(MR.strings.share_image)
    is SharedContent.File -> stringResource(MR.strings.share_file)
    is SharedContent.Forward -> if (sharedContent.chatItems.size > 1) stringResource(MR.strings.forward_multiple) else stringResource(MR.strings.forward_message)
    is SharedContent.ChatLink -> stringResource(MR.strings.share_channel)
    is SharedContent.MyAddress -> stringResource(MR.strings.share_address)
    null -> stringResource(MR.strings.share_message)
  }

@Composable
private fun StoppedBanner() {
  val shape = RoundedCornerShape(14.dp)
  Row(
    Modifier
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .fillMaxWidth()
      .clip(shape)
      .background(AmberGold.copy(alpha = 0.14f))
      .border(1.dp, AmberGold.copy(alpha = 0.4f), shape)
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(
      painterResource(MR.images.ic_report_filled),
      contentDescription = stringResource(MR.strings.chat_is_stopped_indication),
      tint = AmberGold,
      modifier = Modifier.size(16.dp)
    )
    Text(
      text = stringResource(MR.strings.share_screen_stopped_notice),
      fontSize = 13.sp,
      color = MaterialTheme.colors.onBackground
    )
  }
}

/** The incoming content: media thumbnails, file, text, forward batch, or link. */
@Composable
private fun SharedContentPreview(sharedContent: SharedContent?) {
  if (sharedContent == null) return
  SectionTitle(stringResource(MR.strings.share_screen_preview_title))
  MineralCard {
    when (val content = sharedContent) {
      is SharedContent.Media -> MediaPreviewRow(content.uris)
      is SharedContent.File -> FilePreview(content.uri)
      is SharedContent.Text -> TextPreview(content.text, leadingIcon = MR.images.ic_chat)
      is SharedContent.Forward -> Column {
        TextPreview(
          text = stringResource(MR.strings.share_screen_items_count, content.chatItems.size.toString()),
          leadingIcon = MR.images.ic_arrow_forward
        )
      }
      is SharedContent.ChatLink -> TextPreview(content.groupInfo.displayName, leadingIcon = MR.images.ic_link)
      is SharedContent.MyAddress -> TextPreview(stringResource(MR.strings.share_address), leadingIcon = MR.images.ic_link)
      null -> {}
    }
  }
}

@Composable
private fun MediaPreviewRow(uris: List<URI>) {
  // Decode off the main thread; failed decodes fall back to a typed placeholder instead
  // of an empty preview area (the reported "big empty area" bug).
  var previews by remember(uris) { mutableStateOf<List<SharedMediaPreview>?>(null) }
  LaunchedEffect(uris) {
    previews = withContext(Dispatchers.IO) {
      uris.map { uri -> SharedMediaPreview(uri, runCatching { getBitmapFromUri(uri, withAlertOnException = false) }.getOrNull(), !isImage(uri)) }
    }
  }
  val loaded = previews
  if (loaded == null) {
    Box(Modifier.fillMaxWidth().height(84.dp))
  } else {
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      items(loaded) { preview ->
        val shape = RoundedCornerShape(14.dp)
        Box(
          Modifier
            .size(84.dp)
            .clip(shape)
            .background(MaterialTheme.colors.background)
            .border(1.dp, if (isInDarkTheme()) GlassBorderDark else GlassBorderLight, shape),
          contentAlignment = Alignment.Center
        ) {
          val bitmap = preview.bitmap
          if (bitmap != null) {
            Image(
              bitmap = bitmap,
              contentDescription = preview.uri.toString(),
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(
              painterResource(if (preview.isVideo) MR.images.ic_videocam_filled else MR.images.ic_image_filled),
              contentDescription = stringResource(MR.strings.share_screen_preview_unavailable),
              tint = MaterialTheme.colors.secondary,
              modifier = Modifier.size(28.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FilePreview(uri: URI) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    LeadingDisc(MR.images.ic_attach_file_filled_500)
    Text(
      text = uri.toString().substringAfterLast('/').ifEmpty { stringResource(MR.strings.share_file) },
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colors.onBackground,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun TextPreview(text: String, leadingIcon: ImageResource) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    LeadingDisc(leadingIcon)
    Text(
      text = text,
      fontSize = 14.sp,
      color = MaterialTheme.colors.onBackground,
      maxLines = 6,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun LeadingDisc(icon: ImageResource) {
  val isDark = isInDarkTheme()
  Box(
    Modifier
      .size(36.dp)
      .clip(CircleShape)
      .background(if (isDark) SurfaceContainerHighestDark else SurfaceContainerHighLight),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      painterResource(icon),
      contentDescription = null,
      tint = AmberGold,
      modifier = Modifier.size(18.dp)
    )
  }
}

@Composable
private fun SectionTitle(title: String) {
  Text(
    text = title,
    fontSize = 13.sp,
    color = MaterialTheme.colors.secondary,
    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp)
  )
}

@Composable
private fun SearchField(search: MutableState<TextFieldValue>) {
  Box(
    Modifier
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(if (isInDarkTheme()) GlassFrostedDark else GlassFrostedLight)
      .border(1.dp, if (isInDarkTheme()) GlassBorderDark else GlassBorderLight, RoundedCornerShape(16.dp))
      .padding(horizontal = 8.dp, vertical = 2.dp)
  ) {
    SearchTextField(
      modifier = Modifier.fillMaxWidth(),
      placeholder = stringResource(MR.strings.search_verb),
      alwaysVisible = true,
      searchText = search,
      enabled = true,
      trailingContent = null,
      reducedCloseButtonPadding = 0.dp,
    ) { search.value = search.value.copy(it) }
  }
}

/** Grouped mineral card with one tappable row per target chat (design system §3.6). */
@Composable
private fun TargetPickerCard(targets: List<ShareTarget>, onTargetSelected: (Chat) -> Unit) {
  MineralCard {
    targets.forEachIndexed { index, target ->
      Row(
        Modifier
          .fillMaxWidth()
          .clickable { onTargetSelected(target.chat) }
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .alpha(if (target.enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TargetAvatar(target.chat)
        Spacer(Modifier.width(10.dp))
        Text(
          text = target.chat.chatInfo.chatViewName,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colors.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Icon(
          painterResource(MR.images.ic_arrow_forward_ios),
          contentDescription = target.chat.chatInfo.chatViewName,
          tint = MaterialTheme.colors.secondary,
          modifier = Modifier.size(14.dp)
        )
      }
      if (index != targets.lastIndex) {
        Divider(color = canvasColorForCurrentTheme(), thickness = 1.dp, modifier = Modifier.padding(horizontal = 12.dp))
      }
    }
  }
}

@Composable
private fun TargetAvatar(chat: Chat) {
  when (val info = chat.chatInfo) {
    is ChatInfo.Local -> ProfileImage(size = 42.dp, null, icon = MR.images.ic_folder_filled, color = NoteFolderIconColor)
    is ChatInfo.Group -> ProfileImage(size = 42.dp, info.image, icon = MR.images.ic_supervised_user_circle_filled)
    else -> ProfileImage(size = 42.dp, info.image)
  }
}

@Composable
private fun ShareEmptyState() {
  Column(
    Modifier.fillMaxWidth().padding(vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Icon(
      painterResource(MR.images.ic_chat),
      contentDescription = null,
      tint = MaterialTheme.colors.secondary,
      modifier = Modifier.size(36.dp)
    )
    Text(
      text = stringResource(MR.strings.you_have_no_chats),
      fontSize = 14.sp,
      color = MaterialTheme.colors.secondary,
      textAlign = TextAlign.Center
    )
  }
}

/** Inset card with hairline specular rim — the design system's grouped-surface recipe. */
@Composable
private fun MineralCard(content: @Composable ColumnScope.() -> Unit) {
  val shape = RoundedCornerShape(18.dp)
  val isDark = isInDarkTheme()
  Column(
    Modifier
      .padding(horizontal = 16.dp)
      .fillMaxWidth()
      .clip(shape)
      .background(if (isDark) SurfaceContainerHighDark else SurfaceContainerLowLight)
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          if (isDark) listOf(GlassSpecularHighlight.copy(alpha = 0.44f), GlassBorderDark.copy(alpha = 0.35f))
          else listOf(GlassBorderLight, GlassBorderLight.copy(alpha = 0.5f))
        ),
        shape = shape
      )
      .padding(vertical = 6.dp),
    content = content
  )
}
