# Geometry-bound local artwork composition

Local backgrounds retain an owned, uncropped artwork source independently of media-session
updates. The source preserves aspect ratio and has a maximum edge of 1280 px. Player-owned
bitmaps are never retained or recycled. Repeated publication of the same track and identical
pixels keeps the source identity; accepting different source pixels changes it immediately.

A composition key contains the SystemUI window identity, geometry revision, window width and
height, cover mode, card/full composition choice, source identity, blur and vertical bias.
The UI thread captures that immutable key. The existing artwork worker prepares its result
without reading changing screen or style state. Geometry and style events request work
immediately; paused playback does not require metadata publication or a delayed retry.

The worker has one active operation and one replaceable request. Source references protect
an original while it is being copied. A completion is adopted only if its complete key still
matches the latest request, retained source and live scene. Reverse-order completions and
obsolete windows cannot publish a result. Failed work may be retried by a subsequent event.

The renderer distinguishes the desired composition from actual outgoing pixels. During a
same-source, same-window rebuild, the background handoff may keep its existing committed
source, revision and generation. It does not relabel an old crop as the new geometry. A new
source/window, notification ownership, navigation, unlock or another ineligible scene
withdraws that ownership. The incoming renderer must draw its own matching key before its
revision/generation can commit. Sampling ownership uses actual renderer and composition
alpha, separately from bitmap resource lifetime.

Prepared bitmaps have one current frame, at most one outgoing frame and one latest pending
result. An outgoing bitmap is not recycled merely because composition or animation time
completed. Every still-needed rendering surface must first submit a clean display list
without the predecessor and acknowledge it on the following animation frame. Mixed track
crossfades retain both resources; a clean draw at reduced layer opacity can still release an
unused predecessor. Detached/reparented presenter callbacks cannot acknowledge a new
window. A never-drawn intermediate result can be discarded without discarding the actual
outgoing frame. Existing track-change timing is unchanged; geometry replacements do not add
a second fade or wallpaper acknowledgement delay.

## Verification

Pure JVM tests cover complete composition-key equality, reverse completion order, returning
to an earlier window with a new revision, mode/material changes, a paused retained source,
request coalescing, failed-request retry and stale cancellation. Frame-lifetime tests cover
mixed display lists, separate foreground/background acknowledgements, detached callbacks,
unused surfaces and replacement isolation. Production Java is also compiled against the
Android and module interfaces before the release build.

Device diagnostics expose the retained source identity, desired and actual complete keys,
pending state, and independent desired/committed background geometry and generation. For a
square source with blur enabled, representative expected local bitmap sizes are:

| Window | Local bitmap |
| --- | --- |
| 1168 × 1712 | 524 × 768 |
| 1672 × 2364 | 543 × 768 |
| 2364 × 1672 | 768 × 543 |

These dimensions must update while playback is paused after each geometry event. Host tests
do not establish visual acceptance or native sampling behavior; the release device pass
must check the actual and desired keys converge and that old completions do not overwrite
newer geometry.
