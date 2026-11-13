# Lockscreen entry and navigation AOD

## Manual music reopening

Detached-rail taps previously requested artwork, while an upward expansion requested
the native player. Neither path consistently applied the existing music-page setting.
Both now enter through the same verified native-card expansion and select the same
configured page before its first visible frame.

`startupMusicView` is shared by the outer screen and inner portrait: player, artwork
or lyrics. `landscapeMusicView` remains independent and retains its fourth combined
artwork/lyrics page. Existing values, defaults and legacy migration are unchanged.
The UI keeps these choices editable even when music starts in its capsule or the
last-lockscreen option is enabled. Initial lock entry still restores a saved snapshot
when requested; a later explicit reopen uses the configured expansion page each time.
When lyrics have been disabled by the diagnostic interface, only that reopen falls
back to artwork. The normal three/four-page cycle is unchanged.

The native-card presence check is authoritative during this verified reopen. Requiring
the native header to be already visible rejected the first page selection precisely
while that header was still being expanded.

## Navigation priority before AOD capture

Manually closing navigation on the lit lockscreen gives the clock back. The old sleep
path only rearmed category-entry gates; the map reopened on a later refresh, and its
remote surface could return after the OEM had captured the large clock for AOD. A
clock entry requested off-screen deliberately watched the OEM AOD instead of posing
the small clock, leaving that large clock above the returning map.

For full-screen AOD, sleep now applies the navigation presentation synchronously,
then reserves its clock before the OEM handoff. Reservation requires a locked scene,
an actual navigation live card, an enabled/ready map scene, and that scene being open
without yielding. A remote page still on its way back uses the existing bounded
four-second page-return hold. The normal scene pre-draw still creates the page once;
sleep does not bind another remote page or start a polling loop.

The clock poses its existing configured shrink/date-row/status destination first,
then latches that pose for navigation AOD independently of the generic small-clock
AOD preference. Content arriving asynchronously is also posed before it is shown,
including after an expired hold. Plain AOD, missing cards, disabled maps, unlock and
navigation completion retain their existing native/restoration paths.

## Host verification and device boundary

Focused JVM tests cover the shared portrait namespace, independent four-state
landscape pages, legacy/default preservation, keep-last entry versus manual reopen,
sleep ordering, removal during preparation, stale map readiness and all navigation
reservation gates. Selected production Java and Kotlin compilation covers the
cross-language entry and clock bridges.

This iteration uses no phone access. Host tests cannot confirm the actual OEM AOD
capture order, remote SurfaceControl arrival or visual clock geometry. Device
acceptance should cover: manually close a live navigation card, enter full-screen
AOD, then verify the map and configured clock destination together; repeat with a
delayed map and end navigation. For music, select each configured page and reopen
from a capsule by both tap and upward gesture in outer, inner portrait and landscape.
