package chat.simplex.common.views.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import chat.simplex.common.BuildConfigCommon
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.darkStops
import chat.simplex.common.views.newchat.gradientPoints
import chat.simplex.common.views.newchat.lightStops
import chat.simplex.common.views.migration.MigrateToDeviceView
import chat.simplex.common.views.migration.MigrationToState
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun SimpleXInfo(chatModel: ChatModel, onboarding: Boolean = true) {
  if (onboarding) {
    if (appPlatform.isDesktop) {
      SimpleXInfoDesktop(chatModel)
    } else {
      CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
        ModalView({}, showClose = false, showAppBar = false) {
          SimpleXInfoLayout(
            user = chatModel.currentUser.value,
            onboardingStage = chatModel.controller.appPrefs.onboardingStage
          )
        }
      }
    }
  } else {
    SimpleXInfoLayout(
      user = chatModel.currentUser.value,
      onboardingStage = null
    )
  }
}

@Composable
private fun SimpleXInfoDesktop(chatModel: ChatModel) {
  val user = chatModel.currentUser.value
  val onboardingStage = chatModel.controller.appPrefs.onboardingStage
  CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
    ModalView({}, showClose = false) {
      ColumnWithScrollBar(Modifier.padding(horizontal = DEFAULT_PADDING), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(DEFAULT_PADDING))
        Box(Modifier.widthIn(max = 600.dp).fillMaxWidth(0.45f).align(Alignment.CenterHorizontally)) {
          SimpleXLogo()
        }
        Spacer(Modifier.fillMaxHeight().weight(1f))
        Column(Modifier.widthIn(max = 600.dp).align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {
          Box(Modifier.align(Alignment.CenterHorizontally)) {
            AppBarTitle(stringResource(MR.strings.onboarding_be_free), bottomPadding = DEFAULT_PADDING, withPadding = false, overrideTitleColor = MaterialTheme.colors.onBackground, textAlign = TextAlign.Center, lineHeight = 42.sp)
          }
          Text(stringResource(MR.strings.onboarding_private_and_secure), style = MaterialTheme.typography.h3, fontWeight = FontWeight.Medium, color = MaterialTheme.colors.secondary, lineHeight = 25.sp, textAlign = TextAlign.Center)
          Spacer(Modifier.height(DEFAULT_PADDING_HALF))
          ReadableText(MR.strings.onboarding_first_network, TextAlign.Center, padding = PaddingValues(), style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.secondary))
        }
        Spacer(Modifier.fillMaxHeight().weight(1f))
        Column(Modifier.widthIn(max = 1000.dp).align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {
          OnboardingActionButton(user, onboardingStage)
          TextButtonBelowOnboardingButton(stringResource(MR.strings.why_simplex_is_built), icon = painterResource(MR.images.ic_info), onClick = {
            ModalManager.fullscreen.showModal(forceAnimated = true) { HowItWorks(user, onboardingStage) }
          })
        }
      }
    }
  }
  LaunchedEffect(Unit) {
    if (chatModel.migrationState.value != null && !ModalManager.fullscreen.hasModalsOpen()) {
      ModalManager.fullscreen.showCustomModal(animated = false) { close -> MigrateToDeviceView(close) }
    }
  }
}

@Composable
fun SimpleXInfoLayout(
  user: User?,
  onboardingStage: SharedPreference<OnboardingStage>?
) {
  val topBar = onboardingStage == null && !appPrefs.oneHandUI.state.value
  val modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = DEFAULT_ONBOARDING_HORIZONTAL_PADDING)
  Column(if (topBar) modifier.padding(top = AppBarHeight * fontSizeSqrtMultiplier) else modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Box(Modifier.padding(top = DEFAULT_PADDING * 2).widthIn(max = if (appPlatform.isAndroid) 220.dp else 220.dp), contentAlignment = Alignment.Center) {
      SimpleUxWordmark()
    }
    OnboardingShrinkingLayout(
      modifier = Modifier.fillMaxSize(),
      image = {
        Column(Modifier.padding(vertical = DEFAULT_PADDING_HALF, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          val isDarkTheme = isInDarkTheme()
          val cardBrush = if (isDarkTheme) {
            Brush.verticalGradient(listOf(Color(0xFF1E2533), Color(0xFF131720)))
          } else {
            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)))
          }
          val specularRimBrush = if (isDarkTheme) {
            Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x0EFFFFFF)))
          } else {
            Brush.verticalGradient(listOf(Color(0x250F172A), Color(0x0C0F172A)))
          }
          Box(
            Modifier
              .clip(RoundedCornerShape(18.dp))
              .background(cardBrush)
              .border(1.dp, specularRimBrush, RoundedCornerShape(18.dp))
              .padding(4.dp)
          ) {
            OnboardingImage(
              MR.images.intro, MR.images.intro_light, MR.images.ic_forum,
              modifier = if (appPlatform.isAndroid) Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)) else Modifier.heightIn(max = 280.dp).clip(RoundedCornerShape(14.dp))
            )
          }
        }
      },
    content = {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          stringResource(MR.strings.onboarding_be_free),
          style = MaterialTheme.typography.h1,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          lineHeight = 42.sp,
          modifier = Modifier.padding(top = DEFAULT_PADDING_HALF)
        )
        Text(
          stringResource(MR.strings.onboarding_private_and_secure),
          style = MaterialTheme.typography.h3,
          color = MaterialTheme.colors.secondary,
          fontWeight = FontWeight.Medium,
          lineHeight = 25.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 14.dp)
        )
        Text(
          stringResource(MR.strings.onboarding_first_network),
          style = MaterialTheme.typography.body2,
          color = MaterialTheme.colors.secondary,
          textAlign = TextAlign.Center,
          lineHeight = 20.sp,
          modifier = Modifier.padding(top = DEFAULT_PADDING_HALF)
        )
      }
    },
    button = {
      if (onboardingStage != null) {
        Column(Modifier.widthIn(max = if (appPlatform.isAndroid) 450.dp else 1000.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          OnboardingActionButton(user, onboardingStage)
          TextButtonBelowOnboardingButton(stringResource(MR.strings.why_simplex_is_built), icon = painterResource(MR.images.ic_info), onClick = {
            ModalManager.fullscreen.showModal { HowItWorks(user, onboardingStage) }
          })
        }
      } else {
        Spacer(Modifier)
      }
    }
  )
  }
  LaunchedEffect(Unit) {
    if (chatModel.migrationState.value != null && !ModalManager.fullscreen.hasModalsOpen()) {
      ModalManager.fullscreen.showCustomModal(animated = false) { close -> MigrateToDeviceView(close) }
    }
  }
}

@Composable
fun SimpleXLogo() {
  Image(
    painter = painterResource(if (isInDarkTheme()) MR.images.logo_light else MR.images.logo),
    contentDescription = stringResource(MR.strings.image_descr_simplex_logo),
    contentScale = ContentScale.FillWidth,
    modifier = Modifier
      .padding(bottom = 10.dp)
      .fillMaxWidth()
  )
}

@Composable
fun SimpleUxWordmark() {
  val gold = if (isInDarkTheme()) Color(0xFFE2B755) else Color(0xFFD97706)
  Row(verticalAlignment = Alignment.Bottom) {
    Text(
      "Simple",
      color = gold,
      fontFamily = PlusJakartaSans,
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.5.sp
    )
    Text(
      "UX",
      color = MaterialTheme.colors.onBackground,
      fontFamily = PlusJakartaSans,
      fontSize = 28.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = 0.5.sp
    )
  }
}

@Composable
expect fun OnboardingActionButton(user: User?, onboardingStage: SharedPreference<OnboardingStage>, onclick: (() -> Unit)? = null)

@Composable
fun OnboardingActionButton(
  modifier: Modifier = Modifier,
  labelId: StringResource,
  onboarding: OnboardingStage?,
  enabled: Boolean = true,
  icon: Painter? = null,
  iconColor: Color = Color.White,
  onclick: (() -> Unit)?
) {
  val isDark = isInDarkTheme()
  val shape = RoundedCornerShape(22.dp)

  val bgBrush = if (enabled) {
    if (isDark) {
      Brush.verticalGradient(listOf(Color(0xFFE2B755), Color(0xFFB88A2E)))
    } else {
      Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
    }
  } else {
    if (isDark) {
      Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
    } else {
      Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
    }
  }

  val specularBorder = if (enabled) {
    Brush.verticalGradient(listOf(Color(0x60FFFFFF), Color(0x15FFFFFF)))
  } else {
    Brush.verticalGradient(listOf(Color(0x20FFFFFF), Color(0x05FFFFFF)))
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(54.dp)
      .clip(shape)
      .background(bgBrush)
      .border(1.dp, specularBorder, shape)
      .clickable(
        enabled = enabled,
        onClick = {
          performHapticFeedback(SimpleUXHapticType.MEDIUM)
          onclick?.invoke()
          if (onboarding != null) {
            appPrefs.onboardingStage.set(onboarding)
          }
        }
      ),
    color = Color.Transparent,
    shape = shape
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (icon != null) {
        Icon(icon, stringResource(labelId), Modifier.padding(end = DEFAULT_PADDING_HALF), tint = if (enabled) iconColor else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)))
      }
      Text(
        stringResource(labelId),
        color = if (enabled) Color.White else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
        fontSize = 16.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp
      )
    }
  }
}

@Composable
fun TextButtonBelowOnboardingButton(text: String, onClick: (() -> Unit)?, icon: Painter? = null) {
  val isDark = isInDarkTheme()
  val state = getKeyboardState()
  val enabled = onClick != null
  val topPadding by animateDpAsState(if (appPlatform.isAndroid && state.value == KeyboardState.Opened) 0.dp else 10.dp)
  val bottomPadding by animateDpAsState(if (appPlatform.isAndroid && state.value == KeyboardState.Opened) 0.dp else 10.dp)
  if ((appPlatform.isAndroid && state.value == KeyboardState.Closed) || topPadding > 0.dp) {
    Surface(
      modifier = Modifier
        .padding(top = topPadding, bottom = bottomPadding)
        .clip(RoundedCornerShape(16.dp))
        .clickable(
          enabled = enabled,
          onClick = {
            performHapticFeedback(SimpleUXHapticType.LIGHT)
            onClick?.invoke()
          }
        ),
      color = Color.Transparent,
      shape = RoundedCornerShape(16.dp)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        if (icon != null) {
          Icon(
            icon,
            null,
            tint = if (enabled) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else MaterialTheme.colors.secondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(Modifier.width(6.dp))
        }
        Text(
          text,
          color = if (enabled) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else MaterialTheme.colors.secondary,
          fontWeight = FontWeight.Medium,
          fontSize = 14.sp,
          textAlign = TextAlign.Center
        )
      }
    }
  } else {
    // Hide from view when keyboard is open and move the view down
    Spacer(Modifier.height(DEFAULT_PADDING * 2))
  }
}

@Composable
fun OnboardingInformationButton(
  text: String,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .clip(CircleShape)
      .clickable { onClick() }
  ) {
    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Icon(
        painterResource(MR.images.ic_info),
        null,
        tint = MaterialTheme.colors.primary
      )
      // https://issuetracker.google.com/issues/206039942#comment32
      var textLayoutResult: TextLayoutResult? by remember { mutableStateOf(null) }
      Text(
        text,
        Modifier
          .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val newTextLayoutResult = textLayoutResult

            if (newTextLayoutResult == null || newTextLayoutResult.lineCount == 0) {
              // Default behavior if there is no text or the text layout is not measured yet
              layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
              }
            } else {
              val minX = (0 until newTextLayoutResult.lineCount).minOf(newTextLayoutResult::getLineLeft)
              val maxX = (0 until newTextLayoutResult.lineCount).maxOf(newTextLayoutResult::getLineRight)

              layout(ceil(maxX - minX).toInt(), placeable.height) {
                placeable.place(-floor(minX).toInt(), 0)
              }
            }
          },
        onTextLayout = {
          textLayoutResult = it
        },
        style = MaterialTheme.typography.button,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.primary
      )
    }
  }
}

@Preview/*(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)*/
@Composable
fun PreviewSimpleXInfo() {
  SimpleXTheme {
    SimpleXInfoLayout(
      user = null,
      onboardingStage = null
    )
  }
}
