package com.zhuhuibin.hypermusiccover.duo

/** Known OEM signatures have different third arguments; never clear a NotificationViewType. */
internal enum class NotificationMaterialBinding {
    DIRECT, KEYED;

    companion object {
        fun from(parameters: List<String>): NotificationMaterialBinding? {
            val interactor = "com.android.systemui.statusbar.notification.style.domain.NotificationMaterialStateInteractor"
            val row = "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow"
            val type = "com.miui.systemui.material.NotificationViewType"
            val ref = "kotlin.jvm.internal.Ref\$ObjectRef"
            if (parameters.take(2) != listOf(interactor, row)) return null
            return when {
                parameters.drop(2) == listOf(type, ref) -> DIRECT
                parameters.size == 5 && parameters[2] == ref &&
                    parameters.drop(3).sorted() == listOf(type, ref).sorted() -> KEYED
                else -> null
            }
        }
    }
}
