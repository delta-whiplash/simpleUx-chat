# `views/ux/` - layering rules

This package holds all SimpleUX-specific UI (the "Luxury Mineral" design layer).
It exists so the fork can evolve without rewriting upstream screens in place  - 
every rewrite of an upstream file is a future merge conflict.

## Rules (enforced by CI where possible)

1. **No editing upstream files in place.** If a screen needs a SimpleUX look,
   extract the SimpleUX-specific part into `views/ux/` and call it from a single,
   minimal call site in the upstream file. Keep unused upstream composables in
   place (dead-but-intact code is a merge buffer, not clutter).

2. **No `ChatModel` / controller singleton defaults.** Never write
   `chatModelInstance: ChatModel = ChatModel` or reach for a global mutable
   controller from inside a `views/ux/` composable. Components must be
   constructible in isolation (preview/test) with everything passed in
   explicitly. Business actions (mark read, toggle favorite, switch profile,
   connect) are exposed as callback parameters (`onMarkRead`, `onToggleFavorite`,
   ...) implemented by a thin adapter at the call site, not performed directly
   by the component.

3. **No fabricated data.** Never render invented server data, member counts,
   directory listings, or diagnostics as if they came from a real connection.
   If a feature needs real data and the wiring isn't ready, don't ship the UI
   yet - or label it explicitly as illustrative.

4. **Tokens, not literals.** No raw `0xFF...` hex colors and no hardcoded
   user-visible strings in this package - colors come from `ui/theme`, text
   comes from `MR.strings` (with translations, not just the base locale).

5. **One feature at a time.** A new `views/ux/` feature ships complete
   (localized, tokenized, ≥48dp touch targets, no upstream edits) before the
   next one starts. This is a side project - half-finished parallel features
   are the main way it gets unmaintainable.

## Why this exists

See `plans/2026-08-15-ux-audit-backlog.md` for the audit that found: fabricated
directory data shown as live search results, a hardcoded `isPQ = true` security
badge, `ChatModel` singleton defaults in four components, and an upstream file
(`ChatListView.kt`) carrying +1,800 lines of in-place SimpleUX rewrite that
blocks every future upstream merge. This file is the standing rule that is
meant to stop that from recurring.
