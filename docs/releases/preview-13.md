# Preview 13

This iteration addresses artwork exit, artwork-clock destinations and AOD status
alignment. It is prepared offline and keeps the existing preview signing identity.

## Changes and causes

- A tap on the expanded artwork used the old compact-artwork action instead of
  the rail's return-to-clock action. That hid the square but left the cover scene
  active, so the full-screen blurred background and next-action button retained
  their previous state. Route the artwork exit through the shared page transition.
- Replace the outer-screen artwork clock-row switch with three destinations: shrink in place,
  join the date row, or show the time in the right status cluster. Preserve old
  preferences when migrating. The status-cluster destination uses a fade at each
  existing location rather than a flight across the screen.
- Make the shrink/date-row entry and return use the same motion model and actual
  glyph bounds. Reverse an interrupted transition from its current pose and
  retain the endpoint when it becomes stationary. Invalidate cached geometry
  when the display or clock style changes. Keep the existing left/right date
  preference for the date-row destination.
- The status cluster inherited keyguard scaling while capsules and bubbles also
  compensated its horizontal displacement in AOD. Share their AOD/wake anchor
  lifecycle and correct the status ink center in its real parent's coordinate
  space. Keep native vertical motion, scale and opacity; this works even when no
  capsule is currently visible.

## Host validation and artifact

- All 876 tests passed across 143 suites, with no skipped tests, failures or
  errors. Debug and Release APK builds completed.
- The Release APK passed ZIP integrity, 16 KiB-aware alignment and Android
  signature verification. Its package is `com.zhuhuibin.hypermusiccover.duo`,
  version `0.2.0-preview.13` / `20013`, and it is not debuggable.
- The existing preview signing identity is retained; this is not an official
  production-key release.
- APK size: 4,195,085 bytes. SHA-256:
  `0f1334db608ade327644d898ae9c4648f2ad2961d3aad5530259f3bbba0e5c8c`.
- Versioned copies are retained in the local release archive and the owner's
  configured preview archive, with matching hashes. No phone installation,
  scope reload, screenshot capture or device-side verification was performed.

## Focused phone acceptance

1. In square-artwork mode, open artwork from the player, then tap the large artwork
   to close it. The artwork background should leave, the clock return, and the
   player's next action become artwork again. Repeat after entering lyrics.
2. Try all three clock destinations when opening and closing artwork/lyrics. The
   date-row motion should follow the same path both ways. For the status-cluster
   destination, both times should fade at their own locations without flying.
3. Check the status time follows the current minute and the device's 12/24-hour
   preference, and that leaving the scene restores the native clock.
4. During AOD entry, steady AOD and wake, compare the horizontal centers of the
   battery/network cluster and capsule/bubble column. Also check with every card
   expanded, then collapse one again.

These checks cover this preview's changes. This build has not been installed or
reloaded on the disconnected phone. Visual quality and interaction remain pending
device acceptance. The known native-glass sampling limitation from Preview 12 is
unchanged.
