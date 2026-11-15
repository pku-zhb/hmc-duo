package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewTreeObserver;

/** Opt-in wide-window artwork and lyrics, anchored to the native player header. */
final class FoldMusicRuntime {
    private FoldMusicRuntime() {}

    private static View root;
    private static ViewTreeObserver observer;
    private static ViewTreeObserver.OnPreDrawListener listener;
    private static volatile FoldMusicLayout.Columns columns;
    private static View motionOwner;
    private static FoldMusicLayout.Columns motionColumns;
    private static String reason = "not attached";
    private static boolean combinedChosen;
    private static final Matrix toScreen = new Matrix(), fromScreen = new Matrix();
    private static final RectF scratch = new RectF();
    private static final int[] at = new int[2];
    private static boolean failed;
    private static int lastWidth, lastHeight;
    private static boolean lastEnabled;
    private static long columnsRevision = -1;
    private static final LyricFrameReadiness lyricFrame = new LyricFrameReadiness();

    static boolean sideBySide() {
        return motionPlanCurrent() && combinedRequested()
                && unifiedSceneVisible();
    }

    private static boolean motionPlanCurrent() {
        return columns != null && motionColumns != null && root != null && motionOwner != null
                && motionOwner == NativeContentMotion.owner(root)
                && columnsRevision == Main.screenGeometry().revision;
    }

    /** Keep one column plan while the native doze callback crosses the screen-power edge. */
    private static boolean nativeAodScene() {
        return MiniPlayerScene.INSTANCE.getFullScreenAodActive()
                || ClockCollapse.followsNativeAod() && Main.keyguardLocked()
                && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway();
    }

    private static boolean unifiedSceneVisible() { return Main.screenOnCached() || nativeAodScene(); }

    static boolean wideCandidate() {
        View v = root;
        return Main.sFoldMusicLayout && v != null && FoldMusicLayout.wideWindow(
                v.getWidth(), v.getHeight(), v.getResources().getDisplayMetrics().density,
                v.getResources().getConfiguration().smallestScreenWidthDp);
    }

    static boolean supportsCombinedPage() { return wideCandidate(); }

    static boolean combinedRequested() {
        return combinedChosen && supportsCombinedPage() && LockLyrics.requestedLyricsPage();
    }

    static void selectPage(int page) {
        combinedChosen = page == RailMusicPagePolicy.ARTWORK_AND_LYRICS;
        refresh();
    }

    static boolean innerPortrait() {
        ScreenGeometry screen = Main.screenGeometry();
        return FoldArtworkLayout.innerPortrait(screen.width, screen.height, Main.density());
    }

    private static View currentNativeCard() {
        View header = Main.miniPlayerMediaHeader();
        if (header == null) return Main.findLockScreenView("mi_media_controls");
        int id = header.getResources().getIdentifier("mi_media_controls", "id", "com.android.systemui");
        return id == 0 ? null : header.findViewById(id);
    }

    private static final Matrix portraitToScreen = new Matrix(), portraitInverse = new Matrix();
    private static final RectF portraitBounds = new RectF();
    static float portraitPlayerWidth(View host) {
        View card = currentNativeCard();
        if (card == null || !card.isAttachedToWindow() || card.getWidth() <= 0) return Float.NaN;
        portraitToScreen.reset(); card.transformMatrixToGlobal(portraitToScreen);
        if (host != null) {
            portraitInverse.reset(); host.transformMatrixToGlobal(portraitInverse);
            if (!portraitInverse.invert(portraitInverse)) return Float.NaN;
            portraitToScreen.postConcat(portraitInverse);
        }
        portraitBounds.set(0, 0, card.getWidth(), card.getHeight());
        portraitToScreen.mapRect(portraitBounds);
        return portraitBounds.width();
    }

    static CoverCardStyle.Rect capPortraitArtwork(View host, CoverCardStyle.Rect rect) {
        return innerPortrait() ? FoldArtworkLayout.cap(rect, portraitPlayerWidth(host)) : rect;
    }

    static void attach(View anchor) {
        View next = anchor.getRootView();
        if (root == next && observer != null && observer.isAlive()) return;
        detach();
        root = next;
        failed = false;
        observer = root.getViewTreeObserver();
        listener = () -> {
            try {
                Main.captureScreenSize(root);
                update();
            } catch (Throwable error) {
                failed = true;
                lyricFrame.reset();
                reason = "layout unavailable: " + error.getClass().getSimpleName();
                publish(null);
                Xp.w(Main.TAG + "fold music: " + error);
            }
            return true;
        };
        observer.addOnPreDrawListener(listener);
    }

    static void detach() {
        if (observer != null && observer.isAlive() && listener != null) {
            observer.removeOnPreDrawListener(listener);
        }
        observer = null;
        listener = null;
        publish(null);
        lyricFrame.reset();
        CompactPlayerRuntime.restore();
        root = null;
        reason = "not attached";
    }

    static void refresh() {
        failed = false;
        lyricFrame.reset();
        if (root != null) root.invalidate();
    }

    private static void update() {
        View v = root;
        if (v == null) return;
        CompactPlayerRuntime.update(v);
        if (lastWidth != v.getWidth() || lastHeight != v.getHeight()
                || lastEnabled != Main.sFoldMusicLayout) {
            failed = false;
            lastWidth = v.getWidth();
            lastHeight = v.getHeight();
            lastEnabled = Main.sFoldMusicLayout;
            lyricFrame.reset();
            publish(null);
        }
        if (columns != null && !motionPlanCurrent()) publish(null);
        // The held-clock latch outlives wake. Retain the old native height only through
        // actual AOD/motion, then fit the common plan to the current measured header.
        if (columns != null && combinedRequested() && FoldMusicLayout.retainColumns(
                Main.screenOnCached(), MiniPlayerScene.INSTANCE.getFullScreenAodActive(),
                ClockCollapse.aodTransitioning() || LockScreenRailRuntime.holdGeometry())
                && Main.keyguardLocked() && !Main.bouncerShown()
                && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) return;
        // Keep the outgoing artwork/lyric plan until the native header leaves the screen.
        if (columns != null && MiniPlayerRuntime.railMediaExitActive() && Main.keyguardLocked()
                && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) return;
        boolean wanted = !failed && combinedRequested() && Main.coverModeOn()
                && unifiedSceneVisible()
                && (Main.onKeyguardNow() || nativeAodScene()) && Main.keyguardLocked()
                && !Main.bouncerShown() && !LockIslands.INSTANCE.spreadingNow();
        FoldMusicLayout.Columns next = null;
        if (wanted) {
            View card = CompactPlayerRuntime.currentCard();
            if (card == null) card = Main.findLockScreenView("mi_media_controls");
            if (card != null && card.isAttachedToWindow() && card.isShown()
                    && card.getHeight() > 0 && card.getParent() instanceof View) {
                FoldMusicLayout.Box area = LockScreenRailRuntime.logicalContent(scratch) != null
                        ? new FoldMusicLayout.Box(scratch.left, scratch.top, scratch.right, scratch.bottom)
                        : new FoldMusicLayout.Box(0, 0, v.getWidth(), v.getHeight());
                if (!LockScreenRailRuntime.logicalContentPending()) next = measureColumns(v, area, card);
                else lyricFrame.reset();
            } else lyricFrame.reset();
            if (!failed) reason = next == null ? "waiting for measured clock/player room" : "combined artwork and lyrics";
        } else if (!failed) {
            reason = !Main.sFoldMusicLayout ? "disabled" : !wideCandidate()
                    ? "single-column window" : "outside wide lyric scene";
        }
        publish(next);
    }

    /** The stack and both artwork/lyric renderers consume the same absolute column plan. */
    static FoldMusicLayout.Box combinedPlayer(View window, FoldMusicLayout.Box content) {
        if (!combinedRequested() || !Main.coverModeOn() || window == null || content == null) return null;
        FoldMusicLayout.Columns c = columns;
        // The native stack, artwork and lyrics consume one published plan. Independently
        // fitting the stack from its raw content frame loses the landscape leading inset.
        if (c != null && columnsRevision == Main.screenGeometry().revision) return c.player;
        Insets safe = RailWindowInsets.read(window);
        FoldMusicLayout.Box presented = RailContentFrame.presentation(content, window.getWidth(),
                window.getHeight(), window.getResources().getDisplayMetrics().density, safe.right, true);
        FoldMusicLayout.Columns measured = measureColumns(window, presented, currentNativeCard());
        if (measured != null) publish(measured);
        return measured == null ? null : measured.player;
    }

    private static FoldMusicLayout.Columns measureColumns(View window, FoldMusicLayout.Box content, View card) {
        lyricFrame.reset();
        if (card == null || !card.isAttachedToWindow() || card.getHeight() <= 0
                || !map(card, window, new FoldMusicLayout.Box(0, 0, card.getWidth(), card.getHeight()), scratch))
            return null;
        FoldMusicLayout.Box nativePlayer = new FoldMusicLayout.Box(
                scratch.left, scratch.top, scratch.right, scratch.bottom);
        window.getLocationOnScreen(at);
        float clock = ClockCollapse.contentBottomFor(window) - at[1];
        if (!Float.isFinite(clock)) return null;
        ContentMotionMeasurement measurement = NativeContentMotion.measurement(window,
                NativeContentMotion.owner(window));
        if (measurement == null) return null;
        Insets insets = RailWindowInsets.read(window);
        float density = window.getResources().getDisplayMetrics().density;
        float shortcutTop = LockScreenRailRuntime.active() && Main.sForceNotificationFloor ? Float.NaN
                : window.getHeight() - insets.bottom - 80f * density;
        // An actual measurement can legitimately find too little room for two columns.
        // Keep that decision separate from a temporarily missing clock/player frame.
        lyricFrame.measured(Main.screenGeometry().revision, window.getWidth(), window.getHeight());
        // Measured clock/player coordinates already contain the native owner's current
        // AOD affine. Remove only that common motion before natural layout fitting, so a
        // first plan or a rebuilt owner/window cannot apply translation/scale twice.
        return measurement.columns(content.right, window.getHeight(), density,
                window.getResources().getConfiguration().smallestScreenWidthDp,
                content.left, content.top, 0f, insets.bottom, clock, nativePlayer, shortcutTop);
    }

    private static void publish(FoldMusicLayout.Columns next) {
        boolean modeChanged = (columns == null) != (next == null);
        columns = next;
        columnsRevision = next == null ? -1 : Main.screenGeometry().revision;
        motionOwner = next == null ? null : NativeContentMotion.owner(root);
        motionColumns = next == null || motionOwner == null ? null : new FoldMusicLayout.Columns(
                NativeContentMotion.logicalPlan(root, motionOwner, next.artworkRoom),
                NativeContentMotion.logicalPlan(root, motionOwner, next.player),
                NativeContentMotion.logicalPlan(root, motionOwner, next.lyrics));
        if (modeChanged) Main.refreshCoverGeometry();
    }

    private static boolean map(View source, View target, FoldMusicLayout.Box box, RectF result) {
        if (source == null || target == null || !target.isAttachedToWindow()) return false;
        toScreen.reset();
        source.transformMatrixToGlobal(toScreen);
        fromScreen.reset();
        target.transformMatrixToGlobal(fromScreen);
        if (!fromScreen.invert(fromScreen)) return false;
        fromScreen.preConcat(toScreen);
        result.set(box.left, box.top, box.right, box.bottom);
        fromScreen.mapRect(result);
        return FoldMusicLayout.finite(result.left, result.top, result.right, result.bottom)
                && result.width() > 0f && result.height() > 0f;
    }

    static CoverCardStyle.Rect artwork(View target, float aspect) {
        FoldMusicLayout.Columns c = motionColumns;
        if (!sideBySide() || c == null) return null;
        // Fit once in the common logical room, then present the resulting pose through the
        // native content affine. Fitting an inverse-mapped physical room cancelled AOD motion.
        CoverCardStyle.Rect logical = FoldMusicLayout.artwork(c.artworkRoom, aspect,
                target.getResources().getDisplayMetrics().density);
        if (logical == null || !NativeContentMotion.map(motionOwner, target,
                new FoldMusicLayout.Box(logical.x, logical.y,
                    logical.x + logical.side, logical.y + logical.side), scratch)) return null;
        return new CoverCardStyle.Rect(scratch.left, scratch.top, scratch.width());
    }

    static boolean lyrics(View target, RectF result) {
        FoldMusicLayout.Columns c = motionColumns;
        return sideBySide() && c != null && NativeContentMotion.map(motionOwner, target, c.lyrics, result);
    }

    /** Text topology follows the shared plan even across a temporary display-power edge. */
    static View logicalLyrics(RectF result) {
        FoldMusicLayout.Columns c = motionColumns;
        if (!combinedRequested() || !motionPlanCurrent() || !root.isAttachedToWindow()) return null;
        result.set(c.lyrics.left, c.lyrics.top, c.lyrics.right, c.lyrics.bottom);
        return motionOwner;
    }

    static boolean lyricFramePending() {
        if (!combinedRequested()) return false;
        if (columns != null) return !motionPlanCurrent();
        return root == null || !root.isAttachedToWindow()
                || lyricFrame.pending(Main.screenGeometry().revision, root.getWidth(), root.getHeight());
    }

    static String describe() {
        FoldMusicLayout.Columns c = columns;
        return "fold enabled=" + Main.sFoldMusicLayout + " combined=" + combinedRequested() + " active=" + (c != null)
                + " reason=" + reason + " screen=" + Main.screenWidth() + "x" + Main.screenHeight()
                + " revision=" + Main.screenGeometry().revision + " columnsRevision=" + columnsRevision
                + " " + CompactPlayerRuntime.describe()
                + " requestedMode=" + Main.sCoverCardStyle.mode
                + " effectiveMode=" + Main.effectiveCoverStyle().mode
                + " " + LockScreenRailRuntime.describe()
                + " " + NativeGlassRelay.describe()
                + " wallpaperBackground=" + Main.sHidePlayerBackground
                + " wallpaperComposes=" + Main.sWpComposes + " videoWallpaper=" + Main.sVideoWallpaper
                + (c == null ? "" : " player=" + c.player.left + "," + c.player.top
                    + "," + c.player.width() + "x" + c.player.height()
                    + " lyrics=" + c.lyrics.left + "," + c.lyrics.top
                    + "," + c.lyrics.width() + "x" + c.lyrics.height());
    }
}
