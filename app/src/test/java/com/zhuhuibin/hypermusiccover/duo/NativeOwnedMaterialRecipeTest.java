package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class NativeOwnedMaterialRecipeTest {
    static final class Resources implements NativeOwnedMaterialRecipe.Resources {
        final ArrayList<String> reads = new ArrayList<>();
        float value = 1f;
        public int color(String name) { reads.add(name); return name.hashCode(); }
        public int integer(String name) { reads.add(name); return name.hashCode(); }
        public float[] parameters(String name) { reads.add(name); return new float[]{value}; }
    }
    @Test public void noPlayerRecipeReadsCurrentNativeResourcesEveryTime() {
        Resources resources = new Resources();
        NativeOwnedMaterialRecipe first = NativeOwnedMaterialRecipe.current(resources, false, true, null, null);
        first.glass[0] = 900f; // Models a downstream provider changing its own input copy.
        resources.value = 2f;
        NativeOwnedMaterialRecipe refreshed = NativeOwnedMaterialRecipe.current(resources, false, true, null, null);
        assertArrayEquals(new float[]{2f}, refreshed.glass, 0f);
        assertTrue(resources.reads.contains("notification_glass_params_on_keyguard"));
        assertTrue(resources.reads.contains("media_notification_element_blend_shade_color_1"));
    }
    @Test public void rawNativeAodFrameIsCopiedWithoutAnAwakeRecipeOrPlayerBaseline() {
        Resources resources = new Resources();
        int[] blend = {10, 20, 30, 40, 50, 60}; float[] glass = {.4f, .8f};
        NativeOwnedMaterialRecipe recipe = NativeOwnedMaterialRecipe.current(resources, true, true, blend, glass);
        blend[0] = 99; glass[0] = 99;
        assertEquals(10, recipe.blend[0]); assertEquals(.4f, recipe.glass[0], 0f);
        assertTrue(resources.reads.isEmpty());
    }
    @Test public void explicitFullAodEndpointUsesCurrentAodResourcesAndNativeGlassMode() {
        Resources resources = new Resources();
        NativeOwnedMaterialRecipe recipe = NativeOwnedMaterialRecipe.current(resources, true, true, null, null);
        assertTrue(resources.reads.contains("notification_glass_params_full_aod"));
        assertTrue(resources.reads.contains("focus_notification_element_blend_keyguard_color_1"));
        assertFalse(resources.reads.contains("notification_glass_params_on_keyguard"));
        resources.reads.clear();
        assertNull(NativeOwnedMaterialRecipe.current(resources, true, false, null, new float[]{99f}).glass);
        assertFalse(resources.reads.contains("notification_glass_params_full_aod"));
    }
    @Test public void localCompletionFractionZeroDoesNotOverrideNativeTransitionAuthority() {
        assertTrue(NativeOwnedMaterialRecipe.nativeAod(true, 0f));
        assertTrue(NativeOwnedMaterialRecipe.nativeAod(false, 1f));
        assertFalse(NativeOwnedMaterialRecipe.nativeAod(false, 0f));
        AodTransitionProgress progress = new AodTransitionProgress();
        Object window = new Object();
        progress.scene(window, 1, true);
        AodTransitionProgress.Snapshot cold = progress.snapshot(window, 1, true);
        assertTrue(NativeOwnedMaterialRecipe.nativeAod(cold.running, cold.fraction));
        progress.begin(window, 1, false); progress.complete(window, 1, false);
        AodTransitionProgress.Snapshot completed = progress.snapshot(window, 1, true);
        assertFalse(NativeOwnedMaterialRecipe.nativeAod(completed.running, completed.fraction));
    }
    @Test public void playerProjectionCannotBackfeedClaimedOrAbsentSourceSurfaces() {
        assertTrue(NativeOwnedMaterialRecipe.projectPlayer(false, true, true));
        assertFalse(NativeOwnedMaterialRecipe.projectPlayer(true, true, true));
        assertFalse(NativeOwnedMaterialRecipe.projectPlayer(false, false, true));
        assertFalse(NativeOwnedMaterialRecipe.projectPlayer(false, true, false));
    }
}
