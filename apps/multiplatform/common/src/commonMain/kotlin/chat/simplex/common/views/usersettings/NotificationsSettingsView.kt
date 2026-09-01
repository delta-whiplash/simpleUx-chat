package chat.simplex.common.views.usersettings

import SectionBottomSpacer
import SectionTextFooter
import SectionView
import SectionViewSelectable
import androidx.compose.foundation.background
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import chat.simplex.common.ui.theme.*
import androidx.compose.ui.text.AnnotatedString
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import kotlin.collections.ArrayList

@Composable
fun NotificationPreviewView(
  notificationPreviewMode: State<NotificationPreviewMode>,
  onNotificationPreviewModeSelected: (NotificationPreviewMode) -> Unit,
) {
  val previewModes = remember { notificationPreviewModes() }
  ColumnWithScrollBar {
    AppBarTitle(stringResource(MR.strings.settings_notification_preview_title))
    SectionViewSelectable(null, notificationPreviewMode, previewModes, onNotificationPreviewModeSelected)
  }
}

// mode, name, description
fun notificationModes(): List<ValueTitleDesc<NotificationsMode>> {
  val res = ArrayList<ValueTitleDesc<NotificationsMode>>()
  res.add(
    ValueTitleDesc(
      NotificationsMode.OFF,
      generalGetString(MR.strings.notifications_mode_off),
      AnnotatedString(generalGetString(MR.strings.notifications_mode_off_desc)),
    )
  )
  res.add(
    ValueTitleDesc(
      NotificationsMode.PERIODIC,
      generalGetString(MR.strings.notifications_mode_periodic),
      AnnotatedString(generalGetString(MR.strings.notifications_mode_periodic_desc)),
    )
  )
  res.add(
    ValueTitleDesc(
      NotificationsMode.SERVICE,
      generalGetString(MR.strings.notifications_mode_service),
      AnnotatedString(generalGetString(MR.strings.notifications_mode_service_desc)),
    )
  )
  return res
}

// preview mode, name, description
fun notificationPreviewModes(): List<ValueTitleDesc<NotificationPreviewMode>> {
  val res = ArrayList<ValueTitleDesc<NotificationPreviewMode>>()
  res.add(
    ValueTitleDesc(
      NotificationPreviewMode.MESSAGE,
      generalGetString(MR.strings.notification_preview_mode_message),
      AnnotatedString(generalGetString(MR.strings.notification_preview_mode_message_desc)),
    )
  )
  res.add(
    ValueTitleDesc(
      NotificationPreviewMode.CONTACT,
      generalGetString(MR.strings.notification_preview_mode_contact),
      AnnotatedString(generalGetString(MR.strings.notification_preview_mode_contact_desc)),
    )
  )
  res.add(
    ValueTitleDesc(
      NotificationPreviewMode.HIDDEN,
      generalGetString(MR.strings.notification_preview_mode_hidden),
      AnnotatedString(generalGetString(MR.strings.notification_display_mode_hidden_desc)),
    )
  )
  return res
}

fun changeNotificationsMode(mode: NotificationsMode, chatModel: ChatModel) {
  chatModel.controller.appPrefs.notificationsMode.set(mode)
  platform.androidNotificationsModeChanged(mode)
}
