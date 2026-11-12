package com.zhuhuibin.hypermusiccover.duo;

import android.view.View;
import android.view.ViewGroup;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded, read-only layout data: no screenshots, view text, credentials, or report files. */
final class LockScreenLayoutDiagnostics {
    private LockScreenLayoutDiagnostics() {}

    static String describe() {
        JSONObject out = new JSONObject();
        try {
            ScreenGeometry g = Main.screenGeometry();
            out.put("cachedWindow", new JSONArray(new int[]{g.width, g.height}));
            out.put("revision", g.revision);
            out.put("runtimeVersion", BuildConfig.VERSION_CODE);
            out.put("uptimeMillis", android.os.SystemClock.uptimeMillis());
            out.put("linkedCardAnimations", !MiniPlayerRuntime.detachedRail());
            out.put("screenOn", Main.screenOnCached());
            out.put("aod", MiniPlayerScene.INSTANCE.getAodActive());
            out.put("fullScreenAod", MiniPlayerScene.INSTANCE.getFullScreenAodActive());
            out.put("customAod", MiniPlayerScene.INSTANCE.getCustomAodActive());
            out.put("keyguardVisible", Main.onKeyguardNow());
            out.put("bouncer", Main.bouncerShown());
            out.put("locked", Main.keyguardLocked());
            out.put("keyguardGoingAway", MiniPlayerScene.INSTANCE.getKeyguardGoingAway());
            out.put("unlockTrace", MiniPlayerScene.INSTANCE.exitTrace());
            out.put("wallpaperHandoff", WallpaperHandoffDiagnostics.describe());
            out.put("ownedSurfaces", MiniPlayerRuntime.surfaceState());
            out.put("cardSwipe", Main.cardSwipeState());
            out.put("railDismiss", MiniPlayerRuntime.railDismissState());
            out.put("backdropState", CoverBackdrop.describe());
            out.put("chargingHint", LockScreenChargingHint.INSTANCE.describe());
            out.put("foldLayout", FoldMusicRuntime.describe());
            out.put("cover", Main.coverModeOn());
            out.put("requestedMode", Main.sCoverCardStyle.mode);
            out.put("effectiveMode", Main.effectiveCoverStyle().mode);
            out.put("wallpaperBackground", Main.sHidePlayerBackground);
            out.put("nativeWallpaperForSquare", CoverBackdrop.nativeForRailCard());
            View clock = Main.sContainer;
            View root = clock == null ? null : clock.getRootView();
            out.put("root", view(root));
            out.put("clock", view(clock));
            JSONArray clockTargets = new JSONArray();
            for (View clockRoot : Main.clockRoots()) clockTargets.put(view(Main.clockTarget(clockRoot)));
            out.put("clockTargets", clockTargets);
            android.graphics.RectF ink = Main.glyphBox();
            if (ink != null) out.put("clockInk", new JSONArray(new float[]{ink.left, ink.top, ink.right, ink.bottom}));
            out.put("date", view(Main.visibleDate()));
            View card = Main.findLockScreenView("mi_media_controls");
            out.put("player", view(card));
            out.put("playerMaterial", view(named(card, "media_bg")));
            out.put("notifications", view(named(root, "notification_stack_scroller")));
            View pill = MiniPlayerRuntime.pillForDiagnostics();
            out.put("pill", view(pill));
            if (pill instanceof MiniPlayerView) out.put("pillFrame", ((MiniPlayerView) pill).frameState());
            View[] shortcuts = MiniPlayerRuntime.railShortcutViews();
            if (shortcuts != null && shortcuts.length == 2) {
                out.put("leftShortcut", view(shortcuts[0]));
                out.put("rightShortcut", view(shortcuts[1]));
            }
            out.put("rail", LockScreenRailRuntime.describe());
            out.put("railEvents", LockScreenRailRuntime.events());
            out.put("shortcutGesture", ShortcutGestureRuntime.describe());
            out.put("shortcutLayout", RailShortcutRuntime.describe());
            out.put("statusCluster", RailStatusRuntime.describe());
            out.put("railFooter", NumState.INSTANCE.railFooterState());
            out.put("glass", NativeGlassRelay.describe());
        } catch (Throwable failure) {
            try { out.put("unavailable", failure.getClass().getSimpleName()); }
            catch (Exception ignored) {}
        }
        return out.toString();
    }

    private static View named(View root, String name) {
        if (root == null) return null;
        int id = root.getResources().getIdentifier(name, "id", "com.android.systemui");
        return id == 0 ? null : root.findViewById(id);
    }

    private static Object view(View v) throws Exception {
        if (v == null) return JSONObject.NULL;
        JSONObject out = new JSONObject();
        out.put("class", v.getClass().getName());
        try { out.put("id", v.getResources().getResourceEntryName(v.getId())); }
        catch (Exception ignored) {}
        int[] at = new int[2]; v.getLocationOnScreen(at);
        out.put("origin", new JSONArray(at));
        out.put("size", new JSONArray(new int[]{v.getWidth(), v.getHeight()}));
        out.put("measuredSize", new JSONArray(new int[]{v.getMeasuredWidth(), v.getMeasuredHeight()}));
        out.put("layoutOrigin", new JSONArray(new int[]{v.getLeft(), v.getTop()}));
        out.put("visibility", v.getVisibility());
        out.put("shown", v.isShown());
        out.put("attached", v.isAttachedToWindow());
        out.put("layoutRequested", v.isLayoutRequested());
        out.put("alpha", v.getAlpha());
        out.put("scaleX", v.getScaleX()); out.put("scaleY", v.getScaleY());
        out.put("translationX", v.getTranslationX()); out.put("translationY", v.getTranslationY());
        out.put("pivotX", v.getPivotX()); out.put("pivotY", v.getPivotY());
        out.put("clipToOutline", v.getClipToOutline());
        android.graphics.Rect clip = v.getClipBounds();
        if (clip != null) out.put("clipBounds", new JSONArray(new int[]{clip.left, clip.top, clip.right, clip.bottom}));
        if (v instanceof ViewGroup) {
            out.put("clipsChildren", ((ViewGroup) v).getClipChildren());
            out.put("clipsPadding", ((ViewGroup) v).getClipToPadding());
        }
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null) out.put("layoutSize", new JSONArray(new int[]{lp.width, lp.height}));
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) lp;
            out.put("horizontalMargins", new JSONArray(new int[]{margins.leftMargin, margins.rightMargin,
                    margins.getMarginStart(), margins.getMarginEnd()}));
        }
        JSONArray parents = new JSONArray();
        View parent = v.getParent() instanceof View ? (View) v.getParent() : null;
        for (int i = 0; i < 8 && parent != null; i++) {
            JSONObject item = new JSONObject();
            item.put("class", parent.getClass().getName());
            try { item.put("id", parent.getResources().getResourceEntryName(parent.getId())); }
            catch (Exception ignored) {}
            item.put("visibility", parent.getVisibility());
            item.put("alpha", parent.getAlpha());
            item.put("transitionAlpha", parent.getTransitionAlpha());
            item.put("width", parent.getWidth()); item.put("height", parent.getHeight());
            if (parent instanceof ViewGroup) item.put("clipsChildren", ((ViewGroup) parent).getClipChildren());
            parents.put(item);
            parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
        }
        out.put("parents", parents);
        return out;
    }
}
