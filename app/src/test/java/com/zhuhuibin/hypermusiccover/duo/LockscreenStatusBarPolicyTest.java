package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LockscreenStatusBarPolicyTest {
    @Test public void standaloneSettingOwnsOnlyAnUnobscuredLockScreen() {
        assertTrue(LockscreenStatusBarPolicy.hide(true, true, false, false, false, false));
        assertFalse(LockscreenStatusBarPolicy.hide(false, true, false, false, false, false));
        assertFalse(LockscreenStatusBarPolicy.hide(true, false, false, false, false, false));
        assertFalse(LockscreenStatusBarPolicy.hide(true, true, true, false, false, false));
    }
    @Test public void nativeAuthenticationEditorAndControlCenterReleaseTheStrip() {
        assertFalse(LockscreenStatusBarPolicy.hide(true, true, false, true, false, false));
        assertFalse(LockscreenStatusBarPolicy.hide(true, true, false, false, true, false));
        assertFalse(LockscreenStatusBarPolicy.hide(true, true, false, false, false, true));
    }
}
