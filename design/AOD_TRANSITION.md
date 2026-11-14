# Native AOD presentation progress

HMC keeps artwork-page changes and native AOD transitions separate. Cover/lyrics/player page
selection retains its existing spring. Full-screen AOD presentation follows the native
`NotifiFullAodController.FullAodTransitionListener` instead of starting another elapsed-time
animation.

## Progress contract

The native controller's `mCurrentFraction` is direction-local, from zero to one on both sleep
and wake. Its glass arrays are not a reliable progress measurement: desktop entry starts from
the normal material array, while lock-screen entry starts from the keyguard array. Completion
also resets that field to zero before notifying listeners.

`AodTransitionProgress` publishes an absolute fraction, zero for the lit lock screen and one
for full-screen AOD. Native begin/update/complete/cancel events own a running transition.
Reversal begins from the last actually published fraction. Native doze/display events provide
cold endpoints when no animation exists; a native `mIsAnimating`/`mWillDoAodAnim` plan keeps an
early doze event from prematurely publishing the final endpoint. Window identity and geometry
revision bound the snapshot. A different window or display revision cannot borrow the previous
window's progress.

The held clock's AOD zoom and wake consume this fraction. Ordinary/custom AOD remains native.
The existing complete-pose validity and same-domain clock ownership contract still gates every
write; progress alone never authorizes hiding the system clock.

## One geometry publication boundary

Animation callbacks precede traversal. A lyric animation callback previously measured the old
clock pose, then rejected its later pre-draw because that vsync had already integrated. The clock
now publishes geometry to cover and lyric consumers after committing its complete pose.
`LyricView` integrates scrolling, word fill and focus physics once per vsync, while its geometry
can consume a later commit in the same vsync. The separate lyric window keeps one native scale
source through sleep/wake rather than switching pivots when the clock phase changes.

Lyric centering has two components: the band's native geometry and the lyric focus animation.
When clock/card geometry moves, the target delta is carried into the current anchor directly.
The remaining focus error still eases normally. This avoids low-pass filtering already animated
geometry, then cutting that lag away at the still-AOD endpoint.

The rail artwork's fraction-zero box is the same normal placement used after the AOD geometry
lease ends. Its fitted AOD box and its parent-transform compensation consume the current native
fraction and the same pass's clock/media bounds, without another placement lag.

## Backdrop coverage and native player

The committed outgoing backdrop stays opaque while the incoming renderer presents. Retaining a
sampling key while independently fading out its actual pixels would leave a coverage hole when
the new renderer is not ready. Incoming AOD/wake reveal now consumes the native fraction; source,
composition, window and actual-draw acknowledgement still decide the handoff. Delayed removal
rechecks current retention and cannot expire a retained renderer by elapsed time.

The compact player's shell remains driven by native `setActualHeight`; its intrinsic target
still uses the existing affine mapping to compact endpoints. HMC does not feed the changing
actual height back into the native intrinsic-height target. Seek opacity follows physical room
in that same height timeline symmetrically. Native 100-percent mode remains untouched.

## Verification boundary

JVM regressions cover cold endpoint publication, local-to-absolute direction conversion, early
doze state during a planned transition, cancellation/reversal continuity, geometry-domain
invalidation, opaque outgoing coverage, lyric geometry rebasing, and compact seek endpoints.
Compilation and these policy tests do not establish visual smoothness or native callback order
on every supported device. Real sleep/wake, interrupted transitions, and physical fold changes
still require device observation. `aodprobe` and `lyricstate` expose bounded native progress
fields for that verification.
