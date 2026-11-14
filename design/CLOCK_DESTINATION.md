# Clock destination ownership

The selected artwork page owns clock destination intent. Native date/glyph measurement and
status-overlay availability are separate readiness signals; neither changes the user's
selected destination or permanently resets its transition target.

## Direct full-screen AOD entry

A desktop sleep can prepare the clock before the native date and right-rail status overlay
are visible. Previously, non-animated entry stored `intent && ready` as the STATUS target.
If readiness was false, target/progress became zero. The native pose could nevertheless be
held in AOD, and AOD never reevaluated that STATUS target. Later readiness did not help; only
a wake started another entry and recalculated it. A preceding lit lock-screen entry already
had progress one and therefore behaved differently.

`ClockReplacementState` now keeps target/progress independently of readiness. Held full-screen
AOD consumes the selected endpoint even when there was no measurable lit frame, or a native
pose was held before its replacement destination committed. Actual acquisition still requires
an eligible scene owner, supported display, valid native measurement and a ready status
overlay in the same SystemUI window. Native/custom AOD and empty-lyric pages with no clock
owner retain native time.

A date-row destination additionally needs coordinates. It does not latch a cold AOD endpoint
until a valid row plan exists. If a native pose was already held while that plan was missing,
the first ready row is committed through the same pre-draw entry and AOD-latch path. Plan
preparation is not presentation proof: the current date/overlay pose must actually have been
applied. An already presented lit row remains held without re-entering each frame.

## One presentation commit

Readiness arrival invalidates the existing clock pre-draw listener. It does not enter the
clock transition recursively or start a delayed retry. The unified pose writer first applies
the inverse alpha to the native time face and then publishes the replacement alpha for that
window and geometry revision. The status overlay reads this committed alpha, rather than
computing a new alpha from readiness in its own listener. Listener ordering therefore cannot
show a newly ready replacement before the matching native-clock change.

Releasing transforms or changing window/revision invalidates the presentation commit. Clock
and cover-rest pose caches additionally retain weak identities for their measured native time
and date nodes. A replacement native hierarchy in the same window cannot borrow the previous
nodes' coordinates; it must supply its own valid measurement. This is a cache-domain boundary,
not evidence that the observed direct-AOD failure actually replaced its native nodes.

## Verification

Pure JVM regression tests exercise direct entry followed by late readiness without another
entry, parity with lit entry, a held native pose with no replacement commit, unfinished lit
entry followed by held AOD, atomic inverse-alpha publication, no-owner/custom-AOD fallback,
reverse transitions, cold/warm/late date-row destinations and window/revision/native-node replacement. Production Kotlin and Java
are compiled together against the module interfaces before the full release build.

The device pass should compare lit lock-screen to full AOD and desktop to full AOD with a
fresh usable media session. On a valid STATUS lyric page, steady native time targets should
have alpha zero, while the status time has alpha one and clock listening enabled. Without
foreground content or a clock scene owner, native time remains visible. Host tests establish
the state contract; these device counters establish the actual lifecycle handoff.
