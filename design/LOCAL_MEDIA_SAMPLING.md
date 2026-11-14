# Native media glass over local artwork

## Source diagnosis

HMC draws its visible cover backdrop inside the SystemUI window. The shipped
`MediaViewGlassOnKeyguardEffect` applies glass to the holder's `media_bg`;
`applyElementViewBlend` enables element blur mode 1 but does not establish that
leaf's own local background sampler. Numeric glass parameters therefore do not
identify the pixels being refracted. A native cross-window source still sees
wallpaper rather than HMC's local cover. A sampler on a root ancestor also
cannot capture artwork subsequently drawn within that ancestor's child subtree.

The retained framework DEX exposes committed background mode, pass-window,
background type and explicit source-container getters. Its pass-window setter
registers/removes a separate window texture and changes the RenderNode flag.
These are static source observations, not a GPU capture of the reported phone
scene. The exact active native source in that scene still needs device acceptance.

## Narrow sampling lease

`NativeMediaBackdrop` binds only the actual holder's `media_bg`, after the
existing native-material validation. While HMC's backdrop has drawn visible
pixels in the same root window and that leaf consumes native blur, the same
leaf temporarily uses OEM local-content sampling: background mode 1, type 1,
pass-window disabled, with its explicit external container cleared. This is the
OEM local sampling path also used by the local material preview; it does not
add another glass overlay or replace the native recipe.

The glass recipe, refraction radii, blur strength and color palettes remain
under native/Lite ownership. Background mode, pass-window, type and explicit
container are read before the lease and restored when the artwork disappears
or the source is replaced. Subsequent native writes update only their
own baseline property; HMC's own writes are excluded from that capture.
Normal solid media cards do not enter the lease. Unsupported getters or
rejected writes restore native state and retire that binding.

## Final material profile lifetime

The retained framework's View setter describes a false RenderNode result as
"params not updated". The JNI implementation has no exposed glass getter, so
an arbitrary false result is not treated as proof of acceptance. Instead, a
weak, node-identity cache records only observed successful lower-level writes
on structurally verified native media backgrounds. After a scene-profile reset,
a false write can reuse a value only when it exactly matches that same node's
most recent successful write. Unknown false results and rejected/different
values cannot seed the cache, a replacement node cannot borrow another node's
recipe, and newer successful writes invalidate the old evidence. The cache
observes already-known native nodes while keyguard is absent without applying
anything to that scene.

The known OEM effect entry identifies its own holder before the effect runs.
It no longer waits for Main's cached current-card identity, which can still
refer to the pre-fold holder. Final forwarding still requires the genuine
native media lineage, an allowed locked scene and the current keyguard root;
registered HMC destinations and island copies remain excluded as sources.
These are source-path fixes and pure-host node-lifetime tests, not a verified
GPU-material acceptance result.

There is no per-frame hierarchy discovery or native effect rebind. The existing
backdrop pre-draw guard evaluates cached state and only writes when the lease
changes. The content-free `glassRelay` diagnostic reports its active state.

The validated holder is retained weakly across temporary detach and off-keyguard
material calls. Detach pauses writes without removing its source/owner attach
listeners: the framework can already have lost the ViewRoot needed by the
pass-window setter. Reattach rechecks its topology, then reapplies the current
domain or restores the retained native baseline. A changed root is revalidated
through the existing OEM-holder check. Replacing/rejecting the holder or a failed
getter/write still clears the binding. Previously detach removed its own only
attach listener, so a reused holder needed another OEM effect call before local
sampling could return; backdrop pre-draw alone could not recover it.

## Verification boundary

Pure host tests cover local/native transitions, independent native updates and
replacement-source state. They cannot prove GPU input pixels or appearance.
The device check is the same music card with Lite media material enabled, HMC
blurred cover background on/off, and a wallpaper visibly different from the
cover. Glass should follow the visible background and restore normal native
sampling when the HMC backdrop is absent. Also check a track change and unlock
so neither the prior cover nor its sampling lease survives the current scene.

The awake mesh and the full-screen AOD still wash are distinct renderers. The
earlier lease recognized only the mesh; that renderer intentionally stops at
sleep, so even a correctly drawn AOD artwork wash restored desktop sampling.
The lease now also accepts the AOD wash after it has submitted visible pixels
for the current prepared-source generation in the media card's root window.
Prepared bitmaps, stale generations and zero-alpha frames do not authorize it.
The wash's first submitted frame requests one sampling refresh; AOD does not
start the mesh animation or an extra continuous draw loop. Hiding the wash clears
its frame evidence, and native-wallpaper mode and unlock still restore native
sampling.

This AOD correction has host frame-evidence checks. The owner's reported
desktop-to-AOD scene still needs acceptance; no screenshot or live setting change
was used to infer GPU pixels.
