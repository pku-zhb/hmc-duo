# Duo implementation status

This fork targets the owner’s Xiaomi 18 Fold. General device compatibility is not a goal.

## Current Duo work (2026-11-11)

The project, installation package, code namespace, launcher label and project links
now belong to `hypermusiccover-duo` / `com.zhuhuibin.hypermusiccover.duo`.
Maintainer: `pku-zhb`. Automatic updates remain disabled; upstream copyright and
credits are preserved. The history below describes the separate Fold preview
package and must not be read as verification of a Duo installation.

Preview `0.2.0-preview.1` (`20001`) is installed on the connected device. Debug and
Release builds, release lint/R8 and all 625 JVM tests pass. This preview uses the
local debug signing certificate, not a production release key. The old Fold app
is retained, and its four local appearance/language preference files were copied
to Duo with byte-for-byte verification. Existing SystemUI runtime settings remain
in their original shared location.

The owner performs LSPosed selection, all phone UI interactions and restarts.
Background installation does not establish that Duo hooks are active. No process
restart or Duo phone behavior has been verified in this session.

### Shortcut geometry and swipe direction candidate

The owner reports that camera opens on an up-left drag but its animation starts
at MiDuo's earlier position. Torch has also opened with a rightward drag.
The last runtime probe reported Fold build 10101025; a Duo runtime probe after the
owner's module switch/restart is pending.

The live AOD APK matches the analyzed sample (SHA-256
`c336e5ec5e42d2584a972a223b5334a7f9efe97a50cd8a487957d890dac3eec6`).
Its `ShortcutMoveController` tests the displayed icon's screen position, but
calculates positive horizontal travel for the semantic left shortcut and negative
travel for the right shortcut. `ShortcutOccludedAnimController.onTouchUp` uses the
native activation state; the release coordinates do not select or launch an app.
MiDuo's `BaseShortcutAnimController.updateShortcutView` hook separately caches its
own relocated animation origin.

The candidate therefore:

- Leaves MiDuo's button positions and native animation origins together; the capsule
  reserves room above them instead of moving them again.
- Gives the entire gesture starting on a native shortcut back to the native path.
- Projects only the relocated left shortcut's X travel onto the original axis,
  so both buttons use an inward leftward drag. Vertical travel, slop, activation,
  cancellation and selected shortcut identity remain native.
- Removes the unsuccessful forced-tap activation adapter. Read-only counters expose
  native hits, direction and projection via `layoutstate.shortcutGesture`.

Host geometry and native-axis contract tests pass. Device acceptance of the new
Duo package is pending; neither button is declared fixed by these tests.

### Vertical capsule typography

The right column remains the title and the left column the secondary information.
The renderer measures actual font advances: CJK stays upright, horizontal-script
words remain shaped sideways, one to four digits share one horizontal cell, and
punctuation uses vertical forms. Extended grapheme clusters, including joined emoji,
flags and skin tones, stay intact. Long content reserves a vertical ellipsis cell;
opening the native card exposes the full text. The underlying accessible text stays
unmodified. No continuous marquee runs in the small vertical capsule.

Design references: [W3C vertical text](https://www.w3.org/International/articles/vertical-text/)
and [Pages vertical text](https://support.apple.com/en-mide/guide/pages-iphone/tanc8021e516/ios).
These informed the mixed-script layout; the bounded two-column summary is a Duo
interaction choice. Android supports the [extended grapheme matcher](https://developer.android.com/reference/java/util/regex/Pattern.html)
used by the text planner. Phone visual acceptance remains pending.

### Disabled-feature investigation

| Feature | Current finding and recovery boundary |
| --- | --- |
| Full-screen artwork | `Main.effectiveCoverStyle` and `FoldSettingsPolicy` force the supported square style; inner-landscape composition also assumes that geometry. Restore a per-display full-cover scene only after its clock, notification floor, lyrics and AOD transitions have an accepted geometry contract. |
| Background switching / blur / moving backdrop | `FoldSettingsPolicy.nativeBackdrop` remains true for the right rail even if the availability flag is lifted. `CoverPush` then bypasses wallpaper delivery. Separate cover geometry from background selection and retain the existing generation/first-frame handoff; flipping the UI switch alone cannot work. The installed wallpaper has a version-bound GL-handler/re-upload profile, so a generic invalidate is insufficient. |
| Capsule Gaussian blur | Retired during material consolidation. Native Soft Glass and following the final native-player material remain the supported options; a second independent blur layer would need a clear interaction with HyperLight and AOD. |
| Notification marquee | Replaced by measured mixed-script typography and full-text expansion. A perpetual marquee in two narrow columns obscures reading order; it stays off. |
| HyperCanvas / StandBy | Hidden extension pages retain their underlying code. The upstream Canvas provider hardcodes the Enhanced host package and requires matching signatures. Duo host checks now use its own application ID, but the provider and extension build/signing must be migrated together before re-enabling. |
| Waveform | The page is hidden; its audio acquisition/runtime code remains. Validate the capture source, permissions and AOD lifecycle independently before restoring the controls. |
| Donations / experimental iPhone gestures | Retired intentionally, not compatibility defects to re-enable. Unused donation resources and funding configuration are removed. |

## Historical Fold device reports (2026-11-11)

### OPEN: lock-screen shortcuts do not reliably launch

- Affected build: `10101024` / `1.1.1-15-fold-preview`, package `com.zhuhuibin.hypermusiccover.fold`.
- Reproduction: on the lit lock screen, tap the right-column flashlight or camera button. The expected shortcut does not open. Earlier reports included press/shake feedback without activation.
- The owner confirmed that both buttons still fail after installing build 10101024. Treat this as an unresolved functional bug; do not mark it fixed based on compilation, JVM tests, or successful hook installation.
- Previous attempts: build 10101023 changed the occluded animation controller's release branch; build 10101024 observes the native move controller's touch stream and initializes the native shortcut before activation. Neither attempt has resolved the device behavior.
- Existing read-only diagnostics: `layoutstate.shortcutTap` reports installation, down, hit, up, cancel, native-release and activation counts, plus the last error. Build 10101024 was confirmed running with the adapter installed and no reported error before the owner's latest test. Post-tap counters for that test have not been captured.
- Investigation is deferred at the owner's request. No further phone operations or installation are required for this record. On resumption, inspect the native event counters and selected-shortcut launch path on the actual failing screen; affected display forms still need separate verification.

### VERIFIED: player downward-collapse restored

- Reported against installed build `10101024`: the expanded music player no longer returns to its capsule on a downward swipe. The owner could not recall whether it fails on first expansion or only after reopening.
- Read-only state showed a visible outer-screen player and an earlier completed media exit (`lastExit=returned`, 79 frames) in the same process.
- Source inspection found that the music key stayed in the closing set after an exit and was cleared only by the dedicated island-opening path. Startup defaults, artwork and lyrics controls can reopen music without that path.
- Candidate build `10101025` uses the active exit animation as the music close guard, clears old-session pending markers on unlock, and keeps notification-first, one-card-per-gesture order. Regression tests cover repeated close/reopen cycles without the island-opening callback and stale music markers.
- After installing build 10101025, the owner confirmed that the player can collapse again. The new `layoutstate.cardSwipe` and `layoutstate.railDismiss` fields expose bounded gesture and close-state data without screenshots or report-file writes.

### OPEN: native player flashes back after its exit; capsule briefly disappears

- Reported on build `10101025` after collapse functionality was restored: the capsule briefly vanishes, then the player reappears at its native position after sliding off-screen before the capsule takes over.
- Candidate `10101026` (`beta1-animation-preview`) acquires native-header suppression before resetting the animated matrix/transition alpha, retains an existing capsule through the independent exit, and prevents the slower cover-clock exit from re-showing the returned native header. Device verification is pending; this candidate is not installed.
- The separately delivered `beta1` APK is the completed 10101025 behavior with only the version name changed. It intentionally excludes the animation candidate and retains the reported flash issue. It is saved as `00 Temp/hypermusiccover-beta1.apk`; package identity, version code 10101025 and signer are preserved.

### VERIFIED: lyrics button restored

- In build 10101024, the owner confirmed that the lyrics button works again.
- Before the owner's test, the live lock-screen probe reported the button requested and visible, progress `1.0`, alpha `1.0`, and size `110x110` pixels. This resolves the missing lyrics-button report independently of the open shortcut-launch bug.

## Current cleanup

- Remove adaptive widening when shortcuts are disabled, notification pill shrinking/size/drop controls, and “open notifications without the player”. Ignore their old enabled flags when loading or importing configuration.
- Remove the custom Gaussian blur override. Offer native Soft Glass or following the native player’s final material, including HyperLight adjustments.
- Keep notification text scrolling disabled and marked pending redesign. The current two-column vertical text remains.
- Disable full-screen artwork and background switching in settings and at runtime boundaries. Keep their implementation for later diagnosis. Square artwork preserves the existing wallpaper.
- Keep the demonstration unchanged.
- Remove the A/B tab, its hidden unlock gesture, its exclusive iPhone-style native-island hooks, configuration commands and backup fields. Old module state and imported backups cannot reactivate these removed features; the app drops the obsolete unlock flag on load. Shared native media-card rendering and theme selection remain available.

## Dedicated Fold settings consolidation

- Black-flash investigation is deferred at the owner’s request; retain existing diagnostics.
- Replace overlapping music-entry toggles with collapsed-all, expanded-all, and music-only startup layouts. Music-only offers player, artwork or lyrics. Inner landscape has separate initial-state and music-view choices, applied on its first entry in each lock session; AOD wake retains live choices.
- Remove full-size-button and under-display-fingerprint controls. Expose the actual notification-floor override as one switch.
- Hide waveform and extension pages. Native island appearance stays Soft Glass; normal-player backgrounds remain configurable.
- Allow returning from the lyric-provider notice before its acknowledgement countdown ends.
- Pause update checks, disable pending project/community/feedback links, remove donation UI, and list the Enhanced upstream maintainer separately in credits.
- Restore the inner player’s full column width and compact its vertical layout instead.
- Inner-portrait artwork is capped at the measured native player width. The clock can reclaim spare vertical room within the content width.
- Keep the lyrics button visible in every expanded rail music page when lyrics are available. Rail visibility is applied directly rather than waiting for appearance/slot animators; default artwork must not select the legacy hidden button slot. The inner-landscape view selector is labelled Artwork and lyrics.
- Native shortcut taps are observed in ShortcutMoveController and initialize ShortcutOccludedAnimController before its original release/launch branch. The adapter records native down, hit, up, cancel and activation counts. Native shortcut launch remains unresolved on the device, as recorded above. Drags, long presses, multi-touch, stale events and off-lock-screen input retain native handling. The capsule follows the column’s parent transform, excluding individual icon press/shake transforms.

## Deferred work requiring discussion

- Validate the new mixed-script vertical notification renderer on the phone and
  refine it against actual clipping, orientation and reading-order reports.
- Investigate full-screen cover refresh and background rendering before re-enabling any related controls. Restore meaningful blur, dim and animated-background settings only with a supported implementation.
- Maintainer and destination migration is complete in Duo; upstream attribution stays in Credits and NOTICE.
- Duo destinations are configured. A signed release/update policy is still required before enabling automatic updates.
- Revisit the horizontal demonstration after the real interaction and animation design is settled.

The demonstration assets remain unchanged. Duo identity is configured; upstream attribution and license notices are preserved.
