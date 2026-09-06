package chat.simplex.common.platform

/**
 * #127: on desktop, start listening for already-linked mobiles once hosts are
 * loaded at startup, so a phone on the same network can discover and connect
 * without the user re-opening the connect flow. No-op on mobile (the mobile is
 * the ctrl side; discovery is seeded via SimpleUxMigrations instead).
 */
internal expect suspend fun maybeAutoStartRemoteHosts()
