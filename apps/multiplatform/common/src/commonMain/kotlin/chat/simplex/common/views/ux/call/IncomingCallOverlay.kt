package chat.simplex.common.views.ux.call

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.call.CallManager
import chat.simplex.common.views.call.RcvCallInvitation
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.res.MR
import kotlinx.coroutines.CoroutineScope

/**
 * Full-screen incoming call overlay (Luxury Mineral), replacing the upstream inline
 * banner for the app-in-foreground case. Parameterized and constructible in isolation;
 * call actions arrive as callbacks.
 *
 * Deliberately no infinite animation here (perf rule from #99): a static champagne rim
 * marks the avatar instead of a 60fps pulse.
 */
@Composable
fun IncomingCallOverlay(
  invitation: RcvCallInvitation,
  callManager: CallManager,
  playRingtone: Boolean,
  scope: CoroutineScope,
  onReject: () -> Unit,
  onIgnore: () -> Unit,
  onAccept: () -> Unit
) {
  LaunchedEffect(Unit) {
    if (invitation.sentNotification == false || appPlatform.isDesktop) {
      SoundPlayer.start(scope, sound = playRingtone)
    }
  }
  DisposableEffect(Unit) { onDispose { SoundPlayer.stop() } }

  Box(
    Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(listOf(Slate900, SurfaceContainerLowestDark, Slate950))
      )
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(
      Modifier.fillMaxSize().padding(horizontal = DEFAULT_PADDING * 2),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(Modifier.height(64.dp))
      Text(
        invitation.callTypeText,
        color = Slate400,
        style = MaterialTheme.typography.body1
      )
      Spacer(Modifier.weight(1f))
      Box(contentAlignment = Alignment.Center) {
        Box(
          Modifier
            .size(148.dp)
            .border(BorderStroke(2.dp, AmberGoldRim), CircleShape)
        )
        ProfileImage(size = 120.dp, image = invitation.contact.profile.image)
      }
      Spacer(Modifier.height(24.dp))
      Text(
        invitation.contact.chatViewName,
        color = Slate50,
        style = MaterialTheme.typography.h1,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.weight(1f))
      Row(
        Modifier.fillMaxWidth().padding(bottom = 48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OverlayCallButton(
          icon = painterResource(MR.images.ic_call_end_filled),
          label = stringResource(MR.strings.reject),
          background = Coral500,
          onClick = onReject
        )
        OverlayCallButton(
          icon = painterResource(MR.images.ic_close),
          label = stringResource(MR.strings.ignore),
          background = GlassFrostedDark,
          borderColor = GlassBorderDark,
          iconTint = Slate50,
          onClick = onIgnore
        )
        OverlayCallButton(
          icon = painterResource(MR.images.ic_check_filled),
          label = stringResource(MR.strings.accept),
          background = EmeraldGreen,
          onClick = onAccept
        )
      }
    }
  }
}

@Composable
private fun OverlayCallButton(
  icon: Painter,
  label: String,
  background: Color,
  onClick: () -> Unit,
  borderColor: Color = Color.Transparent,
  iconTint: Color = Slate50
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(background)
        .border(BorderStroke(1.dp, borderColor), CircleShape)
        .clickable(onClick = onClick, role = Role.Button, interactionSource = remember { MutableInteractionSource() }, indication = null),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, label, tint = iconTint, modifier = Modifier.size(30.dp))
    }
    Spacer(Modifier.height(8.dp))
    Text(label, color = Slate400, style = MaterialTheme.typography.body2)
  }
}

/** Convenience binding mirroring the upstream banner's behavior for the app-root render site. */
@Composable
fun IncomingCallOverlay(invitation: RcvCallInvitation, callManager: CallManager, chatModel: ChatModel) {
  val scope = rememberCoroutineScope()
  IncomingCallOverlay(
    invitation = invitation,
    callManager = callManager,
    playRingtone = !chatModel.showCallView.value,
    scope = scope,
    onReject = { callManager.endCall(invitation = invitation) },
    onIgnore = {
      chatModel.activeCallInvitation.value = null
      ntfManager.cancelCallNotification()
    },
    onAccept = { callManager.acceptIncomingCall(invitation = invitation) }
  )
}
