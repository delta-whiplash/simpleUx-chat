package chat.simplex.common.views.ux

import chat.simplex.common.model.CIContent

/**
 * Single source of truth for the visual classification of chat-item event types
 * (issue #6). Before this helper existed, three divergent copies of the same
 * CIContent type list lived in the views: the centering list in
 * `ChatItemView.kt` (CIView), a second identical copy in `ChatView.kt`
 * (ChatItemsList.ChatItemView), and a different variant `isSecurityOrFeatureItem`
 * in `ChatView.kt`. Any upstream addition to the sealed `CIContent` hierarchy
 * silently bypassed one or more of them.
 *
 * Call sites (keep this list accurate):
 * - `views/chat/item/ChatItemView.kt` — CIView centering/alignment.
 * - `views/chat/ChatView.kt` — ChatItemBox alignment + swipe-to-reply gating.
 *
 * Policy for future upstream `CIContent` types: the `when` below is
 * deliberately NON-exhaustive (`else -> false`). A type added upstream after
 * this list was written compiles fine (no exhaustiveness error thanks to the
 * `else`) and is classified as NOT a centered event, i.e. it renders with
 * upstream's default message alignment instead of being centered by guess.
 * That is the safe default: a new unknown type flows through automatically and
 * never mis-renders as a centered chip. If upstream adds an event type that
 * should be centered, add it here — once.
 *
 * Deliberate classifications (documented so they are not "fixed" by accident):
 * - `RcvChatFeatureRejected` / `RcvGroupFeatureRejected`: render through the
 *   same `CIChatFeatureView` chip as the other feature events, so they are
 *   centered (the old `isSecurityOrFeatureItem` already classified them as
 *   event-like; the old centering lists missed them).
 * - `SndGroupEventContent`: renders as an event chip but was never classified
 *   as centered by the original restyling (only the received variants were).
 *   Kept as-is — reclassifying it is a visual decision, not a unification.
 * - `ChatBanner`, deleted/moderated/blocked items, calls, invitations, errors:
 *   not centered; they have their own full-width or bubble layouts.
 */
fun isCenteredEvent(content: CIContent): Boolean {
  return when (content) {
    is CIContent.SndDirectE2EEInfo,
    is CIContent.RcvDirectE2EEInfo,
    is CIContent.SndGroupE2EEInfo,
    is CIContent.RcvGroupE2EEInfo,
    is CIContent.RcvChatFeature,
    is CIContent.SndChatFeature,
    is CIContent.RcvGroupFeature,
    is CIContent.SndGroupFeature,
    is CIContent.RcvChatFeatureRejected,
    is CIContent.RcvGroupFeatureRejected,
    is CIContent.RcvChatPreference,
    is CIContent.SndChatPreference,
    is CIContent.RcvDirectEventContent,
    is CIContent.RcvGroupEventContent,
    is CIContent.RcvConnEventContent,
    is CIContent.SndConnEventContent -> true
    else -> false
  }
}
