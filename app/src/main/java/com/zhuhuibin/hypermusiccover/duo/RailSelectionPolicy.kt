package com.zhuhuibin.hypermusiccover.duo

/** An explicit return chooses its island before automatic notification arrival ordering. */
internal object RailSelectionPolicy {
    fun select(returned: String?, arrived: String?, current: String?, keys: List<String>): String? =
        returned?.takeIf { it in keys } ?: arrived?.takeIf { it in keys }
            ?: current?.takeIf { it in keys } ?: keys.firstOrNull()
}
