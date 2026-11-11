package com.miui.miwallpaper.wallpaperservice.impl;

import com.miui.miwallpaper.wallpaperservice.service.MultiDisplayEngineService;

/** The host tests check dispatch/ownership, not EGL rendering or OEM handler execution. */
public class ImageEngineImpl extends MultiDisplayEngineService {
    public boolean w;
    public int requests;
    public boolean keepAlive;
    public boolean fail;
    public void b0(boolean keepAlive) {
        if (fail) throw new IllegalStateException("injected dispatch failure");
        requests++;
        this.keepAlive = keepAlive;
    }
    public void X(boolean ignored) { throw new AssertionError("GL entry called on the caller thread"); }
}
