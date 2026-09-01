package chat.simplex.common.views.ux.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.*
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import chat.simplex.res.MR

// Dark-mineral chrome pieces for the quick-camera sheet. Everything here is
// fixed dark (the camera must read identically in light and dark app themes),
// takes no camera state, and reports actions through callbacks, so each piece
// stays constructible in isolation per the views/ux layering rules.

// Top bar: borderless close button + gold "Scan" title over a soft scrim.
// [endControl] hosts the torch toggle in the top corner (stock-camera
// pattern); the permission-denied branch renders the bar without it.
@Composable
fun CameraTopBar(onClose: () -> Unit, endControl: (@Composable () -> Unit)? = null) {
  Box(
    Modifier
      .fillMaxWidth()
      .background(Brush.verticalGradient(listOf(CameraChromeTopScrim, Color.Transparent)))
      .statusBarsPadding()
      .height(56.dp)
  ) {
    IconButton(
      onClick = onClose,
      modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(48.dp)
    ) {
      Icon(
        painterResource(MR.images.ic_close),
        contentDescription = stringResource(MR.strings.modal_close),
        tint = CameraChromeTextPrimary
      )
    }
    Text(
      stringResource(MR.strings.island_scan),
      color = AmberGold,
      fontWeight = FontWeight.Bold,
      fontSize = 17.sp,
      letterSpacing = 1.5.sp,
      modifier = Modifier.align(Alignment.Center)
    )
    if (endControl != null) {
      Box(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)) {
        endControl()
      }
    }
  }
}

// Round glass control flanking the shutter (gallery, torch): fixed dark
// mineral disc with the specular hairline rim, icon-only on purpose - the
// labeled bar on screen is the persistent island tab bar below (#95), this
// row must not read as a second nav bar. Reports actions via callbacks.
@Composable
fun CameraRoundControl(
  icon: ImageResource,
  contentDescription: String,
  active: Boolean = false,
  onClick: () -> Unit
) {
  val tint = if (active) AmberGold else CameraChromeTextSecondary
  Box(
    Modifier
      .size(52.dp)
      .clip(CircleShape)
      .background(CameraChromeIsland)
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(listOf(CameraChromeRimHighlight, CameraChromeRimLowlight)),
        shape = CircleShape
      )
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(painterResource(icon), contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
  }
}

// Shutter: white mineral disc inside a gold ring, disabled until the capture
// use case is bound.
@Composable
fun CameraShutterButton(enabled: Boolean, onClick: () -> Unit) {
  Box(
    Modifier
      .size(76.dp)
      .shadow(8.dp, CircleShape)
      .border(
        width = 3.dp,
        color = if (enabled) AmberGold else CameraChromeTextMuted.copy(alpha = 0.5f),
        shape = CircleShape
      )
      .padding(6.dp)
      .clip(CircleShape)
      .background(if (enabled) Color.White else Color.White.copy(alpha = 0.35f))
      .clickable(enabled = enabled, onClick = onClick)
  )
}

// Permission-denied state: branded dark mineral card instead of the default
// Material look. Shown once the system permission dialog has been answered
// with a denial (soft or permanent). Two actions, both real: "Open settings"
// deep-links to the app's system settings page (the only reliable path once
// Android stopped re-showing the dialog), and "Close" dismisses the sheet  - 
// the user always has a way out (FB-10).
@Composable
fun CameraPermissionCard(onOpenSettings: () -> Unit, onClose: () -> Unit) {
  val shape = RoundedCornerShape(18.dp)
  Box(
    Modifier
      .padding(horizontal = 32.dp)
      .clip(shape)
      .background(Brush.verticalGradient(listOf(CameraChromeCardTop, CameraChromeCardBottom)))
      .border(1.dp, Brush.verticalGradient(listOf(CameraChromeRimHighlight, CameraChromeRimLowlight)), shape)
      .padding(horizontal = 24.dp, vertical = 28.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        Modifier.size(56.dp).clip(CircleShape).background(AmberGoldWash),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painterResource(MR.images.ic_photo_camera),
          contentDescription = null,
          tint = AmberGold,
          modifier = Modifier.size(28.dp)
        )
      }
      Spacer(Modifier.height(14.dp))
      Text(
        stringResource(MR.strings.enable_camera_access),
        color = CameraChromeTextPrimary,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(8.dp))
      Text(
        stringResource(MR.strings.quick_camera_permission_denied_text),
        color = CameraChromeTextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(18.dp))
      Button(
        onClick = onOpenSettings,
        colors = ButtonDefaults.buttonColors(backgroundColor = AmberGold, contentColor = CameraChromeOnGold)
      ) {
        Text(stringResource(MR.strings.quick_camera_open_settings), fontWeight = FontWeight.SemiBold)
      }
      Spacer(Modifier.height(6.dp))
      TextButton(onClick = onClose) {
        Text(
          stringResource(MR.strings.modal_close),
          color = CameraChromeTextMuted,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}
