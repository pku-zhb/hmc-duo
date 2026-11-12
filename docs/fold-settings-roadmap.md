# Duo implementation status

This fork targets the owner’s Xiaomi 18 Fold. General device compatibility is not a goal.

## Current Duo work (2026-11-12)

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
Preview 3 was delivered by ordinary replacement. At that delivery the installed
package was 20003 while the SystemUI probe still reported 20002, with its process ID
unchanged; the owner subsequently supplied preview-3 feedback. Preview 4 was then
installed after fresh explicit confirmation, temporarily replaced with preview 3
for a compatibility comparison, and restored to preview 4 at the owner's request.
Preview 5 is the next host-built candidate. The owner explicitly requested no
installation, phone debugging or restart for this iteration.

### Preview 5 feedback and preview 6 work (2026-11-12)

The owner accepted full-area downward collapse and immediate long-title avoidance.
Undersized cards have not recurred; long-background session recovery remains untested.
Black flashing and bottom-area horizontal input remain unresolved on the device.
The page-cycle request means reusing the native artwork slot, not adding an adjacent
button. Notification islands can also be absent after a collapse. HyperOS now needs
a visible-item limit of 2/3/4/5 (default 3), top clearance and overflow navigation.

The first host-validated input/notification changes are:

- The inspected stock APK (`074486d6856c8eda2764d8864d99365e178d4a4758836e3c44152862ddc8b7e1`)
  inlines a full-width `mTouchAtKeyguardBottomArea` intercept into
  `NotificationPanelViewController.TouchHandler.onInterceptTouchEvent`. It starts
  below the indication text and claims either axis after slop. Actual visible card
  streams clear that flag and bypass the hidden number-footer interceptor, without
  bypassing the rest of panel handling. Shortcut acquisition uses displayed icon
  screen coordinates and its existing tolerance, latched for the full stream.
- A horizontal notification dismissal keeps native ownership through later vertical
  wobble, even with other islands folded. Empty-space diagonal protection remains.
- A notification-return timeout no longer changes the user's collapsed choice to
  expanded. A single bounded native settle attempt and final publication reconciliation
  replace the premature cancellation. Repeated native NUMBER callbacks still publish.

These are source-supported candidates, not device acceptance. No phone interaction,
installation or restart is authorized for this iteration.

### Preview 4 feedback and preview 5 candidate (2026-11-12)

The owner accepted the content expansion order, the filled gesture gap, notification
entry choices, both independent notification options after disabling HyperLight's
equivalents, and the persistent thumbnail. The owner also confirmed that the veil
was present, then requested replacing the separate artwork/lyrics actions with a
fixed three-page cycle.

Remaining device reports were a rare expanded-notification/bubble position flash,
an intermittent missing notification-return animation (music return was reliable),
glass losing its advanced material during transitions, a stretched rather than
translated capsule, unwanted end-to-start cycling, and occasional undersized media
cards also reported on another device. Artist marquee sizing appeared inconsistent.
Black flashing remains open; the owner requested renewed investigation without
assuming that the MiDuo update caused it.

Before preview-5 delivery, the owner added three reports: paused/background music
could leave an unopenable capsule or button, a downward collapse did not start in
the clock area, and seeking was intercepted whenever another item remained folded.
These are included in this candidate rather than deferred to another preview.
The owner further reproduced long-title overlap with the visible output/source
control immediately after a track change; sleeping and waking corrected the
truncation. The media-control audit therefore includes same-view metadata changes,
visibility changes, text bounds, and touch priority rather than only initial binding.

Candidate `0.2.0-preview.5` (`20005`) addresses this batch:

- Expanded notification intent now also gates publication of its rail bubble, not
  just native wake defaults. A transient NUMBER report cannot publish a bubble for
  an explicitly expanded list. Explicit choices reconcile the published state.
- Native notification return normalizes the initial fade and follows scroll/fade
  progress. A one-traversal native completion retains a short visible bubble birth;
  unavailable completion has a bounded cancellation path rather than blocking later
  gestures. The stable music return path is retained.
- HyperOS switching selects immediate neighbours with hard ends. Its shell keeps
  its height while moving until the leading cap matches the target bubble. Both
  icons reach the joined shell before release; the outgoing icon then separates
  into its bubble. The existing damped nudge spring supplies the release recoil,
  independently of the monotone content handoff. ColorOS keeps its existing cycle.
- The shared glass layer is measured with its native material child before applying
  the material, waits for a valid contour, and updates native SDF bounds as it moves.
  This fixes an incomplete zero-size material setup; its appearance needs device
  acceptance.
- Content reflow keeps the uncontracted width and logical layout specification.
  OEM pixel-width writebacks cannot become a smaller baseline on every frame or
  lock session. Restoration only changes widths still owned by this adapter.
- One fixed control cycles native clock/wallpaper, artwork, lyrics, then clock.
  The icon names the next action; the thumbnail stays visible without a veil.
  Lyrics loading does not discard the requested page. The title and artist reserve
  real text-layout width for the control so native marquee measures the usable area.
  Visible native source/output/action controls also constrain each text row's
  usable bounds; hidden controls restore that space. Metadata and layout updates
  reconcile before drawing rather than waiting for an AOD cycle. Native control
  interaction and the user's existing visibility preferences remain authoritative.
- Repeated showing/doze reports cannot clear an ongoing unlock. Existing verified
  exit hooks share the exit state, explicit cancellation/new sleep can reset it,
  and queued scene work checks the current state. State transitions publish atomically
  across callback threads. This is a source-supported black-flash candidate, not
  a device-confirmed resolution.
- Destroyed media sessions cannot be reconstructed from a stale native card token;
  callbacks belong to the session that registered them. Valid paused sessions stay
  eligible without an arbitrary inactivity timeout. Lock-screen-only controls also
  respect the exit state instead of relying on a native view's lingering visibility.
- Downward collapse accepts the full content region, including the clock, while
  shortcuts and rail controls keep their own gestures. Native player controls claim
  their touch stream at DOWN so an unfinished expansion sequence cannot cancel a
  seek gesture partway through a horizontal drag.

Host validation: all 744 tests in 125 suites passed, with no failures, errors or
skips. Debug and Release builds passed, including release R8 and vital lint. The
final incremental verification includes the last callback and button-depth edits.
The signed release APK was verified as package `com.zhuhuibin.hypermusiccover.duo`,
version `20005` / `0.2.0-preview.5`, with the existing local debug certificate.
This remains a development preview rather than a production signing-key release.

Retained artifact: `releases/local/hypermusiccover-duo-0.2.0-preview.5.apk`, SHA-256
`db3381534006be45d8207906b65e2793a096c4a39280e8c20a820841a4cf104c`.
A matching copy is in the owner's `00 Temp` inbox. The earlier host-only draft was
retained locally with a `pre-feedback` suffix; the final APK includes every added
report above. Build logs and verification metadata remain outside synced storage.
All preview-5 phone acceptance remains pending. No installation or phone debugging
was performed for this iteration; the owner requested stopping after delivery.

### Preview 3 compatibility comparison (2026-11-12)

After updating MiDuo and another plugin, the owner explicitly requested installing
the retained preview 3 to compare behavior. Direct replacement with downgrade
enabled returned `Success`, preserving application data without uninstalling.
The previous installed package was 20004; the package became 20003 at that step and
its APK SHA-256 is
`d343d91b40de4506ab35a454fccc60dc465517db2f2446fc0e3e9f941462f028`, matching
`releases/local/hypermusiccover-duo-0.2.0-preview.3.apk`. Both APK signatures matched
before replacement. The temporary device APK was removed.

At the downgrade, SystemUI process IDs stayed unchanged and the read-only probe
reported runtime 20004 pending the owner's restart. No phone UI interaction,
scope change or restart was performed.

The owner subsequently reported that black flashing also occurs on preview 3
after updating MiDuo and its other plugin, and requested restoring preview 4.
This points to the updated environment rather than a preview-4-only regression;
it does not yet isolate MiDuo from the other updated plugin or prove a root cause.

The authorized restore used ordinary data-preserving replacement and returned
`Success`. Installed version 20004 and the device APK SHA-256 matched the retained
preview-4 artifact. SystemUI PID 3143 stayed unchanged, and the read-only probe
reported runtime 20003 pending the owner's manual restart. The temporary device
APK was removed. No GUI operation, scope change or restart was performed. The
preview-4 checks below describe that installation's acceptance plan. The subsequent
partial acceptance and remaining reports are recorded above.

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

### Owner feedback on preview 2

The owner tested the outer screen with music, notifications and a real system
calendar card. Right-side switching and first artwork/lyrics taps now work. Torch
is more forgiving after the screen settles. Direct AOD unlock did not reproduce
the black flash in the latest attempts; this is encouraging, not a definitive
resolution. Inner-screen acceptance remains deferred.

Requested follow-up: tapping a bubble should open its card, while swipes alone
switch the capsule. Left-area upward swipes should follow notification/live-card/
music order regardless of the selected capsule. The switch should merge/split
continuously. The clock still starts late and jumps horizontally at handoff, and
needs a small rightward visual adjustment. The active lyric flashes during sleep/
wake, initial lyrics can wait before appearing, and torch cannot start before
the wake animation finishes.

### Owner feedback on preview 3

The owner requested a new left-area order matching the native stack from top to
bottom: music, live cards, then ordinary notifications. Notifications unchecked
in the collapsed-category selector still disappeared after wake settled. The
thumbnail disappearing also moved the lyrics control. Rail switching needed one
finger-driven progress value, a rounded connecting neck, and a gradual return
bubble. Other tested behavior had no newly reported issue; inner-screen acceptance
remains deferred.

The owner additionally reported a narrow unowned touch gap between the right
controls and the content area: an upward drag there could bypass all expansion
and enter the unlock screen. Shortcut tolerance should remain generous while that
gap is filled.

### Preview 4 implementation and delivery history

The candidate is `0.2.0-preview.4` (`20004`).

- Content-area upward swipes open music, live cards in their stable order, then
  ordinary notifications; missing or already-open items are skipped. The empty
  space beside the right controls uses the same rule. Capsule/bubble hits and the
  shortcuts keep their own gestures, selected at DOWN. Both shortcut hit tests
  and routing share the same nearest-icon 8 dp tolerance. A diagonal upward start
  cannot abandon expansion and later become an unlock; an insufficient release
  cancels without opening content.
- Ordinary notifications now retain the user's chosen LIST/NUMBER presentation.
  Native AOD geometry does not change that intent; wake defaults and delayed
  automatic folding follow it. A real manual collapse can still choose NUMBER.
  Keep-last snapshots store this intent rather than an AOD transition's visibility.
- The capsule settings page gains two independent, initially off switches:
  show all lock-screen notifications and retain notifications after unlock/relock.
  They target the installed OEM's app-visibility and seen-after-unlock filters,
  while retaining the system privacy provider. The UI tells owners to disable
  matching HyperLight options when enabling these in Duo; coexistence is not a
  supported configuration for this iteration.
- The native artwork thumbnail keeps its slot throughout large-artwork changes.
  Its existing toggle action remains; an approximately 16% black veil marks the
  expanded artwork. No additional icon is added, and the lyrics control keeps its
  anchor. The veil is removed on collapse, AOD, unlock and view replacement.
- Capsule shell, icons, details and bubbles follow one drag progress, capped at
  the handoff while held. Release continues from that frame or returns to the
  original state. The native glass uses a joined path with tangent Bezier necks,
  preserving the material through connection and separation.
- Returning content grows a bubble from the existing capsule and separates into
  its fixed seat. The first returning item grows from a circle into the capsule.
  Return progress follows the card's actual collapse/exit rather than an unrelated
  timer; scene interruption and reduced motion settle to a valid final state.

Host verification: all 684 tests in 116 suites passed, with zero failures, errors
or skips. Debug and Release builds passed, including release R8 and vital lint.
The initial compile caught SDK 37's non-null Kotlin Animator signature; the native
Folme callbacks legitimately pass null, so a Java adapter now preserves the OEM
callback contract without introducing a Kotlin null assertion.

The owner requires fresh explicit confirmation before every install and supplied
that confirmation for preview 4 after the earlier pause. Ordinary replacement
returned `Success`. Installed version 20004 and the device APK SHA-256 match the
retained artifact. At that installation the SystemUI PID stayed unchanged and its
read-only probe reported runtime 20003. No GUI operation, scope change or restart
was performed; the temporary device APK was removed. Phone acceptance belongs to
the owner. The retained artifact is
`releases/local/hypermusiccover-duo-0.2.0-preview.4.apk`, SHA-256
`1ce3b566236bca0faa189f8c948e469874b8784f2083b0c9434a1d8a55c62ce8`.
Package identity, version 20004 and the local debug signing certificate were
verified before installation. The then-installed preview-3 APK was pulled
read-only and its signing certificate matched; the ordinary replacement retains
application data. This is a development preview, not a production signing key.

Manual acceptance for preview 4 (outer screen):

1. In capsule settings, test the two Duo notification options after disabling the
   corresponding HyperLight options. A notification hidden by app lock-screen
   rules should appear when show-all is enabled; an undismissed notification
   should remain after unlock/relock when retention is enabled.
2. Leave ordinary notifications unchecked in the collapsed-category selector.
   On the next lock they should stay expanded after the wake animation, and after
   another AOD cycle. Then check the category, start a new lock, and tap its bubble:
   the list should open and stay open. A manual downward collapse must still work.
3. Collapse all three kinds and swipe upward from the clock/content area:
   music, live cards, notifications, then unlock. Repeat from the empty gap beside
   the rail. Near-icon torch/camera drags should retain their forgiving native
   targets, while a short diagonal in the gap should cancel instead of unlocking.
4. Slowly drag the right capsule, hold midway, reverse, and release short/long.
   Glass, artwork and text should follow one progress without a second motion
   starting on release. Try catching a settling animation with a new gesture.
   Capsule and bubble taps should still open the intended card directly.
5. With one capsule remaining, collapse another card. Its bubble should bud from
   the capsule and separate as the card exits. Also return the first item to an
   empty rail. Test a notification disappearing during this motion; later gestures
   must still work. Sleep or open the control center mid-motion and check for
   leftover glass or pressed buttons on return.
6. Toggle large artwork repeatedly and then open lyrics. The native thumbnail
   remains in its original slot with only a light veil when expanded; the lyrics
   control and native card layout should remain stable.

### Preview 3 implementation and delivery history

The candidate is `0.2.0-preview.3` (`20003`).

- A bubble tap opens that content on the left. Capsule swipes remain selection
  gestures. Left-area upward swipes open ordinary notifications, then live cards,
  then music, skipping missing/open items; native unlock follows last.
- Adjacent capsule switches join and separate the native glass surfaces over
  420 ms, keeping both icons continuous while details retract and reveal. A
  nonadjacent wrap resizes both end seats without covering intervening bubbles.
  ColorOS contracts its single capsule before changing the face. Interrupted
  switches settle once; scene/list changes and disabled animations use valid
  final seats. The visual result still needs manual acceptance.
- Lock-screen entry now has independent collapsed-category checkboxes for music,
  live cards and notifications. None checked expands all; all checked collapses
  all. Older presets migrate to the corresponding mask without changing their
  music page. Inner landscape retains separate defaults.
- A separate keep-last-state switch disables those manual defaults and restores
  category state, known live-card states, selected capsule and music page. New
  live cards follow the saved category default; missing kinds retain their saved
  category preference. AOD wake does not start a new lock session.
- The date-adjacent status-text option is hidden and normalized off, including
  old imports. It was a separate relocation feature for status/owner text, not
  the charging-hint suppression; charging-hint suppression remains in place.
- Torch can receive an inward drag during the visible wake fade rather than
  waiting for full opacity. Sleeping, AOD, bouncer and unlock gates remain. This
  changes the input gate, not the system-wide sleep/wake animation duration.
- The moving and resting clock use the same ink center and ancestor-coordinate
  mapping. Outer portrait adds an 8 dp rightward optical adjustment. The outgoing
  player stops reserving clock space when its collapse starts.
- During sleep/wake, the active lyric keeps one ambient rendering treatment until
  the screen and clock agree they are awake. Extended-range lyric ink waits for
  the HDR window's first committed frame; stale callbacks cannot re-enable it.
  Stable-AOD frame scheduling is retained.
- Opening lyrics skips the old blur acknowledgement wait when Duo's artwork
  backdrop is hidden. Existing parsed lyrics and pending lookups remain reusable;
  a genuine network/lyrics lookup can still take time.

Host verification: all 660 tests in 110 suites passed, with zero failures, errors
or skips. Debug and Release builds passed, including release R8 and vital lint.
The retained APK is `releases/local/hypermusiccover-duo-0.2.0-preview.3.apk`, SHA-256
`d343d91b40de4506ab35a454fccc60dc465517db2f2446fc0e3e9f941462f028`.
Its local debug certificate matches the installed preview 2, verified from the
device APK before installation. This remains a development preview.

Device delivery: ordinary `cmd package install -r` returned `Success`. Installed
version 20003 and the on-device APK hash both match the retained artifact. The
SystemUI PID did not change and its read-only probe still reports 20002. No GUI,
scope change, process restart or device reboot was performed. The temporary APK
in `/data/local/tmp` was removed. The owner restarts SystemUI to load these fixes;
a whole-device reboot is not required for this iteration.

Manual acceptance after loading preview 3 (outer screen only):

1. With notifications, a live calendar/delivery card and music present, tap each
   bubble: that card opens directly on the left. Collapse them and swipe the right
   capsule in both directions. Check joined/split motion, persistent icons, fixed
   order and no disappearing/duplicate bubble. Include one rapid second swipe.
2. Collapse all, select music on the right, then swipe up repeatedly from the left
   clock/notification region: notifications, live cards, music, then native unlock.
   Missing or already-open kinds are skipped. Repeat in ColorOS style.
3. Test the category checkboxes with none, all and one kind selected. Check that
   only selected kinds collapse on the next lock. Enable keep-last, adjust the
   live presentation and music page, then unlock/relock; the choices should
   return, including the selected capsule. Include a quick screen-off after a
   change and an AOD wake, which should preserve the same session.
4. Collapse the player with other cards open and with everything else collapsed.
   The clock should resize during the card motion, land without a lateral hop,
   and sit slightly farther right in outer portrait.
5. From player-only entry, open lyrics for the first time. Ready lyrics should no
   longer wait for a hidden backdrop. Watch the active line through sleep/wake and
   stable AOD, including HDR if enabled; there should be no transition flash.
6. Begin an inward torch drag once the waking icon is visible, before its fade
   completes. It should use the normal native launch/cancel thresholds. Check the
   settled-screen torch and camera too. Do not expect activation while asleep.
7. Repeat direct AOD unlock as a regression check. The owner did not reproduce the
   black flash in the last preview-2 attempts; preview-3 device acceptance remains
   pending and must not be inferred from host tests.

### Preview 2 implementation and delivery history

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

Original preview 2 test checklist (superseded by preview 3's interaction contract):

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
