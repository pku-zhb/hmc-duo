package com.zhuhuibin.hypermusiccover.duo;

import java.lang.ref.WeakReference;

/** Selection evidence from one successful native effect, never an initial state-flow value. */
final class NativeOwnedMaterialSelection<S, W> {
    private WeakReference<S> source = new WeakReference<>(null);
    private WeakReference<W> window = new WeakReference<>(null);
    private long geometry;
    private Object configuration;
    private WeakReference<Object> assets = new WeakReference<>(null);
    private boolean aod;
    private NativeOwnedMaterialRecipe.Mode mode;

    void accepted(String effect, S source, W window, long geometry, Object configuration, Object assets, boolean aod) {
        this.mode = effect(effect);
        this.source = new WeakReference<>(source);
        this.window = new WeakReference<>(window);
        this.geometry = geometry;
        this.configuration = configuration;
        this.assets = new WeakReference<>(assets);
        this.aod = aod;
    }

    S source() { return source.get(); }

    void clear() { source.clear(); window.clear(); assets.clear(); mode = null; configuration = null; }

    NativeOwnedMaterialRecipe.Mode current(S source, W window, long geometry, Object configuration, Object assets,
                                            boolean aod, boolean currentSource) {
        return currentSource && source != null && window != null && this.source.get() == source
                && this.window.get() == window && this.geometry == geometry && this.aod == aod
                && configuration != null && configuration.equals(this.configuration) && assets != null && assets == this.assets.get()
                ? mode : null;
    }

    private static NativeOwnedMaterialRecipe.Mode effect(String name) {
        if (name == null) return null;
        switch (name) {
            case "MediaViewGlassEffect":
            case "MediaViewGlassOnKeyguardEffect":
            case "MediaViewGlassOnKeyguardLightWallPaperEffect":
            case "MediaViewGlassFullAodEffect": return NativeOwnedMaterialRecipe.Mode.GLASS;
            case "MediaViewBlurOnKeyguardEffect": return NativeOwnedMaterialRecipe.Mode.BLUR;
            case "MediaViewBlurEffect": return NativeOwnedMaterialRecipe.Mode.SHADE_BLUR;
            case "MediaViewNormalEffect": return NativeOwnedMaterialRecipe.Mode.NONE;
            default: return null;
        }
    }
}
