package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationMaterialBindingTest {
    private val prefix = listOf(
        "com.android.systemui.statusbar.notification.style.domain.NotificationMaterialStateInteractor",
        "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow")
    private val type = "com.miui.systemui.material.NotificationViewType"
    private val ref = "kotlin.jvm.internal.Ref\$ObjectRef"

    @Test fun currentFourArgumentBinderHasNoEffectKeyCacheToClear() {
        assertEquals(NotificationMaterialBinding.DIRECT,
            NotificationMaterialBinding.from(prefix + listOf(type, ref)))
    }

    @Test fun fiveArgumentBinderRequiresAnExplicitKeyRefBeforeTheTypeAndEffectRef() {
        assertEquals(NotificationMaterialBinding.KEYED,
            NotificationMaterialBinding.from(prefix + listOf(ref, type, ref)))
        assertEquals(NotificationMaterialBinding.KEYED,
            NotificationMaterialBinding.from(prefix + listOf(ref, ref, type)))
        assertNull(NotificationMaterialBinding.from(prefix + listOf(type, ref, ref)))
    }

    @Test fun unknownLayoutAndUnrelatedRowsDoNotGetReflectivelyMutated() {
        assertNull(NotificationMaterialBinding.from(prefix + listOf(ref, ref)))
        assertNull(NotificationMaterialBinding.from(prefix + listOf(type, "java.lang.Object")))
        assertNull(NotificationMaterialBinding.from(listOf("wrong.Interactor", prefix[1], type, ref)))
    }
}
