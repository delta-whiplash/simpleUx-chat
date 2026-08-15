<!-- labels: priority:high, area:ux, drift:low -->
# [UX] Onboarding: routing broken for existing users & desktop paths lost

**Priority:** High · **Upstream drift risk:** Low

**Files:**
- `views/onboarding/HowItWorks.kt:30`
- `views/ux/modals/ZeroJargonOnboarding.kt`

**Problem:**
The rewrite hardcodes `onboardingStage.set(OnboardingStage.Step2_CreateProfile)`. Upstream `OnboardingActionButton` routed conditionally: `user == null → Step2_CreateProfile`, `user != null → OnboardingComplete`, and desktop additionally offered LinkAMobile + DB-passphrase branches. Consequence: an **existing user** opening HowItWorks gets pushed back into profile creation; desktop users lose the link-a-mobile path. The new onboarding copy is French-only.

**Recommended fix:**
Restore conditional routing based on user state (and platform branches); provide the ZeroJargonOnboarding copy in all locales via MR.strings.

**Acceptance criteria (DoD):**
- [ ] Existing user taps start → onboarding completes (no profile re-creation)
- [ ] Desktop branches (LinkAMobile, DB passphrase) intact
- [ ] Onboarding copy localized
