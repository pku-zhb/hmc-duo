package com.zhuhuibin.hypermusiccover.duo;

import android.content.res.Configuration;
import android.graphics.RenderNode;
import android.graphics.Point;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** One-way relay from committed OEM player material to explicitly registered Duo layers. */
final class NativeGlassRelay {
    private NativeGlassRelay() {}
    private static final int SOURCE = 0, GLASS_TARGET = 1, BLUR_TARGET = 2;
    private static final Map<View, Integer> targets = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, GlassProfile> committedGlass = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, GlassProfile.Blur> committedBlur = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, GlassProfile.Background> committedBackground = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Integer> containerWrites = Collections.synchronizedMap(new WeakHashMap<>());
    private static final NativeMaterialCommitScope<View> scopes = new NativeMaterialCommitScope<>();
    private static final NativeMaterialBatch<View> nativeBatches = new NativeMaterialBatch<>();
    private static final NativeMaterialNodeCache<Object> nodeCommits = new NativeMaterialNodeCache<>();
    private static final NativeMaterialSourceBinding<View> sourceBinding = new NativeMaterialSourceBinding<>();
    private static final NativePaletteBatch<View> palettes = new NativePaletteBatch<>();
    private static final ThreadLocal<View> paletteReplay = new ThreadLocal<>();
    private static volatile GlassProfile profile;
    private static volatile GlassProfile.Blur blur;
    private static volatile GlassProfile.Background background;
    private static GlassProfile appliedProfile;
    private static GlassProfile.Blur appliedBlur;
    private static GlassProfile.Background appliedBackground;
    private static boolean installed, refreshPosted, paletteFramePosted;
    private static volatile boolean glassInstalled, blurInstalled, backgroundInstalled;
    private static Method glassSetter, blurSetter;
    private static Method backgroundRadius, backgroundScale, backgroundColors;
    private static Method readRadius, readScale, readColors;
    private static Field renderNode;
    private static int captures, applications, blurApplications, refreshes, paletteRestores;

    static void register(View target) { register(target, GLASS_TARGET); }
    static void registerBlurContainer(View target) { register(target, BLUR_TARGET); }
    private static void register(View target, int role) {
        // A caller cannot turn an OEM card into a relay destination by registering it.
        if (NativeMaterialSource.ownedTarget(target))
            synchronized (targets) { targets.put(target, targets.getOrDefault(target, 0) | role); }
    }
    static void unregister(View target) { targets.remove(target); palettes.remove(target); containerWrites.remove(target); }

    /** Effect.apply and the verified media AOD listener each own one complete native recipe. */
    static NativeMaterialBatch.Frame<View> beginNativeUpdate(View source) {
        return backgroundInstalled && sourceBinding.isSource(source) && sourceStillBound(source) ? nativeBatches.begin(source) : null;
    }

    static void endNativeUpdate(NativeMaterialBatch.Frame<View> frame, boolean success) {
        if (frame == null) return;
        if (nativeBatches.end(frame, success) && sourceBinding.isSource(frame.source) && sourceStillBound(frame.source))
            captureNativeSnapshot(frame.source);
        // Native mode/pass/type writes in the same recipe must finish before our sampler
        // chooses its input domain. A failed recipe still releases this synchronous scope.
        NativeMediaBackdrop.refresh();
    }

    static boolean nativeUpdateInProgress(View source) { return nativeBatches.active(source); }

    static boolean nativeUpdateSucceeded(NativeMaterialBatch.Frame<View> frame) {
        return frame == null || !nativeBatches.failed(frame);
    }

    /** Complete owned recipes restore their final palette once, before returning to draw. */
    static void beginOwnedPaletteUpdate(View target) {
        Integer role = targets.get(target);
        if (role != null && (role & GLASS_TARGET) != 0) palettes.begin(target);
    }
    static void endOwnedPaletteUpdate(View target) {
        if (palettes.finish(target)) {
            restorePalette(target);
            restoreContainerBackground(target);
        }
    }

    /** View hook order is irrelevant: only the known RenderNode commit bridge supplies values. */
    static void install() {
        if (installed) return;
        installed = true;
        NativeMediaBackdrop.install();
        try {
            renderNode = View.class.getDeclaredField("mRenderNode");
            renderNode.setAccessible(true);
        } catch (Throwable error) {
            Xp.w(Main.TAG + "native material commit identity unavailable: " + error);
            return;
        }
        try {
            glassSetter = View.class.getMethod("setMiGlass", float[].class);
            Method commit = RenderNode.class.getDeclaredMethod("setMiGlass", float[].class);
            Xp.api().hook(commit).intercept(chain -> {
                if (!glassInstalled) return chain.proceed();
                Object node = chain.getThisObject();
                NativeMaterialCommitScope.Frame<View> frame = scopes.matching(node);
                if (frame == null && !nodeCommits.tracked(node)) return chain.proceed();
                Object[] args = chain.getArgs().toArray();
                if (frame != null && (frame.role & GLASS_TARGET) != 0
                        && !NativeOwnedMaterialBridge.claimed(frame.target) && followsNow() && profile != null) {
                    float[] values = profile.forScene(frame.nightMode, frame.awake);
                    if (values != null) { args[0] = values; ++applications; }
                }
                Object result;
                try { result = chain.proceed(args); }
                catch (Throwable error) {
                    // A downstream callback can throw after the native write. Its actual
                    // node state is uncertain, so no later palette-only frame may reuse it.
                    nodeCommits.uncertainGlass(node);
                    throw error;
                }
                float[] accepted = nodeCommits.glass(node, args[0] instanceof float[] ? (float[]) args[0] : null,
                        Boolean.TRUE.equals(result));
                frame = scopes.matching(node);
                if (frame != null && frame.role == SOURCE && nativeBatches.defer(frame.target)) return result;
                if (frame != null && frame.role == SOURCE && accepted != null) {
                    GlassProfile next = GlassProfile.capture(accepted, frame.nightMode, frame.awake);
                    if (next != null) {
                        committedGlass.put(frame.target, next);
                        if (!next.same(profile)) { profile = next; ++captures; refreshTargets(); }
                    }
                }
                return result;
            });
            installScope(glassSetter);
            glassInstalled = true;
        } catch (Throwable error) { Xp.w(Main.TAG + "native glass commit relay unavailable: " + error); }
        try {
            blurSetter = View.class.getMethod("setMiGlassBlurRadius", int.class, int.class);
            Method commit = RenderNode.class.getDeclaredMethod("setMiGlassBlurRadius", int.class, int.class);
            Xp.api().hook(commit).intercept(chain -> {
                if (!blurInstalled) return chain.proceed();
                Object node = chain.getThisObject();
                NativeMaterialCommitScope.Frame<View> frame = scopes.matching(node);
                if (frame == null && !nodeCommits.tracked(node)) return chain.proceed();
                Object[] args = chain.getArgs().toArray();
                if (frame != null && (frame.role & BLUR_TARGET) != 0
                        && !NativeOwnedMaterialBridge.claimed(frame.target) && followsNow() && blur != null) {
                    int[] values = blur.forScene(frame.nightMode, frame.awake);
                    if (values != null) { args[0] = values[0]; args[1] = values[1]; ++blurApplications; }
                }
                Object result;
                try { result = chain.proceed(args); }
                catch (Throwable error) {
                    nodeCommits.uncertainBlur(node);
                    throw error;
                }
                int[] accepted = nodeCommits.blur(node, (Integer) args[0], (Integer) args[1],
                        Boolean.TRUE.equals(result));
                frame = scopes.matching(node);
                if (frame != null && frame.role == SOURCE && nativeBatches.defer(frame.target)) return result;
                if (frame != null && frame.role == SOURCE && accepted != null) {
                    GlassProfile.Blur next = GlassProfile.Blur.capture(accepted[0], accepted[1],
                            frame.nightMode, frame.awake);
                    if (next != null) {
                        committedBlur.put(frame.target, next);
                        if (!next.same(blur)) { blur = next; refreshTargets(); }
                    }
                }
                return result;
            });
            installScope(blurSetter);
            blurInstalled = true;
        } catch (Throwable error) { Xp.w(Main.TAG + "native blur commit relay unavailable: " + error); }
        installBackgroundReadback();
    }

    /** These OEM getters read state written by the original setter, including downstream edits. */
    private static void installBackgroundReadback() {
        try {
            readRadius = View.class.getMethod("getMiBackgroundBlurRadius");
            readScale = View.class.getMethod("getMiBackgroundBlurScaleRatio");
            readColors = View.class.getMethod("getMiBackgroundBlendColors");
            backgroundRadius = View.class.getMethod("setMiBackgroundBlurRadius", int.class);
            backgroundScale = View.class.getMethod("setMiBackgroundBlurScaleRatio", float.class);
            backgroundColors = View.class.getMethod("setMiBackgroundBlendColors", ArrayList.class);
            Method clear = View.class.getMethod("clearMiBackgroundBlendColor");
            Method add = View.class.getMethod("addMiBackgroundBlendColor", int.class, int.class);
            for (Method setter : new Method[]{backgroundRadius, backgroundScale, backgroundColors, clear, add}) {
                Xp.api().hook(setter).intercept(chain -> {
                    // Failed installation can leave earlier hooks alive. The complete set
                    // is the admission boundary; partial observers must stay native no-ops.
                    if (!backgroundInstalled) return chain.proceed();
                    View view = (View) chain.getThisObject();
                    Integer role = targets.get(view);
                    boolean source = role == null && sourceBinding.isSource(view);
                    if (role != null && NativeOwnedMaterialBridge.claimed(view)) return chain.proceed();
                    // No scene queries, hierarchy discovery, reflection or argument copies
                    // for unrelated views. Native mode still samples the one known source.
                    if (!source && (role == null || !Main.sFollowNativeMaterial)) return chain.proceed();
                    if (!relaySceneNow() || !sourceStillBound(source ? view : sourceBinding.source()))
                        return chain.proceed();
                    if (role != null && paletteReplay.get() == view) return chain.proceed();
                    Object[] args = chain.getArgs().toArray();
                    GlassProfile.Background state = currentBackground(view);
                    // An inner View interceptor can claim this exact pair. Preserve raw
                    // inputs here; only the completed owned recipe may restore source values.
                    Object result = chain.proceed(args);
                    // Never sample on throw, and never infer committed values from args.
                    if (source && sourceStillBound(view)) {
                        if (!nativeBatches.defer(view)) captureBackground(view);
                    }
                    else if (role != null && (role & GLASS_TARGET) != 0 && state != null &&
                            (setter == clear || setter == add)) {
                        // clear + N add calls form one palette, not N+1 full replays.
                        if (palettes.mark(view)) schedulePaletteFrame();
                    } else if (role != null && setter == backgroundColors) {
                        palettes.completed(view);
                    }
                    if (role != null && (role & BLUR_TARGET) != 0) {
                        int changed = setter == backgroundRadius ? NativeMaterialDelta.RADIUS
                                : setter == backgroundScale ? NativeMaterialDelta.SCALE : 0;
                        if (changed != 0) synchronized (containerWrites) {
                            containerWrites.put(view, containerWrites.getOrDefault(view, 0) | changed);
                        }
                    }
                    return result;
                });
            }
            backgroundInstalled = true;
        } catch (Throwable error) { Xp.w(Main.TAG + "native background readback unavailable: " + error); }
    }

    private static ArrayList<Point> points(GlassProfile.Background state) {
        int[] colors = state.colors(), modes = state.modes();
        ArrayList<Point> result = new ArrayList<>(colors.length);
        for (int i = 0; i < colors.length; ++i) result.add(new Point(colors[i], modes[i]));
        return result;
    }

    private static void schedulePaletteFrame() {
        if (paletteFramePosted) return;
        paletteFramePosted = true;
        Runnable schedule = () -> Choreographer.getInstance().postFrameCallback(frameTime -> {
            paletteFramePosted = false;
            for (View view : palettes.drain()) restorePalette(view);
        });
        if (Looper.myLooper() == Looper.getMainLooper()) schedule.run(); else Main.main().post(schedule);
    }

    private static void restorePalette(View view) {
        Integer role = targets.get(view);
        if (role == null || (role & GLASS_TARGET) == 0 || NativeOwnedMaterialBridge.claimed(view)
                || !view.isAttachedToWindow() || !followsNow()) return;
        GlassProfile.Background state = currentBackground(view);
        // A clear-only/incomplete native frame must not erase the complete raw owned input.
        if (state == null || !state.hasPalette() || profile == null
                || !profile.matchesScene(night(view), Main.screenOnCached()) || !backgroundInstalled) return;
        View previous = paletteReplay.get();
        paletteReplay.set(view);
        try {
            // The bulk setter reaches RenderNode directly. Do not rebuild the same palette
            // in its own relay hook or treat this owned write as a new native sample.
            backgroundColors.invoke(view, points(state));
            palettes.completed(view);
            ++paletteRestores;
        } catch (Throwable error) { Xp.d(Main.TAG + "owned palette replay skipped: " + error); }
        finally {
            if (previous == null) paletteReplay.remove(); else paletteReplay.set(previous);
        }
    }

    private static void restoreContainerBackground(View element) {
        if (NativeOwnedMaterialBridge.claimed(element) || !followsNow() || !backgroundInstalled) return;
        View container = element.getParent() instanceof View ? (View) element.getParent() : null;
        Integer role = targets.get(container);
        GlassProfile.Background state = currentBackground(element);
        if (role == null || (role & BLUR_TARGET) == 0 || state == null || !container.isAttachedToWindow()) return;
        Integer changed = containerWrites.remove(container);
        if (changed == null) return;
        try {
            writeContainerBackground(container, state, changed);
        } catch (Throwable error) {
            synchronized (containerWrites) {
                containerWrites.put(container, containerWrites.getOrDefault(container, 0) | changed);
            }
            Xp.d(Main.TAG + "owned container replay skipped: " + error);
        }
    }

    private static void writeContainerBackground(View container, GlassProfile.Background state, int changes) throws Exception {
        if ((changes & (NativeMaterialDelta.RADIUS | NativeMaterialDelta.SCALE)) == 0) return;
        View previous = paletteReplay.get();
        paletteReplay.set(container);
        try {
            if ((changes & NativeMaterialDelta.RADIUS) != 0) backgroundRadius.invoke(container, state.radius);
            if ((changes & NativeMaterialDelta.SCALE) != 0) backgroundScale.invoke(container, state.scale);
        } finally {
            if (previous == null) paletteReplay.remove(); else paletteReplay.set(previous);
        }
    }

    private static GlassProfile.Background readBackground(View view) {
        return readBackground(view, night(view), Main.screenOnCached());
    }

    private static GlassProfile.Background readBackground(View view, int mode, boolean awake) {
        try {
            ArrayList<?> palette = (ArrayList<?>) readColors.invoke(view);
            int[] colors = new int[palette.size()], modes = new int[palette.size()];
            for (int i = 0; i < palette.size(); ++i) {
                Point point = (Point) palette.get(i);
                colors[i] = point.x; modes[i] = point.y;
            }
            return GlassProfile.Background.capture((Integer) readRadius.invoke(view),
                    (Float) readScale.invoke(view), colors, modes, mode, awake);
        } catch (Throwable error) { Xp.d(Main.TAG + "native background readback skipped: " + error); }
        return null;
    }

    private static void captureBackground(View view) {
        GlassProfile.Background next = readBackground(view);
        if (next != null) {
            committedBackground.put(view, next);
            if (!next.same(background)) { background = next; refreshTargets(); }
        }
    }

    /** No clear/add intermediate palette or previous-scene recipe reaches owned layers. */
    private static void captureNativeSnapshot(View source) {
        int mode = night(source);
        boolean awake = Main.screenOnCached();
        GlassProfile.Background nextBackground = readBackground(source, mode, awake);
        if (nextBackground == null) return;
        Object node;
        try { node = renderNode.get(source); } catch (Throwable ignored) { return; }
        // The node cache observes actual accepted writes even when this complete frame
        // leaves glass/radii unchanged. Screen power alone cannot invalidate those values.
        GlassProfile next = GlassProfile.capture(nodeCommits.currentGlass(node), mode, awake);
        int[] radii = nodeCommits.currentBlur(node);
        GlassProfile.Blur nextBlur = radii == null ? null : GlassProfile.Blur.capture(radii[0], radii[1], mode, awake);
        if (!sourceBinding.isSource(source) || !sourceStillBound(source)) return;
        if (next == null) committedGlass.remove(source); else committedGlass.put(source, next);
        if (nextBlur == null) committedBlur.remove(source); else committedBlur.put(source, nextBlur);
        committedBackground.put(source, nextBackground);
        if (next != null && !next.same(profile)) ++captures;
        profile = next; blur = nextBlur; background = nextBackground;
        // These writes share the native callback's current frame. Handler.post can run
        // after that frame's traversal, leaving the discs on the previous material.
        refreshTargetsNow();
    }

    private static GlassProfile.Background currentBackground(View view) {
        GlassProfile.Background state = background;
        return state != null && state.forScene(night(view), Main.screenOnCached()) ? state : null;
    }

    private static void installScope(Method setter) {
        // The verified RenderNode bridge is tiny and can be inlined into this OEM caller.
        // Deoptimize only these two known callers, not class loading or unrelated rendering.
        Xp.api().deoptimize(setter);
        Xp.api().hook(setter).intercept(chain -> {
            if (setter == glassSetter ? !glassInstalled : !blurInstalled) return chain.proceed();
            Object self = chain.getThisObject();
            if (!(self instanceof View)) return chain.proceed();
            View view = (View) self;
            Integer role = targets.get(view);
            boolean source = role == null && sourceBinding.isSource(view);
            if (role != null && NativeOwnedMaterialBridge.claimed(view)) return chain.proceed();
            if (!source && (role == null || !Main.sFollowNativeMaterial)) return chain.proceed();
            if (!relaySceneNow() || !sourceStillBound(source ? view : sourceBinding.source())) return chain.proceed();
            Object node;
            try { node = renderNode.get(view); } catch (Throwable ignored) { return chain.proceed(); }
            NativeMaterialCommitScope.Frame<View> frame = scopes.enter(view, node,
                    role == null ? SOURCE : role, night(view), Main.screenOnCached());
            try { return chain.proceed(); } finally { scopes.leave(frame); }
        });
    }

    private static int night(View view) {
        return view.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    private static boolean followsNow() {
        return Main.sFollowNativeMaterial && relaySceneNow() && sourceStillBound(sourceBinding.source());
    }

    static boolean hasCurrentPlayerMaterial() {
        View source = sourceBinding.source();
        return followsNow() && source != null && source.isShown() && profile != null;
    }

    /** Selection evidence is independent of the optional follow preference and parameter profile. */
    static boolean isCurrentNativePlayerSource(View source) {
        return sourceBinding.isSource(source) && sourceStillBound(source);
    }

    private static boolean relaySceneNow() {
        return Main.keyguardLocked() && Main.onKeyguardNow()
                && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway();
    }

    private static void refreshTargets() {
        if (!Main.sFollowNativeMaterial || refreshPosted) return;
        refreshPosted = true;
        Main.main().post(() -> {
            refreshPosted = false;
            refreshTargetsNow();
        });
    }

    private static void refreshTargetsNow() {
        if (!followsNow()) return;
        int changes = NativeMaterialDelta.between(profile, blur, background, appliedProfile, appliedBlur, appliedBackground);
        if (changes == 0) return;
        // No generation bump, card rebind, or whole-row refresh. Owned target commits
        // can never become sources, even when replay invokes the same vendor setters.
        ArrayList<Map.Entry<View, Integer>> snapshot;
        synchronized (targets) { snapshot = new ArrayList<>(targets.entrySet()); }
        for (Map.Entry<View, Integer> item : snapshot) {
            View view = item.getKey();
            if (view == null || !view.isAttachedToWindow() || !targets.containsKey(view)
                    || NativeOwnedMaterialBridge.claimed(view)) continue;
            try {
                int mode = night(view);
                boolean awake = Main.screenOnCached();
                GlassProfile current = profile;
                GlassProfile.Blur radius = blur;
                float[] values = (changes & NativeMaterialDelta.GLASS) == 0 || current == null ? null : current.forScene(mode, awake);
                int[] radii = (changes & NativeMaterialDelta.BLUR) == 0 || radius == null ? null : radius.forScene(mode, awake);
                if (glassInstalled && (item.getValue() & GLASS_TARGET) != 0 && values != null)
                    glassSetter.invoke(view, (Object) values);
                if (blurInstalled && (item.getValue() & BLUR_TARGET) != 0 && radii != null)
                    blurSetter.invoke(view, radii[0], radii[1]);
                GlassProfile.Background state = currentBackground(view);
                if (backgroundInstalled && state != null) {
                    if ((item.getValue() & GLASS_TARGET) != 0 && (changes & NativeMaterialDelta.PALETTE) != 0) restorePalette(view);
                    if ((item.getValue() & BLUR_TARGET) != 0) {
                        writeContainerBackground(view, state, changes);
                    }
                }
            } catch (Throwable error) { Xp.d(Main.TAG + "owned material relay skipped: " + error); }
        }
        appliedProfile = profile; appliedBlur = blur; appliedBackground = background;
        ++refreshes;
    }

    /** Called only by the known OEM effect binder, independently of the follow preference. */
    static boolean bindNativePlayerBackground(View view, View owner) {
        Object parent = view == null ? null : view.getParent();
        Object ownerParent = owner == null ? null : owner.getParent();
        // Observe only successful lower-level writes on a structurally verified OEM node,
        // even if its first material is installed in the shade before keyguard is visible.
        if (!nativePlayerBackgroundShape(view, owner)) return false;
        try { if (renderNode != null) nodeCommits.track(renderNode.get(view)); }
        catch (Throwable ignored) { return false; }
        if (!relaySceneNow()) { NativeMediaBackdrop.suspend(); return false; }
        boolean sameSource = sourceBinding.isSource(view);
        boolean bound = sourceBinding.bind(view, parent, owner, ownerParent,
                () -> nativePlayerBackground(view, owner));
        // Reparenting the same accepted background does not reset its RenderNode. Its
        // next identical setter can return false, so keep that source's committed state.
        if (!bound || !sameSource) clearSourceProfiles();
        if (bound) NativeMediaBackdrop.bind(view, owner);
        else NativeMediaBackdrop.clear();
        return bound;
    }

    private static boolean sourceStillBound(View view) {
        if (view == null) return false;
        View owner = sourceBinding.owner();
        if (owner != null && sourceBinding.isSource(view) && view.isAttachedToWindow() && owner.isAttachedToWindow()) {
            if (sourceBinding.matches(view, view.getParent(), owner, owner.getParent())) return true;
            // Only a previously bound source reaches this rare topology-change validation.
            return bindNativePlayerBackground(view, owner);
        }
        sourceBinding.clear();
        NativeMediaBackdrop.suspend();
        clearSourceProfiles();
        return false;
    }

    private static void clearSourceProfiles() {
        profile = null; blur = null; background = null;
        appliedProfile = null; appliedBlur = null; appliedBackground = null;
        committedGlass.clear(); committedBlur.clear(); committedBackground.clear();
    }

    private static boolean nativePlayerBackground(View view, View owner) {
        View container = Main.sContainer;
        return relaySceneNow() && container != null && view != null
                && view.getRootView() == container.getRootView() && nativePlayerBackgroundShape(view, owner);
    }

    private static boolean nativePlayerBackgroundShape(View view, View owner) {
        if (view == null || owner == null || view.getId() == View.NO_ID
                || view.getRootView() != owner.getRootView()) return false;
        String id;
        try { id = view.getResources().getResourceEntryName(view.getId()); }
        catch (Throwable ignored) { return false; }
        if (!"media_bg".equals(id)) return false;
        ArrayList<String> lineage = new ArrayList<>();
        boolean containsOwner = false;
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            if (NativeMaterialSource.ownedTarget(v)) return false;
            lineage.add(v.getClass().getName());
            if (v == owner) containsOwner = true;
        }
        return containsOwner && NativeMaterialSource.accepts(id, lineage, targets.containsKey(view));
    }

    /** Signature inputs only: never use these as a baseline for the independent native mode. */
    static float[] committedGlass(View source) {
        GlassProfile found = committedGlass.get(source);
        return found == null || source == null ? null : found.forScene(night(source), Main.screenOnCached());
    }
    static int[] committedBlur(View source) {
        GlassProfile.Blur found = committedBlur.get(source);
        return found == null || source == null ? null : found.forScene(night(source), Main.screenOnCached());
    }
    static GlassProfile.Background committedBackground(View source) {
        GlassProfile.Background found = committedBackground.get(source);
        return found != null && source != null && found.forScene(night(source), Main.screenOnCached()) ? found : null;
    }

    static String describe() {
        return "glassRelay enabled=" + Main.sFollowNativeMaterial + " installed=" + installed
                + " committed=" + glassInstalled + "/" + blurInstalled + "/" + backgroundInstalled
                + " profile=" + (profile != null) + " captures=" + captures
                + " blurProfile=" + (blur != null) + " backgroundProfile=" + (background != null)
                + " applications=" + applications + " blurApplications=" + blurApplications
                + " ownedRefreshes=" + refreshes + " paletteRestores=" + paletteRestores + " targets=" + targets.size()
                + " ownedProvider=[" + NativeOwnedMaterialBridge.describe() + "]"
                + " mediaBackdrop=[" + NativeMediaBackdrop.describe() + "]";
    }
}
