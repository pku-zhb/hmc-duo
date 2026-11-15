package com.zhuhuibin.hypermusiccover.duo;

/** Event-driven attachment/layout/scene readiness, with no timer or pre-draw polling. */
final class NativeOwnedMaterialReady {
    private boolean ready;
    boolean update(boolean current) {
        boolean first = current && !ready;
        ready = current;
        return first;
    }
    void applied(boolean current) { ready = current; }
}
