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
    private static String reason = "not attached";
    private static boolean combinedChosen;
    private static final Matrix toScreen = new Matrix(), fromScreen = new Matrix();
    private static final RectF scratch = new RectF();
    private static final int[] at = new int[2];
    private static boolean failed;
    private static int lastWidth, lastHeight;
    private static boolean lastEnabled;
    private static long columnsRevision = -1;

    static boolean sideBySide() {
        return columns != null && columnsRevision == Main.screenGeometry().revision && combinedRequested()
                && (Main.screenOnCached() || MiniPlayerScene.INSTANCE.getFullScreenAodActive());
    }

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
        CompactPlayerRuntime.restore();
        root = null;
        reason = "not attached";
    }

    static void refresh() {
        failed = false;
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
            publish(null);
        }
        if (columns != null && columnsRevision != Main.screenGeometry().revision) publish(null);
        // AOD changes visibility, not the same-window combined page's layout generation.
        if (columns != null && combinedRequested() && MiniPlayerScene.INSTANCE.getFullScreenAodActive()
                && Main.keyguardLocked() && !Main.bouncerShown()
                && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) return;
        // Keep the outgoing artwork/lyric plan until the native header leaves the screen.
        if (columns != null && MiniPlayerRuntime.railMediaExitActive() && Main.keyguardLocked()
                && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) return;
        boolean wanted = !failed && combinedRequested() && Main.coverModeOn()
                && (Main.screenOnCached() || MiniPlayerScene.INSTANCE.getFullScreenAodActive())
                && (Main.onKeyguardNow() || MiniPlayerScene.INSTANCE.getFullScreenAodActive()) && Main.keyguardLocked()
                && !Main.bouncerShown() && !LockIslands.INSTANCE.spreadingNow();
        FoldMusicLayout.Columns next = null;
        if (wanted) {
            View card = CompactPlayerRuntime.currentCard();
            if (card == null) card = Main.findLockScreenView("mi_media_controls");
            if (card != null && card.isAttachedToWindow() && card.isShown()
                    && card.getHeight() > 0 && card.getParent() instanceof View) {
                v.getLocationOnScreen(at);
                float clock = ClockCollapse.contentBottomFor(v) - at[1];
                // Use the same stable cutout/navigation domain as the native stack owner.
                Insets insets = RailWindowInsets.read(v);
                // Keep the native shortcut row's space. A missing row gets a conservative margin.
                float shortcutTop = LockScreenRailRuntime.reflowing() && Main.sForceNotificationFloor ? Float.NaN
                        : v.getHeight() - insets.bottom - 80f * v.getResources().getDisplayMetrics().density;
                float contentWidth = LockScreenRailRuntime.reflowing()
                        ? LockScreenRailRuntime.contentWidth() : v.getWidth();
                // Read the entire native header's placement; never translate its child away
                // from the parent's clipping/touch frame or the live rows below it.
                if (map(card, v, new FoldMusicLayout.Box(0, 0, card.getWidth(), card.getHeight()), scratch)) {
                    FoldMusicLayout.Box nativePlayer = new FoldMusicLayout.Box(
                            scratch.left, scratch.top, scratch.right, scratch.bottom);
                    next = FoldMusicLayout.columns(contentWidth, v.getHeight(),
                            v.getResources().getDisplayMetrics().density,
                            v.getResources().getConfiguration().smallestScreenWidthDp,
                            insets.left, insets.top, LockScreenRailRuntime.reflowing() ? 0 : insets.right,
                            insets.bottom, clock, nativePlayer, shortcutTop);
                }
            }
            if (!failed) reason = next == null ? "waiting for measured clock/player room" : "combined artwork and lyrics";
        } else if (!failed) {
            reason = !Main.sFoldMusicLayout ? "disabled" : !wideCandidate()
                    ? "single-column window" : "outside wide lyric scene";
        }
        publish(next);
    }

    private static void publish(FoldMusicLayout.Columns next) {
        boolean modeChanged = (columns == null) != (next == null);
        columns = next;
        columnsRevision = next == null ? -1 : Main.screenGeometry().revision;
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
        FoldMusicLayout.Columns c = columns;
        if (!sideBySide() || c == null || !map(root, target, c.artworkRoom, scratch)) return null;
        return FoldMusicLayout.artwork(new FoldMusicLayout.Box(
                scratch.left, scratch.top, scratch.right, scratch.bottom), aspect,
                target.getResources().getDisplayMetrics().density);
    }

    static boolean lyrics(View target, RectF result) {
        FoldMusicLayout.Columns c = columns;
        return sideBySide() && c != null && map(root, target, c.lyrics, result);
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
