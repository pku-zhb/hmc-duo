package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class NativeMaterialCommitScopeTest {
    @Test public void downstreamArgumentReplacementIsCapturedAtTheCommitRatherThanTheViewEntry() {
        NativeMaterialCommitScope<String> scopes = new NativeMaterialCommitScope<>();
        Object node = new Object();
        float[] oem = new float[42];
        float[] adjusted = oem.clone(); adjusted[34] = .63f;
        NativeMaterialCommitScope.Frame<String> frame = scopes.enter("media_bg", node, 0, 32, true);
        try {
            // The downstream View hook replaces its array; the upstream array remains intact.
            NativeMaterialCommitScope.Frame<String> committed = scopes.committed(node, true);
            GlassProfile profile = GlassProfile.capture(adjusted, committed.nightMode, committed.awake);
            assertEquals(0f, oem[34], 0f);
            assertEquals(.63f, profile.forScene(32, true)[34], 0f);
        } finally { scopes.leave(frame); }
        assertNull(scopes.matching(node));
    }

    @Test public void rejectedAndUnrelatedNodeWritesNeverBecomeMaterialSamples() {
        NativeMaterialCommitScope<String> scopes = new NativeMaterialCommitScope<>();
        Object node = new Object();
        NativeMaterialCommitScope.Frame<String> frame = scopes.enter("media_bg", node, 0, 16, true);
        try {
            assertNull(scopes.committed(node, false));
            assertNull(scopes.committed(new Object(), true));
            assertNull(scopes.committed(null, true));
        } finally { scopes.leave(frame); }
    }

    @Test public void nestedOwnedReplayAndAnExceptionRestoreTheOuterSourceWithoutLeakingScope() {
        NativeMaterialCommitScope<String> scopes = new NativeMaterialCommitScope<>();
        Object media = new Object(), own = new Object();
        NativeMaterialCommitScope.Frame<String> source = scopes.enter("media_bg", media, 0, 32, true);
        try {
            NativeMaterialCommitScope.Frame<String> target = scopes.enter("our glass", own, 1, 32, true);
            try {
                assertNull(scopes.matching(media));
                assertEquals(1, scopes.committed(own, true).role);
                throw new IllegalStateException("setter failed");
            } catch (IllegalStateException expected) {
                assertEquals("setter failed", expected.getMessage());
            } finally { scopes.leave(target); }
            assertSame(source, scopes.committed(media, true));
        } finally { scopes.leave(source); }
        assertNull(scopes.matching(media));
        assertNull(scopes.matching(own));
    }

    @Test public void trueNativeMediaIsTheOnlySourceEvenWithAnIdenticalResourceName() {
        String media = "com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaHeaderView";
        assertTrue(NativeMaterialSource.accepts("media_bg", Arrays.asList("android.widget.ImageView", media), false));
        assertFalse(NativeMaterialSource.accepts("media_bg", Arrays.asList("android.widget.ImageView", media), true));
        assertFalse(NativeMaterialSource.accepts("media_bg", Arrays.asList("com.zhuhuibin.hypermusiccover.duo.MaterialElement", media), false));
        assertFalse(NativeMaterialSource.accepts("media_bg", Arrays.asList("com.zhuhuibin.hyperchange.lite.Overlay", media), false));
        assertFalse(NativeMaterialSource.accepts("media_bg", Arrays.asList("miui.systemui.dynamicisland.Player", media), false));
        assertFalse(NativeMaterialSource.accepts("media_bg", Arrays.asList("com.android.systemui.controlcenter.MediaPanel"), false));
        assertFalse(NativeMaterialSource.accepts("other_background", Arrays.asList(media), false));
    }

    @Test public void aNativeViewCannotBeRegisteredAsAnOwnedDestination() {
        assertFalse(NativeMaterialSource.ownedTarget(new Object()));
        assertFalse(NativeMaterialSource.ownedTarget("com.zhuhuibin.hypermusiccover.duo.MaterialElement"));
        assertFalse(NativeMaterialSource.ownedTarget(null));
    }

    private static final class a implements NativeMaterialTarget {}

    @Test public void aRepackagedViewKeepsItsExplicitDestinationIdentity() {
        assertFalse(a.class.getSimpleName().startsWith("Material"));
        assertTrue(NativeMaterialSource.ownedTarget(new a()));
    }
}
