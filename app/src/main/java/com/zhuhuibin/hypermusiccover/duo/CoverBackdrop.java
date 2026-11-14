package com.zhuhuibin.hypermusiccover.duo;

import android.content.Intent;
import android.os.SystemClock;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

/** Keep the artwork itself cached while the compact player shows the normal wallpaper. */
final class CoverBackdrop {
    private static volatile long decision;
    private static android.view.View dimLayer;
    private static float dimTarget = Float.NaN;
    private static boolean nativeWallpaper;
    static final int SURFACE_OFF = 0, SURFACE_LIT = 1, SURFACE_FULL_AOD = 2, SURFACE_OTHER_AOD = 3;
    private static volatile Snapshot published;
    private static final ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
    private static final Presented[] pending = new Presented[3];
    private static final View[] presenters = new View[3];
    private static boolean refreshing, commitPosted, commitFrames;
    private static boolean sleepConfigurationRead, sleepFullScreen;

    private static boolean fullScreenAod(boolean awake) {
        if (awake) { sleepConfigurationRead = false; return false; }
        if (!sleepConfigurationRead) {
            sleepFullScreen = Main.fullAodOn();
            sleepConfigurationRead = true;
        }
        return MiniPlayerScene.INSTANCE.getAodActive()
                ? MiniPlayerScene.INSTANCE.getFullScreenAodActive() : sleepFullScreen;
    }

    /** One immutable input for the page, clock and background consumers of this frame. */
    static final class Snapshot {
        final long epoch, geometryRevision, sourceIdentity;
        final int page, surface, generation, desiredRenderer, width, height, blur, dim, clockDestination;
        final View window;
        final boolean navigation, cover, playerChosen, lyricsRequested, notificationCompact;
        final boolean combinedRequested, pageClockRequested, foregroundReady, hidden, backgroundWanted, fullComposition;
        final boolean locked, goingAway, onKeyguard, awake, compact, lyrics, wantsLyrics;
        final float bias;

        private Snapshot(long epoch) {
            this.epoch = epoch;
            awake = Main.screenOnCached();
            locked = Main.keyguardLocked();
            goingAway = MiniPlayerScene.INSTANCE.getKeyguardGoingAway();
            onKeyguard = Main.onKeyguardNow();
            View layer = CoverPush.coverLayer();
            if (layer == null || !layer.isAttachedToWindow()) layer = Main.sContainer;
            window = layer != null && layer.isAttachedToWindow() ? layer.getRootView() : null;
            boolean visible = window != null && window.isShown() && locked && !goingAway;
            boolean fullAod = fullScreenAod(awake);
            surface = !visible ? SURFACE_OFF : awake && onKeyguard ? SURFACE_LIT
                    : !awake && fullAod ? SURFACE_FULL_AOD
                    : MiniPlayerScene.INSTANCE.getAodActive() ? SURFACE_OTHER_AOD : SURFACE_OFF;
            ScreenGeometry geometry = Main.screenGeometry();
            geometryRevision = geometry.revision;
            width = geometry.width;
            height = geometry.height;
            generation = CoverCardLayer.backdropGeneration();
            sourceIdentity = CoverCardLayer.backdropSourceIdentity();
            cover = Main.coverModeOn();
            navigation = ImmersiveHost.navigationClock();
            playerChosen = LockLyrics.playerChosen();
            lyricsRequested = LockLyrics.requestedLyricsPage();
            notificationCompact = LockLyrics.notificationCompact();
            combinedRequested = FoldMusicRuntime.combinedRequested();
            page = RailMusicPagePolicy.current(cover, lyricsRequested, combinedRequested);
            pageClockRequested = navigation || cover && !playerChosen && !notificationCompact;
            compact = LockLyrics.userWantsCompactArtwork() || notificationCompact;
            lyrics = LockLyrics.hasLyrics();
            wantsLyrics = lyricsRequested && !notificationCompact && !playerChosen
                    && LockLyrics.hasDisplayContent();
            foregroundReady = ArtworkBackdropState.foregroundReady(playerChosen, compact, wantsLyrics);
            hidden = Main.sHidePlayerBackground || Main.coverOnTapResting()
                    || NotificationArtworkPolicy.nativeBackdrop(notificationCompact) || !foregroundReady;
            fullComposition = CoverCardLayer.fullScene();
            backgroundWanted = cover && !hidden && !navigation && generation >= 0
                    && (surface == SURFACE_LIT || surface == SURFACE_FULL_AOD);
            desiredRenderer = !backgroundWanted ? ArtworkBackdropState.NATIVE
                    : surface == SURFACE_LIT ? ArtworkBackdropState.MESH : ArtworkBackdropState.STILL;
            blur = Main.sFullBlur;
            dim = Main.sWallpaperDim;
            bias = Main.sBias;
            clockDestination = Main.sArtworkClockDestination;
        }

        private boolean same(Snapshot s) {
            return s != null && surface == s.surface && page == s.page && window == s.window
                    && geometryRevision == s.geometryRevision && generation == s.generation
                    && sourceIdentity == s.sourceIdentity
                    && cover == s.cover && navigation == s.navigation && playerChosen == s.playerChosen
                    && lyricsRequested == s.lyricsRequested && notificationCompact == s.notificationCompact
                    && combinedRequested == s.combinedRequested && hidden == s.hidden
                    && foregroundReady == s.foregroundReady
                    && compact == s.compact && lyrics == s.lyrics && wantsLyrics == s.wantsLyrics
                    && fullComposition == s.fullComposition && locked == s.locked
                    && goingAway == s.goingAway && onKeyguard == s.onKeyguard && awake == s.awake
                    && clockDestination == s.clockDestination
                    && blur == s.blur && dim == s.dim && Float.compare(bias, s.bias) == 0;
        }
    }

    /** No clock pose or render readiness is read while capturing page intent. */
    static Snapshot snapshot() {
        Snapshot before = published;
        Snapshot next = new Snapshot(before == null ? 1L : before.epoch + 1L);
        if (next.same(before)) return before;
        published = next;
        return next;
    }

    private static final class Presented {
        final Snapshot scene;
        final View renderer;
        final int generation;
        final float alpha;
        Presented(Snapshot scene, View renderer, int generation, float alpha) {
            this.scene = scene;
            this.renderer = renderer;
            this.generation = generation;
            this.alpha = alpha;
        }
    }

    /** Called after draw recording. Commit only on the next frame, never from inside onDraw. */
    static void drawn(int renderer, Snapshot scene, View view, int generation, float alpha) {
        drawn(renderer, scene, view, generation, alpha, alpha);
    }

    static void drawn(int renderer, Snapshot scene, View view, int generation,
                      float compositionAlpha, float rendererAlpha) {
        if (generation != scene.generation || generation < 0) return;
        float alpha = ArtworkBackdropState.compositionEvidence(compositionAlpha, rendererAlpha);
        if (!Float.isFinite(alpha)) {
            // The outgoing pixels are still visible, but the new composition contributed
            // nothing. It cannot acknowledge or revoke the outgoing display list.
            pending[renderer] = null;
            return;
        }
        boolean invisible = alpha <= .001f;
        // An old composition may remain visible during a rebuild, but its draw cannot
        // attest that those pixels were composed for this new geometry/window/mode.
        if (!invisible && !CoverCardLayer.backdropCompositionMatches(scene)) {
            pending[renderer] = null;
            return;
        }
        pending[renderer] = new Presented(scene, view, generation, alpha);
        if (!(handoff.needsPresentation(renderer, alpha)
                || alpha <= .001f && handoff.owner() == renderer) || commitPosted) return;
        commitPosted = true;
        view.postOnAnimation(() -> {
            commitPosted = false;
            commitFrames = true;
            try { refreshVisibility(); }
            finally { commitFrames = false; }
        });
    }

    static void rendererHidden(int renderer) {
        pending[renderer] = null;
        presenters[renderer] = null;
        handoff.withdraw(renderer);
        if (!refreshing) refreshVisibility();
    }

    static boolean retains(int renderer, Snapshot scene) {
        return scene.backgroundWanted && handoff.matches(scene.epoch, scene.window,
                scene.sourceIdentity, scene.geometryRevision, scene.generation) && handoff.retains(renderer);
    }

    /** Sampling follows the final renderer submission, not requested cover or AOD flags. */
    static boolean samplingBackdropIn(View source) {
        Snapshot scene = snapshot();
        select(scene);
        return source != null && source.getRootView() == scene.window && scene.backgroundWanted
                && handoff.matches(scene.epoch, scene.window, scene.sourceIdentity,
                    scene.geometryRevision, scene.generation)
                && handoff.local();
    }

    static boolean backdropPresented() { return samplingBackdropIn(published == null ? null : published.window); }

    /** Small live snapshot for diagnosing dim visibility without dumping the view tree. */
    static String describe() {
        Snapshot scene = snapshot();
        android.view.View view = dimLayer;
        String state = "epoch=" + scene.epoch + " surface=" + scene.surface + " page=" + scene.page
                + " renderer=" + scene.desiredRenderer + " owner=" + handoff.owner()
                + " generation=" + scene.generation + " committed="
                + (handoff.local() ? handoff.generation() : -1) + " ownerEpoch=" + handoff.ownerEpoch()
                + " geometry=" + scene.geometryRevision + " ownerGeometry=" + handoff.ownerGeometry()
                + " source=" + scene.sourceIdentity + " ownerSource=" + handoff.ownerSource() + " window="
                + (scene.window == null ? "none" : Integer.toHexString(System.identityHashCode(scene.window)))
                + " hidden=" + scene.hidden + " foregroundReady=" + scene.foregroundReady
                + " lyricsReady=" + scene.wantsLyrics + " cover=" + scene.cover
                + " locked=" + Main.keyguardLocked() + " dim=" + Main.sWallpaperDim
                + " target=" + dimTarget + " compact=" + LockLyrics.wantsCompactArtwork()
                + " lyrics=" + LockLyrics.hasLyrics() + " window=" + LockLyrics.wantsWindow()
                + " composition=[" + CoverCardLayer.compositionDescription() + "]"
                + " localBackdrop=[" + VideoBackdropMesh.describe() + "]";
        if (view == null) return state + " layer=none";
        state += " alpha=" + view.getAlpha() + " shown=" + view.isShown()
                + " size=" + view.getWidth() + "x" + view.getHeight();
        for (Object current = view; current instanceof android.view.View;
                current = ((android.view.View) current).getParent()) {
            android.view.View v = (android.view.View) current;
            state += " [" + v.getClass().getSimpleName() + " a=" + v.getAlpha()
                    + " vis=" + v.getVisibility() + "]";
        }
        return state;
    }

    static boolean hidden() {
        // The artwork worker must use the same normalized decision without walking Views.
        Snapshot scene = Looper.myLooper() == Looper.getMainLooper() ? snapshot() : published;
        return scene == null || scene.hidden;
    }

    static boolean nativeForRailCard() {
        return FoldSettingsPolicy.nativeBackdrop(Main.sHidePlayerBackground);
    }

    static boolean nativeWallpaperScene() {
        return Main.coverModeOn() && hidden() && LockLyrics.compactWithoutLyrics()
                && (!LockLyrics.hasLyrics() || Main.coverOnTapResting());
    }

    static void sync(boolean force) {
        Snapshot scene = snapshot();
        boolean next = scene.hidden;
        long previous = decision;
        boolean changed = force || previous == 0L || ((previous & 1L) != 0L) != next;
        // Publish the background decision before invoking any page/clock consumer.
        if (changed) {
            long sequence = Math.max((previous >>> 1) + 1L, SystemClock.uptimeMillis());
            decision = sequence << 1 | (next ? 1L : 0L);
        }
        syncClock(scene);
        updateDim(scene);
        refreshVisibility();
        boolean wallpaper = nativeWallpaperScene();
        if (force || wallpaper != nativeWallpaper) {
            nativeWallpaper = wallpaper;
            StatusBarArtwork.refresh();
        }
        if (!changed) return;
        Intent out = CoverPush.wallpaperIntent("backdrop");
        putState(out);
        if (Main.sAppCtx != null) ProbeGuard.send(Main.sAppCtx, out);
        if (Main.sVideoWallpaper && Main.sCover != null)
            Main.sCover.setVisibility(CoverPush.videoCoverSuppressed()
                    ? android.view.View.INVISIBLE : android.view.View.VISIBLE);
    }

    /** A lyricless compact player leaves the native wallpaper and clock unobstructed. */
    private static void syncClock(Snapshot scene) {
        if (!scene.cover || !scene.locked || scene.goingAway) return;
        boolean nativeScene = nativeWallpaperScene();
        boolean held = LockHold.heldBy(LockHold.Owner.COVER);
        if (nativeScene && held) {
            LockHold.give(LockHold.Owner.COVER, scene.awake);
        } else if (!nativeScene && !held) {
            LockHold.take(LockHold.Owner.COVER, scene.awake, "wallpaper artwork");
        }
    }

    /** Lifecycle/scene transitions own visibility, not a condition inside the cached drawing. */
    static void refreshVisibility() {
        if (refreshing) return;
        refreshing = true;
        try {
            Snapshot scene = snapshot();
            select(scene);
            // Both renderers see this exact scene. Neither may restore the sampler halfway
            // through the other renderer's handoff.
            VideoBackdropMesh.refreshVisibility(scene);
            CoverCardLayer.refreshBackdrop(scene);
            refreshDimVisibility(scene);
            NativeMediaBackdrop.refresh();
        } finally { refreshing = false; }
    }

    private static void select(Snapshot scene) {
        handoff.select(scene.epoch, scene.desiredRenderer, scene.window,
                scene.sourceIdentity, scene.geometryRevision, scene.generation);
        for (int renderer = ArtworkBackdropState.MESH; renderer <= ArtworkBackdropState.STILL; renderer++) {
            View view = presenters[renderer];
            if (view != null && (!view.isAttachedToWindow() || !view.isShown()
                    || view.getRootView() != scene.window)) handoff.withdraw(renderer);
        }
        if (!commitFrames) return;
        for (int renderer = ArtworkBackdropState.MESH; renderer <= ArtworkBackdropState.STILL; renderer++) {
            Presented frame = pending[renderer];
            if (frame == null) continue;
            pending[renderer] = null;
            if (!frame.renderer.isAttachedToWindow() || !frame.renderer.isShown()
                    || frame.renderer.getRootView() != scene.window) continue;
            if (frame.alpha <= .001f) {
                handoff.invisible(frame.scene.epoch, renderer, frame.renderer.getRootView(), frame.scene.sourceIdentity);
                continue;
            }
            if (!CoverCardLayer.backdropCompositionMatches(scene)) continue;
            if (handoff.presented(frame.scene.epoch, renderer, frame.renderer.getRootView(),
                    frame.scene.sourceIdentity, frame.scene.geometryRevision, frame.generation, frame.alpha))
                presenters[renderer] = frame.renderer;
        }
    }

    private static void refreshDimVisibility(Snapshot scene) {
        if (dimLayer == null) return;
        boolean eligible = scene.surface == SURFACE_LIT || scene.surface == SURFACE_FULL_AOD;
        int visibility = eligible ? android.view.View.VISIBLE : android.view.View.INVISIBLE;
        boolean changed = dimLayer.getVisibility() != visibility;
        if (changed) dimLayer.setVisibility(visibility);
        if (!eligible) {
            dimLayer.animate().cancel();
            dimLayer.setAlpha(0f);
            dimTarget = Float.NaN;
        } else if (changed) updateDim(scene);
    }

    private static void updateDim(Snapshot scene) {
        android.view.ViewGroup layer = CoverPush.coverLayer();
        if (layer == null) return;
        android.view.ViewGroup parent = layer;
        android.view.View below = null;
        int rootId = layer.getResources().getIdentifier("keyguard_root_view", "id", "com.android.systemui");
        // AOD scales and clips the keyguard root. A sibling covers the original wallpaper bounds.
        // Video artwork lives inside SystemUI, so its dim must sit above that local backdrop.
        boolean localBackdrop = Main.sVideoWallpaper && !scene.hidden;
        for (Object current = layer; !localBackdrop && rootId != 0 && current instanceof android.view.View;
                current = ((android.view.View) current).getParent()) {
            android.view.View view = (android.view.View) current;
            if (view.getId() == rootId && view.getParent() instanceof android.view.ViewGroup) {
                parent = (android.view.ViewGroup) view.getParent();
                below = view;
                break;
            }
        }
        if (dimLayer == null || dimLayer.getParent() != parent) {
            if (dimLayer != null && dimLayer.getParent() instanceof android.view.ViewGroup)
                ((android.view.ViewGroup) dimLayer.getParent()).removeView(dimLayer);
            // Keep the display list independent of lock state. A conditional draw can cache
            // an empty background while unlocked and retain it when lyrics no longer blur.
            dimLayer = new android.view.View(parent.getContext());
            dimLayer.setTag("HMCWallpaperDim");
            dimLayer.setBackgroundColor(android.graphics.Color.BLACK);
            dimLayer.setClickable(false);
            dimLayer.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            dimLayer.setAlpha(0f);
            int index = below != null ? parent.indexOfChild(below) : parent.getChildCount();
            for (int i = 0; i < index; i++) {
                android.view.View child = parent.getChildAt(i);
                // Both local backgrounds (awake mesh and static AOD wash) belong below
                // this dim layer. Only the foreground square is an insertion boundary.
                if (child instanceof CoverCardLayer) {
                    index = i;
                    break;
                }
            }
            parent.addView(dimLayer, index, new android.view.ViewGroup.LayoutParams(-1, -1));
            dimTarget = Float.NaN;
        }
        boolean dimScene = scene.cover && (!scene.compact || scene.lyrics && scene.wantsLyrics);
        float alpha = dimScene && (scene.surface == SURFACE_LIT || scene.surface == SURFACE_FULL_AOD)
                ? scene.dim / 100f : 0f;
        if (alpha == dimTarget) return;
        dimTarget = alpha;
        dimLayer.animate().cancel();
        if (scene.awake) dimLayer.animate().alpha(alpha).setDuration(240L).start();
        else dimLayer.setAlpha(alpha);
    }

    static void putState(Intent out) {
        long state = decision;
        out.putExtra("backdrophidden", (state & 1L) != 0L);
        out.putExtra("backdropseq", state >>> 1);
    }
}
