package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Insets;
import android.graphics.Rect;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import java.util.Collections;
import java.util.List;

/** One safe window for the content, shortcuts and display-only status cluster. */
final class RailWindowInsets {
    static Insets read(View root) {
        WindowInsets wi = root.getRootWindowInsets();
        if (wi == null) return Insets.NONE;
        Insets bars = wi.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
        DisplayCutout cutout = wi.getDisplayCutout();
        List<Rect> bounds = cutout == null ? Collections.emptyList() : cutout.getBoundingRects();
        FoldMusicLayout.Box[] holes = new FoldMusicLayout.Box[bounds.size()];
        for (int i = 0; i < holes.length; i++) {
            Rect r = bounds.get(i);
            holes[i] = new FoldMusicLayout.Box(r.left, r.top, r.right, r.bottom);
        }
        FoldMusicLayout.Box safe = RailSafeArea.resolve(root.getWidth(), root.getHeight(),
                bars.left, bars.top, bars.right, bars.bottom, holes);
        return Insets.of(Math.round(safe.left), Math.round(safe.top),
                Math.round(root.getWidth() - safe.right), Math.round(root.getHeight() - safe.bottom));
    }
    private RailWindowInsets() {}
}
