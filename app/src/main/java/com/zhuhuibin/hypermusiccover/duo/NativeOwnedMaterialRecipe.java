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
    private NativeOwnedMaterialRecipe(int[] blend, float[] glass, boolean glassMode) {
        this.blend = blend.clone();
        this.glass = glass == null ? null : glass.clone();
        this.glassMode = glassMode;
    }
    static NativeOwnedMaterialRecipe current(Resources resources, boolean aod, boolean glassMode,
                                             int[] frameBlend, float[] frameGlass) {
        int[] blend;
        if (frameBlend != null && frameBlend.length == 6) blend = frameBlend;
        else {
            String prefix = aod ? "focus_notification_element_blend_keyguard_color_"
                    : "media_notification_element_blend_shade_color_";
            blend = new int[]{resources.color(prefix + "1"), resources.integer("media_notification_blend_mode_linear_light"),
                    resources.color(prefix + "2"), resources.integer("media_notification_blend_mode_lab"),
                    resources.color(prefix + "3"), resources.integer("media_notification_blend_mode_pure")};
        }
        float[] glass = !glassMode ? null : frameGlass != null ? frameGlass
                : resources.parameters(aod ? "notification_glass_params_full_aod" : "notification_glass_params_on_keyguard");
        return new NativeOwnedMaterialRecipe(blend, glass, glassMode);
    }

    static boolean nativeAod(boolean running, float absoluteFraction) {
        return running || absoluteFraction > 0f;
    }
    static boolean projectPlayer(boolean claimed, boolean follow, boolean currentSource) {
        return !claimed && follow && currentSource;
    }
}
