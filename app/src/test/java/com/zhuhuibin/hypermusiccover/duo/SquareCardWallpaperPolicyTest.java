package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class SquareCardWallpaperPolicyTest {
    @Test public void enteringNativeReleasesTheOldTextureButTrackChangesDoNotReloadWallpaper() {
        SquareCardWallpaperPolicy policy = new SquareCardWallpaperPolicy();
        assertEquals(SquareCardWallpaperPolicy.Route.RESTORE_NATIVE, policy.route(true, true));
        for (int metadataUpdate = 0; metadataUpdate < 8; metadataUpdate++) {
            assertEquals(SquareCardWallpaperPolicy.Route.CARD_ONLY, policy.route(true, true));
        }
    }

    @Test public void nativeExitsNeverReloadTheWallpaperAfterInitialRestoration() {
        SquareCardWallpaperPolicy policy = new SquareCardWallpaperPolicy();
        assertEquals(SquareCardWallpaperPolicy.Route.RESTORE_NATIVE, policy.route(true, true));
        for (int unlock = 0; unlock < 8; unlock++) {
            assertFalse(policy.releaseNeeded(true, true));
            assertEquals(SquareCardWallpaperPolicy.Route.CARD_ONLY, policy.route(true, true));
        }
    }

    @Test public void exitBeforeFirstArtworkCanStillReleaseALegacyTextureExactlyOnce() {
        SquareCardWallpaperPolicy policy = new SquareCardWallpaperPolicy();
        assertTrue(policy.releaseNeeded(true, true));
        assertFalse(policy.releaseNeeded(true, true));
        assertTrue(policy.releaseNeeded(false, false));
        assertTrue(policy.releaseNeeded(true, true));
        assertFalse(policy.releaseNeeded(true, true));
    }

    @Test public void returningFromArtworkBackgroundNeedsOneNewRestoration() {
        SquareCardWallpaperPolicy policy = new SquareCardWallpaperPolicy();
        policy.route(true, true);
        assertEquals(SquareCardWallpaperPolicy.Route.WALLPAPER, policy.route(false, false));
        assertEquals(SquareCardWallpaperPolicy.Route.RESTORE_NATIVE, policy.route(true, true));
        assertEquals(SquareCardWallpaperPolicy.Route.CARD_ONLY, policy.route(true, true));
    }

    @Test public void foregroundShapeNeverOverridesTheBackgroundSelection() {
        SquareCardWallpaperPolicy policy = new SquareCardWallpaperPolicy();
        assertEquals(SquareCardWallpaperPolicy.Route.WALLPAPER, policy.route(false, true));
        assertFalse(SquareCardWallpaperPolicy.nativeBackdrop(false, true));
        assertTrue(SquareCardWallpaperPolicy.nativeBackdrop(true, false));
        assertTrue(SquareCardWallpaperPolicy.nativeBackdrop(true, true));
    }
    @Test public void changingForegroundShapeDoesNotReloadNativeWallpaper() {
        SquareCardWallpaperPolicy policy = new SquareCardWallpaperPolicy();
        assertEquals(SquareCardWallpaperPolicy.Route.RESTORE_NATIVE, policy.route(true, true));
        assertEquals(SquareCardWallpaperPolicy.Route.CARD_ONLY, policy.route(true, false));
        assertFalse(policy.releaseNeeded(true, false));
        assertEquals(SquareCardWallpaperPolicy.Route.CARD_ONLY, policy.route(true, true));
    }

}
