# Duo implementation status

This page describes the active Duo codebase. A feature listed as implemented is
not automatically verified on every device, display or OS version.

## Maintained scope

| Area | Current implementation |
| --- | --- |
| Music and lyrics | Square/full-screen artwork, synchronized/word-timed lyrics, lyric effects, providers and optional translation. The artwork slot cycles between artwork, lyrics and the lock-screen clock. |
| Background | Native wallpaper, artwork background and animated background are independent of foreground artwork mode. Blur and dimming are configurable. Older inactive backdrop preferences migrate to native wallpaper. |
| Layout and gestures | Duo-owned card bounds and shortcut geometry, native secure camera/flashlight launch, empty-space expansion/collapse, and native interactions on actual cards. |
| Capsules | HyperOS spring/fusion/split motion and bounded switching; ColorOS switching; automatic size reduction and top overflow hiding. |
| Notifications | Independent lock-screen visibility/retention, per-category collapse defaults and last-state restoration. Native rows remain represented by a capsule until visibly expanded. |
| Clock and status | Independent status-bar visibility, outer-screen compact time/date row with a side preference, and a display-only battery/network cluster. Full-screen AOD can retain the compact row; ordinary AOD uses native presentation. |
| Inner screen | Portrait/landscape handling, landscape category defaults and two-column artwork/lyrics layout. Logical portrait/landscape layouts are device-checked; physical and visual acceptance remains outstanding. |
| Delivery | Duo release identity, preview-compatible signing path, guarded official signing and the project's own update feed. Production key configuration remains a separate owner setup step. |
| Settings | Export/import with legacy normalization; credentials stay on-device. Donation controls await owner-provided payment assets. |

## Current acceptance scope

Preview 11 has partial device evidence. The final candidate passed 819 host
tests in 137 suites, including the local-background first-frame state guards.
See the [Preview 11 record](releases/preview-11.md) for the measured results and
delivery limits.

The mixed notification stack now passes the three-swipe sequence from empty left
content: player, live card, then ordinary notifications. The default native LIST
target was `0`, while the explicit target and reached position were `533`. The
first ordinary row exposed `177` pixels against a `150` pixel threshold at native
scale/alpha `1`; the capsule disappeared only after the list settled. The sequence
did not enter the bouncer. This is a sampled device result, not a claim about every
notification combination.

Both shortcut backgrounds initialized, and their AOD transition ancestor alpha
chains read `1`. These checks used structural diagnostics without screenshots.
Material appearance and transition smoothness still need visual acceptance.
The local artwork backdrop also reported real drawn frames at full opacity, and
the dynamic shader advanced frames on both outer and inner layouts despite the
wallpaper process having no separate keyguard renderer. AGSL color indices were
expanded to constants for this device's shader compiler. Full-screen AOD stops
this local animation and keeps the existing full-viewport static wash.

The remaining checks are limited to this iteration's changes:

1. Shortcut icons and their material remain visible through lock screen → AOD →
   lock screen, including an entry before any capsule interaction. Ordinary and
   full-screen AOD must keep their respective behavior.
2. Native/artwork/animated backgrounds switch independently of square/full-screen
   foreground artwork. Blur, dimming, lyrics, card expansion and unlock restore
   the selected wallpaper correctly.
3. The outer-screen compact row follows its time/date side setting; the status-bar
   switch is independent, and the right cluster displays battery/network without
   another clock. The self-drawing `ClassicTextAreaView` adapter passed structural
   checks for
   placement, side selection and restoration. Ordinary AOD remains native.
4. Inner-screen portrait/landscape keeps cards, lyrics, capsule touch targets and
   full-screen AOD inside their available bounds. Logical portrait and landscape
   layouts have been read back on-device; physical
   opening/rotation and visual acceptance remain outstanding.
5. Restoring an older module backup chooses native wallpaper instead of reviving
   dormant background switches; an app-only backup changes no background settings.

Long-paused music-session recovery has implementation coverage but no explicit
long-duration device acceptance. It does not need repeating in every preview.
Official signing and update delivery are verified separately when the production
key and an official release are configured. The maintainer deferred key setup to
a later session. The workflow template at `tools/ci-check.yml` is ready, but the
current GitHub OAuth authorization lacks workflow-management permission; hosted
CI has not been activated.

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
corrects that path and has the structural evidence above; its visual result still
awaits acceptance.

## Validation policy

Host tests, build/signature checks and observed device behavior are reported
separately. Each preview's checklist covers only that iteration's changed behavior
and any specific regression. Full-device reboot is not part of verification;
module updates do not restart scopes automatically.

See [release procedure](releasing.md), [lyric presentation](lyric-effects.md),
[translation](online-lyric-translation.md) and
[media-card customization](media-card-customization.md).
