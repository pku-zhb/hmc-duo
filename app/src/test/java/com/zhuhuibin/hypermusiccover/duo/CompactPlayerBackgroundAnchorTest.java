package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompactPlayerBackgroundAnchorTest {
    public static final class Params {
        public float verticalBias = .5f;
        public int topToTop = 0, bottomToBottom = 0;
        public int topToBottom = -1, bottomToTop = -1;
    }

    @Test public void shrinkingShellKeepsTheContentOriginWhileTheSeekRowStillMeasures() {
        CompactPlayerBackgroundAnchor anchor = new CompactPlayerBackgroundAnchor();
        Params params = new Params();
        assertTrue(anchor.apply(params));
        // The real 386px normal card can keep measuring its fading seek row until the
        // native full-AOD callback makes that row GONE; its shell already reaches 312px.
        for (int shell = 384; shell >= 312; shell--) {
            assertFalse(anchor.apply(params));
            int backgroundTop = Math.round((386 - shell) * params.verticalBias);
            assertEquals(0, backgroundTop);
            assertEquals(31, 31 - backgroundTop);
        }
        assertTrue(anchor.restore(params));
        assertEquals(.5f, params.verticalBias, 0f);
    }

    @Test public void nativeConstraintReapplicationBecomesTheRestorationBaseline() {
        CompactPlayerBackgroundAnchor anchor = new CompactPlayerBackgroundAnchor();
        Params params = new Params();
        anchor.apply(params);
        params.verticalBias = .25f;
        assertTrue(anchor.apply(params));
        assertTrue(anchor.restore(params));
        assertEquals(.25f, params.verticalBias, 0f);
    }

    @Test public void nativeRewriteAfterOurLastApplyIsNeverRolledBack() {
        CompactPlayerBackgroundAnchor anchor = new CompactPlayerBackgroundAnchor();
        Params params = new Params();
        anchor.apply(params);
        params.verticalBias = .75f;
        assertFalse(anchor.restore(params));
        assertEquals(.75f, params.verticalBias, 0f);
    }

    @Test public void replacementLayoutParamsReleaseOnlyTheirOwnProperty() {
        CompactPlayerBackgroundAnchor anchor = new CompactPlayerBackgroundAnchor();
        Params first = new Params(), replacement = new Params();
        replacement.verticalBias = .2f;
        anchor.apply(first);
        assertTrue(anchor.apply(replacement));
        assertEquals(.5f, first.verticalBias, 0f);
        assertEquals(0f, replacement.verticalBias, 0f);
        assertTrue(anchor.restore(replacement));
        assertEquals(.2f, replacement.verticalBias, 0f);
    }

    @Test public void unknownOrSiblingAnchoredVariantsKeepTheirNativeGeometry() {
        CompactPlayerBackgroundAnchor anchor = new CompactPlayerBackgroundAnchor();
        assertFalse(anchor.apply(new Object()));
        Params params = new Params();
        params.bottomToBottom = 42;
        assertFalse(anchor.apply(params));
        assertEquals(.5f, params.verticalBias, 0f);
        params.bottomToBottom = 0;
        anchor.apply(params);
        params.topToBottom = 42;
        assertTrue(anchor.apply(params));
        assertEquals(.5f, params.verticalBias, 0f);
    }
}
