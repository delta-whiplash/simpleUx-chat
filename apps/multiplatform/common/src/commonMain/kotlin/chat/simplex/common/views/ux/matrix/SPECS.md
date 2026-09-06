# Matrix co-protocol UX specs (issues #137, #140)

This package is the canonical home for everything the UI needs to know about a chat's
transport protocol. The model layer stays byte-frozen: Matrix chats are *synthetic*
values of the existing model types, and the only discriminator is the chat id prefix
`"mx|"` (e.g. `mx|!room:server`). See `ChatProtocol.kt`.

## 1. Protocol badge (issue #137)

### Variants

| Variant | Component | Placement | Size |
|---|---|---|---|
| Avatar-corner overlay | `ProtocolBadgeOverlay` | `ChatPreviewView` avatar `Box`, aligned `TopEnd` (the `BottomEnd` corner is occupied by status/incognito overlay icons) | 14.dp disc, 9.dp glyph |
| Name-row inline | `ProtocolBadgeInline` | reserved for chat headers / info views (v1: component only, no call site yet) | 11.dp disc, 7.dp glyph |

### Design rules (Luxury Mineral)

- **Disc, not logo.** The badge renders a circular mineral disc with a `Hub` glyph from
  material-icons-extended. It reads "network / federated protocol" without shipping an
  official Matrix brand asset (brand-guideline and asset-pipeline reasons; explicitly out
  of scope for v1).
- **Accent token:** `Sky400` (#38BDF8) — the design system's "Links / Connection Azure"
  jewel, the semantic family for cross-network identity. Gold is reserved for
  active/verified states and must not be diluted by protocol identity.
- **Specular rim:** 1.dp hairline using the existing `GlassBorderDark` / `GlassBorderLight`
  tokens, matching the bevel language of every other disc in the system. No new color
  tokens, no raw hex literals.
- **Glyph tint:** `Slate800` on the azure disc (dark-mode legible; the disc itself is
  theme-invariant so the badge reads identically in both modes).
- **Statelessness:** both composables take no data and no callbacks — they are pure
  ornaments. Visibility is entirely the caller's protocol gate
  (`chat.protocol == ChatProtocol.Matrix`), so SimpleX rendering is bit-identical to
  before the feature. Previews included (`ProtocolBadgeOverlayPreview`).

### Wiring (minimal diff)

`views/chatlist/ChatPreviewView.kt`, inside the avatar overlay `Box`:

```kotlin
if (cInfo.protocol == ChatProtocol.Matrix) {
  ProtocolBadgeOverlay(Modifier.align(Alignment.TopEnd))
}
```

## 2. Per-protocol feature gating matrix (issue #140)

Derived from reading `views/chat/ChatInfoView.kt` (ChatInfoLayout) and
`views/chat/SendMsgView.kt`. `isMatrix = chat.chatInfo.protocol == ChatProtocol.Matrix`.

| Feature | Where (file:line, pre-edit) | SimpleX | Matrix | Guard |
|---|---|---|---|---|
| E2EE / PQ status row | ChatInfoView.kt ~585-589 | visible | hidden | `conn != null && !isMatrix` |
| Security code verification (`VerifyCodeButton`) | ChatInfoView.kt ~592-594 | visible | hidden | inside `!isMatrix` block |
| Send receipts option | ChatInfoView.kt ~595 | visible | hidden | inside `!isMatrix` block (SimpleX-protocol receipts; Matrix room receipts not bridged) |
| Ratchet sync button | ChatInfoView.kt ~596-598 | visible | auto-hidden (`cStats` null for Matrix) | unchanged |
| Timed messages (`ChatTTLOption`) | ChatInfoView.kt ~600 | visible | hidden | wrapped `if (!isMatrix)` |
| Incognito random profile row | ChatInfoView.kt ~618-622 | visible | hidden | `customUserProfile != null && !isMatrix` |
| SMP relay / servers / address switch / contact link QR card | ChatInfoView.kt ~628-668 | visible (when ready+active) | hidden | `contact.ready && contact.active && !isMatrix` |
| Per-send disappearing message chooser | SendMsgView.kt ~231-241 (menu item) + ~276-280 / 298-337 (dialog, reachable only from that item) | visible (if `timedMessageAllowed`) | hidden | `timedMessageAllowed && !cs.editing && !isMatrixChat` on the menu item |
| Search / audio / video call buttons | ChatInfoView.kt ~576-581 | visible | visible (v1; calls for Matrix rooms are a later wave — revisit before E2E) |
| Wallpaper / theme card, clear/delete chat, local alias | ChatInfoView.kt ~605+, ~671+ | visible | visible (local-only features, protocol-agnostic) |

### Known deviation: SendMsgView wiring

`SendMsgView` does not receive the `Chat`, and its call sites (`ChatView.kt`,
`ComposeView.kt`) belong to Agent C's cluster. The gate is therefore exposed as a
defaulted parameter `isMatrixChat: Boolean = false` (default = SimpleX, zero behavior
change for existing callers). Agent C's wave must pass
`isMatrixChat = chat.protocol == ChatProtocol.Matrix` from the compose call sites;
until then the per-send TTL chooser is the one gate in this table that is armed but
not yet driven. The `ChatInfoView` gates are fully driven (`chat` is in scope there).

## 3. Test coverage

`common/src/desktopTest/kotlin/chat/simplex/common/views/ux/matrix/ChatProtocolTest.kt`
runs under the existing `desktopTest` source set (kotlin.test, already wired — no
build.gradle.kts changes were needed). It pins: `mx|`-prefixed ids classify as Matrix
(including the bare prefix), SimpleX id shapes (`<@1`, numeric, empty) classify as
SimpleX, and the prefix must be anchored at the start of the id.
