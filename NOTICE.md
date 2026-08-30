# NOTICE

SimpleUX — a UX-focused fork of [SimpleX Chat](https://github.com/simplex-chat/simplex-chat).

SimpleUX is a frontend-only fork: the Haskell core (`libsimplex` / `libapp`), the
JNI/FFI bridge, the SimpleX protocol and all native runtime components are the
unchanged work of the upstream SimpleX Chat project. SimpleUX changes are confined
to the frontend (Kotlin Multiplatform / Compose UI layer).

- **Upstream project:** SimpleX Chat — Copyright (c) 2020-2026 SimpleX Chat
  (https://github.com/simplex-chat/simplex-chat)
- **License:** GNU Affero General Public License, version 3 — see [LICENSE](./LICENSE).
  This is the same license as upstream SimpleX Chat; all fork modifications are
  licensed under the same terms.
- **Distribution identity:** Android applicationId `chat.simplex.ux` (provider
  authority `chat.simplex.ux.provider`); desktop product name "SimpleUX" with
  macOS bundle id `chat.simplex.ux` and a dedicated Windows upgrade code, so the
  fork installs side-by-side with the official SimpleX app.
- **Package namespace rationale:** the internal Kotlin/Gradle namespace remains
  `chat.simplex.app` — it is a code-organization identifier for the shared and
  generated sources inherited from upstream, not a shipping identity. Renaming
  it would add merge friction against every upstream sync for zero
  user-visible benefit. The shipping identity is the applicationId
  `chat.simplex.ux`, the provider authority, the "SimpleUX" product label and
  the fork branding; the desktop packaging identity mirrors it in
  `apps/multiplatform/desktop/build.gradle.kts`.
