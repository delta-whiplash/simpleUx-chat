<!-- labels: priority:high, area:ux, drift:low -->
# [Regression] Voice notes: fake speed control, fake waveform, lost compact layouts & brokenAudio indicator

**Priority:** High · **Upstream drift risk:** Low

**Files:**
- `views/ux/components/VoiceWaveformPlayer.kt:35-37,134-138`
- `views/chat/item/CIVoiceView.kt:26,92-104,122`

**Problem:**
The speed toggle (1x/1.5x/2x) cycles local state but is never applied to `AudioPlayer` — a no-op control. Waveform bars are static fake data. Compared to upstream `VoiceLayout` (now dead code at `CIVoiceView.kt:122`), the rewrite lost: `smallView` compact rendering (chat-list previews now show a full 38dp interactive player), the `hasText` compact layout, and the `brokenAudio` error indicator. The dead `GlassVoiceNotePlayer` import remains at line 26.

**Recommended fix:**
Apply the selected speed to `AudioPlayer` (upstream playback supports it); render a real waveform or an honest placeholder; restore the compact variants and error state; remove the dead import.

**Acceptance criteria (DoD):**
- [ ] Speed control actually changes playback speed
- [ ] Real waveform data or clearly non-misleading placeholder
- [ ] `smallView` / `hasText` / `brokenAudio` variants restored
