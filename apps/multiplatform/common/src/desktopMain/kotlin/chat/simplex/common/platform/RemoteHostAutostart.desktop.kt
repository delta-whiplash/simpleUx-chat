package chat.simplex.common.platform

import chat.simplex.common.model.*
import java.net.Inet4Address
import java.net.NetworkInterface

internal actual suspend fun maybeAutoStartRemoteHosts() {
  if (!SimpleUxPrefs.remoteHostAutostart()) return
  // Starting a host without any usable interface makes the core alert
  // RCENoLocalAddress - on an offline machine that would pop an error dialog
  // on every launch, so probe the interfaces first and skip silently.
  val hasUsableNetwork = runCatching {
    NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { n ->
      n.isUp && !n.isLoopback && n.inetAddresses.toList().any { it is Inet4Address && !it.isLoopbackAddress }
    }
  }.getOrDefault(false)
  if (!hasUsableNetwork) return
  var started = 0
  for (h in chatModel.remoteHosts.toList()) {
    if (h.sessionState is RemoteHostSessionState.Connected || h.sessionState is RemoteHostSessionState.Starting) continue
    val r = chatModel.controller.startRemoteHost(
      rhId = h.remoteHostId,
      multicast = chatModel.controller.appPrefs.offerRemoteMulticast.get(),
      address = h.bindAddress_,
      port = h.bindPort_,
    )
    if (r != null) started++
  }
  if (started > 0) {
    Log.d(TAG, "auto-started $started remote host(s) at launch")
    chatModel.controller.reloadRemoteHosts()
  }
}
