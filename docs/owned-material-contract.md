# Optional owned material provider, version 1

The public interface is `com.zhuhuibin.hypermusiccover.duo.NativeMaterialTarget`.
It uses only boot-classloader types and has no provider dependency. Interface
membership alone grants no access: `hmcMaterialState()` has no role bits until
Duo explicitly registers that exact glass element and its exact blur container.

Methods:

- `int hmcMaterialContractVersion()`: supported protocol version, currently 1.
- `int hmcMaterialState()`: current recipe bitmask: glass 1, blur 2, follow 4,
  current attached keyguard scope 8, native AOD recipe/frame 16, provider claim 32.
- `View hmcMaterialOwner()`: registered glass element for either member; null for
  an invalid or unregistered pair.
- `boolean hmcMaterialClaim(int version)`: synchronously and idempotently claims
  the exact pair. Unsupported versions and invalid registrations return false.
- `boolean hmcMaterialReapply()`: synchronously applies a fresh current native
  recipe on the UI thread; false when unavailable. It never replays saved player
  setters or a provider-mutated baseline.

Each current carrier (`MiniPlayerView`, `ShortcutDisc`, `GooGlass`) owns exactly
one `MaterialElement`. A live sibling registration is rejected; removing or
reparenting the previous element retires that pair before reuse. Pair values
hold weak references, including when a detached pair retains its claim.

A provider admits only version 1, a valid role/owner, and a successful claim.
The claim remains while its tuning is disabled: Duo suppresses player parameter
relay for that pair so tuned player values cannot be applied twice. Sampling
remains bound solely to the actual native player source. Querying never claims.

Tuning requires follow, keyguard scope, the provider's existing lockscreen
notification range, and its existing material switches. Native AOD bit 16 forbids
replacing the frame with an awake unified recipe. Providers refresh each owner
once through `hmcMaterialReapply()`, including when disabling, rather than replay
historical setters. Registration precedes all native material setters, allowing
the first View setter to claim before the final native RenderNode commit.

Raw recipes come directly from current native resources and full-AOD callback
parameters. A recipe builder must never pass through a media-classified probe;
unregistered interface instances have no role and receive no provider tuning.
Fresh complete recipes preserve the native outline/view-blur setup, then write
one complete raw palette through `View.setMiBackgroundBlendColors(ArrayList)`.
This is the provider's atomic palette-substitution boundary. Per-frame native
AOD callbacks may retain the OEM legacy clear/add path: their native-AOD flag
forbids replacing those frames with an awake unified palette.
The renderer's native default blur scale is read once from a fresh unregistered
`MaterialElement`, without effects, setters, attachment or media classification.
Fresh reapplication writes that raw scale and the current resource blur radius
unconditionally, so enabling and disabling cannot depend on an OEM unchanged
value shortcut. Owner attach/layout callbacks finish the first measured recipe;
an unready locked owner temporarily observes native global-layout visibility
events (the clock host can be a sibling), then removes that observer on success
or detach. No timer or per-frame readiness polling is used.

## Synchronous recipe and scene boundaries

The complete owned recipe opens `NativeOwnedMaterialBridge.beginRecipe()` only
after registration and closes it in `finally`. All provider queries during its
nested setters share one pair/scene snapshot. The provider-claimed bit remains
live, so the first setter's claim is visible to every later setter. Nothing is
retained between recipes: the next AOD frame rechecks its current scene, pair and
native-AOD state. This bounds repeated provider inspection without a timed cache.

Keyguard, control-center and editor events update readiness. Repeated unchanged
notifications do not replay the full recipe. Blocking by the control center or
editor removes the global-layout listener; the explicit closing event rechecks
readiness and applies once before returning. Settings/provider requests and AOD
completion retain forced-refresh semantics.

The native source still publishes complete accepted frames synchronously. The
relay compares glass, blur radii, palette, background radius and background scale
independently. A palette-only frame does not replay unchanged container channels.
Completing an owned palette restores container radius/scale only after the raw
recipe actually wrote those channels. Empty source palettes or absent current
glass evidence cannot erase a complete raw owned palette.

`glassRelay` diagnostics include owned-provider recipe-entry, state-query,
scene-read, refresh, readiness-check and reapplication counters. Compare deltas
over separately timestamped transitions; host tests do not establish device
latency or visual acceptance.
