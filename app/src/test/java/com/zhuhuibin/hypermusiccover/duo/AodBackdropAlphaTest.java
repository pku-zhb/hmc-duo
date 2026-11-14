package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class AodBackdropAlphaTest {
    @Test public void opaqueOutgoingPreservesCoverageThroughWholeHandoff() {
        for (int i = 0; i <= 100; i++) {
            float p = i / 100f;
            float outgoing = AodBackdropAlpha.value(true, true, false, p, .2f);
            float incoming = AodBackdropAlpha.value(false, true, true, p, .2f);
            assertEquals(1f, incoming + outgoing * (1f - incoming), .00001f);
        }
    }
    @Test public void incomingWakeUsesNativeReverseEndpoint() {
        assertEquals(0f, AodBackdropAlpha.value(false, true, false, 1f, .2f), 0f);
        assertEquals(.75f, AodBackdropAlpha.value(false, true, false, .25f, .2f), 0f);
        assertEquals(1f, AodBackdropAlpha.value(false, true, false, 0f, .2f), 0f);
    }
    @Test public void retainedWakeRendererCannotFadeOnFollowingSleep() {
        float lastWake = AodBackdropAlpha.value(false, true, false, 0f, .2f);
        assertEquals(1f, lastWake, 0f);
        for (int i = 0; i <= 100; i++)
            assertEquals(lastWake, AodBackdropAlpha.value(true, true, false, i / 100f, lastWake), 0f);
    }
    @Test public void pageTapStillUsesItsOwnReveal() {
        assertEquals(.3f, AodBackdropAlpha.value(false, false, false, .7f, .3f), 0f);
    }
}
