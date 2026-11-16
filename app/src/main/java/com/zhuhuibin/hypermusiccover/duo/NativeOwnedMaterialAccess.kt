package com.zhuhuibin.hypermusiccover.duo

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.view.View
import java.lang.reflect.Method
import java.util.WeakHashMap

/** Resolved native API boundary; no player/provider is needed to construct an owned recipe. */
internal class NativeOwnedMaterialAccess private constructor(classLoader: ClassLoader) {
    private val glass = Class.forName("com.miui.systemui.util.MiGlassCompat", false, classLoader)
    private val util = Class.forName("com.android.systemui.statusbar.notification.utils.NotificationUtil", false, classLoader)
    val materialType: Method = glass.getMethod("setMiViewMaterialTypeCompat", Int::class.javaPrimitiveType, View::class.java)
    val glassParameters: Method = glass.getMethod("setMiGlassCompat", View::class.java, FloatArray::class.java)
    val roundRect: Method = util.getMethod("setRoundRect", View::class.java,
        Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
    val viewBlurMode: Method = View::class.java.getMethod("setMiViewBlurMode", Int::class.javaPrimitiveType)
    val containerBlurMode: Method = View::class.java.getMethod("setMiBackgroundBlurMode", Int::class.javaPrimitiveType)
    val passWindow: Method = View::class.java.getMethod("setPassWindowBlurEnabled", Boolean::class.javaPrimitiveType)
    val blurRadius: Method = View::class.java.getMethod("setMiBackgroundBlurRadius", Int::class.javaPrimitiveType)
    val blurScale: Method = View::class.java.getMethod("setMiBackgroundBlurScaleRatio", Float::class.javaPrimitiveType)
    val defaultBlurScale: Method = View::class.java.getMethod("getMiBackgroundBlurScaleRatio")
    val glassBlurRadius: Method = View::class.java.getMethod("setMiGlassBlurRadius",
        Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    val palette: Method = View::class.java.getMethod("setMiBackgroundBlendColors", ArrayList::class.java)
    val blurWindow: Method? = runCatching {
        Class.forName("miuix.core.util.MiuiBlurUtils", false, classLoader).getMethod("setMiBlurWinType", View::class.java)
    }.getOrNull()

    fun palette(view: View, blend: IntArray) {
        val points = ArrayList<Point>(blend.size / 2)
        for (i in blend.indices step 2) points.add(Point(blend[i], blend[i + 1]))
        palette.invoke(view, points)
    }

    companion object {
        private var loader: ClassLoader? = null
        private var access: NativeOwnedMaterialAccess? = null
        fun get(classLoader: ClassLoader): NativeOwnedMaterialAccess {
            if (loader !== classLoader) {
                access = NativeOwnedMaterialAccess(classLoader)
                loader = classLoader
            }
            return checkNotNull(access)
        }

        // IDs and endpoint arrays are stable within this Resources/asset configuration.
        // Colors still come from the current context; provider-modified inputs are never cached.
        private val resources = WeakHashMap<Resources, Values>()
        fun values(context: Context): Values {
            val res = context.resources
            val current = resources[res]
            if (current != null && current.configuration == res.configuration && current.assets === res.assets) return current
            return Values(res).also { resources[res] = it }
        }
    }

    class Values(res: Resources) {
        val configuration = Configuration(res.configuration)
        val assets = res.assets
        private val ids = HashMap<String, Int>()
        private val parameters = HashMap<String, FloatArray>()
        fun id(res: Resources, type: String, name: String): Int = ids.getOrPut("$type/$name") {
            res.getIdentifier(name, type, "com.android.systemui").also { require(it != 0) { "no $type/$name" } }
        }
        fun parameters(res: Resources, name: String): FloatArray = parameters.getOrPut(name) {
            res.getStringArray(id(res, "array", name)).map { if (it.isNullOrBlank()) 0f else it.toFloat() }.toFloatArray()
                .also { require(NativeOwnedMaterialRecipe.validGlass(it)) { "incomplete array/$name" } }
        }.copyOf()
    }
}
