package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeOwnedMaterialSelectionTest {
    @Test public void constructorDefaultsCannotSelectNativeFlatBeforeAnAcceptedEffect() {
        NativeOwnedMaterialSelection<Object, Object> selection = new NativeOwnedMaterialSelection<>();
        assertNull(selection.current(new Object(), new Object(), 1, "day", new Object(), false, true));
        assertNull(selection.source());
    }

    @Test public void acceptedNormalIsFlatOnlyWhileItsExactSourceAndSceneRemainCurrent() {
        NativeOwnedMaterialSelection<Object, Object> selection = new NativeOwnedMaterialSelection<>();
        Object source = new Object(), window = new Object(), assets = new Object();
        selection.accepted("MediaViewNormalEffect", source, window, 1, "day", assets, false);
        assertEquals(NativeOwnedMaterialRecipe.Mode.NONE, selection.current(source, window, 1, "day", assets, false, true));
        assertNull(selection.current(source, window, 1, "day", assets, false, false));
        assertNull(selection.current(new Object(), window, 1, "day", assets, false, true));
        assertNull(selection.current(source, new Object(), 1, "day", assets, false, true));
        assertNull(selection.current(source, window, 2, "day", assets, false, true));
        assertNull(selection.current(source, window, 1, "another theme in same night mode", assets, false, true));
        assertNull(selection.current(source, window, 1, "day", new Object(), false, true));
        assertNull(selection.current(source, window, 1, "day", assets, true, true));
    }

    @Test public void aNativeAttemptOutsideTheAcceptedDomainCannotReviveThePreviousFlatSelection() {
        NativeOwnedMaterialSelection<Object, Object> selection = new NativeOwnedMaterialSelection<>();
        Object source = new Object(), window = new Object(), assets = new Object();
        selection.accepted("MediaViewNormalEffect", source, window, 1, "day", assets, false);
        selection.clear(); // A shade effect attempted to redress this same source while unlocked.
        assertNull(selection.current(source, window, 1, "day", assets, false, true));
        assertNull(selection.source());
    }

    @Test public void eachAcceptedNativeEffectKeepsItsOwnSelectionAndUnknownEffectsStayUnknown() {
        Object source = new Object(), window = new Object(), assets = new Object();
        NativeOwnedMaterialSelection<Object, Object> selection = new NativeOwnedMaterialSelection<>();
        String[] effects = {"MediaViewGlassOnKeyguardEffect", "MediaViewGlassOnKeyguardLightWallPaperEffect",
                "MediaViewGlassFullAodEffect", "MediaViewBlurOnKeyguardEffect", "MediaViewBlurEffect", "UnknownEffect"};
        NativeOwnedMaterialRecipe.Mode[] modes = {NativeOwnedMaterialRecipe.Mode.GLASS,
                NativeOwnedMaterialRecipe.Mode.GLASS, NativeOwnedMaterialRecipe.Mode.GLASS,
                NativeOwnedMaterialRecipe.Mode.BLUR, NativeOwnedMaterialRecipe.Mode.SHADE_BLUR, null};
        for (int i = 0; i < effects.length; ++i) {
            selection.accepted(effects[i], source, window, 1, "day", assets, false);
            assertEquals(modes[i], selection.current(source, window, 1, "day", assets, false, true));
        }
    }
}
