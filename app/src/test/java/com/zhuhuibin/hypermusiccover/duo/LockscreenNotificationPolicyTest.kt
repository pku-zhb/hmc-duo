package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class LockscreenNotificationPolicyTest {
    @Test fun disabledOptionsPreserveBothNativeDecisions() {
        val policy = LockscreenNotificationPolicy()
        for (original in listOf(false, true)) {
            assertEquals(original, policy.canShowOnKeyguard(original))
            assertEquals(original, policy.shownAfterUnlock(original))
        }
    }

    @Test fun showingAllDoesNotAlsoKeepPreviouslySeenNotifications() {
        val policy = LockscreenNotificationPolicy(showAll = true)
        assertTrue(policy.canShowOnKeyguard(false))
        assertTrue(policy.shownAfterUnlock(true))
    }

    @Test fun retainingDoesNotGrantAnAppLockscreenPermission() {
        val policy = LockscreenNotificationPolicy(retain = true)
        assertFalse(policy.canShowOnKeyguard(false))
        assertFalse(policy.shownAfterUnlock(true))
        assertFalse(policy.shownAfterUnlock(false))
    }
}
