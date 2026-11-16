package com.zhuhuibin.hypermusiccover.duo;

/** Fresh raw inputs, never a recorded player's setters or a provider-mutated baseline. */
final class NativeOwnedMaterialRecipe {
    interface Resources {
        int color(String name);
        int integer(String name);
        float[] parameters(String name);
    }
    final int[] blend;
    final float[] glass;
    final boolean glassMode;
    final boolean aod;
    private NativeOwnedMaterialRecipe(int[] blend, float[] glass, boolean glassMode, boolean aod) {
        this.blend = blend.clone();
        this.glass = glass == null ? null : glass.clone();
        this.glassMode = glassMode;
        this.aod = aod;
    }
    static NativeOwnedMaterialRecipe current(Resources resources, boolean aod, boolean glassMode,
                                             int[] frameBlend, float[] frameGlass) {
        return current(resources, aod, glassMode, null, frameBlend, frameGlass);
    }
    static NativeOwnedMaterialRecipe current(Resources resources, boolean aod, boolean glassMode,
                                             Boolean currentNativeGlass, int[] frameBlend, float[] frameGlass) {
        // mIsGlassMode belongs to the full-AOD notification style. The native awake
        // GlassOnKeyguard effect always writes type 1 and its glass parameters. An
        // explicitly current material type may select blur; a stale AOD flag may not.
        glassMode = aod ? glassMode : currentNativeGlass == null || currentNativeGlass;
        int[] blend;
        if (frameBlend != null && frameBlend.length == 6) blend = frameBlend;
        else {
            String prefix = aod ? "focus_notification_element_blend_keyguard_color_"
                    : glassMode ? "media_notification_element_blend_shade_color_"
                    : "media_notification_element_blend_keyguard_color_";
            blend = new int[]{resources.color(prefix + "1"), resources.integer(aod ? "notification_element_blend_keyguard_mode_1" : "media_notification_blend_mode_linear_light"),
                    resources.color(prefix + "2"), resources.integer(aod ? "notification_element_blend_keyguard_mode_2" : "media_notification_blend_mode_lab"),
                    resources.color(prefix + "3"), resources.integer(aod ? "blur_mode_pure" : "media_notification_blend_mode_pure")};
        }
        float[] glass = !glassMode ? null : validGlass(frameGlass) ? frameGlass
                : resources.parameters(aod ? "notification_glass_params_full_aod" : "notification_glass_params_on_keyguard");
        if (glassMode && !validGlass(glass)) throw new IllegalArgumentException("incomplete native glass parameters");
        return new NativeOwnedMaterialRecipe(blend, glass, glassMode, aod);
    }

    static boolean validGlass(float[] glass) {
        if (glass == null || glass.length != 42) return false;
        for (float value : glass) if (!Float.isFinite(value)) return false;
        return true;
    }

    interface Target {
        void container(boolean blurOpened);
        void background(String drawable);
        void materialType(int type);
        void palette(int[] blend);
        void glass(float[] parameters);
    }

    /** Complete native element/container setup, including the native non-blur path. */
    void apply(boolean blurOpened, Target target) {
        target.container(blurOpened);
        target.materialType(0);
        target.background(blurOpened ? "notification_heads_up_transparent_bg" : aod
                ? "notification_media_fullaod_item_bg" : "notification_media_item_bg");
        // Empty explicitly clears a previous glass palette when native blur is disabled.
        target.palette(blurOpened ? blend.clone() : new int[0]);
        if (blurOpened && glassMode) {
            target.materialType(1);
            target.glass(glass.clone());
        }
    }

    static boolean nativeAod(boolean running, float absoluteFraction) {
        return running || absoluteFraction > 0f;
    }
    static boolean projectPlayer(boolean claimed, boolean follow, boolean currentSource) {
        return !claimed && follow && currentSource;
    }
}
