# SimpleUX Chat 💬✨

> **The modern, delightful UI/UX fork of SimpleX Chat — where world-class design meets uncompromising, zero-metadata privacy.**

[![License: AGPL v3](https://img.shields.io/badge/License-AGPLv3-blue.svg)](./LICENSE)
[![SimpleX Protocol Compatible](https://img.shields.io/badge/SimpleX_Protocol-100%25_Compatible-brightgreen.svg)](https://simplex.chat)
[![Coexistence Ready](https://img.shields.io/badge/Coexistence-Side--by--Side_Ready-blueviolet.svg)](#-side-by-side-coexistence)
[![Platform](https://img.shields.io/badge/Platforms-Android%20%7C%20iOS%20%7C%20Desktop-orange.svg)](#-building--running)

---

## 🌟 Why SimpleUX?

**SimpleX Chat** introduced a cryptographic masterclass to the world: the first messaging platform with **no user identifiers of any kind**, end-to-end encryption with quantum resistance, and isolated unidirectional messaging queues.

However, great privacy shouldn't come at the cost of great user experience.

**SimpleUX Chat** is a community-driven, interface-focused fork with a single mission:
> **Elevate the user experience to match the brilliance of the underlying protocol.**

We rebuild and refine the frontend interface from the ground up — modernizing navigation, typography, animations, chat ergonomics, and onboarding — while leaving the robust Haskell backend and SMP/XFTP cryptographic protocols untouched.

---

## 🚀 Key Pillars

| Pillar | Description |
|---|---|
| 🎨 **Elevated UI/UX** | Refined modern design system, fluid micro-interactions, swipe gestures, elegant dark/light themes, and ergonomic message bubbles. |
| 🤝 **100% SimpleX Interoperability** | Fully compatible with standard SimpleX users, groups, SMP relays, XFTP file servers, and bots. You can chat seamlessly across SimpleUX and SimpleX! |
| 📱 **Side-by-Side Coexistence** | Engineered with distinct application IDs and sandbox paths so you can install SimpleUX alongside the official SimpleX app on the same phone. |
| 🔒 **Untouched Zero-Metadata Core** | Retains all SimpleX cryptographic guarantees: double ratchet E2EE, post-quantum key exchanges, no user IDs, no phone numbers, and local-only database encryption. |
| ⚡ **Frontend Focused** | The entire Haskell backend engine (`libsimplex` / `libapp`) is preserved as the authoritative core. We focus 100% of our energy on UI excellence. |

---

## 📱 Side-by-Side Coexistence

SimpleUX is designed so users don't have to choose between testing a new interface and keeping their existing setup. You can run both the official **SimpleX Chat** and **SimpleUX Chat** simultaneously on the same device:

- **Distinct Package ID & Namespaces:** Configured with dedicated application IDs (e.g. `chat.simplex.ux`) to avoid conflicts with `chat.simplex.app`.
- **Isolated Storage:** Independent local encrypted databases and media cache directories.
- **Independent Notification Channels & Services:** Background message sync services run without interfering with the official client.

---

## 🏗️ Architecture

SimpleUX preserves the rock-solid layered architecture of SimpleX, isolating UI/UX improvements to the client interface layer:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     SimpleUX UI Layer (Modernized)                      │
│  • Compose Multiplatform (Android + Desktop) / SwiftUI (iOS)            │
│  • Modern Themes, Fluid Motion, Gestures, Streamlined Navigation        │
└─────────────────────────────────────────────────────────────────────────┘
                                   │  ▲
             User Interactions     │  │  StateFlows / Recomposition
                                   ▼  │
┌─────────────────────────────────────────────────────────────────────────┐
│                        Application Logic Layer                          │
│  • ChatModel (Reactive state) & ChatController (Command dispatcher)     │
│  • AppPreferences, NotificationManager, ThemeManager                    │
└─────────────────────────────────────────────────────────────────────────┘
                                   │  ▲
                   sendCmd()       │  │  recvMsg() / processReceivedMsg()
                                   ▼  │
┌─────────────────────────────────────────────────────────────────────────┐
│                   JNI / C FFI Bridge (Core.kt / C)                      │
│  • chatSendCmdRetry() / chatRecvMsgWait()                               │
└─────────────────────────────────────────────────────────────────────────┘
                                   │  ▲
                     C Foreign Function Interface (FFI)
                                   ▼  │
┌─────────────────────────────────────────────────────────────────────────┐
│              Haskell Core Engine (libsimplex / libapp)                  │
│  • Double-Ratchet E2EE, SMP Queues, XFTP File Relays, SQLite Storage    │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Building & Running

### Android & Desktop (Kotlin Multiplatform)

Prerequisites: JDK 17+, Android SDK (API 35), CMake, NDK 23.1+.

```bash
# Navigate to the multiplatform project directory
cd apps/multiplatform

# Build Android Debug APK (FOSS Flavor)
./gradlew assembleFossDebug

# Build Android Release APK
./gradlew assembleFossRelease

# Package Desktop App for Current OS (Windows / macOS / Linux)
./gradlew :desktop:packageDistributionForCurrentOS

# Run Desktop / JVM Tests
./gradlew desktopTest
```

### iOS (Native Swift / SwiftUI)

Prerequisites: macOS with Xcode 15+.

```bash
cd apps/ios
# Open the Xcode project
open SimpleX.xcodeproj
```

---

## 🗺️ UI/UX Roadmap

- [ ] **Modern Chat List:** Refined list item hierarchy, smooth swipe actions (pin, mute, archive), and instant contact filtering.
- [ ] **Next-Gen Message View:** Beautiful chat bubbles, seamless media gallery, inline voice note waveforms, and intuitive swipe-to-reply.
- [ ] **Streamlined Onboarding:** Friendly zero-metadata privacy introduction, instant QR code generator, and simplified contact invites.
- [ ] **Design System & Theming:** True OLED black dark mode, modern soft light mode, dynamic Material You color matching, and custom accent palettes.
- [ ] **Ergonomic Settings:** Reorganized, categorized settings with simplified privacy toggles and intuitive server management.

---

## 📖 Developer & Agent Resources

- **[AGENTS.md](./AGENTS.md)** — Architectural overview, coexistence guidelines, and instructions for AI agents and contributors.
- **[KMP Coding Guide](./apps/multiplatform/CODE.md)** — Detailed multiplatform guidelines and three-layer navigation documentation.
- **[Original SimpleX Documentation](./docs/)** — In-depth specifications of the SMP/XFTP protocols and core cryptography.

---

## 🙏 Credits & Upstream Acknowledgment

SimpleUX Chat is an independent frontend fork powered by the groundbreaking work of **Evgeny Poberezkin** and the **SimpleX Chat community**.

We extend our deep gratitude and admiration to the SimpleX team for pioneering true identifier-free, privacy-preserving communication protocols.

- **Upstream Repository:** [simplex-chat/simplex-chat](https://github.com/simplex-chat/simplex-chat)
- **SimpleX Chat Official Website:** [simplex.chat](https://simplex.chat)

---

## 📄 License

This software is licensed under the **GNU Affero General Public License version 3 (AGPLv3)**. See the [LICENSE](./LICENSE) file for details.
