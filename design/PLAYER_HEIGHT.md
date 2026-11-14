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
its animation target. The compact full-AOD endpoint has a separate visible-content
budget: header, playback row, current vertical padding and a bounded native AOD
bottom gap. The hidden 38dp seek row and its inter-row gap do not reserve space.
At 100% the original 138dp AOD endpoint is untouched. At reduced settings, the
background, header minimum, mapped target and wake seek alpha use the same compact
AOD endpoint; no unrelated clock/lyric transition policy is changed.
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

The two native control-row rectangles are larger than their visible pixels. On
this ROM the seek row is 38dp with 16dp top/bottom padding, so reducing its 2dp
external margin alone barely changes the visible playback-to-track distance.
The high-end `HyperProgressSeekBar` shader draws in the whole view's coordinates
and ignores that padding; changing padding would affect only the primary path.
For the one currently owned lock-screen seek, a bounded Canvas translation moves
its native pixels up by at most 6dp, and the time labels receive the same offset.
The drawing area, track/thumb sizes, touch rectangle and horizontal coordinates
remain native. Unrelated seek bars pass through. Shift changes invalidate once;
100%, holder release or leaving keyguard clears the drawing/label leases.

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
restoration, independent AOD visible-row budgets, pixel-shift bounds, endpoint
continuity and independent orientation profiles. OEM clipping,
stack placement and transition comfort still require owner acceptance.
