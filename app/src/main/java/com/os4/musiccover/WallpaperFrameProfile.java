package com.os4.musiccover;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Bytecode-verified frame dispatch for the installed foldable wallpaper release. */
final class WallpaperFrameProfile {
    private WallpaperFrameProfile() {}
    static final String PACKAGE = "com.miui.miwallpaper";
    static final long VERSION_CODE = 500827000L;
    static final String VERSION_NAME = "8.2.7-flip-ALPHA-09301622";
    static final String IMAGE_ENGINE = PACKAGE + ".wallpaperservice.impl.ImageEngineImpl";
    static final String MULTI_DISPLAY = PACKAGE + ".wallpaperservice.service.MultiDisplayEngineService";

    static boolean matches(String pkg, long code, String name) {
        return PACKAGE.equals(pkg) && VERSION_CODE == code && VERSION_NAME.equals(name);
    }

    static final class Binding {
        private final Method enqueue;
        private final Field recreate;
        Binding(Method enqueue, Field recreate) { this.enqueue = enqueue; this.recreate = recreate; }

        /** b0 posts a Runnable onto the OEM GL handler; X must never run on our caller thread. */
        void request(Object engine, boolean keepAlive) throws ReflectiveOperationException {
            enqueue.invoke(engine, keepAlive);
        }

        /** j requests onSurfaceCreated/re-upload; w is only the surface-readiness guard. */
        void reload(Object engine, boolean keepAlive) throws ReflectiveOperationException {
            boolean previous = recreate.getBoolean(engine);
            recreate.setBoolean(engine, true);
            try { request(engine, keepAlive); }
            catch (ReflectiveOperationException | RuntimeException failure) {
                recreate.setBoolean(engine, previous);
                throw failure;
            }
        }

        String describe() { return "8.2.7-flip b0(Z) via OEM handler, reupload=MultiDisplayEngineService.j"; }
    }

    static Binding bind(Class<?> engine) {
        Class<?> image = null, multi = null;
        for (Class<?> c = engine; c != null; c = c.getSuperclass()) {
            if (IMAGE_ENGINE.equals(c.getName())) image = c;
            if (MULTI_DISPLAY.equals(c.getName())) multi = c;
        }
        if (image == null || multi == null) return null;
        try {
            Method enqueue = image.getDeclaredMethod("b0", boolean.class);
            Field recreate = multi.getDeclaredField("j");
            if (enqueue.getReturnType() != void.class || Modifier.isStatic(enqueue.getModifiers())
                    || Modifier.isAbstract(enqueue.getModifiers()) || recreate.getType() != boolean.class
                    || Modifier.isStatic(recreate.getModifiers()) || Modifier.isFinal(recreate.getModifiers())) return null;
            enqueue.setAccessible(true);
            recreate.setAccessible(true);
            return new Binding(enqueue, recreate);
        } catch (ReflectiveOperationException | RuntimeException failure) { return null; }
    }
}
