# Preview 16

This iteration implements the HMC side of the HMC Duo / HyperChange Lite
material-ownership handoff. It does not modify Lite, MiDuo, module scopes or
phone settings. Preview 15 remains included, with its visual acceptance pending.

## Changes and causes

- Default music background, default motion and system theme previously kept
  restoring construction-time alpha/context and could replay a cached native
  effect. Defaults now relinquish material ownership. A custom appearance only
  restores values it actually wrote and still owns. Settings changes, detach and
  replacement render requests invalidate old background work. Artwork, lyrics,
  controls and card geometry remain independent. A real ownership handoff asks
  the current OEM calculation to run once; an AOD deferral waits for a native
  lifecycle event, without polling or adding another binder collector.
- The previous glass relay sampled View setter arguments before other modules
  could replace their arrays. The new relay observes the verified RenderNode
  commit bridge under the exact native media background's View scope. Background
  blur and blend values use native getter readback. Only registered HMC capsule
  and shortcut layers receive the final profile; the source player is never a
  relay destination. Identical profiles do not refresh the whole rail or bump
  its material generation. Native recipes stay independent of final follow data.
- Notification transitions restored stale alpha/outline/clip and blend state,
  and some delayed work outlived its scene. Each temporary property now has a
  conditional handback; scene exit, AOD, credentials, detach and heads-up role
  changes retire owned work. Geometry is released before one current OEM
  material recalculation. Current firmware uses a four-argument notification
  binder, while the old hook expected five; both verified layouts are handled
  explicitly. Heads-up notifications are excluded from ordinary capsule morphs.

## Supported combinations

| Combination | HMC music settings | Lite responsibility |
| --- | --- | --- |
| A | Default background, default motion, system theme | Music-material control enabled |
| B | Custom music background or motion | Music-material control disabled; ordinary notification control may remain enabled |

Capsules retain their independent native/follow-final-material choice. This is
not a cross-module settings broker: neither module edits the other's preferences.
Lite's new music master switch, notification scope, disabled-work cleanup and
configuration migration must be supplied by its separate implementation.

## Validation status

Host build, tests and artifact checks are recorded after completion below.
The ROM-specific media calculation and notification binder signatures were
verified against retained SystemUI code. The View/RenderNode bridge and material
readback signatures were verified against the phone's framework, copied through
read-only ADB. No screenshot, GUI input, installation, scope reload or device
reboot was used. Missing OEM signatures fail conservatively rather than replaying
a cached effect.

A compatible fixed Lite version and paired device acceptance remain pending.
The previous Lite baseline is not evidence that its handoff requirements are
implemented. Passing host tests cannot establish visual compatibility.

## Focused paired acceptance

1. With combination A, compare the music card and follow-mode capsules/shortcuts
   while changing Lite music material. Repeated song changes must not make the
   layers alternate between styles. HMC artwork, lyric and control settings must
   remain effective.
2. With combination B, change HMC backgrounds and motion, then return all three
   material settings to defaults. The current native/Lite material must return
   without a stale overlay, alpha or previous theme. Leave Lite music control off
   while HMC uses a custom appearance.
3. Expand, collapse, reverse and interrupt notifications, then open the native
   lock-screen list. Completed rows must retain their current native material
   without a delayed fade/clip write. Heads-up material must remain independent.
4. Repeat the ownership handoff through AOD/wake and fold/rotation. No borrowed
   material or unfinished notification transition should survive its old scene.
5. Record exact HMC and Lite versions. Verify HMC alone, Lite alone and the fixed
   pair before declaring compatibility accepted. Installation and scope reload
   require separate owner authorization.
