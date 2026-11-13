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

This lease covers the lit local artwork renderer. On AOD or another scene where
that renderer is ineligible, it restores native sampling rather than retaining a
stale local source.
