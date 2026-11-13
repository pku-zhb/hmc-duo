# Native notification edge ownership

The previous edge treated only positive row translation and placed a blur band at
its nominal stack width. Its alpha was `1 - smoothstep(x) * swipeStrength`, so a
small swipe retained nonzero alpha at the original hard cut. It also ignored the
horizontal native/parent clip and kept pixels beyond the feather band. This can
leave a hard boundary with blur attached, and differ between directions or native
expanded/offscreen rendering.

The edge now uses a symmetric premultiplied-alpha feather without additional blur.
Absolute translation controls band width (at most 24 dp, capped at 192 px), never
terminal opacity: every nonzero swipe reaches alpha zero at its clipping edge.
Positive motion treats the right side; negative motion treats the left. The active
row's public actual clip is mapped into stack coordinates with a reused Matrix;
only axis-aligned transforms are supported. The effective boundary intersects the
fixed Duo content slot and the original Canvas clip. Pixels beyond that boundary
are excluded too, so they cannot reappear after passing through the fade. Without any active swipe the dispatch stays entirely native. Unknown actual
clips are left untreated.

OEM evidence from the matched SystemUI sample: `NotificationStackScrollLayout`
`dispatchDraw` normally dispatches directly, but with `mBlurEffect` it sets the
native blur node size from `canvas.getWidth()/getHeight()` and records children
there. `updateClipping` clears the View clip and disables outline clipping.
`ExpandableView.updateClipping` supplies `(0,0,width,actualHeight-clipBottom)` with
a minimum-height bound, or null for an unclipped row. The wrapper therefore retains
the original Canvas dimensions, one native dispatch and its original Z ordering.
It does not replace any OEM glass effect or intercept input.

The GPU effect only samples the same composed native content at the same pixel;
there is no bitmap, screen readback, extra blur pass or moving light/tint layer.
Effects cache immutable shader snapshots by integer width and direction, bounded
at 384 entries. Overlapping bands composite once and preserve each other's fully
transparent terminal region. Content or shadows overlapping the same local band
share its alpha treatment. Hardware/GPU appearance remains device acceptance.
