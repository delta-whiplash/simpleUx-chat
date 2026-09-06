package chat.simplex.common.views.ux

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import chat.simplex.common.model.CR
import chat.simplex.common.model.RemoteCtrlAddress

/**
 * Desktop linking UX (#127, level 1 - same-network seamlessness).
 *
 * The core puts exactly one address in the signed invitation: the head of its own
 * interface enumeration, which frequently lands on a virtual adapter (VirtualBox,
 * Hyper-V, WSL, vmnet) on real machines, producing a QR code the phone cannot
 * reach. [pickDefaultCtrlAddress] re-ranks the addresses the core reported so the
 * QR is valid out of the box; the interface dropdown stays as the manual override.
 */

private val virtualInterfaceMarkers = listOf(
  "virtual", "vmnet", "vmware", "hyper-v", "hyperv", "wsl", "docker", "veth", "loopback", "pseudo", "bluetooth"
)

private val physicalInterfaceMarkers = listOf("wi-fi", "wifi", "wlan", "wireless", "ethernet", "eth")

fun pickDefaultCtrlAddress(addresses: List<RemoteCtrlAddress>): RemoteCtrlAddress? {
  if (addresses.size <= 1) return addresses.firstOrNull()
  fun score(a: RemoteCtrlAddress): Int {
    val iface = a.`interface`.lowercase()
    if (a.address == "127.0.0.1" || a.address.startsWith("169.254.")) return -100
    var s = 0
    if (virtualInterfaceMarkers.any { iface.contains(it) }) s -= 50
    if (physicalInterfaceMarkers.any { iface.contains(it) }) s += 10
    if (a.address.startsWith("192.168.")) s += 5
    if (a.address.startsWith("10.") || a.address.startsWith("172.")) s += 3
    return s
  }
  // maxByOrNull keeps the first maximum, so the core's own order breaks ties.
  return addresses.maxByOrNull(::score)
}

/**
 * When the freshly started host advertises a different address than the smart
 * default (and the user has not picked one manually), restart the host once with
 * the default so the QR becomes valid without a manual "refresh" tap. One-shot
 * per open of the pairing flow: if the core still disagrees after the restart,
 * the stale-QR overlay and the dropdown take over.
 */
@Composable
fun AutoApplyDefaultCtrlAddress(
  cachedR: CR.RemoteHostStarted?,
  customAddress: MutableState<RemoteCtrlAddress?>,
  userChangedAddress: () -> Boolean,
  applyDefault: () -> Unit,
) {
  var autoApplied by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(cachedR) {
    val head = cachedR?.localAddrs?.firstOrNull()
    if (!autoApplied && cachedR != null && !userChangedAddress() &&
      customAddress.value != null && customAddress.value != head
    ) {
      autoApplied = true
      applyDefault()
    }
  }
}
