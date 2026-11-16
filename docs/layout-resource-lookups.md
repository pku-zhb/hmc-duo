# Native layout resource names are cached within their resource table

The compact native player's per-frame layout and the artwork background lookup
use `LayoutResourceIds` for SystemUI resource names. The cache stores identifiers
only. Every native frame still calls `findViewById()` and
`getDimensionPixelSize()` at the original point, so late inflation, replacement
Views, current AOD dimensions and external resource-value hooks remain live.

An entry belongs to one weak `Resources` owner, a full copied `Configuration`,
and its asset table. The asset boundary includes both the `AssetManager` identity
and the identity/order of its `ApkAssets`; replacing assets inside the same
manager therefore invalidates the entry. Missing names are cached as zero within
that epoch and are resolved again after configuration or asset-table changes.
Resolution failures are never cached as a missing resource.

The compact layout and seek-alpha callbacks each open a synchronous lookup scope.
It snapshots the asset table at most once per Resources owner during that call,
then releases all scope references before returning. The hidden `getApkAssets`
accessor is resolved once. If that accessor is unavailable or fails, lookups use
native name resolution without caching.

This changes resource-name lookup cost. It does not skip layout updates, freeze
pixel values, change animation duration, retain a missing child View, or alter
material ownership and restoration.
