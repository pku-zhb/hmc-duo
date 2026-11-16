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
        public float[] parameters(String name) { reads.add(name); return glass(value); }
    }
    static float[] glass(float value) {
        float[] result = new float[42]; java.util.Arrays.fill(result, value); return result;
    }
    @Test public void noPlayerRecipeReadsCurrentNativeResourcesEveryTime() {
        Resources resources = new Resources();
        NativeOwnedMaterialRecipe first = NativeOwnedMaterialRecipe.current(resources, false, true, null, null);
        first.glass[0] = 900f; // Models a downstream provider changing its own input copy.
        resources.value = 2f;
        NativeOwnedMaterialRecipe refreshed = NativeOwnedMaterialRecipe.current(resources, false, true, null, null);
        assertArrayEquals(glass(2f), refreshed.glass, 0f);
        assertTrue(resources.reads.contains("notification_glass_params_on_keyguard"));
        assertTrue(resources.reads.contains("media_notification_element_blend_shade_color_1"));
    }
    @Test public void rawNativeAodFrameIsCopiedWithoutAnAwakeRecipeOrPlayerBaseline() {
        Resources resources = new Resources();
        int[] blend = {10, 20, 30, 40, 50, 60}; float[] glass = glass(.4f);
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
        assertTrue(resources.reads.contains("notification_element_blend_keyguard_mode_1"));
        assertTrue(resources.reads.contains("notification_element_blend_keyguard_mode_2"));
        assertTrue(resources.reads.contains("blur_mode_pure"));
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

    static final class Target implements NativeOwnedMaterialRecipe.Target {
        final ArrayList<String> writes = new ArrayList<>();
        boolean containerBlur;
        String background;
        int materialType;
        int[] palette;
        float[] glass;
        public void container(boolean blur) { writes.add("container"); containerBlur = blur; }
        public void background(String drawable) { writes.add("background"); background = drawable; }
        public void materialType(int type) { writes.add("type" + type); materialType = type; }
        public void palette(int[] blend) { writes.add("palette"); palette = blend; }
        public void glass(float[] params) { writes.add("glass"); glass = params; }
    }

    @Test public void coldAwakeGlassDoesNotInheritTheUninitializedAodMode() {
        // OEM factory initializes mIsGlassMode=false. No player or AOD transition has
        // populated it, but GlassOnKeyguard still unconditionally requires type 1 + glass.
        NativeOwnedMaterialRecipe recipe = NativeOwnedMaterialRecipe.current(new Resources(), false, false, null, null);
        Target target = new Target(); recipe.apply(true, target);
        assertTrue(recipe.glassMode);
        assertEquals(1, target.materialType);
        assertTrue(target.containerBlur);
        assertEquals("container", target.writes.get(0));
        assertEquals("glass", target.writes.get(target.writes.size() - 1));
        assertEquals(42, target.glass.length);
        assertEquals(6, target.palette.length);
    }

    @Test public void standaloneRawInputsExistWithOrWithoutPlayerAndFollow() {
        // Optional following chooses projection later; constructing a native recipe must
        // produce complete inputs for every combination, including a detached source.
        for (boolean player : new boolean[]{false, true}) for (boolean follow : new boolean[]{false, true}) {
            Target target = new Target();
            NativeOwnedMaterialRecipe.current(new Resources(), false, false, null, null).apply(true, target);
            assertEquals(42, target.glass.length);
            assertEquals(6, target.palette.length);
            assertEquals(1, target.materialType);
            assertEquals(follow && player, NativeOwnedMaterialRecipe.projectPlayer(false, follow, player));
        }
    }

    @Test public void disabledNativeBlurRestoresTheNativeDrawableAndClearsThePreviousPalette() {
        Target target = new Target();
        NativeOwnedMaterialRecipe recipe = NativeOwnedMaterialRecipe.current(new Resources(), false, true, null, null);
        recipe.apply(true, target);
        target.writes.clear(); target.glass = null;
        recipe.apply(false, target);
        assertFalse(target.containerBlur);
        assertEquals("notification_media_item_bg", target.background);
        assertEquals(0, target.materialType);
        assertEquals(0, target.palette.length);
        assertNull(target.glass);
        assertFalse(target.writes.contains("type1"));
    }

    @Test public void nativeAodBlurFrameKeepsItsCompletePaletteWithoutAwakeGlass() {
        int[] frame = {0xff111111, 101, 0xff222222, 102, 0xff333333, 103};
        Target target = new Target();
        NativeOwnedMaterialRecipe recipe = NativeOwnedMaterialRecipe.current(new Resources(), true, false, frame, null);
        recipe.apply(true, target);
        assertArrayEquals(frame, target.palette);
        assertEquals(0, target.materialType);
        assertTrue(target.containerBlur);
        assertNull(target.glass);
    }

    @Test public void explicitCurrentNativeBlurTypeIsHonoredWithoutUsingTheCachedAodFlag() {
        Target target = new Target();
        Resources resources = new Resources();
        NativeOwnedMaterialRecipe.current(resources, false, true, false, null, null).apply(true, target);
        assertEquals(0, target.materialType);
        assertEquals(6, target.palette.length);
        assertTrue(target.containerBlur);
        assertNull(target.glass);
        assertTrue(resources.reads.contains("media_notification_element_blend_keyguard_color_1"));
        assertFalse(resources.reads.contains("media_notification_element_blend_shade_color_1"));
    }

    @Test public void coldAodWithoutBlurStillHasTheNativeFullAodBackground() {
        Target target = new Target();
        NativeOwnedMaterialRecipe.current(new Resources(), true, true, null, null).apply(false, target);
        assertEquals("notification_media_fullaod_item_bg", target.background);
        assertEquals(0, target.palette.length);
        assertFalse(target.containerBlur);
    }

    @Test public void incompleteOrNonfiniteFrameGlassFallsBackToCurrentNativeEndpoint() {
        for (float[] frame : new float[][]{new float[0], new float[41], glass(Float.NaN), glass(Float.POSITIVE_INFINITY)}) {
            Resources resources = new Resources(); resources.value = .75f;
            NativeOwnedMaterialRecipe recipe = NativeOwnedMaterialRecipe.current(resources, true, true, null, frame);
            assertArrayEquals(glass(.75f), recipe.glass, 0f);
            assertTrue(resources.reads.contains("notification_glass_params_full_aod"));
        }
    }

    @Test public void reapplyingNeverReusesDownstreamProviderMutations() {
        NativeOwnedMaterialRecipe recipe = NativeOwnedMaterialRecipe.current(new Resources(), false, true, null, null);
        Target first = new Target(); recipe.apply(true, first);
        first.glass[0] = 900f; first.palette[0] = 0;
        Target second = new Target(); recipe.apply(true, second);
        assertEquals(1f, second.glass[0], 0f);
        assertEquals(recipe.blend[0], second.palette[0]);
    }
}
