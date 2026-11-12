# Preview 11

Preview 11 continues the independent Duo lock-screen implementation. It uses the
existing preview signing identity; production signing and stable update delivery
are not enabled by this preview.

## Changes

- Keep initialized flashlight/camera materials visible through their native
  ancestor's AOD fade, including before the first capsule interaction.
- Open ordinary notifications with the native explicit-list strategy. The default
  `SHOW_LIST` strategy can leave normal notifications underneath a mixed media and
  live-card stack. Duo now keeps the notification capsule until a readable row is
  exposed, accounting for clipping, sibling overlap, native scale and native alpha.
  Pending gestures remain consumed until exposure settles; retries and settling
  have a deadline.
- Separate foreground artwork from the background selection: native wallpaper,
  artwork background or animated background. Blur and dimming are explicit
  controls. A local layer also supports wallpaper
  builds without an independent keyguard renderer; animated colors use constant
  AGSL indices compatible with this device.
  Older backups cannot reactivate previously dormant background preferences.
- Add an independent lock-screen status-bar switch and an outer-screen compact
  time/date row, with a side preference. The right status cluster displays battery
  and network information without duplicating the clock. Ordinary AOD keeps the
  native clock presentation; full-screen AOD can retain the compact row.
- Remove retired integrations and settings from the active tree while preserving
  upstream attribution. Donation controls await the maintainer's own payment assets.
- Prepare release identity checks, explicit official signing, certificate-migration
  verification and a workflow template. These preparations do not activate stable
  delivery or GitHub Actions.

## Verification

The final candidate passed **819 host tests in 137 suites**, with no failures,
errors or skipped tests. Debug and release builds completed. ZIP integrity, 16 KiB
alignment, package/version, non-debuggable manifest and the existing preview
signature were verified. Transfer and both retained APK copies matched SHA-256.

On the test phone, three upward swipes from empty left content successfully opened
**player → live card → ordinary notifications**. In the mixed stack, the default
native LIST target was `0`; the explicit-list target was `533`, and the third
swipe reached `533`. The first ordinary row exposed `177` pixels against a `150`
pixel usability threshold, with native scale and alpha both `1`. Its capsule
remained until the list settled, and the sequence did not enter the bouncer.

Flashlight and camera background initialization and AOD transitions were checked
through the view hierarchy. Both background alpha chains read `1`. This confirms
the measured visibility path; it does not replace visual acceptance of material
appearance or transition smoothness. No screenshots were used for these checks.

The self-drawing `ClassicTextAreaView` adapter now reaches its compact row on the
phone. Its native baseline is derived from the original drawing code rather than
`TextView.getBaseline()` (which returns `-1` here). Left/right placement, switching
the feature off, artwork/lyrics entry, full-screen AOD retention, ordinary AOD
restoration and return from the inner display passed structural checks. The
independent status-bar switch changes its owned hidden-strip count from `1` to
`0` and back without changing native alpha.

Device-state and rotation overrides exercised actual `1672 × 2364` portrait and
`2364 × 1672` landscape layouts. Landscape lyrics entered the two-column layout;
returning to the `1168 × 1712` outer display released all nine inner-layout leases.
All overrides were restored. These are device layout checks, not physical-hinge or
visual acceptance.

The six foreground/background combinations reached their requested modes and
retained the foreground layer, but wallpaper diagnostics exposed a separate gap:
this wallpaper build has no independent keyguard renderer, so the remote backdrop
path has no uploaded texture. The local presentation path now submits its owned
bitmap before fading in. Device
diagnostics confirmed matching source/drawn generations, a completed first-frame
gate, alpha `1` and full viewport bounds. Native selection hides this layer.

This device also rejects variable AGSL array indices. Expanding the 4 × 4 color
interpolation to constant indices preserved its formula and enabled the shader.
The final dynamic path reported `shader=ready`, with frame count advancing from
`74` to `106` in about 1.1 seconds. It also drew across both actual inner-display
sizes. Full-screen AOD stopped this animated layer and retained the existing
full-viewport static wash. These checks establish rendering activity and geometry;
they do not establish visual quality without looking at the display.

## Remaining checks for this iteration

1. Visually check the compact time/date row and its transition with artwork and
   lyrics; check the independent status-bar switch and time-free right cluster.
2. Visually compare native/artwork/animated backgrounds with square/full-screen
   artwork, blur and dimming. Structural mode/first-frame checks have passed;
   the owner-assisted fingerprint unlock check is pending.
3. Visually confirm both shortcut materials during full-screen AOD entry and exit.
   Ordinary AOD should retain its native presentation.
4. Check inner-display touch targets, artwork and lyric placement after physically
   opening/rotating the device; logical portrait/landscape geometry has been read
   back without screenshots.

Only this iteration's changed behavior needs repeating. Previously accepted
behavior is not a blanket retest requirement; report any observed regression.

## Delivery limits

The maintainer deferred production-key setup to a later session. Official signing,
preview-to-production certificate migration and stable updates remain unverified.
The retained preview APK is `hypermusiccover-duo-0.2.0-preview.11.apk` (version
code `20011`, 4,178,497 bytes). Its SHA-256 is
`33700a5ba0954ebbe266506e519d9f04313f43281417c24d0491eda524e8d672`.
This is a preview signature, not the deferred production identity.

`tools/ci-check.yml` is ready as a workflow template. The current GitHub OAuth
authorization lacks workflow-management permission, so it has not been activated
under `.github/workflows/`. A local passing test run is not a hosted CI result.

See [current implementation status](../fold-settings-roadmap.md) and the
[release procedure](../releasing.md).
