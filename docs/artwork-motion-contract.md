# Artwork content motion

Artwork layout belongs to the native content's logical coordinate frame. Native
AOD scale and offset then present that frame once. The same contract applies to
the artwork and lyric columns on the combined page.

`NativeContentMotion` chooses `shared_notification_container`, matching the native
affine source already consumed by lyric presentation. It falls back to
`keyguard_root_view`, then the window when those sources are unavailable. A held
single-page frame is valid only for its original window, geometry revision and
native owner. A rebuilt owner cannot inherit another view's logical snapshot.

For a native owner matrix `N`, consumer matrix `C`, and logical pose `P`, the
consumer receives `inverse(C) * N * P`. Drawing the consumer produces `N * P`.
This works both when the consumer already inherits the native transform and when
it lives in another branch or window. Holding `inverse(C) * P` instead freezes
physical bounds and cancels the intended AOD motion. Multiplying another AOD
scale afterward would apply it twice.

The combined plan is fitted in natural layout pixels. Converting that plan to its
native owner's coordinates subtracts layout offsets only; it never inverts an
animated scale to derive paragraph width. Artwork is fitted in that logical room
before its finished pose is mapped. Lyrics keep their logical line width while
mapping their center and vertical room through the same owner.

New and rebuilt plans follow the same contract even if the native owner is
already translated or scaled. `ContentMotionMeasurement` removes that common
affine from the measured clock/player inputs before fitting, then restores their
natural layout offsets. Window dimensions, content width, insets and dp parameters
stay in natural layout pixels. This preserves a native child's own compact
layout while preventing the common AOD transform from entering the plan twice.
The complete fitted plan is never inverse-scaled to normalize measurements.

Pre-draw, clock publication and artwork animation use `followPlace` as the single
placement writer. A motion-owned pose cannot be carried by a second local-layout
delta or capped again against a transient native-player width. Playback scaling
and track-change treatment remain separate artwork effects.

The JVM regressions replay native shrink/offset through independent consumer
transforms, cancellation and reversal, combined-column spacing, transient native
card width loss, and snapshot owner/window/revision changes. These checks establish
the source contract; fresh device recordings are required to accept visible motion.
Measurement-boundary tests also build a new plan directly from non-identity native
measurements, including translated/scaled first entry and invalidation/rebuild.

The existing `motiontrace` diagnostic can record immediately with `now=true` and
`ms=4000` (maximum). `read=true` returns its last completed sample without arming a
new run. Each line includes the native fraction, affine source, logical artwork
frame and presented window bounds. Retention/readback is capped at 240,000
characters and reports an explicit capacity stop. Samples are read-only and do
not establish display smoothness or recording fidelity.
