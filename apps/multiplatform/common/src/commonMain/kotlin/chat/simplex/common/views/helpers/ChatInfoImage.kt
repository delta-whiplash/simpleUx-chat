package chat.simplex.common.views.helpers

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.shape.*
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import chat.simplex.common.model.BadgeStatus
import chat.simplex.common.model.BadgeType
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.LocalBadge
import chat.simplex.common.model.localDate
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import kotlin.math.max
import kotlin.math.roundToInt

import androidx.compose.ui.text.style.TextAlign

fun extractInitials(name: String?): String {
  if (name.isNullOrBlank()) return ""
  val clean = name.trim()
  val lower = clean.lowercase()
  if (lower.startsWith("invited") || lower.startsWith("invitation") || lower.startsWith("connexion") ||
      lower.startsWith("private note") || lower.startsWith("notes priv") || lower.startsWith("simplex") ||
      clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("smp://")) {
    return ""
  }
  val words = clean.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() && it.first().isLetterOrDigit() }
  return when {
    words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
    words.size == 1 -> "${words[0].first().uppercaseChar()}"
    clean.isNotEmpty() && clean.first().isLetterOrDigit() -> "${clean.first().uppercaseChar()}"
    else -> ""
  }
}

@Composable
fun ChatInfoImage(chatInfo: ChatInfo, size: Dp, iconColor: Color = MaterialTheme.colors.secondaryVariant, shadow: Boolean = false) {
  val icon =
    when (chatInfo) {
      is ChatInfo.Group -> chatInfo.groupInfo.chatIconName
      is ChatInfo.Local -> MR.images.ic_folder_filled
      is ChatInfo.Direct -> chatInfo.contact.chatIconName
      else -> MR.images.ic_account_circle_filled
    }
  val name = when (chatInfo) {
    is ChatInfo.Direct -> chatInfo.localDisplayName.ifEmpty { chatInfo.fullName }
    is ChatInfo.Group -> chatInfo.localDisplayName
    is ChatInfo.Local -> "Private notes"
    else -> chatInfo.localDisplayName
  }
  ProfileImage(
    size = size,
    image = chatInfo.image,
    icon = icon,
    color = if (chatInfo is ChatInfo.Local) NoteFolderIconColor else iconColor,
    name = name
  )
}

@Composable
fun IncognitoImage(size: Dp, iconColor: Color = MaterialTheme.colors.secondaryVariant) {
  Box(Modifier.size(size)) {
    Icon(
      painterResource(MR.images.ic_theater_comedy_filled), stringResource(MR.strings.incognito),
      modifier = Modifier.size(size).padding(size / 12),
      iconColor
    )
  }
}

@Composable
fun ProfileImage(
  size: Dp,
  image: String? = null,
  icon: ImageResource = MR.images.ic_account_circle_filled,
  color: Color = MaterialTheme.colors.secondaryVariant,
  backgroundColor: Color? = null,
  name: String? = null,
  blurred: Boolean = false,
  async: Boolean = false
) {
  val isDark = isInDarkTheme()
  Box(
    Modifier
      .size(size)
      .clip(CircleShape),
    contentAlignment = Alignment.Center
  ) {
    if (image == null) {
      val initials = extractInitials(name)
      val isNote = icon == MR.images.ic_folder_filled || name?.contains("note", ignoreCase = true) == true
      val isLink = icon == MR.images.ic_link || name?.contains("invite", ignoreCase = true) == true || name?.contains("connect", ignoreCase = true) == true
      val isGroup = icon == MR.images.ic_supervised_user_circle_filled || name?.contains("groupe", ignoreCase = true) == true || name?.contains("group", ignoreCase = true) == true

      // Unified Luxury Mineral & Jewel Surfaces
      val bgGradient = when {
        isNote -> if (isDark) listOf(Color(0xFF2C2214), Color(0xFF161109)) else listOf(Color(0xFFFDFBF7), Color(0xFFF1E9DA))
        isLink -> if (isDark) listOf(Color(0xFF182234), Color(0xFF0D1320)) else listOf(Color(0xFFF4F8FD), Color(0xFFDEEBFA))
        isGroup -> if (isDark) listOf(Color(0xFF142426), Color(0xFF0A1416)) else listOf(Color(0xFFF0FDFB), Color(0xFFD3F5EF))
        else -> if (isDark) listOf(Color(0xFF1F2533), Color(0xFF121620)) else listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
      }

      val rimGradient = when {
        isNote -> if (isDark) listOf(Color(0x60E2B755), Color(0x18E2B755)) else listOf(Color(0x40854D0E), Color(0x15854D0E))
        isLink -> if (isDark) listOf(Color(0x6038BDF8), Color(0x1838BDF8)) else listOf(Color(0x402563EB), Color(0x152563EB))
        isGroup -> if (isDark) listOf(Color(0x602DD4BF), Color(0x182DD4BF)) else listOf(Color(0x400F766E), Color(0x150F766E))
        else -> if (isDark) listOf(Color(0x4594A3B8), Color(0x1294A3B8)) else listOf(Color(0x350F172A), Color(0x120F172A))
      }

      val accentColor = when {
        isNote -> if (isDark) Color(0xFFE2B755) else Color(0xFF854D0E)
        isLink -> if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
        isGroup -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
        else -> if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
      }

      if (initials.isNotEmpty()) {
        Box(
          Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
            .border(1.dp, Brush.verticalGradient(rimGradient), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = initials,
            color = accentColor,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.body1.copy(letterSpacing = 0.6.sp),
            textAlign = TextAlign.Center
          )
        }
      } else {
        val iconToReplace = when (icon) {
          MR.images.ic_account_circle_filled -> AccountCircleFilled
          MR.images.ic_supervised_user_circle_filled -> SupervisedUserCircleFilled
          else -> null
        }

        Box(
          Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
            .border(1.dp, Brush.verticalGradient(rimGradient), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (iconToReplace != null) {
            Icon(
              iconToReplace,
              contentDescription = stringResource(MR.strings.icon_descr_profile_image_placeholder),
              tint = accentColor,
              modifier = Modifier.size(size * 0.54f)
            )
          } else {
            Icon(
              painterResource(icon),
              contentDescription = stringResource(MR.strings.icon_descr_profile_image_placeholder),
              tint = accentColor,
              modifier = Modifier.size(size * 0.48f)
            )
          }
        }
      }
    } else {
      if (async) {
        Base64AsyncImage(
          base64ImageString = image,
          contentDescription = stringResource(MR.strings.image_descr_profile_image),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(CircleShape)
        )
      } else {
        val imageBitmap = base64ToBitmap(image)
        Image(
          bitmap = imageBitmap,
          contentDescription = stringResource(MR.strings.image_descr_profile_image),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(CircleShape)
        )
      }
    }
  }
}

// badge height in em: calibrated visually so the badge top matches capital letters and digits
// (Inter's declared cap height is 2048/2816 = 0.727em, but the rendered text is taller than the metrics predict)
private const val fontCapHeightRatio = 0.95f

// fraction of the badge height pushed below the text baseline (like the undershoot of round letters)
private const val badgeBaselineOffsetRatio = 0.05f

// the badge glyph's width / height (the SVGs are cropped to the glyph: 300 x 399)
private const val badgeAspectRatio = 300f / 399f

// A contact/member name with the badge right after it: the badge is baseline-aligned with the name
// and sized to its font (fontSize if given, otherwise style.fontSize), and a truncated name keeps it visible.
@Composable
fun NameWithBadge(
  name: String,
  badge: LocalBadge?,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  fontSize: TextUnit = TextUnit.Unspecified,
  fontStyle: FontStyle? = null,
  fontWeight: FontWeight? = null,
  overflow: TextOverflow = TextOverflow.Clip,
  maxLines: Int = Int.MAX_VALUE,
  style: TextStyle = LocalTextStyle.current
) {
  Row(modifier) {
    Text(
      name,
      Modifier.alignByBaseline().weight(1f, fill = false),
      color = color,
      fontSize = fontSize,
      fontStyle = fontStyle,
      fontWeight = fontWeight,
      overflow = overflow,
      maxLines = maxLines,
      style = style
    )
    NameBadge(badge, if (fontSize.isSpecified) fontSize else style.fontSize)
  }
}

// Badge next to the contact name in a Row: top aligned with capital letters, bottom just below the text baseline.
// Use NameWithBadge unless the row needs special arrangement; then the name Text must use Modifier.alignByBaseline().
@Composable
fun RowScope.NameBadge(badge: LocalBadge?, fontSize: TextUnit = LocalTextStyle.current.fontSize) {
  // a badge that expired over a month ago (ExpiredOld) is not shown
  if (badge == null || badge.status == BadgeStatus.ExpiredOld) return
  val height = with(LocalDensity.current) { (if (fontSize.isSpecified) fontSize else 14.sp).toDp() } * fontCapHeightRatio
  BadgeGlyph(
    badge,
    // the alignment line sits badgeBaselineOffsetRatio above the badge's bottom edge,
    // so the Row places the badge that much below the text baseline;
    // 6.dp matches the visible gap between the name and the verification shield:
    // the shield has 3.dp end padding plus ~17% internal glyph margin, the badge artwork has none
    Modifier.alignBy { (it.measuredHeight * (1 - badgeBaselineOffsetRatio)).roundToInt() }.padding(start = 6.dp).height(height).aspectRatio(badgeAspectRatio)
  )
}

// badge inside a Text via appendInlineContent(id): bottom on the baseline, cap-height tall.
// precede with append(" ") for the space between the name and the badge.
fun nameBadgeInline(badge: LocalBadge, fontSize: TextUnit, onBadgeClick: (() -> Unit)? = null): InlineTextContent {
  val height = fontSize * fontCapHeightRatio
  return InlineTextContent(
    Placeholder(height * badgeAspectRatio, height, PlaceholderVerticalAlign.AboveBaseline)
  ) {
    // the placeholder bottom sits on the baseline and can't extend below it,
    // so the badge is drawn shifted down by badgeBaselineOffsetRatio instead
    BadgeGlyph(badge, Modifier.fillMaxSize().graphicsLayer { translationY = size.height * badgeBaselineOffsetRatio }, onBadgeClick)
  }
}

@Composable
private fun BadgeGlyph(badge: LocalBadge, modifier: Modifier, onBadgeClick: (() -> Unit)? = null) {
  val mod = modifier.let { if (onBadgeClick != null) it.clickable(onClick = onBadgeClick) else it }
  if (badge.status == BadgeStatus.Failed || badge.status == BadgeStatus.UnknownKey) {
    Icon(painterResource(MR.images.ic_warning_filled), contentDescription = null, tint = WarningOrange, modifier = mod)
  } else {
    Image(
      painterResource(badgeImage(badge.badge.badgeType)),
      contentDescription = null,
      contentScale = ContentScale.Fit,
      alpha = if (badge.status == BadgeStatus.Expired) 0.4f else 1f,
      modifier = mod
    )
  }
}

fun showBadgeInfoAlert(name: String, badge: LocalBadge, uriHandler: UriHandler) {
  // a verified badge's type is signed and can't be faked, so the real (possibly unknown) type name is the title
  val title = badge.badge.badgeType.text.replaceFirstChar { it.uppercase() }
  when {
    badge.status == BadgeStatus.Failed ->
      AlertManager.shared.showAlertMsg(
        title = generalGetString(MR.strings.badge_unverified_title),
        text = generalGetString(MR.strings.badge_unverified_desc)
      )
    badge.status == BadgeStatus.UnknownKey ->
      AlertManager.shared.showAlertMsg(
        title = generalGetString(MR.strings.badge_unknown_key_title),
        text = generalGetString(MR.strings.badge_unknown_key_desc)
      )
    badge.badge.badgeType is BadgeType.Investor ->
      AlertManager.shared.showAlertDialog(
        title = title,
        text = String.format(generalGetString(MR.strings.badge_invested), name),
        confirmText = generalGetString(MR.strings.ok),
        dismissText = generalGetString(MR.strings.learn_more),
        onDismiss = { uriHandler.openUriCatching("https://simplex.chat/crowdfunding") }
      )
    else -> {
      // Supporter, Legend and unknown types use the supporter wording
      val expiry = badge.badge.badgeExpiry
      val supports =
        if (badge.status == BadgeStatus.Expired && expiry != null)
          String.format(generalGetString(MR.strings.badge_supported_simplex), name, localDate(expiry))
        else
          String.format(generalGetString(MR.strings.badge_supports_simplex), name)
      AlertManager.shared.showAlertMsg(
        title = title,
        text = supports + "\n\n" + generalGetString(MR.strings.badge_support_from_v7)
      )
    }
  }
}

private fun badgeImage(t: BadgeType): ImageResource = when (t) {
  is BadgeType.Legend -> MR.images.badge_legend
  is BadgeType.Investor -> MR.images.badge_investor
  else -> MR.images.badge_supporter // Supporter + Unknown
}

@Composable
fun ProfileImage(size: Dp, image: ImageResource) {
  Image(
    painterResource(image),
    stringResource(MR.strings.image_descr_profile_image),
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(size).clip(CircleShape)
  )
}

@Composable
fun ProfileIconModifier(size: Dp, padding: Boolean = false, blurred: Boolean = false): Modifier {
  val m = Modifier.size(size).clip(CircleShape)
  return if (blurred) m.blur(size / 4) else m
}

/** [AccountCircleFilled] has its inner padding which leads to visible border if there is background underneath.
 * This is workaround
 * */
@Composable
fun ProfileImageForActiveCall(
  size: Dp,
  image: String? = null,
  color: Color = MaterialTheme.colors.secondaryVariant,
  backgroundColor: Color? = null,
  ) {
  if (image == null) {
    Box(Modifier.requiredSize(size).clip(CircleShape).then(if (backgroundColor != null) Modifier.background(backgroundColor) else Modifier)) {
      Icon(
        AccountCircleFilled,
        contentDescription = stringResource(MR.strings.icon_descr_profile_image_placeholder),
        tint = color,
        modifier = Modifier.requiredSize(size + 14.dp)
      )
    }
  } else {
    val imageBitmap = base64ToBitmap(image)
    Image(
      imageBitmap,
      stringResource(MR.strings.image_descr_profile_image),
      contentScale = ContentScale.Crop,
      modifier = ProfileIconModifier(size, padding = false)
    )
  }
}

@Preview
@Composable
fun PreviewChatInfoImage() {
  SimpleXTheme {
    ChatInfoImage(
      chatInfo = ChatInfo.Direct.sampleData,
      size = 55.dp
    )
  }
}
