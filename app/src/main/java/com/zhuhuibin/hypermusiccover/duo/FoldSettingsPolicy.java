package com.zhuhuibin.hypermusiccover.duo;

/** Availability boundaries shared by UI commands, saved state and imported settings. */
final class FoldSettingsPolicy {
    static final boolean FULL_COVER_AVAILABLE = false;
    static final boolean BACKGROUND_SWITCH_AVAILABLE = false;

    static CoverCardStyle coverStyle(CoverCardStyle saved) {
        return FULL_COVER_AVAILABLE || saved.mode == CoverCardStyle.CARD ? saved
                : new CoverCardStyle(CoverCardStyle.CARD, saved.fill, saved.pos, saved.corner);
    }

    static boolean wallpaperBackground(boolean requested) {
        return BACKGROUND_SWITCH_AVAILABLE && requested;
    }

    static boolean nativeBackdrop(boolean rightRail) {
        return !BACKGROUND_SWITCH_AVAILABLE || rightRail;
    }

    private FoldSettingsPolicy() {}
}
