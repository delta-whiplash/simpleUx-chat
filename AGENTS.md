# AGENTS.md — Developer & AI Agent Guide for SimpleUX Chat

Welcome to **SimpleUX Chat**! This document provides essential context, architectural guidelines, coexistence rules, and development instructions for any developer or AI agent working on this codebase.

---

## 1. Project Vision & Core Philosophy

### 1.1 What is SimpleUX Chat?
SimpleUX Chat is an interface-first fork of **SimpleX Chat** — the first messaging platform with no user identifiers of any kind (100% private and metadata-free by design).

While the underlying SimpleX protocol and Haskell backend are cryptographic masterclasses, the user experience and UI have historically had a high learning curve and visual friction.

**The Mission of SimpleUX:**
> **"Uncompromising privacy meets world-class user experience."**  
> We completely overhaul and modernize the frontend user interface, navigation, micro-interactions, and visual design while keeping the backend core, encryption protocols, and network compatibility 100% intact.

### 1.2 Non-Negotiable Core Tenets

1. **100% Protocol & Network Compatibility:**
   - SimpleUX users MUST be able to seamlessly communicate with users on official SimpleX clients (Android, iOS, Desktop, CLI).
   - All standard SimpleX features (1-to-1 chats, private groups, SMP servers, XFTP file transfers, audio/video calls, bots, hidden profiles) must remain fully interoperable.
   - NEVER alter wire formats, serialization protocols, or cryptographic invariants.

2. **Frontend-Only Scope:**
   - The Haskell core engine (`libsimplex` / `libapp`) and JNI/FFI bindings are treated as the stable, authoritative backend layer.
   - Our primary development playground is the **Frontend Layer**: Kotlin Multiplatform / Jetpack Compose Multiplatform (`apps/multiplatform/`) and Swift / SwiftUI (`apps/ios/`).

3. **Coexistence on the Same Device:**
   - SimpleUX is explicitly engineered to **coexist** alongside the official SimpleX app on a user's phone or computer.
   - Users should be able to run both apps simultaneously to test, migrate, or use different profiles without namespace, data, or package collisions.

---

## 2. Architecture Overview

SimpleUX inherits SimpleX's multi-tier architecture, maintaining clear boundaries between UI, application state, FFI bridge, and Haskell core:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     SimpleUX Compose UI / SwiftUI                       │
│  • Modernized Themes (Color, Shape, Typography, Glassmorphism)          │
│  • Redesigned Views (ChatList, ChatView, MediaPreview, Settings, etc.)  │
│  • Fluid Gestures, Micro-animations & Streamlined Onboarding            │
└─────────────────────────────────────────────────────────────────────────┘
                                   │  ▲
             User Interactions     │  │  State Flows / Recomposition
                                   ▼  │
┌─────────────────────────────────────────────────────────────────────────┐
│                        Application Logic Layer                          │
│  • ChatModel (State container, reactive StateFlows)                     │
│  • ChatController (API dispatch, command queuing, event handlers)       │
│  • AppPreferences, NotificationManager, ThemeManager                    │
└─────────────────────────────────────────────────────────────────────────┘
                                   │  ▲
                   sendCmd()       │  │  recvMsg() / processReceivedMsg()
                                   ▼  │
┌─────────────────────────────────────────────────────────────────────────┐
│                   JNI / C FFI Bridge (Core.kt / C)                      │
│  • external fun chatSendCmdRetry()   external fun chatRecvMsgWait()     │
└─────────────────────────────────────────────────────────────────────────┘
                                   │  ▲
                     C Foreign Function Interface (FFI)
                                   ▼  │
┌─────────────────────────────────────────────────────────────────────────┐
│                     Haskell Core (libsimplex / libapp)                  │
│  • Double-ratchet E2EE, SMP queues, XFTP transfers, SQLite DB           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.1 Key Codebase Locations

| Directory / Path | Platform / Component | Purpose |
|---|---|---|
| `apps/multiplatform/` | Android + Desktop (KMP) | Kotlin Multiplatform app using Compose Multiplatform. |
| `apps/multiplatform/common/` | Shared KMP Code | Shared UI (`views/`), state (`model/`), design system (`ui/theme/`), and platform bridge (`platform/`). |
| `apps/multiplatform/android/` | Android App Container | Android application entry point, manifest, Android-specific services and activities. |
| `apps/multiplatform/desktop/` | Desktop App Container | JVM desktop entry point, distribution packaging, native desktop windowing. |
| `apps/ios/` | iOS Native App | Native iOS client in Swift / SwiftUI / UIKit. |
| `src/Simplex/Chat/` | Haskell Core Engine | Core chat logic, database migrations, cryptographic double-ratchet, SMP/XFTP protocol handling. |
| `apps/simplex-chat/` | CLI Terminal Client | Standalone console client for Linux, macOS, and Windows. |
| `plans/` | Architectural Plans | Design documents and technical roadmaps. |

---

## 3. Coexistence Strategy & App Identity

To allow SimpleUX to be installed side-by-side with official SimpleX on Android and iOS:

### 3.1 Android Identity
- **Application ID:** `chat.simplex.ux` (or debug suffix `chat.simplex.ux.debug`)
- **App Name:** `SimpleUX` / `SimpleUX Chat`
- **Content Provider Authorities:** `chat.simplex.ux.provider` (must never collide with `chat.simplex.app.provider`)
- **Database & Storage Paths:** Isolated sandbox directory created by Android OS under the unique package namespace.
- **Deep Links & Schemes:** Custom URL scheme handlers configured to avoid hijacking official app intents while supporting direct links.

### 3.2 iOS Identity
- **Bundle Identifier:** `chat.simplex.ux`
- **App Display Name:** `SimpleUX`
- **App Group / Keychain Sharing:** Dedicated `group.chat.simplex.ux` app groups and isolated keychain access groups.

---

## 4. UI/UX Modernization Focus Areas

When contributing to SimpleUX, prioritize the following user experience enhancements:

### 4.1 Visual Hierarchy & Design System
- **Modern Color Palettes & Contrast:** Rich dark modes (deep charcoal/slate with vibrant accents), accessible light modes, and dynamic Material You / expressive themes.
- **Typography & Readability:** Clean, modern font pairings with clear hierarchy between message body, metadata timestamps, sender names, and status indicators.
- **Fluid Micro-Interactions:** Smooth spring animations for message sends, swipe-to-reply, reaction pickers, and transition effects between screens.

### 4.2 Chat List & Navigation
- **Gesture-Driven Interactions:** Smooth swipe actions for pin, archive, mute, and mark-as-read.
- **Visual Status Badges:** Unobtrusive, clear delivery receipts (sent, delivered, read) and active call indicators.
- **Contextual Search & Filters:** Instant filtering by unread, groups, contacts, or media.

### 4.3 Chat View & Media Experience
- **Bubble Ergonomics:** Modern, rounded message bubbles with clear separation between incoming and outgoing messages.
- **Rich Media & Audio Player:** Polished inline audio waveforms with scrubber for voice notes, full-bleed media galleries, and snappy image previews.
- **Reply & Quote Context:** Clean, compact quote boxes that quickly jump to referenced messages.

### 4.4 Approachable Onboarding
- **Zero-Friction First Run:** Friendly, welcoming onboarding explaining zero-metadata privacy in simple, human terms without overwhelming cryptographic jargon.
- **Instant Connection Sharing:** Quick QR code display, animated scan viewfinder, and single-tap copyable connection links.

---

## 5. Development & Build Workflows

### 5.1 Prerequisites
- **Android / Desktop:** JDK 17+, Android SDK (API 35), NDK (23.1+), CMake.
- **iOS:** macOS with Xcode 15+, CocoaPods / Swift Package Manager.
- **Haskell (Core Development, optional):** GHC 9.4+, Cabal 3.8+.

### 5.2 Common Build Commands

```bash
# Navigate to multiplatform module
cd apps/multiplatform

# Build Android Debug APK (FOSS flavor)
./gradlew assembleFossDebug

# Build Android Release APK
./gradlew assembleFossRelease

# Package Desktop distribution for current OS (Windows MSI/EXE, macOS DMG, Linux DEB)
./gradlew :desktop:packageDistributionForCurrentOS

# Run JVM / Desktop tests
./gradlew desktopTest

# Run all code formatting / resource validation
./gradlew adjustFormatting
```

---

## 6. Guidelines for AI Agents

1. **Check Existing Specs & Documentation:**
   - Consult `apps/multiplatform/product/` and `apps/multiplatform/spec/` before implementing complex features.
   - When modifying UI views, ensure state flows in `ChatModel.kt` and `ChatController.kt` are respected.
2. **Preserve Native Core Integration:**
   - Do NOT modify JNI signatures in `Core.kt` unless explicitly paired with corresponding Haskell FFI changes.
   - Handle all asynchronous core events via existing coroutine streams (`chatRecvMsgWait` dispatcher).
3. **Maintain Clean Code & Diffs:**
   - Write idiomatic Kotlin and Compose code.
   - Avoid massive reorganizations of unrelated legacy files to ensure smooth upstream merges and clean git history.
   - Test UI responsiveness on both portrait mobile and multi-window desktop layouts.
