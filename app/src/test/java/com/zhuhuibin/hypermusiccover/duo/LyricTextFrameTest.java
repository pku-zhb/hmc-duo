package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LyricTextFrameTest {
    @Test public void aodWakeTransformsPositionWithoutChangingParagraphWidth() {
        LyricStyle style = LyricStyle.DEFAULT;
        int expectedInner = 954 - Math.round(2f * style.sidePx(954, 1168, 2.75f, 68.75f));
        for (int step = 0; step <= 50; step++) {
            float scale = .95f + step * .001f;
            // The shared window frame is 0..954, while the lyric view scales about x=584.
            float mappedCenter = 584f + (477f - 584f) / scale;
            LyricTextFrame frame = LyricTextFrame.at(954f, mappedCenter);
            assertEquals(954, frame.width);
            assertEquals(expectedInner, frame.width - Math.round(2f
                    * style.sidePx(frame.width, 1168, 2.75f, 68.75f)));
            // Mapping back carries the frame's center, rather than drifting with its left edge.
            assertEquals(477f, 584f + (frame.left + frame.width / 2f - 584f) * scale, .001f);
        }
    }

    @Test public void rotationAndBothPageUseTheNewOwnersActualLogicalWidth() {
        assertEquals(954, LyricTextFrame.at(954, 477).width);
        assertEquals(680, LyricTextFrame.at(680, 1300).width);
        assertEquals(1168, LyricTextFrame.at(1168, 584).width);
        // A center-only movement never masquerades as a paragraph-width change.
        assertEquals(680, LyricTextFrame.at(680, 1200).width);
    }

    @Test public void hdrWindowAndKeyguardChildUseTheSameComposedTransform() {
        for (boolean hdr : new boolean[]{false, true}) {
            // HDR owns the zoom on the lyric view; the child inherits it from keyguard.
            float parentScale = hdr ? 1f : .95f;
            float viewScale = hdr ? .95f : 1f;
            float composedScale = parentScale * viewScale;
            float center = 584f + (477f - 584f) / composedScale;
            LyricTextFrame frame = LyricTextFrame.at(954, center);
            assertEquals(954, frame.width);
            assertEquals(477f, 584f + (frame.left + frame.width / 2f - 584f) * composedScale, .001f);
        }
    }

    @Test public void unavailableGeometryCannotPublishANarrowOrNonFiniteLayout() {
        assertNull(LyricTextFrame.at(0, 100));
        assertNull(LyricTextFrame.at(Float.NaN, 100));
        assertNull(LyricTextFrame.at(954, Float.POSITIVE_INFINITY));
    }
}
