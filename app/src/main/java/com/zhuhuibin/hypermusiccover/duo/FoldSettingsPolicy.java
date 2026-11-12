package com.zhuhuibin.hypermusiccover.duo;

/** Availability boundaries shared by UI commands, saved state and imported settings. */
final class FoldSettingsPolicy {
    static final boolean FULL_COVER_AVAILABLE = true;
    static final boolean BACKGROUND_SWITCH_AVAILABLE = true;

    static CoverCardStyle coverStyle(CoverCardStyle saved) {
        return saved;
    }

    static boolean wallpaperBackground(boolean requested) {
        return BACKGROUND_SWITCH_AVAILABLE && requested;
    }

    /** Background choice never follows foreground shape or the shortcut rail. */
    static boolean nativeBackdrop(boolean requested) {
        return requested;
    }

    private FoldSettingsPolicy() {}
}
