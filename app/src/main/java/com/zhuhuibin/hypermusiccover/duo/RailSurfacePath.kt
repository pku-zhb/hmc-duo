package com.zhuhuibin.hypermusiccover.duo

import android.graphics.Path

/** One outline and one native blur/rim: overlapping shells never draw doubled glass. */
internal class RailSurfacePath {
    val path = Path()
    private val part = Path()
    fun update(shape: RailSurfaceContour.Shape) {
        path.reset()
        for (box in shape.boxes) {
            part.reset()
            part.addRoundRect(box.x, box.y, box.x + box.w, box.y + box.h,
                box.w / 2f, box.w / 2f, Path.Direction.CW)
            if (path.isEmpty) path.set(part) else path.op(part, Path.Op.UNION)
        }
        shape.neck?.let { neck ->
            part.reset()
            val l = neck.left; val r = neck.right
            part.moveTo(l.start.x, l.start.y)
            part.cubicTo(l.control1.x, l.control1.y, l.control2.x, l.control2.y, l.end.x, l.end.y)
            part.lineTo(r.start.x, r.start.y)
            part.cubicTo(r.control1.x, r.control1.y, r.control2.x, r.control2.y, r.end.x, r.end.y)
            part.close()
            path.op(part, Path.Op.UNION)
        }
    }
}
