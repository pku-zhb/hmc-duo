# Native media card lifecycle

Media metadata and native View ownership are independent. A fold/configuration
change can replace the visible native card without changing the current track
or reattaching the clock container. Retaining the previous guard in that case
leaves the page action on a detached card; tapping the new native artwork opens
the player through the system's authentication path.

`NativeMediaCardLifecycle` observes only the native `MiuiMediaHeaderView` attach,
detach, configuration, visibility and holder events, plus its layout listener for
child reinflation. It discovers native headers once per window and then resolves
their current `mi_media_controls` children. A visible attached candidate in the
current window replaces the previous owner. An attached hidden current card can
remain bound while its capsule owns the screen; island layouts are ineligible.

Main publishes one binding containing the window/revision, card, artwork, title,
artist and ViewTreeObserver. Replacing any identity retires the previous guard,
page control and text leases before publishing new children. Consumer entry
points validate this binding before touch routing, page selection, material or
morph geometry. A stale callback from the previous card cannot clear the current
holder. Attach may precede child layout: the guard commits at the next native
pre-draw once the children are ready. There is no timer, metadata dependency or
retry loop.

`layoutstate` reports `player`, `playerGuard`, `playerBinding` and the actual
`playerPageControl`, including View identity, visible bounds and clickability.
The guard/control values are serialized before other diagnostics run; the
native player is discovered through a separate read-only scan. A probe cannot
hide a missing lifecycle handoff by repairing it before taking these values. The page action
shares the native artwork slot; tests must use its current visible bounds and
must stop if the bouncer becomes visible. Native transport, seeking, source
controls and secure app launch keep their existing behaviors.

Host regressions cover metadata-independent fold replacement, same-card child
reinflation, observer/window/revision changes, hidden-current retention and
rejection of stale detach or non-native candidates. Actual inner portrait and
landscape page cycling remains a device check.

Artwork visibility, alpha and scale have conditional property leases. An actual
binding retirement restores only values that are still this module's last
output. A scoped, observation-only `View.setRenderEffect` hook records effects
on SystemUI's `album_art` or already tracked artwork. This public API has no
getter: unless a setter value was actually observed, its original effect is
unknown and thumbnail blur is skipped. External later effects remain intact;
unchanged owned blur radii reuse their effect. The observer uses weak storage
and never changes unrelated views or material effects.
