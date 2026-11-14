# Native player height

`notification.height` controls the outer screen and inner portrait together;
`notification.heightLandscape` independently controls inner landscape. Both are
integer percentages in 75–100, defaulting to 100, in the existing `mediastyle`
configuration and normalized backup JSON. Large text and unsupported compact
native variants retain enough height for their content.

## Native geometry and transition contract

The retained ROM's normal constraint set has a 185dp background, 52.5dp artwork,
50dp action row and 38dp seek touch row. The vertical margins are 16dp above art,
11dp before actions, 2dp before seek and 16dp below seek; resource rounding accounts
for the half-dp difference between their sum and the shell. The full-AOD shell is
138dp. These are static observations from the retained APK, not phone measurements.

The OEM linkage reads normal/full-AOD resource endpoints into `mHeightList` and
sets `MiuiMediaHeaderView.setAnimateHeight(target)` once. The native notification
stack then animates `actualHeight`. Its minimum comes from `mediaLockScreenHeight`;
`onFullAodStateChanged` separately sets the final background height. On sleep the
OEM hides the seek and time labels only at completion; on wake it shows them first.
Changing margins only in pre-draw therefore leaves the animator and final card
with different normal endpoints and clips the seek before it disappears.

## Compression and ownership

A single plan supplies the compact normal shell, the native header's minimum and
its animation target. The AOD shell retains the verified native 138dp endpoint.
The actual native stack height drives the background during transition; there is
no second position/scale animator. Normal native layout and final AOD callbacks
apply the same plan before drawing, rather than correcting an original-height
animation only at its last frame. The target hook resolves the triggering header's
own card, so a reused cached header cannot redirect a fold transition.

Artwork decreases from 52.5dp toward 40dp as the setting reaches 75%. Its bitmap
and the page-action slot follow that same header geometry. The title and output
chooser align inside the header; measured two-line text can raise the effective
minimum. Playback buttons and the 38dp seek touch row do not shrink. Gap allocation
preferentially consumes the two inter-row margins and retains at least the smaller
of the native upper/lower margin or 4dp. Negative/tight native margins are not
increased and all reductions are capped by available space.

The seek/time pixels are concealed when a compact sleep target starts, before
outline clipping can cut through them. Native visibility and touch ownership
remain unchanged. During wake the pixels return as actual height provides room;
completion releases the alpha lease. Independent external writes remain authoritative.

Layout dimensions and the native header field use conditional leases: only the
last owned value can be restored, and native updates replace their baselines.
100%, unlock, bouncer, holder replacement or detach release owned geometry.
Missing native fields, invalid dimensions and unknown variants fail closed.
The native colour-transition cancellation does not normally finish media geometry;
while a compact transition is owned, that boundary completes the native current
media scene and releases its target/alpha state instead of retaining a partial frame.

Host regressions cover gap/header budgets, 75% safety, monotonicity, native-height
restoration, endpoint continuity and independent orientation profiles. OEM clipping,
stack placement and transition comfort still require owner acceptance.
