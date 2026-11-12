# Preview 15

This iteration addresses credential-screen capsules, square artwork in full-screen
AOD, inner-landscape clock placement and inconsistent capsule expansion exits.
It retains the existing development signing identity.

## Changes and causes

- The credential screen disables rail geometry while capsule presentation keeps
  ownership of collapsed cards. Without the usual native shortcut fade, the
  capsule could remain visible and fall back to the old bottom-shortcut position.
  Reversibly clip only owned capsule/transition overlays behind credentials and
  skip that placement fallback. Keep selection, collapsed categories and native
  card ownership intact. First-boot reproduction remains unverified.
- A read-only capture of the owner's full-screen AOD reproduction showed the
  cover still selected, a live media card, held AOD, and the clock in the status
  cluster. The log recorded artwork being compacted to avoid the clock. Collision
  measurement was using the transparent, full-size native clock retained for
  animation geometry. Check actual alpha through its ancestor chain before
  treating that clock as an occluder. Do not change the animation's measurement
  geometry or the existing status-time shadow fade.
- The page-cycle action used a screen-on gesture condition for visibility. Keep
  the glyph on retained full-screen AOD cards while disabling interaction; block
  it during credentials, control center and keyguard exit. Ordinary AOD retains
  its native behavior.
- Clock destination selection, reverse motion and frame cleanup were gated to
  the outer display. Apply the same three choices to inner landscape, with fresh
  bounds after rotation/folding. Inner portrait keeps its current layout. Settings
  explicitly describe the supported display modes.
- HyperOS expansion reused the rail view immediately for music but retained a
  notification representative until native exposure verification, giving them
  different exit timing. Capture an independent, non-interactive representative
  with the same native material, then expand the native card immediately. Music,
  real-time cards and notifications share a 260 ms, small swell-and-dissolve exit.
  Keep notification verification ownership and recover the original representative
  if opening fails. Cancel the temporary visual on reversal, AOD, unlock, layout
  changes and destruction. ColorOS retains its original animation path.

## Evidence limits

The full-screen AOD reproduction was read through state and log probes only.
No screenshots, GUI input, installation, scope reload or whole-phone reboot were
used. The AOD static artwork wash was still marked visible in the capture; the
awake background renderer intentionally hands off to that separate wash. Its
existing fade and brightness are preserved. Visual acceptance must distinguish
restored square artwork from any remaining background-appearance complaint.

Short-capsule content remains awaiting sufficient real-time cards. It is not
changed or required for this iteration's acceptance.

## Focused phone acceptance

1. With square artwork, blurred artwork background and the status-cluster time
   selected, enter full-screen AOD. The square and page-action glyph should remain;
   compare the static dim background and the wake transition. The action should
   become interactive again after waking.
2. In inner landscape, select the date-row and status-cluster clock destinations,
   then open artwork/lyrics and return to the clock. Each choice should apply in
   both directions. Shrink-in-place remains the third choice.
3. With music and notifications collapsed, open each using taps and empty-space
   swipes. Their right-side representatives should swell and dissolve consistently
   while the left cards appear. A quick collapse should restore a representative
   without a stuck invisible seat or a second exit animation.
4. Opportunistically at the next normal first unlock, check that the credential
   screen contains no capsule overlay and that leaving it restores the previous
   collapsed choices. Do not reboot just for this check.
