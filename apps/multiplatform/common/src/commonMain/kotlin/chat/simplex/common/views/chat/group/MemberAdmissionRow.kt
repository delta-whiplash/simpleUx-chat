package chat.simplex.common.views.chat.group

import InfoRow
import SectionTextFooter
import androidx.compose.runtime.*
import chat.simplex.common.model.*
import chat.simplex.common.platform.chatModel
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * #63: the member-admission setting inlined - it was a full page holding one
 * Off/All dropdown plus reset/save and an unsaved-changes guard. Owners get the
 * dropdown applying immediately through the same apiUpdateGroup path the page
 * used; non-owners keep the read-only row the page showed.
 */
@Composable
fun MemberAdmissionRow(groupInfo: GroupInfo, rhId: Long?) {
  if (groupInfo.isOwner) {
    ExposedDropDownSettingRow(
      generalGetString(MR.strings.member_admission),
      memberCriterias,
      remember(groupInfo) { mutableStateOf(groupInfo.groupProfile.memberAdmission?.review) },
      onSelected = { criteria ->
        withBGApi {
          val base = groupInfo.groupProfile.memberAdmission ?: GroupMemberAdmission()
          val gp = groupInfo.groupProfile.copy(memberAdmission = base.copy(review = criteria))
          val g = chatModel.controller.apiUpdateGroup(rhId, groupInfo.groupId, gp, groupInfo.useRelays)
          if (g != null) {
            withContext(Dispatchers.Main) {
              chatModel.chatsContext.updateGroup(rhId, g)
            }
          }
        }
      }
    )
    SectionTextFooter(stringResource(MR.strings.admission_stage_review_descr))
  } else {
    InfoRow(
      stringResource(MR.strings.member_admission),
      groupInfo.groupProfile.memberAdmission?.review?.text ?: generalGetString(MR.strings.member_criteria_off)
    )
  }
}

private val memberCriterias: List<Pair<MemberCriteria?, String>> = listOf(
  null to generalGetString(MR.strings.member_criteria_off),
  MemberCriteria.All to generalGetString(MR.strings.member_criteria_all)
)
