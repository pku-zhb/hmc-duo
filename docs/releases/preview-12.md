# Preview 12

This iteration corrects the full-screen artwork composition, inner-landscape
layout, notification handoff and HyperOS return animation. It keeps the existing
preview signing identity. The candidate is prepared offline; it has not been
installed or accepted on a phone.

## Causes and changes

- FULL artwork had incorrectly been routed through the independent square-card
  renderer. Restore the upstream full-width, aspect-preserving clear artwork band
  with mirrored blurred extensions and feathered edges. Keep the local renderer
  for wallpaper builds without an independent keyguard engine. Native and animated
  backgrounds remain separate choices. Artwork morphs hand off to an actually
  drawn generation, rather than a preparation timestamp alone.
- Inner-landscape notification bounds used the entire left content width. The
  sampled phone measured 2096 px with that reflow and 1246 px with only reflow
  disabled. Use a fixed 453 dp maximum width, matching the latter at density 2.75;
  do not infer geometry from animated native bounds. Preserve the native media
  header and live-row vertical stack instead of moving the player child outside
  its clipping and touch parent. When the expanded native stack leaves no room
  above the player, place artwork above lyrics in the free right column instead.
- Inner landscape now cycles clock → artwork → lyrics → artwork and lyrics →
  clock. Page intent is separate from asynchronous lyric availability. Existing
  landscape combined-page settings and saved state migrate to the explicit fourth
  state; outer and portrait pages keep the three-state cycle.
- Native notification state describes a mixed player/live/ordinary stack, not
  whether an ordinary notification row is actually exposed. Capture stable hidden
  ordinary rows from LIST and NUMBER as well as STACK. A downward empty-space
  gesture closes unrepresented ordinary notifications before closing a live card.
  Fade the notification capsule with native scroll/alpha while retaining logical
  retry ownership until exposure is confirmed, eliminating the delayed second
  disappearance animation.
- HyperOS returning bubbles split from the nearest visible neighboring seat in
  category order. They no longer travel from a distant selected capsule across an
  intervening bubble. Keep the selected capsule's material when a neighboring
  circle supplies the split surface. Slow the HyperOS settling and split motions
  moderately; ColorOS retains its own motion.

## Host verification and artifact

The final candidate passed **851 host tests in 140 suites**, with no failures,
errors or skipped tests. Debug and release builds completed, including release
lint checks. APK ZIP integrity, 16 KiB alignment, package/version, non-debuggable
manifest and the existing preview signature were verified.

The retained APK is `hypermusiccover-duo-0.2.0-preview.12.apk`, version code `20012`,
4,178,497 bytes. Its SHA-256 is
`b2b780e9c3ec7dfc1fccb71ff1513d7d82778add4136744f5027e66eb5cf4012`.

This is an offline build using the preview certificate. It has not been installed
or reloaded on the disconnected phone. Host checks are not device acceptance.

## Evidence and limits

Before the phone disconnected, diagnostics recorded outer-screen category
expansion, native notification scrolling, inner-display bounds and the stock
SystemUI APK identity. Temporary rotation and layout settings were restored.
No screenshots, installation, scope restart or whole-phone reboot were used in
that capture. The intermittent hidden-notification-without-capsule failure was
not reliably reproduced. The notification changes address identified source-level
gaps and require device acceptance.

The owner accepted the previous AOD shortcut material transitions. They are not
part of this iteration's blanket retest list.

Native glass/refraction can still sample the original wallpaper instead of Duo's
local artwork. This is a known rendering-layer limitation; changing the foreground
composition does not establish that the native glass sampler has changed.

Production signing, certificate migration and hosted CI remain deferred as
described in the [release procedure](../releasing.md).

## Focused phone acceptance after reconnection

1. Compare FULL and square artwork. FULL should use the screen-wide composition;
   enter and leave artwork/lyrics without a square-card flash or late size jump.
2. On inner landscape, cycle all four pages, including when lyrics load slowly.
   With player, live cards and notifications present, check their bounded widths,
   vertical order, and taps on the cycle control.
3. Open player → live cards → notifications, then hide notifications by dragging
   in the native card region. Confirm the notification representative returns,
   opens again, and does not lag the list animation. A subsequent empty-space
   downward gesture must not skip hidden notifications and close a live card first.
4. With three categories, return music while another category is the main capsule.
   The new music bubble should grow from its adjacent visible neighbor, retain
   material throughout, and settle at the slightly slower pace.

These are the changed behaviors for this preview. Previously accepted features
only need reporting if a regression is observed.
