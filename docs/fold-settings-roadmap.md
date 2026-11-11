# Duo implementation status

This fork targets the owner’s Xiaomi 18 Fold. General device compatibility is not a goal.

## Current Duo work (2026-11-11)

The project, installation package, code namespace, launcher label and project links
now belong to `hypermusiccover-duo` / `com.zhuhuibin.hypermusiccover.duo`.
Maintainer: `pku-zhb`. Automatic updates remain disabled; upstream copyright and
credits are preserved. The history below describes the separate Fold preview
package and must not be read as verification of a Duo installation.

Preview `0.2.0-preview.1` (`20001`) was installed on the connected device. Debug and
Release builds, release lint/R8 and all 625 JVM tests pass. This preview uses the
local debug signing certificate, not a production release key. The old Fold app
is retained, and its four local appearance/language preference files were copied
to Duo with byte-for-byte verification. Existing SystemUI runtime settings remain
in their original shared location.

The owner performs LSPosed selection, all phone UI interactions and restarts.
The current read-only runtime probe confirms Duo build 20002 after the authorized
direct update below. The owner performs phone UI interactions and explicit restarts.

### Owner acceptance and remaining issues in preview 1

- Camera opens correctly; shortcut animation origins and release cancellation work.
- Torch still needs a precise start and only opens intermittently.
- The mixed-script vertical text is acceptable on the outer screen.
- Collapsing the player no longer flashes the native media card. The clock still
  jumps between its animated endpoint and its final position.
- Unlock from the lit lock screen and PIN screen works. Direct AOD unlock still
  flashes black, for less time than before.
- Artwork and lyrics can require repeated initial taps from player-only startup.
- Left-area upward swipes can unlock or show notifications without opening music.
  Two-item HyperOS capsule switching can lose the other bubble or repeat an item.
- Inner-screen and inner-landscape acceptance is explicitly deferred.

### Preview 2 changes awaiting device acceptance

The candidate is `0.2.0-preview.2` (`20002`). Its interaction contract is:

- HyperOS has one main capsule and a bubble for each other collapsed item, in fixed
  top-to-bottom order: ordinary notifications, individual focus/live cards, music.
  Missing kinds leave no placeholder. Existing focus-card order survives updates.
- A bubble tap selects that capsule; a main-capsule tap retains its open action.
  Vertical swipes cycle in either direction, including with only two items. One
  remaining item does not turn a swipe on the capsule into a card-open gesture.
- An upward swipe starting in the left clock/notification area opens the selected
  main capsule first, then the next selected collapsed item on a later gesture.
  Only after all are expanded does that gesture return to native swipe-to-unlock.
  Short, reversed and cancelled claimed drags open nothing. ColorOS uses the same
  selected-item priority while retaining its single-capsule presentation.
- The outer-screen rail is bottom aligned above the native shortcuts, moving it
  down. An overflow window retains all items for cycling if many live cards cannot
  fit at once. Inner-screen placement retains its existing baseline.
- Focus cards are identified through the system's focus-notification predicate and
  `param_island`, not a hardcoded vendor list. A real Meituan delivery card still
  needs device acceptance; a missing focus payload cannot be inferred from its app.

Other corrections in this candidate:

- Torch can start within an extra 8 dp of its displayed icon, only when that point
  is closer to torch than camera. Native activation distance and cancellation stay
  intact. Counters now retain assisted hits, peak travel and activation at release.
- The clock exit targets the rail's resting vertical lift and horizontal centre,
  then hands off to that same geometry before drawing the final frame.
- Artwork/lyrics controls select a concrete page before entering cover mode;
  lyrics page toggles retain parsed lyrics and an in-flight lookup.
- Repeated doze callbacks cannot clear an ongoing keyguard exit, and AOD wake
  callbacks cannot re-enter the cover while unlocking. This is a black-flash
  candidate, not a confirmed fix. Bounded transition and dim histories are separate
  so a fade's alpha samples cannot erase the unlock timeline.
- Package replacement only logs that a manual restart is needed. Preview 1's
  already loaded receiver kills scoped processes on replacement; the owner
  explicitly authorized a direct replacement for this transition.

Host verification for preview 2: all 638 tests in 108 suites passed, with zero
failures, errors or skips. Debug and Release APKs built successfully, including
release R8 and vital lint. The retained release APK is
`releases/local/hypermusiccover-duo-0.2.0-preview.2.apk`, SHA-256
`efd278ed6e7add37b4583ed4e1819d104cb844e3510eb49e11e00a6e808e719e`.
Its debug signing certificate matches the installed preview 1, whose APK hash was
checked directly on the device. This is a preview artifact, not a production key.

Device delivery: the owner requested ordinary replacement instead of the prepared
staged update. Session `1254977386` was abandoned and verified absent before the
direct install. PackageManager returned `Success`; the installed package is now
20002 and its on-device APK hash matches the retained artifact above. A read-only
SystemUI probe reports `runtimeVersion=20002`, so the new hooks are already loaded.
The old receiver restarted SystemUI on replacement; no explicit restart command,
phone UI interaction, scope change or device reboot was performed. A whole-device
reboot is not needed for this update. The temporary APK in `/data/local/tmp` was
removed. Future authorized previews use ordinary replacement, with explicit
restarts left to the owner and no automatic restart in the new module code.

Manual acceptance with preview 2 loaded (outer screen only):

1. In HyperOS style with music and notifications, swipe the capsule up and down
   repeatedly. Exactly one capsule and one bubble remain; either direction changes
   the selection. Tapping the bubble selects it. With a real focus card, verify the
   fixed notification/focus/music order and that absent categories leave no gap.
2. Collapse all content, select music on the right, then swipe upward from the left
   clock/notification area. Music opens first. Repeat to open remaining collapsed
   content; only the following upward swipe can unlock. Repeat with notifications
   selected first and with ColorOS style. Capsule swipes only switch selections.
3. Start a fresh lock session in player-only mode. The first artwork and lyrics
   button taps should select their intended pages. Repeat after collapsing/reopening.
4. Collapse the expanded player several times. Neither the native card nor the
   clock should jump back after the collapse animation lands.
5. Try torch from the icon centre and near its rim, dragging left/up-left. Record
   which starts still fail. Short releases must still cancel; camera must still work.
6. Compare direct AOD unlock with lit lock-screen unlock. Report any remaining black
   flash and which route causes it. Do not infer success from the host AOD tests.

### Preview 1 shortcut geometry and swipe direction

The owner reports that camera opens on an up-left drag but its animation starts
at MiDuo's earlier position. Torch has also opened with a rightward drag.
These were the reports before Duo preview 1; the current acceptance above
supersedes them. The current runtime is Duo build 20002.

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

Host geometry and native-axis contract tests pass. The owner accepted camera and
animation behavior; torch reliability remains open and has the preview 2 candidate.

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
used by the text planner. The owner accepted the outer-screen visual result.

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

## Historical Fold settings consolidation

- Black-flash investigation was deferred at that stage; the preview 2 work above
  resumes it with a bounded candidate and diagnostics.
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

- Keep the accepted outer-screen mixed-script renderer; inner-screen validation
  remains deferred until the owner resumes it.
- Investigate full-screen cover refresh and background rendering before re-enabling any related controls. Restore meaningful blur, dim and animated-background settings only with a supported implementation.
- Maintainer and destination migration is complete in Duo; upstream attribution stays in Credits and NOTICE.
- Duo destinations are configured. A signed release/update policy is still required before enabling automatic updates.
- Revisit the horizontal demonstration after the real interaction and animation design is settled.

The demonstration assets remain unchanged. Duo identity is configured; upstream attribution and license notices are preserved.
