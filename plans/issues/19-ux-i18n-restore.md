<!-- labels: priority:high, area:ux, drift:low -->
# [UX/i18n] Restore MR.strings localization; fix localized app_name leftovers

**Priority:** High · **Upstream drift risk:** Low

**Files:**
- 26+ hardcoded literals in `views/ux/**` (FilterPillsRow labels, SwipeableChatCard actions, SecurityBadge alert, ThemeAnimation labels, ZeroJargonOnboarding copy)
- `views/chat/item/ChatItemView.kt:743` ("Chiffrement de bout en bout actif"), `ChatListView.kt:1601` ("Aucune discussion locale trouvée…"), tab labels in ChatListView
- `views/usersettings/UserProfileView.kt` (mixed FR placeholders / EN buttons)
- `resources/MR/{fr,de,es,fi,ja,el,iw,lt,pt,cs,ko,it}/strings.xml` (`app_name` still "SimpleX")

**Problem:**
Dozens of user-visible strings bypass the MR localization system entirely (hardcoded French, some mixed with English). Localized `app_name` overrides still say "SimpleX", so French/German/etc. devices show the wrong launcher label despite the base rename to "SimpleUX".

**Recommended fix:**
Extract every user-visible literal into `MR.strings` (base EN + FR translations at minimum); remove the stale localized `app_name` overrides so the base name applies everywhere.

**Acceptance criteria (DoD):**
- [ ] Zero hardcoded user-visible strings in fork code (grep audit)
- [ ] Launcher label shows SimpleUX in all locales
- [ ] Language switch works for all new UI
