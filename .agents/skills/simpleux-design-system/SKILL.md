---
name: simpleux-design-system
description: >-
  Official design system and UI/UX engineering standard for SimpleUX Chat.
  Defines the Luxury Mineral & Haute Horlogerie design language, color tokens,
  specular rims, typography, micro-interactions, component patterns, and coexistence rules.
---

# SimpleUX Luxury Mineral Design System

This skill defines the authoritative design language and UI component standards for **SimpleUX Chat**. All new views, features, and refactors MUST follow these specifications.

---

## 1. Core Visual Philosophy: "Haute Horlogerie & Luxury Mineral"

SimpleUX combines 100% metadata-free cryptographic privacy with an ultra-premium, tactile, and minimalist aesthetic inspired by precision luxury hardware, frosted minerals, and haute horlogerie.

### Key Pillars:
1. **Sober & Unified Mineral Surfaces:** Smoked Titanium / Obsidian in Dark Mode, Frosted Satin Platinum in Light Mode. Avoid rainbow pastel patchwork.
2. **Specular Bevels & Hairline Rim Highlights:** Subtly illuminated top-down rim gradients (`1.dp` `Brush.verticalGradient`) giving tactile depth without heavy dropshadows.
3. **Warm Champagne Gold Accents:** `#E2B755` (Dark) / `#D97706` (Light) for active states, CTAs, interactive highlights, and verification.
4. **Fluid Spring Micro-Interactions:** Snappy, bouncy animations for message sending, swipe-to-reply, reaction pickers, and tab transitions.

---

## 2. Color Palette & Material Tokens

### 2.1 Surfaces & Backgrounds
| Surface Role | Dark Mode (Obsidian / Titanium) | Light Mode (Platinum / Satin) |
|---|---|---|
| **Canvas / Window Background** | `Color(0xFF0B0F17)` / `0xFF0F172A` | `Color(0xFFF8FAFC)` / `Color.White` |
| **Card / Inset Background** | `Brush.verticalGradient(listOf(Color(0xFF1E2533), Color(0xFF131720)))` | `Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)))` |
| **Floating Island Bar** | `Color(0xEE121A26)` (solid glass) | `Color(0xF5F8FAFC)` |
| **Action Discs (Quick Actions)** | `Color(0xFF1E293B)` | `Color(0xFFF1F5F9)` |
| **Hover / Pressed Tint** | `Color(0x2A94A3B8)` | `Color(0x1F64748B)` |

### 2.2 Specular Rim Gradients (1.dp hairline stroke)
```kotlin
// Dark Mode Specular Rim
val darkSpecularRim = Brush.verticalGradient(
    listOf(Color(0x38FFFFFF), Color(0x0EFFFFFF))
)

// Light Mode Precision Bevel
val lightSpecularRim = Brush.verticalGradient(
    listOf(Color(0x250F172A), Color(0x0C0F172A))
)
```

### 2.3 Accent & Functional Jewels
- **Primary / Active Gold:** `#E2B755` (Dark) / `#D97706` (Light)
- **Security / Privacy (Emerald):** `#10B981` (Dark) / `#059669` (Light)
- **Destructive / Alert (Ruby):** `#EF4444` (Dark) / `#DC2626` (Light)
- **Links / Connection Azure:** `#38BDF8` (Dark) / `#0284C7` (Light)

### 2.4 Typography Hierarchy
- **Primary Text:** `Color(0xFFF8FAFC)` (Dark) / `Color(0xFF0F172A)` (Light) - `FontWeight.Bold` / `SemiBold`
- **Secondary / Subtitles:** `Color(0xFFCBD5E1)` (Dark) / `Color(0xFF334155)` (Light) - `FontWeight.Medium`
- **Muted / Section Titles:** `Color(0xFF94A3B8)` (Dark) / `Color(0xFF64748B)` (Light) - `13.sp`, `FontWeight.Normal`

---

## 3. Core Component Standards

### 3.1 Avatars & Default Monograms (`ProfileImage` / `ChatInfoImage`)
- **Default Monogram:** 1-2 character initials inside a circular Titanium Satin / Smoked Obsidian disc with vertical specular rim and bold typography (`fontSize = size * 0.38f`, `letterSpacing = 0.6.sp`).
- **System Jewels:** Specialized discs for Private Notes (Champagne Gold), Invitations (Azure Ice), and Groups (Teal Sage).
- **No Square-in-Circle Artifacts:** Strict `clip(CircleShape)` with no duplicate outer borders.

### 3.2 Top Bar Action Icons & Standard Dropdown Menus (`DefaultDropdownMenu` & `ChatListView`)
- **Top Bar Action Icons:** Borderless `IconButton` integrating naturally into the canvas without container pressure, tinted in muted slate (`#CBD5E1` dark / `#475569` light).
- **Dropdown Card:** Clean rounded card (`RoundedCornerShape(16.dp)`), width `220.dp`, specular hairline border.
- **Menu Rows:** Vanilla, standard layout with left-aligned icons (`20.dp`), standard label typography (`15.sp`), and fine logical dividers.

### 3.3 Floating Bottom Navigation (`TelegramBottomIslandBar`)
- Capsule pill shape (`RoundedCornerShape(32.dp)`) floating above the bottom edge.
- 3 Root Tabs: **Chats**, **Contacts**, **Settings** (Profile fused into Settings).
- Active tab indicated by warm gold pill container with subtle border.

### 3.4 Message Reply / Quoted Context Bar (`ContextItemView`)
- Card container with `RoundedCornerShape(16.dp)` and specular hairline rim.
- Vertical Champagne Gold accent bar (`3.5.dp` width, rounded caps) on the left.
- Dedicated circular disc for context icon (`ic_reply`, `ic_edit`, `ic_forward`).
- Polished close button (`24.dp` circular disc with centered `ic_close`).

### 3.5 Full-Row Swipe-to-Reply Interaction (`ChatView` & `ChatItemView`)
- **Full Horizontal Line Coverage (100% Width):** The user can swipe left anywhere on the horizontal row (on the message bubble, on empty space to the left or right).
- **Universal Support:** Works identically for received AND sent outgoing messages.
- **Swipe threshold:** `44.dp` travel distance.
- **Animated indicator:** Smooth scaling (`0.45f` -> `1.1f`) and rotation (`-35 deg` -> `0 deg`) as the user drags.
- **Trigger feedback:** Turns into a solid Glowing Gold disc (`#E2B755`) upon crossing the trigger threshold.

### 3.6 Grouped Inset Settings Cards (`SectionView` / `SettingsView`)
- Rounded cards (`RoundedCornerShape(18.dp)`) with `1.dp` specular rim.
- Section titles in muted mineral slate (`#94A3B8` / `#64748B`).
- Action rows with discrete circular action icons and subtle chevrons.

---

## 4. Development Checklist for New Features

- [ ] Does the screen strictly respect Dark & Light mode mineral tokens?
- [ ] Are top bar action buttons borderless and naturally blended without container pressure?
- [ ] Are cards encapsulated with `RoundedCornerShape(16.dp)` or `18.dp` and specular hairline rims?
- [ ] Are all avatars rendering as clean, clipped circles without rainbow pastel clashes?
- [ ] Are root tabs navigation arrows removed in favor of clean title headers?
- [ ] Is there zero content bleed-through (solid background applied to root tab containers)?
- [ ] Is full-row swipe gesture behavior maintained without restricting gestures to the text container?
- [ ] Is Android package identity (`chat.simplex.ux`) and protocol compatibility strictly preserved?
