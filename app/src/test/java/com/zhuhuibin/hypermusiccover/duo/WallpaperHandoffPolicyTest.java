package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class WallpaperHandoffPolicyTest {
    @Test public void squareCoverCannotForceTheNativeWallpapersUnlockAnimation() {
        assertFalse(WallpaperHandoffPolicy.fadeCoverWallpaper(true, true));
        assertFalse(WallpaperHandoffPolicy.fadeCoverWallpaper(false, false));
        assertTrue(WallpaperHandoffPolicy.fadeCoverWallpaper(true, false));
    }
    @Test public void delayedAodCallbacksCannotRedimAnAwakeOrUnlockedWallpaper() {
        assertTrue(WallpaperHandoffPolicy.reassertAodDim(true, false, true, false, true));
        assertFalse(WallpaperHandoffPolicy.reassertAodDim(true, false, true, false, false));
        assertFalse(WallpaperHandoffPolicy.reassertAodDim(true, false, true, true, true));
        assertFalse(WallpaperHandoffPolicy.reassertAodDim(true, false, false, false, true));
        assertFalse(WallpaperHandoffPolicy.reassertAodDim(true, true, true, false, true));
    }
}
