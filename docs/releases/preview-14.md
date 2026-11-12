# Preview 14

This iteration addresses status-clock fade residue, intermittent full-screen
artwork visibility, lyric availability cues, compact capsule content and repeated
notification interactions. It retains the existing preview signing identity.

## Changes and causes

- The status time faded its paint alpha while its translucent shadow retained an
  independent alpha. Composite the time and its shadow in one fading layer, so
  neither remains visible through the animation's near-zero tail. Battery/network
  rendering and the accepted AOD fade remain unchanged.
- Full-screen artwork already uses the upstream proportional full-width image,
  mirrored blurred extension and edge feather. The owner confirmed its correct
  appearance during this session; a read-only probe also observed a ready,
  fully opaque sharp band. Its sharp-layer visibility previously reused the
  square-card/clock-container rules, so temporary notification compaction could
  remove the clear image. Base this background layer on the user's page choice,
  preserving no-lyrics defaults and the existing AOD path. The reported intermittent
  failure was not reproduced, so this fixes an identified source path rather than
  claiming a device-confirmed root cause for that occurrence.
- The next-page lyric action now distinguishes available, loading and unavailable
  lyrics. Pending lyrics show dots; an unavailable lyric uses a struck-through
  glyph. Tooltips and accessibility labels explain both states. Availability uses
  the current track's answer and is refreshed when loading completes or a late
  provider supplies content. Page cycling remains available and unchanged.
- A fixed 140 dp detail threshold hid the contents of actual three-seat
  (approximately 139.5 dp) and two-seat (approximately 87.5 dp) capsules. Reduce
  padding and ornament sizes first, keeping the summary and primary action.
  The secondary action yields to text in short capsules; a single-seat bubble
  remains icon-only.
- Notification collapse could leave an earlier LIST-opening retry alive; a stale
  native gesture flag could also treat an unrelated card reflow as a new opening.
  Cancel opening ownership on the latest collapse and require fresh native
  vertical-interaction evidence. Preserve the single-notification case where
  NUMBER and LIST can share a scroll target. Sequence tests cover interruption
  and repeated changes, but rare device recurrence still needs observation.

## Host validation and artifact

- All 897 tests passed across 144 suites, with no failures, errors or skipped
  tests. Debug and Release builds, including Release vital lint, completed.
- The APK passed ZIP integrity, 16 KiB-aware alignment and Android signature
  verification. Package: `com.zhuhuibin.hypermusiccover.duo`; version:
  `0.2.0-preview.14` / `20014`; not debuggable.
- The existing preview signing certificate is retained; this is not an official
  production-key release.
- APK size: 4,195,085 bytes. SHA-256:
  `76599a0a59d647603533d0e55dcb541f759a212cbb3ded6c6872ba8124925031`.
- Versioned copies in the local release archive and the owner's preview archive
  have matching hashes. The earlier lint analysis process failed internally
  while source was still being finalized; the final frozen-tree run passed all
  checks without suppressing or disabling lint.

## Focused phone acceptance

1. Select the right status cluster as the artwork-clock destination. Switch
   lyrics back to the clock and check that the small time leaves without a dark
   outline lingering after it.
2. In full-screen artwork mode, expand/collapse notifications and return from
   lyrics. The clear proportional artwork should remain whenever the artwork
   page is selected. The accepted square-cover close and AOD fade do not need a
   blanket retest.
3. Compare a track with lyrics, a pending lookup and a track with no lyrics. The
   action should identify the availability state, update when content arrives,
   and still permit returning to the clock.
4. With enough real-time cards to shrink the selected capsule to three or two
   seats, check that summary text and the primary action still fit. A one-seat
   bubble should show only its icon.
5. Mix card/capsule taps and native vertical scrolling in a non-sequential order.
   Check that a newly collapsed notification stays represented and does not
   reopen from an older request; also try the case with only one normal notification.

Only read-only wireless ADB state/log probes were used for the development
diagnosis. No GUI automation or screenshots were used. This preview has not been
installed or reloaded on the phone; host checks do not establish visual acceptance.
The native-glass wallpaper-sampling limitation remains open.
