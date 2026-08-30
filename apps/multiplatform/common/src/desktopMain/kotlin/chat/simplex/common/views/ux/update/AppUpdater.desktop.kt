package chat.simplex.common.views.ux.update

// The in-app updater UI is Android-only (AppUpdateSection is not composed on
// desktop, which has its own upstream update channel), so nothing here is ever
// invoked. This stub only satisfies the expect/actual contract for
// :common:compileKotlinDesktop.
internal object NoopAppUpdateInstaller : AppUpdateInstaller {
  override suspend fun downloadApk(
    candidate: AppUpdateCandidate,
    onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit,
  ): String = throw UnsupportedOperationException("App updates are Android-only")

  override fun canInstallPackages(): Boolean = false

  override fun installApk(filePath: String) {
    throw UnsupportedOperationException("App updates are Android-only")
  }

  override fun openInstallPermissionSettings() {
    throw UnsupportedOperationException("App updates are Android-only")
  }
}

actual fun provideAppUpdateInstaller(): AppUpdateInstaller = NoopAppUpdateInstaller
