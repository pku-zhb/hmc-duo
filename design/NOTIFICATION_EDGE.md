# Native-card horizontal feathering

## 0.2.4: isolate the card, not a strip of the combined stack

The owner accepted the symmetric edge curve in 0.2.3 but reported two gaps:
media/live cards did not all participate, and swiping a partially shelf-covered
notification affected the stationary card overlapping it. The old implementation
recorded all of NSSL and filtered a rectangular strip of that composite. No
rectangle refinement can separate two cards whose pixels already overlap.

The replacement composes an alpha mask into the moving **card's** native
RenderEffect instead. NSSL dispatchDraw, its blur node, each child's RenderNode,
actual clip, outline, elevation, shadows and native Z order remain in their
original drawing path. There is no copied stack, second native draw, bitmap or
readback. The stationary card is never included in another card's mask input.
The previous full-stack recorder and its draw-pass helper are retired outside
Git under the project's local development cache.

## Translation and baseline contracts

The matching SystemUI APK exposes separate `setTranslation(float)` methods on
ExpandableNotificationRow and MiuiMediaHeaderView. Only observing the former
missed the media header's swipe. Both exact setters now supply the same curve;
focus/live content inside native notification rows uses its row's native path.
The adapter only acts on native cards directly parented by Duo's owned NSSL.
It does not take over touches or require a partly visible notification to expand.

View.setRenderEffect only scopes the native card/RenderNode identity and whether
this is an owned mask or restore request. It never commits a lease at View entry.
Exact native card constructors establish a
fresh null baseline only when no native effect write was already observed.
On ROMs exposing getRenderEffect, an already-existing card can also be captured
at first use. An unknown late card without that getter stays native. An observed
external setter becomes the new baseline. During an active swipe its mask is
composed over that fresh baseline in the same native commit; it does not wait
for the next draw. Cleanup restores only the adapter's current output.

The retained current-ROM framework sources were checked directly: View only
exposes `setRenderEffect` (View.java:11789), and its RenderNode setter directly
calls `nSetRenderEffect` (RenderNode.java:798). Neither class exposes a getter or
retains the Java RenderEffect object. The current ROM therefore uses the early
constructor/setter observation path, not an inferred RenderNode getter. Unknown
preexisting cards remain native on this ROM.

The observer therefore sits at the verified RenderNode.setRenderEffect bridge,
at the lowest hook priority. Its current-ROM body contains only nSetRenderEffect,
with no View/user callbacks. The one known View caller is deoptimized so inlining
cannot bypass this bridge. A lease is committed only after this native bridge
returns, using the argument that actually reached it. View-level argument
replacement, short-circuit, exceptions and nested setters cannot publish an
uncommitted baseline. In particular, when a downstream View hook calls an inner
setter before the outer native write, the outer native commit correctly wins.
An owned mask/restore whose argument was replaced becomes a fresh native write.
Direct RenderNode setters on registered native cards are also observed.
Owned mask/restore writes invoke the original View setter body with an explicit
node scope. This preserves OEM property invalidation while preventing a separate
View effect owner's callbacks from retaining, replacing or short-circuiting the
temporary overlay or its cleanup. Ordinary external setters keep their complete
hook chain; their eventual RenderNode input remains authoritative.

The bridge contract is the retained OEM implementation, not arbitrary third-party
reentrant hooks inside RenderNode.setRenderEffect itself. Installation is all-or-
nothing: missing node identity, bridge hook or deoptimization leaves feathering
disabled. There is no fallback to setter-entry ordering or a guessed last write.

Where a getter exists, other effect owners read their native baseline rather
than accidentally retaining the temporary HMC mask as their restore snapshot.
The adapter's own verification can still read the actual output. Unobserved
private native writes that bypass the verified RenderNode bridge are not claimed
to be intercepted. On a ROM without the getter, restoration relies on the early
constructor and native-commit contract; no arbitrary preexisting effect is guessed.

## Geometry and cost

Both directions use the same smoothstep. Absolute swipe distance controls the
1..24 dp feather width, while terminal alpha is always zero. The parent viewport
is mapped into the card's local coordinates and intersected with its existing
horizontal clip. The native vertical/shelf clip is untouched. Unsupported
rotation, perspective or invalid geometry restores the native effect.

Only active swipe/return cards are revisited before native dispatch. There is no
View-tree scan. Shader effects are immutable uniform snapshots in an LRU limited
to 384 entries. Effect/baseline records use weak View keys, without value-to-key
references. Scene release removes the masks but keeps weak early-baseline records
for cards reused by the next lockscreen. Hook installation is transactional and
passive until complete; unsupported contracts retain native behavior.

## 0.2.6 timing and native inner-screen bounds

The previous external-material branch committed an unmasked value even when a
card was already moving. A later dispatchDraw repaired it, leaving a possible
one-frame gap when material updates landed after the translation callback. Native
and owned-mask commits now share the moving-card policy; restore commits always
remain unmasked. The incoming native effect is the new chain baseline, and the
existing post-native lease commit remains authoritative. Shelf clipping, per-card
isolation and the original native drawing path are unchanged.

The 0.2.6 native-width experiment has been superseded by 0.2.7 owner feedback.
Both inner orientations now use their current Duo content frame, with the same
center as the clock/artwork rather than the OEM full-window axis. This may be
wider than the OEM card column, deliberately preserving common alignment. The
frame is computed from the current window and fixed rail reserve, never from an
animated card width. Native vertical stacking and swipe behavior remain unchanged;
geometry is released outside keyguard. The edge renderer uses that actual viewport.

## Verification boundary

Production Java compilation passes against the existing compiled project and
API 37/libxposed 102. Thirteen pure JVM tests cover the edge curve plus independent
card leases, external/native-null baselines, uncommitted/failed entries, replaced
arguments and an outer-last View reentry simulated through the production commit
scope. Native effect state and the lease finish on the same outer commit.
Full Gradle and actual GPU acceptance are performed by the owning release task.
No phone UI, screenshots, installation, parameter changes or scope restart were
used here. Device acceptance must confirm that all intended media/live card
implementations use the retained native translation contract and that native
material sampling remains correct with the per-card effect chain.
