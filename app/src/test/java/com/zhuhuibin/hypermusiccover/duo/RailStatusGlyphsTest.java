package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailStatusGlyphsTest {
    // Golden ink widths evaluated from the MiDuo 469 h renderer's original outlines,
    // advances and kerning. These are not system-font advance widths.
    @Test public void regularGlyphsKeepReferenceInkWidths() {
        assertEquals(114.69130f, RailStatusGlyphs.measure("23:59", 29.986198f, false), .0001f);
        assertEquals(83.44886f, RailStatusGlyphs.measure("7:07", 29.986198f, false), .0001f);
        assertEquals(52.09714f, RailStatusGlyphs.measure("100", 21.869999f, false), .0001f);
        assertEquals(36.15597f, RailStatusGlyphs.measure("63", 21.869999f, false), .0001f);
        assertEquals(38.43053f, RailStatusGlyphs.measure("5G", 21.869999f, false), .0001f);
    }

    @Test public void alternateStrokeWeightRetainsItsOwnOpticalSpacing() {
        assertEquals(114.92131f, RailStatusGlyphs.measure("23:59", 29.986198f, true), .0001f);
        assertEquals(83.434685f, RailStatusGlyphs.measure("7:07", 29.986198f, true), .0001f);
        assertEquals(51.41749f, RailStatusGlyphs.measure("100", 21.869999f, true), .0001f);
    }

    @Test public void unknownValuesDoNotIndexIntoTheGlyphTable() {
        assertFalse(RailStatusGlyphs.supports(""));
        assertFalse(RailStatusGlyphs.supports(null));
        assertFalse(RailStatusGlyphs.supports("—"));
        assertFalse(RailStatusGlyphs.supports("5G+"));
        assertEquals(0, RailStatusGlyphs.measure("—", 21.87f, false), 0);
        assertEquals(0, RailStatusGlyphs.measure("100", Float.NaN, false), 0);
    }

    @Test public void radioFamiliesUseTheReferenceVectorAlphabet() {
        assertEquals("5G", RailStatusValues.networkLabel(5, 13));
        assertEquals("5G", RailStatusValues.networkLabel(4, 13));
        assertEquals("4G", RailStatusValues.networkLabel(1, 13));
        assertEquals("3G", RailStatusValues.networkLabel(0, 15));
        assertEquals("2G", RailStatusValues.networkLabel(0, 2));
        assertEquals("", RailStatusValues.networkLabel(0, 18));
        for (int override = 0; override <= 5; override++) {
            for (int type = 0; type <= 20; type++) {
                String label = RailStatusValues.networkLabel(override, type);
                assertTrue(label.isEmpty() || RailStatusGlyphs.supports(label));
            }
        }
    }
}
