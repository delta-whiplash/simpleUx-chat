package chat.simplex.common.views.ux.update

import SectionItemView
import SectionView
import itemHPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.simplex.common.BuildConfigCommon
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.ui.theme.DEFAULT_PADDING_HALF
import chat.simplex.common.ui.theme.HighOrLowlight
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * "App updates" section of the version screen (issue #79, Android only — the
 * desktop target renders nothing, the section is not even composed there).
 * Owns a screen-scoped [AppUpdater]; all platform work is behind
 * [AppUpdateInstaller].
 */
@Composable
fun AppUpdateSection() {
  val scope = rememberCoroutineScope()
  val installer = remember { provideAppUpdateInstaller() }
  val updater = remember(scope, installer) { AppUpdater(scope, installer, BuildConfigCommon.ANDROID_VERSION_NAME) }
  val state by updater.state.collectAsState()

  SectionView {
    Column {
      val busy = state is AppUpdateState.Checking || state is AppUpdateState.Downloading
      SectionItemView(
        click = if (busy) null else updater::checkForUpdates,
        disabled = busy
      ) {
        if (state is AppUpdateState.Checking) {
          CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colors.secondary)
          Spacer(Modifier.width(DEFAULT_PADDING))
        }
        Text(
          generalGetString(MR.strings.updater_check),
          Modifier.weight(1f),
          textAlign = TextAlign.Center,
          color = if (busy) HighOrLowlight else MaterialTheme.colors.primary
        )
      }

      when (val s = state) {
        is AppUpdateState.Idle, is AppUpdateState.Checking -> {}
        is AppUpdateState.UpToDate -> StatusText(stringResource(MR.strings.updater_up_to_date))
        is AppUpdateState.UpdateAvailable -> {
          StatusText(String.format(stringResource(MR.strings.updater_new_version_found), s.candidate.version))
          SectionItemView(click = updater::downloadUpdate) {
            Text(
              generalGetString(MR.strings.updater_download),
              Modifier.fillMaxWidth(),
              textAlign = TextAlign.Center,
              color = MaterialTheme.colors.primary
            )
          }
        }
        is AppUpdateState.Downloading -> {
          val progress = s.totalBytes?.takeIf { it > 0 }?.let { (s.receivedBytes.toFloat() / it).coerceIn(0f, 1f) }
          if (progress != null) LinearProgressIndicator(progress = progress, Modifier.fillMaxWidth())
          else LinearProgressIndicator(Modifier.fillMaxWidth())
          val percent = (s.totalBytes?.takeIf { it > 0 }?.let { ((s.receivedBytes * 100) / it).toInt() } ?: 0).toString() + "%"
          StatusText(String.format(stringResource(MR.strings.updater_downloading), percent))
        }
        is AppUpdateState.ReadyToInstall ->
          ActionText(generalGetString(MR.strings.updater_install), updater::installUpdate)
        is AppUpdateState.InstallPermissionNeeded -> {
          StatusText(generalGetString(MR.strings.updater_install_permission_hint))
          ActionText(generalGetString(MR.strings.updater_install), updater::installUpdate)
        }
        is AppUpdateState.Failed -> StatusText(generalGetString(MR.strings.updater_failed))
      }
    }
  }
}

@Composable
private fun StatusText(text: String) {
  Text(
    text,
    Modifier
      .fillMaxWidth()
      .padding(horizontal = itemHPadding, vertical = DEFAULT_PADDING_HALF),
    textAlign = TextAlign.Center,
    color = HighOrLowlight
  )
}

@Composable
private fun ActionText(text: String, action: () -> Unit) {
  SectionItemView(click = action) {
    Text(
      text,
      Modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colors.primary
    )
  }
}
