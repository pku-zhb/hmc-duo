package com.zhuhuibin.hypermusiccover.duo

/** Run the original horizontal ColorOS geometry on an upright rail without rotating its content. */
internal class ColorOsPillAxis(val vertical: Boolean, val screen: CoverMorphMotion.Box) {
    val logical = if (vertical) CoverMorphMotion.Box(screen.cx() - screen.h / 2f,
        screen.cy() - screen.w / 2f, screen.h, screen.w) else screen

    fun toScreen(box: CoverMorphMotion.Box): CoverMorphMotion.Box {
        if (!vertical) return box
        val x = screen.cx() - (box.cy() - screen.cy())
        val y = screen.cy() + (box.cx() - screen.cx())
        return CoverMorphMotion.Box(x - box.h / 2f, y - box.w / 2f, box.h, box.w)
    }
}
