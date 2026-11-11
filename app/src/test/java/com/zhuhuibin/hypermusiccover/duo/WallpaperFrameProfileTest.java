package com.zhuhuibin.hypermusiccover.duo;

import com.miui.miwallpaper.wallpaperservice.impl.ImageEngineImpl;
import org.junit.Test;
import static org.junit.Assert.*;

public class WallpaperFrameProfileTest {
    @Test public void renamedEntryIsSelectedOnlyForTheInspectedRelease() {
        assertTrue(WallpaperFrameProfile.matches("com.miui.miwallpaper", 500827000L,
                "8.2.7-flip-ALPHA-09301622"));
        assertFalse(WallpaperFrameProfile.matches("com.miui.miwallpaper", 500827001L,
                "8.2.7-flip-ALPHA-09301622"));
        assertFalse(WallpaperFrameProfile.matches("com.miui.miwallpaper", 500827000L,
                "8.0.8-flip-q18"));
        assertFalse(WallpaperFrameProfile.matches("another.package", 500827000L,
                "8.2.7-flip-ALPHA-09301622"));
        assertFalse(WallpaperFrameProfile.matches("com.miui.miwallpaper", 500827000L, null));
    }

    @Test public void reloadArmsReuploadAndEnqueuesInsteadOfCallingTheGlEntry() throws Exception {
        ImageEngineImpl engine = new ImageEngineImpl();
        WallpaperFrameProfile.Binding binding = WallpaperFrameProfile.bind(engine.getClass());
        assertNotNull(binding);
        binding.reload(engine, false);
        assertTrue(engine.j);
        assertFalse(engine.w); // Readiness must remain owned by the OEM.
        assertEquals(2, engine.b); assertEquals(1, engine.c);
        assertEquals(1, engine.requests);
        assertFalse(engine.keepAlive);
    }

    @Test public void animationFramesKeepTheContextWithoutReuploadingEveryFrame() throws Exception {
        ImageEngineImpl engine = new ImageEngineImpl();
        WallpaperFrameProfile.Binding binding = WallpaperFrameProfile.bind(engine.getClass());
        binding.reload(engine, true);
        engine.j = false; // The simulated GL consumer has handled the initial upload.
        for (int i = 0; i < 45; i++) binding.request(engine, true);
        assertEquals(46, engine.requests);
        assertTrue(engine.keepAlive);
        assertFalse(engine.j);
        assertFalse(engine.w);
    }

    @Test public void aFailedDispatchRestoresThePriorReuploadFlag() throws Exception {
        ImageEngineImpl engine = new ImageEngineImpl();
        engine.fail = true;
        WallpaperFrameProfile.Binding binding = WallpaperFrameProfile.bind(engine.getClass());
        for (boolean before : new boolean[]{false, true}) {
            engine.j = before;
            try { binding.reload(engine, true); fail("dispatch should fail"); }
            catch (ReflectiveOperationException expected) { assertEquals(before, engine.j); }
        }
        assertEquals(0, engine.requests);
        assertFalse(engine.w);
    }

    static class SimilarEngine {
        public boolean j;
        public void b0(boolean ignored) { throw new AssertionError("must not be invoked"); }
    }
    static class ShadowEngine extends ImageEngineImpl { public int j = 7; }

    @Test public void reloadUsesTheVerifiedDeclaringClassEvenIfASubclassShadowsTheName() throws Exception {
        ShadowEngine engine = new ShadowEngine();
        WallpaperFrameProfile.Binding binding = WallpaperFrameProfile.bind(engine.getClass());
        assertNotNull(binding);
        binding.reload(engine, true);
        assertEquals(7, engine.j);
        assertTrue(((ImageEngineImpl) engine).j);
        assertEquals(2, engine.b);
    }
    @Test public void similarlyNamedMembersInAnotherHierarchyAreRejected() {
        assertNull(WallpaperFrameProfile.bind(SimilarEngine.class));
        assertNull(WallpaperFrameProfile.bind(Object.class));
    }
}
