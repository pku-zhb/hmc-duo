# Native player height

`notification.height` controls the outer screen and inner portrait together.
`notification.heightLandscape` independently controls inner landscape. Both are
integer percentages, bounded to 85–100, defaulting to 100. They use the existing
`mediastyle` configuration, query and normalized backup JSON; old settings acquire
100 for both fields. Island geometry and native material ownership are unchanged.

The retained SystemUI normal media constraint set uses a 185dp background,
52.5dp artwork, 50dp action row and 38dp seek touch row. Its main vertical gaps
are 16dp above artwork, 11dp before actions, 2dp before seek and 16dp below seek.
`loadLayout$1` loads those constraints and `updateLayout$1` reapplies them to the
native player. These are static observations from the retained APK, not new
measurements from a connected phone.

The runtime leases only those vertical gaps and the matching background/container
height. The title and output chooser move upward by the same offset as artwork.
Artwork dimensions, all action and seek dimensions, padding, widths, scale and
native constraint anchors are preserved. Targets keep at least the smaller of
their native gap or 4/2/2/4dp respectively. The requested reduction is capped by
both available whitespace and the complete native control-row height. An already
tight AOD/tiny variant or unknown flexible-height layout stays native if it has no
safe reduction budget. 100 restores the latest observed native values rather
than retaining the former automatic inner-screen compaction.

Observed native geometry updates replace each property's baseline independently;
the runtime's own last write does not become the next baseline. Returning to 100,
unlocking, replacing the holder or detaching the fold layout releases the lease.
Conditional writes settle after one native layout, without reassigning a height
derived from animated/measured bounds every frame.

Seven metric regressions cover native defaults, safe gap allocation, tight native
variants, unknown dimensions, monotonicity and display profile selection. Two new
configuration regressions cover old settings, independent profiles, limits and
backup round trips. The complete focused suites passed 16 tests together with
the production Kotlin/Java compile. This work did not connect to a phone. Later
acceptance should adjust the two profiles, return each to 100 and confirm that
playback/seek touch areas remain unchanged through lock/AOD/fold transitions.
