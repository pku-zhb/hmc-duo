package com.zhuhuibin.hypermusiccover.duo;

/** Track only our writes; an OEM rewrite becomes the new baseline, never a stale rollback. */
final class GeometryLease {
    int originalWidth, writtenWidth;
    float originalX, writtenX;
    GeometryLease(int width, float x) {
        originalWidth = writtenWidth = width;
        originalX = writtenX = x;
    }
    void observe(int width, float x) {
        if (width != writtenWidth) originalWidth = width;
        if (x != writtenX) originalX = x;
    }
    void wrote(int width, float x) { writtenWidth = width; writtenX = x; }
    int restoreWidth(int current) { return current == writtenWidth ? originalWidth : current; }
    float restoreX(float current) { return current == writtenX ? originalX : current; }
}
