package chat.simplex.common.views.ux.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.UpdaterPrefs
import chat.simplex.common.ui.theme.*
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

/**
 * #109: launch update notice - an elegant, non-intrusive Mineral card anchored to the
 * bottom of the screen, driven entirely by the shared [AppUpdater] state machine.
 * Rendering is inert unless an update is in flight (available / downloading / ready /
 * permission needed); "up to date", "checking" and failures render nothing, so an
 * offline or current install is never bothered.
 *
 * The host positions it (bottom, above the island bar / nav inset); this composable
 * only sizes to its card so the untouched screen area keeps passing touches through.
 */
@Composable
fun UpdateNoticeBanner(updater: AppUpdater, modifier: Modifier = Modifier) {
  val state by updater.state.collectAsState()
  val isDark = isInDarkTheme()

  Column(modifier) {
    when (val s = state) {
      is AppUpdateState.UpdateAvailable ->
        // #109: a version the user already dismissed is never re-offered - only a
        // newer release makes the banner come back
        if (UpdaterPrefs.noticeDismissedVersion() == s.candidate.version) {} else {
          NoticeCard(isDark, onDismiss = { updater.dismissNotice(s.candidate.version) }) {
            Icon(
              painterResource(MR.images.ic_download),
              null,
              Modifier.size(20.dp),
              tint = if (isDark) ChampagneGold else Amber600
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
              Text(
                stringResource(MR.strings.updater_new_version_found, s.candidate.version),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Slate100 else Slate900,
                maxLines = 2
              )
              s.candidate.sizeBytes?.takeIf { it > 0 }?.let {
                Text(
                  formatNoticeSize(it),
                  fontSize = 12.sp,
                  color = if (isDark) Slate400 else Slate500
                )
              }
            }
            Spacer(Modifier.width(8.dp))
            NoticeAction(isDark, stringResource(MR.strings.updater_download)) { updater.downloadUpdate() }
          }
        }
      is AppUpdateState.Downloading -> {
        val percent = (s.totalBytes?.takeIf { it > 0 }?.let { (s.receivedBytes * 100) / it } ?: 0).toInt()
        NoticeCard(isDark, onDismiss = null) {
          Column(Modifier.weight(1f)) {
            Text(
              stringResource(MR.strings.updater_downloading, "$percent%"),
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (isDark) Slate100 else Slate900
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = percent / 100f,
              Modifier.fillMaxWidth(),
              color = if (isDark) ChampagneGold else Amber600,
              backgroundColor = if (isDark) Slate800 else Slate200
            )
          }
        }
      }
      is AppUpdateState.ReadyToInstall -> {
        NoticeCard(isDark, onDismiss = null) {
          Icon(
            painterResource(MR.images.ic_download),
            null,
            Modifier.size(20.dp),
            tint = if (isDark) ChampagneGold else Amber600
          )
          Spacer(Modifier.width(10.dp))
          Text(
            stringResource(MR.strings.updater_new_version_found, s.candidate.version),
            Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Slate100 else Slate900,
            maxLines = 2
          )
          Spacer(Modifier.width(8.dp))
          NoticeAction(isDark, stringResource(MR.strings.updater_install)) { updater.installUpdate() }
        }
      }
      is AppUpdateState.InstallPermissionNeeded -> {
        NoticeCard(isDark, onDismiss = null) {
          Text(
            stringResource(MR.strings.updater_install_permission_hint),
            Modifier.weight(1f),
            fontSize = 13.sp,
            color = if (isDark) Slate100 else Slate900
          )
          Spacer(Modifier.width(8.dp))
          NoticeAction(isDark, stringResource(MR.strings.updater_install)) { updater.installUpdate() }
        }
      }
      else -> {}
    }
  }
}

private fun formatNoticeSize(bytes: Long): String =
  String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024f * 1024f))

@Composable
private fun NoticeCard(
  isDark: Boolean,
  onDismiss: (() -> Unit)?,
  content: @Composable RowScope.() -> Unit
) {
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(if (isDark) Slate900 else Slate50)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    content()
    if (onDismiss != null) {
      Spacer(Modifier.width(4.dp))
      Icon(
        painterResource(MR.images.ic_close),
        stringResource(MR.strings.cancel_verb),
        Modifier
          .size(18.dp)
          .clip(RoundedCornerShape(9.dp))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismiss
          ),
        tint = if (isDark) Slate400 else Slate500
      )
    }
  }
}

@Composable
private fun NoticeAction(isDark: Boolean, title: String, onClick: () -> Unit) {
  Text(
    title,
    Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isDark) ChampagneGold else Amber600)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
      )
      .padding(horizontal = 14.dp, vertical = 8.dp),
    fontSize = 13.sp,
    fontWeight = FontWeight.Bold,
    color = if (isDark) Slate900 else Color.White
  )
}
