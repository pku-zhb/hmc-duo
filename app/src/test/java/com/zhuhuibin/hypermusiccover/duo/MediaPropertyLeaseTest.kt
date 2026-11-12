package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class MediaPropertyLeaseTest {
    @Test fun detachedThemeChangesCoalesceIntoOneCurrentRebindOnAttach() {
        val request = MediaRebindRequest()
        assertFalse(request.take(true)) // A native/default attachment owns nothing.
        request.defer() // Custom surface detached.
        request.defer() // User selected the native theme while it was detached.
        repeat(10) { assertFalse(request.take(false)) }
        assertTrue(request.take(true))
        assertFalse(request.take(true))
        request.defer()
        request.clear() // Controller destroyed; its old child must not rebind later.
        assertFalse(request.take(true))
    }

    @Test fun nativeModeNeverWritesAnUnownedProperty() {
        val lease = MediaPropertyLease<Float>()
        repeat(10) { assertFalse(lease.release(.42f) { fail("default must not write") }) }
        assertFalse(lease.write(.42f, .42f) { fail("equal values need no ownership") })
        assertFalse(lease.held)
    }

    @Test fun handoffRestoresTheValueAtAcquisitionExactlyOnce() {
        val lease = MediaPropertyLease<Float>()
        var alpha = .36f
        lease.write(alpha, 0f) { alpha = it }
        assertTrue(lease.release(alpha) { alpha = it })
        assertEquals(.36f, alpha, 0f)
        assertFalse(lease.release(alpha) { fail("handoff must not repeat") })
    }

    @Test fun oemOrAnotherModuleWinsOverAnOldLease() {
        val lease = MediaPropertyLease<Float>()
        var alpha = .7f
        lease.write(alpha, 0f) { alpha = it }
        alpha = .23f
        assertTrue(lease.release(alpha) { fail("do not replace the new owner's value") })
        assertEquals(.23f, alpha, 0f)
    }

    @Test fun reassertingAnOverrideCapturesANewExternalBaseline() {
        val lease = MediaPropertyLease<Float>()
        var alpha = .7f
        lease.write(alpha, 0f) { alpha = it }
        alpha = .23f
        lease.write(alpha, 0f) { alpha = it }
        lease.release(alpha) { alpha = it }
        assertEquals(.23f, alpha, 0f)
    }

    @Test fun contextOwnershipUsesIdentityAndPreservesExternalThemeChanges() {
        data class Theme(val night: Boolean)
        val lease = MediaPropertyLease<Theme> { a, b -> a === b }
        val native = Theme(false)
        val forced = Theme(true)
        var current = native
        lease.write(current, forced) { current = it }
        assertSame(native, lease.nativeValue(current))
        current = Theme(true)
        assertFalse(lease.owns(current))
        lease.release(current) { fail("equal-looking current context belongs to someone else") }
    }

    @Test fun oldWorkerResultsCannotReturnAfterDefaultDetachOrReactivation() {
        val work = MediaRenderGeneration()
        val first = work.invalidate()
        assertTrue(work.accepts(first))
        work.invalidate() // Switching to native mode or detaching ends this ownership.
        assertFalse(work.accepts(first))
        val second = work.invalidate()
        assertFalse(work.accepts(first))
        assertTrue(work.accepts(second))
        work.invalidate() // A newer track supersedes a queued UI result too.
        assertFalse(work.accepts(second))
    }
}
