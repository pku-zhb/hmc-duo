# Duo implementation status

This page describes the active Duo codebase. A feature listed as implemented is
not automatically verified on every device, display or OS version.

## Maintained scope

| Area | Current implementation |
| --- | --- |
| Music and lyrics | Square/full-screen artwork, synchronized/word-timed lyrics, lyric effects, providers and optional translation. The artwork slot cycles between artwork, lyrics and the lock-screen clock, with current-track loading/unavailable lyric cues; inner landscape adds an explicit combined page. |
| Background | Native wallpaper, artwork background and animated background are independent of foreground artwork mode. Blur and dimming are configurable. Older inactive backdrop preferences migrate to native wallpaper. |
| Layout and gestures | Duo-owned card bounds and shortcut geometry, native secure camera/flashlight launch, empty-space expansion/collapse, and native interactions on actual cards. |
| Capsules | HyperOS spring/fusion/split motion and bounded switching; ColorOS switching; automatic size reduction and top overflow hiding. Short two/three-seat capsules retain their summary and primary action; single-seat bubbles remain icon-only. |
| Notifications | Independent lock-screen visibility/retention, per-category collapse defaults and last-state restoration. Native visibility is tracked separately from shared-stack state; capsule animation follows native movement while exposure verification retains retry ownership. |
| Clock and status | Independent status-bar visibility. Artwork/lyrics on outer and inner screens can shrink the clock, join it to the date row with a side preference, or fade the time into the display-only battery/network cluster. Full-screen AOD retains the selected compact presentation; ordinary AOD uses native presentation. Status and capsule columns share their horizontal AOD/wake anchor. |
| Inner screen | Portrait/landscape handling, fixed bounded landscape card widths, category defaults and an explicit combined artwork/lyrics page. Preview 12 layout changes await device acceptance. |
| Delivery | Duo release identity, preview-compatible signing path, guarded official signing and the project's own update feed. Production key configuration remains a separate owner setup step. |
| Settings | Export/import with legacy normalization; credentials stay on-device. Donation controls await owner-provided payment assets. |

## Current acceptance scope

### 0.2.4 card edges, backdrop sampling and settings

Per-card native effects replace the old combined-stack edge copy so an overlapping
stationary notification is not sampled into the moving row. Media headers' own
translation method is covered. The current ROM lacks an effect getter; exact
constructor/native-commit observation establishes known baselines, and unknown
late cards retain their native path. The local artwork sampler now targets the
existing native music background without replacing its final glass recipe.
Inner portrait no longer excludes date-row/status clock destinations. The settings
restructure preserves keys and adds real prerequisites, concise explanations,
noninteractive notes and a working dial-code copy action. See the
[0.2.4 record](releases/0.2.4.md). GPU effects and native transitions await the
owner's focused acceptance; desktop-to-AOD status overlap remains deferred.

### 0.2.2 independent controls and notification edge

The current source adds a separate AMap navigation-map switch, a common
lockscreen/AOD vertical status offset, and row-scoped progressive blur/fade at
the right notification boundary. The shared launcher artwork is now native
vector art. See the [0.2.2 release record](releases/0.2.2.md) for causes,
ownership boundaries, host checks and the focused device acceptance list.
Historical preview sections below retain their original validation state.


### Preview 17 AOD performance

The owner reported severe AOD and native-island animation slowdown with the
Preview 16 / Lite Preview 3 pair. A read-only CPU sample identified repeated
reflection and exception construction in Lite, with HMC-side palette/source
processing amplifying the shared material path. Both projects are now maintained
by the same executor. HMC Preview 17 and Lite Preview 5 implement the paired
source fixes; the unified material editor stays on its own worktree. See the
[Preview 17 record](releases/preview-17.md). Device improvement is not yet accepted.

### Preview 16 compatibility handoff

The HMC side now separates default music material ownership, committed native
material following and notification transition handback. See the
[Preview 16 record](releases/preview-16.md) for source causes, supported music
configurations and the focused paired checks. Lite has a separate implementation
owner. The fixed candidate is HMC Preview 16 with Lite Preview 3; paired device
acceptance remains pending. HMC passed 953 host tests and artifact checks.
The owner deferred Preview 15 testing until this compatibility work is ready.

### Preview 15

This iteration addresses square artwork disappearing in full-screen AOD,
inner-landscape clock placement, inconsistent capsule expansion exits and
credential-screen capsule occlusion. Short-capsule content remains awaiting an
opportunity with enough real-time cards; it is not a required repeat test. See the
[Preview 15 record](releases/preview-15.md) for the causes, 917 passing host tests,
verified artifact and focused acceptance list. The new APK has not been installed
or reloaded; its visual behavior remains awaiting acceptance.

### Credential-screen capsule occlusion

The owner reports capsules at an unexpected lower-right position on the first
mandatory credential screen after boot. Source inspection found a mismatch:
the rail drops its layout plan while the bouncer is visible, but capsule
presentation intentionally retains ownership of collapsed cards. The old path
relies on native shortcut fading to hide the capsule and can fall back to the
bottom-shortcut layout when the rail plan is absent.

Owned capsules, bubbles and their transition overlays now receive a reversible
empty drawing clip while credentials are visible. Their selection, native-card
ownership and animation alpha remain unchanged. Placement also stops before the
legacy fallback, and dismissing credentials restores each overlay's prior clip.
The native credential UI, cards and shortcut buttons are not clipped.

This source fix is included in the Preview 15 work, not the archived
Preview 14 APK. First-boot reproduction remains unverified; no phone reboot,
installation or scope reload is required for this low-priority report. Check it
opportunistically at the next normal first unlock, including that capsules return
with the same collapsed choices after leaving the credential screen.

### Preview 14

Preview 14 follows the owner's Preview 13 feedback: status-time shadow residue,
intermittent full-screen sharp-layer loss, lyric availability cues, short-capsule
content and repeated notification gestures. See the
[Preview 14 record](releases/preview-14.md) for validation and the focused acceptance
list. Its new behavior still awaits device acceptance; this development session
used only read-only phone state/log probes, without GUI automation or screenshots.

The owner accepted AOD fade and square-artwork dismissal in Preview 13. During
this session the owner also confirmed the proportional clear full-screen artwork
was displaying correctly; a read-only probe observed the ready, fully visible
sharp layer. The intermittent blur-only report was not reproduced. The
[Preview 13 record](releases/preview-13.md) retains the prior host-validation results.

The [Preview 12 record](releases/preview-12.md) retains that iteration's causes and
evidence limits. The owner now reports the multi-step notification interaction
works in most cases, with rare failures after repeated non-sequential operations.
Inner-layout and other unreported cases remain pending; they do not need a
repeated checklist in every later preview.

Before disconnection, the inner-landscape stack measured 2096 px with Duo reflow
and 1246 px with only that reflow disabled. The new 453 dp cap is an explicit Duo
layout parameter derived from that observation, not an ongoing dependency on
native animation geometry. Notification expansion and scrolling were captured,
but the intermittent missing representative was not reliably reproduced. Its
changes address source-level state gaps rather than a claimed device pass.

Outstanding earlier acceptance includes inner-landscape layout/four-state
cycling, interrupted notification sequences and adjacent-seat HyperOS splits.
The previous AOD shortcut material transition was accepted by the owner and is
not requested again. Each iteration's checklist stays limited to its changed
behavior rather than repeating all pending cases.

Native glass/refraction sampling the original wallpaper has a source correction
in 0.2.4; local artwork plus third-party final material still needs device acceptance. Long-paused
music-session recovery has implementation coverage but no explicit long-duration
device acceptance and does not need repeating in every preview.

Official signing and update delivery are verified separately when the production
key and an official release are configured. The maintainer deferred key setup to
a later session. The workflow template at `tools/ci-check.yml` is ready, but the
current GitHub OAuth authorization lacks workflow-management permission; hosted
CI has not been activated.

## Status alignment included in Preview 13

The owner reported that the battery/network status cluster does not align with the
capsules and bubbles in AOD. The status overlay inherited native keyguard scaling,
whereas the rail also compensated the resulting horizontal displacement. This
source-level mismatch is corrected by sharing the AOD/wake anchor lifecycle
and converting the status ink center's horizontal correction into its parent's
local coordinates. Native vertical motion, scale and opacity remain inherited.

The isolated alignment change passed 858 host tests in 140 suites and a debug
build before its inclusion in Preview 13. Device acceptance remains pending:
check the status ink and the capsule/bubble centers during AOD entry, steady AOD
and wake, including when every card is expanded and no capsule is drawn.

## Previous acceptance retained

The owner accepted vertical text, ordered content expansion, the empty-space
interaction gap repair, title/source-button avoidance and the independent
notification visibility/retention options in earlier previews. The status cluster's
reference geometry was accepted before this iteration removed its clock.

Preview 10 restored drawing after a shortcut pre-draw listener could veto the
whole shade window while hidden layout remained pending. Desktop notification and
control-center panels then advanced rendered frames. The owner also reported no
black flash in the sampled direct AOD fingerprint unlock. That trial does not
establish that every earlier flash shared the same cause.

The AOD shortcut issue had a separate visibility path: the native shortcut
ancestor could fade out even when both material discs had initialized. Preview 11
corrects that path. Its structural evidence is recorded in Preview 11, and the
owner subsequently accepted the material transitions.

## Validation policy

Host tests, build/signature checks and observed device behavior are reported
separately. Each preview's checklist covers only that iteration's changed behavior
and any specific regression. Full-device reboot is not part of verification;
module updates do not restart scopes automatically.

See [release procedure](releasing.md), [lyric presentation](lyric-effects.md),
[translation](online-lyric-translation.md) and
[media-card customization](media-card-customization.md).
