package chat.simplex.common.views.usersettings

import SectionItemView
import SectionView
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import chat.simplex.common.model.ChatModel
import chat.simplex.common.platform.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import com.jakewharton.processphoenix.ProcessPhoenix
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
actual fun AdvancedSettingsAppSection(
  showSettingsModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  withAuth: (title: String, desc: String, block: () -> Unit) -> Unit,
) {
  SectionView {
    SettingsActionItem(painterResource(MR.images.ic_code), stringResource(MR.strings.settings_developer_tools), showSettingsModal { DeveloperView(withAuth) }, badgeColor = Color(0xFF5E35B1))
  }
}

fun restartApp() {
  ProcessPhoenix.triggerRebirth(androidAppContext)
  shutdownApp()
}

private fun shutdownApp() {
  androidAppContext.getWorkManagerInstance().cancelAllWork()
  platform.androidServiceSafeStop()
  Runtime.getRuntime().exit(0)
}
