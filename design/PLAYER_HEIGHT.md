# Native player height

`notification.height` controls the outer screen and inner portrait together;
`notification.heightLandscape` independently controls inner landscape. Both are
two presets: compact (75) and native (100, the default), in the existing
`mediastyle` configuration and normalized backup JSON. Legacy values below 88
map to compact; values at least 88 map to native. Large text and unsupported compact
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
budget: header, playback row, current vertical padding and enough lower space
to match the visible playback glyph's bottom breathing room to the header's top.
The glyph bottom is measured from its native drawable/image matrix and padding;
an unavailable measurement uses the complete native row conservatively. The hidden 38dp seek row and its inter-row gap do not reserve space.
At 100% the original 138dp AOD endpoint is untouched. At reduced settings, the
background, header minimum, mapped target and wake seek alpha use the same compact
AOD endpoint; no unrelated clock/lyric transition policy is changed.
The actual native stack height drives the background during transition; there is
no second position/scale animator. Normal native layout and final AOD callbacks
apply the same plan before drawing, rather than correcting an original-height
animation only at its last frame. The target hook resolves the triggering header's
own card, so a reused cached header cannot redirect a fold transition.

The compact preset uses approximately 40dp header art instead of the native
52.5dp. Its bitmap and the page-action slot follow that same header geometry. The title and output
chooser align inside the header; measured two-line text can raise the effective
minimum without interpolating another mode. Playback buttons and the 38dp seek
touch row do not shrink. Gap allocation consumes the two inter-row margins and
available lower margin before the upper margin, leaving more space above the
header. Already tight lower margins and negative native margins are preserved;
all reductions remain capped by available space.

The two native control-row rectangles are larger than their visible pixels. On
this ROM the seek row is 38dp with 16dp top/bottom padding, so reducing its 2dp
external margin alone barely changes the visible playback-to-track distance.
The high-end `HyperProgressSeekBar` shader draws in the whole view's coordinates
and ignores that padding; changing padding would affect only the primary path.
The playback glyphs move down within the original row by transferring at most
5dp of existing lower padding to upper padding. Their total padding, drawable
scale, size and touch rectangle are unchanged. The seek/time union is measured
using the native track padding and actual text heights. For the one currently
owned lock-screen seek, a bounded Canvas translation then moves its native pixels
up by at most 6dp (normally 1–3dp with the retained layout), and the time labels
receive that same offset. This matches the two visible inter-row gaps rather
than assigning identical raw margins to different touch rows.
The drawing area, track/thumb sizes, touch rectangle and horizontal coordinates
remain native. Unrelated seek bars pass through. Shift changes invalidate once;
100%, holder release or leaving keyguard clears the padding/drawing/label leases.
The selected AOD endpoint is held during the transition and settled AOD instead
of being retargeted when a drawable first reports its bounds.

## Offline visible-bounds check

A 2px/dp model using the retained rows and a 16dp time-label union gives these
visible gaps (not touch-rectangle margins):

| Compact playback drawable | Top | Header to playback | Playback to seek/time | Bottom | AOD shell |
| --- | ---: | ---: | ---: | ---: | ---: |
| 40dp | 11.5dp | 10dp | 10dp | 12dp | 113dp |
| 24dp | 11.5dp | 18dp | 18dp | 12dp | 105dp |

The two AOD outer visible gaps are both 11.5dp in these models. Normal compact
height is about 139–139.5dp depending on resource rounding. All three original
touch rows remain disjoint and inside the shell. Native 100% is unchanged.
With 40/50/38dp touch rows inside a 139dp shell, only about 11dp total layout
whitespace exists. Exact equal visible gaps for every possible drawable cannot
be guaranteed without overlapping/reducing hit areas or increasing real height;
this implementation preserves input and matches the two internal visible gaps.
These are computed source/resource examples, not observations from a phone.

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
