# UI Rework · Direction contract · Luxury Mineral v2

Surface brief for the core messaging surfaces (chat list, conversation).
Mockups (visual authority for this contract):
`plans/ui-rework/mockups/core-screens.html` (rendered + reviewed 2026-09-22).

## THESIS

One committed accent on cut-stone surfaces. The app reads as a single mineral
object: obsidian canvas (dark) or satin platinum (light), stone-slab islands
with 1dp specular rims, and champagne gold reserved exclusively for live
attention (unread, active states, read checks, send, brand). It refuses the
incumbent split personality: TelegramBlue Material primary with sprinkled gold.

## OWN-WORLD

- Canvas: `#0B0F17` dark / `#F5F7FA` light; faint static mineral glows, no animation.
- Islands (top bar, bottom bar, composer, menus): vertical gradient
  `#141C2A → #101622` dark / white → near-white light + hairline rim
  (white 7-16% dark; slate 7% light) + one soft shadow.
- Accent: gold `#E2B755` dark / `#B45309` text + `#D97706` fills light.
  Functional jewels only: emerald = presence/security, azure = links/pending,
  ruby = destructive.
- Outgoing bubble: sapphire-steel gradient `#2C4C78 → #1E3557` dark,
  champagne `#FAEFD3 → #F2E0B4` light. Incoming: `#1A2130` dark, white light.
  Tail corner 7dp, others 19dp. Meta 10.5sp; read checks gold.
- Type: Plus Jakarta Sans everywhere; titles 15.5-16 bold tracked -0.01em;
  preview 13.5 medium; meta 11-11.5 semibold.

## STORY

A member sees unread attention in one scan (gold timestamps + badges + island
dot), opens a chat, and reads a conversation whose ownership is unambiguous:
right = mine (colored), left = theirs (neutral). The composer is the only
always-lit affordance at rest.

## FIRST VIEWPORT (chat list)

Status bar → brand row ("SimpleUX" gold wordmark 21sp bold + search + kebab,
borderless slate icons) → folder pill row (active = gold wash + gold rim +
gold text) → flat mineral list, 68-72dp rows: 52dp jewel-tone avatar with
specular ring, bold one-line title (verified tick, pin mark), 13.5sp slate
preview (sender prefix, draft tag, pending azure), right column = timestamp
(gold when unread) over badge (gold gradient; slate when muted) → floating
island bar (Chats with unread dot, Contacts, Scan, Settings; active = gold
wash pill).

## FORM

Established world extended, not replaced: Luxury Mineral per
`.agents/skills/simpleux-design-system/SKILL.md`. Signature interactions:
spring press states (existing bounceClick), gold send morph in composer,
swipe-to-reply glow. Motion budget stays small: instant tab switches (#58
lint guard), no list entrance animations.

## FINISH

Unreviewed and undocumented is unfinished; this build ends with the finish
review (emulator captures of both themes), the verdict, and design tokens
documented where the repo expects them (design-system SKILL.md / plans/).
