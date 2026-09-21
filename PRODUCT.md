# PRODUCT.md

## What this is

SimpleUX Chat: a frontend-only fork of simplex-chat. Same protocol, same Haskell
core, same cryptography, radically better experience. "Uncompromising privacy
meets world-class user experience." It must coexist side-by-side with the
official SimpleX app on the same device (applicationId `chat.simplex.ux`).

## Audience and scene

Privacy-conscious people who refuse to trade polish for safety: journalists,
developers, design-sensitive early adopters. They use the app in the same
contexts they use Telegram or iMessage: one-handed, on the move, at night,
dozens of times a day. Many came *from* Telegram; the app must feel at least
as fluid or they leave.

## What success looks like

- A new user knows exactly where to look and what to do, without onboarding.
- The interface feels premium in the hand: precise spacing, spring motion,
  one confident accent (champagne gold) on a dark obsidian / light platinum
  mineral surface system ("Luxury Mineral").
- Trust is visible: encryption status is legible at a glance, never noisy.
- Nothing about the UI reads as generic or machine-assembled: no mixed accent
  families, no leftover Material defaults, no decorative noise.

## Constraints (non-negotiable)

- Frontend-only: no Haskell/JNI/FFI/wire changes. 100% interoperability with
  official SimpleX clients.
- Model layer (`model/SimpleXAPI.kt`) byte-frozen.
- Design language: Luxury Mineral (see
  `.agents/skills/simpleux-design-system/SKILL.md`). One accent system, tokens
  only, no raw hex in view code. No second glass/surface system.
- All user-visible strings through `MR.strings` (English base + French).
- Upstream high-churn files carry a diff budget; new UI lives in
  `views/ux/` and fork-owned extracted views.

## Surfaces in scope of the current effort

Core messaging loop, in priority order: chat list (rows, pills, top bar,
island bar), conversation (bubbles, date pills, composer, top bar), shared
chrome (sheets, menus, badges), then settings and onboarding.
