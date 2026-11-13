package com.zhuhuibin.hypermusiccover.duo;

/** Route one native draw; renderer failures must never execute native drawing twice. */
final class RailEdgeDrawPass {
    interface Native<C> { Object draw(C canvas) throws Throwable; }
    interface Target<C> {
        /** Null means software, unsupported or an unowned geometry: draw natively. */
        C begin(C original) throws Throwable;
        void complete(C original) throws Throwable;
        void abort();
        /** Replay the already recorded native commands, if output has not started. */
        void recover(C original);
        void failed(Throwable error);
    }

    static <C> Object draw(C original, Target<C> target, Native<C> nativeDraw) throws Throwable {
        C recording;
        try {
            recording = target.begin(original);
        } catch (Throwable error) {
            target.abort();
            target.failed(error);
            return nativeDraw.draw(original);
        }
        if (recording == null) return nativeDraw.draw(original);
        Object result;
        try {
            result = nativeDraw.draw(recording);
        } catch (Throwable error) {
            target.abort();
            // Preserve a native exception, rather than hiding it by re-entering native code.
            throw error;
        }
        try {
            target.complete(original);
        } catch (Throwable error) {
            target.abort();
            target.recover(original);
            target.failed(error);
        }
        return result;
    }

    private RailEdgeDrawPass() {}
}
