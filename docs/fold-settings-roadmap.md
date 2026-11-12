# Duo implementation status

This page describes the active Duo codebase. A feature listed as implemented is
not automatically verified on every device, display or OS version.

## Maintained scope

| Area | Current implementation |
| --- | --- |
| Music and lyrics | Square/full-screen artwork, synchronized/word-timed lyrics, lyric effects, providers and optional translation. The artwork slot cycles between artwork, lyrics and the lock-screen clock; inner landscape adds an explicit combined page. |
| Background | Native wallpaper, artwork background and animated background are independent of foreground artwork mode. Blur and dimming are configurable. Older inactive backdrop preferences migrate to native wallpaper. |
| Layout and gestures | Duo-owned card bounds and shortcut geometry, native secure camera/flashlight launch, empty-space expansion/collapse, and native interactions on actual cards. |
| Capsules | HyperOS spring/fusion/split motion and bounded switching; ColorOS switching; automatic size reduction and top overflow hiding. |
| Notifications | Independent lock-screen visibility/retention, per-category collapse defaults and last-state restoration. Native visibility is tracked separately from shared-stack state; capsule animation follows native movement while exposure verification retains retry ownership. |
| Clock and status | Independent status-bar visibility. Outer-screen artwork/lyrics can shrink the clock, join it to the date row with a side preference, or fade the time into the display-only battery/network cluster. Full-screen AOD retains the selected compact presentation; ordinary AOD uses native presentation. Status and capsule columns share their horizontal AOD/wake anchor. |
| Inner screen | Portrait/landscape handling, fixed bounded landscape card widths, category defaults and an explicit combined artwork/lyrics page. Preview 12 layout changes await device acceptance. |
| Delivery | Duo release identity, preview-compatible signing path, guarded official signing and the project's own update feed. Production key configuration remains a separate owner setup step. |
| Settings | Export/import with legacy normalization; credentials stay on-device. Donation controls await owner-provided payment assets. |

## Current acceptance scope

Preview 13 is an offline follow-up for artwork exit, three outer-screen clock
destinations, reversible clock motion and status-cluster AOD alignment. The
candidate passed 876 host tests in 143 suites and both APK builds; the APK also
passed package, integrity, alignment and preview-signature checks. See the
[Preview 13 record](releases/preview-13.md) for the focused acceptance list.
It has not been installed or reloaded on the disconnected phone.

The [Preview 12 record](releases/preview-12.md) retains the previous offline
iteration's causes and evidence limits. Its revised phone behavior has not been
accepted; it does not need a repeated checklist in every later preview.

Before disconnection, the inner-landscape stack measured 2096 px with Duo reflow
and 1246 px with only that reflow disabled. The new 453 dp cap is an explicit Duo
layout parameter derived from that observation, not an ongoing dependency on
native animation geometry. Notification expansion and scrolling were captured,
but the intermittent missing representative was not reliably reproduced. Its
changes address source-level state gaps rather than a claimed device pass.

Preview 12's pending scope covers full-screen composition, inner-landscape
layout/four-state cycling, notification representation during native scrolling
and adjacent-seat HyperOS splits. Preview 13 adds only the artwork-close,
clock-destination/motion and status alignment checks linked above. The previous
AOD shortcut material transition was accepted by the owner and is not requested
again.

Native glass/refraction may still sample the original wallpaper instead of local
artwork; this known layer-composition limitation remains open. Long-paused
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
