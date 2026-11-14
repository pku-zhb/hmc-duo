package com.zhuhuibin.hypermusiccover.duo;

import android.view.View;
import android.view.ViewGroup;
import java.util.IdentityHashMap;

/** Compact native geometry with the same height targets used by the OEM stack animator. */
final class CompactPlayerRuntime {
    private static View card;
    private static final IdentityHashMap<View, Lease> owned = new IdentityHashMap<>();
    private static View background, album, image, title, artist, seamless, action, seek;
    private static int nativeNormal, nativeAod, compactAod;
    private static CompactPlayerMetrics.Plan currentPlan;
    private static boolean installed, animating, fullAod;
    private static int animatedHeight;
    private static java.lang.reflect.Field headerMinimum, animateHeight;
    private static View animateHeader;
    private static int nativeTarget, writtenTarget;
    private static View heightHeader;
    private static int headerBaseline, headerWritten;
    private static final IdentityHashMap<View, Alpha> seekAlphas = new IdentityHashMap<>();
    private static final class Alpha {
        float base, written;
        Alpha(View view) { base = written = view.getAlpha(); }
        void apply(View view, float factor) {
            if (view.getAlpha() != written) base = view.getAlpha();
            written = base * factor;
            if (view.getAlpha() != written) view.setAlpha(written);
        }
        void restore(View view) { if (view.getAlpha() == written) view.setAlpha(base); }
    }
    private static final IdentityHashMap<View, Integer> seekDrawShifts = new IdentityHashMap<>();
    private static final IdentityHashMap<View, ButtonPadding> buttonPaddings = new IdentityHashMap<>();
    private static final android.graphics.RectF glyphBounds = new android.graphics.RectF();
    private static final View[] actionButtons = new View[5];
    private static final class ButtonPadding {
        int top, bottom, topWritten, bottomWritten;
        ButtonPadding(View view) { capture(view); }
        void capture(View view) {
            top = topWritten = view.getPaddingTop(); bottom = bottomWritten = view.getPaddingBottom();
        }
        void observe(View view) {
            if (view.getPaddingTop() != topWritten || view.getPaddingBottom() != bottomWritten) capture(view);
        }
        void apply(View view, int shift) {
            topWritten = top + shift; bottomWritten = bottom - shift;
            write(view, topWritten, bottomWritten);
        }
        void write(View view, int nextTop, int nextBottom) {
            if (view.getPaddingTop() == nextTop && view.getPaddingBottom() == nextBottom) return;
            if (view.isPaddingRelative())
                view.setPaddingRelative(view.getPaddingStart(), nextTop, view.getPaddingEnd(), nextBottom);
            else view.setPadding(view.getPaddingLeft(), nextTop, view.getPaddingRight(), nextBottom);
        }
        void restore(View view) {
            if (view.getPaddingTop() == topWritten && view.getPaddingBottom() == bottomWritten)
                write(view, top, bottom);
        }
    }
    private static final IdentityHashMap<View, Offset> seekLabelOffsets = new IdentityHashMap<>();
    private static final class Offset {
        float base, written;
        Offset(View view) { base = written = view.getTranslationY(); }
        void apply(View view, int shift) {
            if (view.getTranslationY() != written) base = view.getTranslationY();
            written = base - shift;
            if (view.getTranslationY() != written) view.setTranslationY(written);
        }
        void restore(View view) { if (view.getTranslationY() == written) view.setTranslationY(base); }
    }
    private static int appliedPercent = 100;

    private static final class Lease {
        int height, heightWritten, width, widthWritten, top, topWritten, bottom, bottomWritten, minimum, minimumWritten;
        Lease(View view) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            height = heightWritten = params.height;
            width = widthWritten = params.width;
            minimum = minimumWritten = view.getMinimumHeight();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
                top = topWritten = margins.topMargin;
                bottom = bottomWritten = margins.bottomMargin;
            }
        }
        void observe(View view) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params.height != heightWritten) height = params.height;
            if (params.width != widthWritten) width = params.width;
            if (view.getMinimumHeight() != minimumWritten) minimum = view.getMinimumHeight();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
                if (margins.topMargin != topWritten) top = margins.topMargin;
                if (margins.bottomMargin != bottomWritten) bottom = margins.bottomMargin;
            }
        }
        void apply(View view, int nextHeight, int nextTop, int nextBottom, int nextMinimum) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            boolean changed = params.height != nextHeight;
            params.height = heightWritten = nextHeight;
            topWritten = nextTop; bottomWritten = nextBottom;
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
                changed |= margins.topMargin != nextTop || margins.bottomMargin != nextBottom;
                margins.topMargin = nextTop; margins.bottomMargin = nextBottom;
            }
            if (changed) view.setLayoutParams(params);
            minimumWritten = nextMinimum;
            if (view.getMinimumHeight() != nextMinimum) view.setMinimumHeight(nextMinimum);
        }
        void width(View view, int value) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params.width != value) { params.width = value; view.setLayoutParams(params); }
            widthWritten = value;
        }
        void restore(View view) {
            if (view.getLayoutParams() == null) return;
            observe(view);
            apply(view, height, top, bottom, minimum);
            width(view, width);
        }
    }

    private static Lease lease(View view) {
        Lease value = owned.get(view);
        if (value == null) { value = new Lease(view); owned.put(view, value); }
        else value.observe(view);
        return value;
    }

    private static View named(View parent, String name) {
        int id = parent.getResources().getIdentifier(name, "id", "com.android.systemui");
        return id == 0 ? null : parent.findViewById(id);
    }

    private static boolean ready(View view) {
        return view != null && view.isAttachedToWindow() && view.getLayoutParams() != null;
    }

    static void update(View root) { update(root, null); }

    private static void update(View root, View preferred) {
        if (!installed || !Main.keyguardLocked() || Main.bouncerShown()) {
            if (!Main.keyguardLocked()) fullAod = false;
            restore(); return;
        }
        if (MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) return;
        View header = preferred == null ? Main.miniPlayerMediaHeader() : null;
        View next = preferred != null ? preferred : header == null
                ? Main.findLockScreenView("mi_media_controls") : named(header, "mi_media_controls");
        if (!ready(next)) { restore(); return; }
        if (next != card) {
            restore(); card = next;
            resolveChildren();
        }
        float density = root.getResources().getDisplayMetrics().density;
        int percent = MediaCardRuntime.heightPercent(CompactPlayerMetrics.innerLandscape(
                root.getWidth(), root.getHeight(), density));
        if (percent >= 88) { restoreProperties(); return; }
        if (!ready(background) || !ready(album) || !ready(action) || !ready(seek)) {
            restoreProperties();
            resolveChildren();
            if (!ready(background) || !ready(album) || !ready(action) || !ready(seek)) return;
        }
        Lease bg = lease(background), art = lease(album), controls = lease(action), track = lease(seek);
        // The retained normal constraint set has fixed native rows and a fixed background.
        // Unknown/match-constraint variants remain native rather than guessing a measured size.
        if (bg.height <= 0 || art.height <= 0 || controls.height <= 0 || track.height <= 0) {
            restoreProperties(); return;
        }
        nativeNormal = dimension(card, "qs_media_session_height_expanded");
        nativeAod = dimension(card, "qs_media_session_height_expanded_fullAod");
        if (nativeNormal <= nativeAod || nativeAod <= 0
                || (bg.height != nativeNormal && bg.height != nativeAod)) {
            restoreProperties(); return;
        }
        int controlHeight = art.height + controls.height + track.height
                + card.getPaddingTop() + card.getPaddingBottom();
        CompactPlayerMetrics.Plan plan = CompactPlayerMetrics.plan(nativeNormal, percent,
                art.top, controls.top, track.top, track.bottom, controlHeight, art.height, density);
        currentPlan = plan;
        int headerFloor = textHeaderHeight();
        plan = CompactPlayerMetrics.withHeaderFloor(plan, headerFloor, nativeNormal, art.height);
        currentPlan = plan;
        int actionShift = compactActionPixels(percent, density);
        int actionBottom = playbackGlyphBottom(controls.height);
        // Do not retarget an in-flight/AOD shell when a drawable first reports its bounds.
        if (compactAod <= 0 || !fullAod && !animating)
            compactAod = CompactPlayerMetrics.aodHeight(nativeAod, plan, art.height, art.top,
                    controls.top, controls.height, actionBottom,
                    card.getPaddingTop() + card.getPaddingBottom(), density);
        applyHeaderMinimum(fullAod ? compactAod : plan.height);
        int shell = animating && animatedHeight > 0 ? animatedHeight : fullAod ? compactAod : plan.height;
        bg.apply(background, shell, bg.top, bg.bottom, bg.minimum);
        art.apply(album, plan.artwork, plan.top, art.bottom, Math.min(art.minimum, plan.artwork));
        art.width(album, art.width > 0 ? Math.min(art.width, plan.artwork) : art.width);
        if (ready(image)) {
            Lease bitmap = lease(image);
            bitmap.apply(image, Math.min(bitmap.height, plan.artwork), bitmap.top, bitmap.bottom,
                    Math.min(bitmap.minimum, plan.artwork));
            bitmap.width(image, bitmap.width > 0 ? Math.min(bitmap.width, plan.artwork) : bitmap.width);
        }
        controls.apply(action, controls.height, plan.action, controls.bottom, controls.minimum);
        track.apply(seek, track.height, plan.seekTop, plan.seekBottom, track.minimum);
        compactSeekPixels(percent, track.height, actionShift, density);
        alignHeader(title, plan.top + Math.max(0, (plan.artwork - headerFloor) / 2));
        if (ready(seamless)) alignHeader(seamless, plan.top + Math.max(0,
                (plan.artwork - Math.max(0, seamless.getLayoutParams().height > 0 ? seamless.getLayoutParams().height : seamless.getMeasuredHeight())) / 2));
        Lease container = lease(card);
        // WRAP_CONTENT remains native. Explicit height/minimum follow the safely reduced shell.
        int height = container.height > 0 ? shell : container.height;
        container.apply(card, height, container.top, container.bottom,
                Math.min(container.minimum, shell));
        appliedPercent = 75;
    }

    private static void resolveChildren() {
        background = named(card, "media_bg"); album = named(card, "album_art");
        image = named(card, "album_art_image");
        title = named(card, "header_title"); artist = named(card, "header_artist"); seamless = named(card, "media_seamless");
        action = named(card, "action0"); seek = named(card, "media_progress_bar");
        for (int i = 0; i < actionButtons.length; ++i) actionButtons[i] = named(card, "action" + i);
    }

    private static void alignHeader(View view, int top) {
        if (!ready(view)) return;
        Lease value = lease(view);
        value.apply(view, value.height, top, value.bottom, value.minimum);
    }

    private static int textHeaderHeight() {
        int value = title == null ? 0 : title.getMeasuredHeight();
        if (artist != null && artist.getVisibility() != View.GONE) {
            value += artist.getMeasuredHeight();
            if (artist.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)
                value += Math.max(0, ((ViewGroup.MarginLayoutParams) artist.getLayoutParams()).topMargin);
        }
        return value;
    }

    private static int dimension(View view, String name) {
        int id = view.getResources().getIdentifier(name, "dimen", "com.android.systemui");
        return id == 0 ? 0 : view.getResources().getDimensionPixelSize(id);
    }

    private static void applyHeaderMinimum(int height) {
        if (headerMinimum == null) return;
        View header = card;
        while (header != null && !headerMinimum.getDeclaringClass().isInstance(header))
            header = header.getParent() instanceof View ? (View) header.getParent() : null;
        if (header == null) return;
        try {
            int current = headerMinimum.getInt(header);
            if (heightHeader != header) {
                restoreHeaderMinimum();
                heightHeader = header; headerBaseline = headerWritten = current;
            } else if (current != headerWritten) headerBaseline = current;
            headerWritten = height;
            if (current != height) headerMinimum.setInt(header, height);
        } catch (IllegalAccessException error) { Xp.w(Main.TAG + "native media minimum unavailable: " + error); }
    }

    private static void restoreHeaderMinimum() {
        if (heightHeader != null && headerMinimum != null) try {
            if (headerMinimum.getInt(heightHeader) == headerWritten)
                headerMinimum.setInt(heightHeader, headerBaseline);
        } catch (IllegalAccessException ignored) { }
        heightHeader = null;
    }

    private static int compactActionPixels(int percent, float density) {
        int shift = Math.round(5f * density);
        boolean found = false;
        for (View button : actionButtons) {
            if (!ready(button) || button.getVisibility() == View.GONE) continue;
            ButtonPadding padding = buttonPaddings.get(button);
            if (padding == null) { padding = new ButtonPadding(button); buttonPaddings.put(button, padding); }
            padding.observe(button);
            shift = Math.min(shift, CompactPlayerMetrics.actionContentShift(percent, padding.bottom, density));
            found = true;
        }
        if (!found) return 0;
        for (java.util.Map.Entry<View, ButtonPadding> entry : buttonPaddings.entrySet())
            entry.getValue().apply(entry.getKey(), shift);
        return shift;
    }

    private static int playbackGlyphBottom(int rowHeight) {
        float bottom = 0f;
        for (View button : actionButtons) {
            if (!(button instanceof android.widget.ImageView) || button.getVisibility() != View.VISIBLE) continue;
            android.widget.ImageView image = (android.widget.ImageView) button;
            android.graphics.drawable.Drawable drawable = image.getDrawable();
            if (drawable == null || drawable.getBounds().isEmpty()) continue;
            glyphBounds.set(drawable.getBounds());
            image.getImageMatrix().mapRect(glyphBounds);
            bottom = Math.max(bottom, glyphBounds.bottom + image.getPaddingTop());
        }
        return bottom > 0f ? Math.min(rowHeight, Math.round(bottom)) : rowHeight;
    }

    private static void compactSeekPixels(int percent, int rowHeight, int actionShift, float density) {
        if (!ready(seek)) return;
        int visibleHeight = Math.max(0, rowHeight - seek.getPaddingTop() - seek.getPaddingBottom());
        for (String name : new String[]{"media_elapsed_time", "media_total_time"}) {
            View label = named(card, name);
            if (label != null) visibleHeight = Math.max(visibleHeight, label.getMeasuredHeight());
        }
        int shift = CompactPlayerMetrics.seekContentShift(percent, rowHeight, visibleHeight,
                actionShift, seek.getPaddingTop(), density);
        Integer previous = seekDrawShifts.put(seek, shift);
        if (previous == null || previous != shift) seek.invalidate();
        for (String name : new String[]{"media_elapsed_time", "media_total_time"}) {
            View label = named(card, name);
            if (label == null) continue;
            Offset offset = seekLabelOffsets.get(label);
            if (offset == null) { offset = new Offset(label); seekLabelOffsets.put(label, offset); }
            offset.apply(label, shift);
        }
    }

    private static void restoreSeekPixels() {
        for (View view : seekDrawShifts.keySet()) view.invalidate();
        for (java.util.Map.Entry<View, Offset> e : seekLabelOffsets.entrySet()) e.getValue().restore(e.getKey());
        for (java.util.Map.Entry<View, ButtonPadding> e : buttonPaddings.entrySet()) e.getValue().restore(e.getKey());
        seekDrawShifts.clear(); seekLabelOffsets.clear(); buttonPaddings.clear();
    }

    private static void applySeekAlpha(float factor) {
        if (card == null) return;
        for (String name : new String[]{"media_progress_bar", "media_elapsed_time", "media_total_time"}) {
            View row = named(card, name);
            if (row == null) continue;
            Alpha alpha = seekAlphas.get(row);
            if (alpha == null) { alpha = new Alpha(row); seekAlphas.put(row, alpha); }
            alpha.apply(row, factor);
        }
    }

    private static void restoreSeekAlpha() {
        for (java.util.Map.Entry<View, Alpha> e : seekAlphas.entrySet()) e.getValue().restore(e.getKey());
        seekAlphas.clear();
    }

    private static void restoreProperties() {
        if (animateHeader != null && animateHeight != null) try {
            if (animateHeight.getInt(animateHeader) == writtenTarget)
                animateHeight.setInt(animateHeader, nativeTarget);
        } catch (IllegalAccessException ignored) { }
        animateHeader = null;
        restoreHeaderMinimum();
        restoreSeekAlpha();
        restoreSeekPixels();
        currentPlan = null; compactAod = 0;
        animating = false; animatedHeight = 0;
        for (java.util.Map.Entry<View, Lease> entry : owned.entrySet()) entry.getValue().restore(entry.getKey());
        owned.clear();
        appliedPercent = 100;
    }

    static void install(ClassLoader loader) {
        if (installed) return;
        try {
            Class<?> header = Xp.findClass("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaHeaderView", loader);
            headerMinimum = header.getDeclaredField("mediaLockScreenHeight");
            headerMinimum.setAccessible(true);
            animateHeight = header.getDeclaredField("mAnimateHeight");
            animateHeight.setAccessible(true);
            Class<?> controller = Xp.findClass("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewControllerImpl", loader);
            Xp.hookAll(header, "setAnimateHeight", chain -> {
                View view = (View) chain.getThisObject();
                if (!eligible(view)) return chain.proceed();
                update(view.getRootView(), named(view, "mi_media_controls"));
                int requested = (Integer) chain.getArg(0);
                if (!matches(view) || currentPlan == null) return chain.proceed();
                if (requested == 0) {
                    animateHeader = null;
                    animating = false; animatedHeight = 0;
                    Object result = chain.proceed();
                    update(view.getRootView(), named(view, "mi_media_controls"));
                    restoreSeekAlpha();
                    return result;
                }
                fullAod = requested == nativeAod;
                animating = true;
                try { animatedHeight = (Integer) Xp.callMethod(view, "getActualHeight"); }
                catch (Throwable ignored) { animatedHeight = fullAod ? currentPlan.height : compactAod; }
                // Visibility follows the native physical room in both directions. Hiding at
                // request time made the first sleep frame differ from the same wake height.
                applySeekAlpha(CompactPlayerMetrics.seekAlpha(animatedHeight,
                        currentPlan.height, compactAod));
                int target = CompactPlayerMetrics.transitionHeight(requested, nativeNormal, nativeAod,
                        currentPlan.height, compactAod);
                // Preserve the original request if our scope exits before native completion.
                animateHeader = target != requested ? view : null;
                nativeTarget = requested; writtenTarget = target;
                // The native stack now animates toward the actual compact layout endpoint.
                return chain.proceed(new Object[]{target});
            });
            Xp.hookAll(header, "setActualHeight", chain -> {
                Object result = chain.proceed();
                View view = (View) chain.getThisObject();
                if (animating && eligible(view) && matches(view) && currentPlan != null) {
                    animatedHeight = (Integer) chain.getArg(0);
                    if (ready(background)) {
                        Lease bg = lease(background);
                        bg.apply(background, animatedHeight, bg.top, bg.bottom, bg.minimum);
                    }
                    applySeekAlpha(CompactPlayerMetrics.seekAlpha(animatedHeight,
                            currentPlan.height, compactAod));
                }
                return result;
            });
            Xp.hookAll(controller, "onFullAodStateChanged", chain -> {
                Object result = chain.proceed();
                Object holder = Xp.getObjectField(chain.getThisObject(), "holder");
                View player = holder == null ? null : (View) Xp.getObjectField(holder, "player");
                if (player != null && eligible(player)) {
                    fullAod = (Boolean) chain.getArg(0);
                    update(player.getRootView(), player);
                }
                return result;
            });
            Class<?> transition = Xp.findClass("com.android.systemui.statusbar.notification.fullaod.NotifiFullAodController$FullAodTransitionListener", loader);
            Xp.hookAll(transition, "onCancel", chain -> {
                Object result = chain.proceed();
                if (animating && card != null && eligible(card)) {
                    Object owner = Xp.getObjectField(chain.getThisObject(), "this$0");
                    // Native cancellation omits media completion. Settle only our active
                    // compact transition through the native current-scene completion path.
                    Xp.callMethod(owner, "updateMediaNotificationStyle");
                    Xp.callMethod(owner, "finishForMediaNotificationAnim");
                    animating = false; animatedHeight = 0;
                    restoreSeekAlpha();
                    if (card != null) update(card.getRootView(), card);
                }
                return result;
            });
            Class<?> seekType = Xp.findClass("miuix.miuixbasewidget.widget.HyperProgressSeekBar", loader);
            Xp.hookAll(seekType, "onDraw", chain -> {
                View view = (View) chain.getThisObject();
                Integer shift = seekDrawShifts.get(view);
                if (shift == null || shift <= 0 || !eligible(view)) return chain.proceed();
                // The high-end native shader ignores vertical padding. Move only its draw
                // coordinates, retaining the original seek size, thumb and touch rectangle.
                android.graphics.Canvas canvas = (android.graphics.Canvas) chain.getArg(0);
                int save = canvas.save();
                try {
                    canvas.translate(0f, -shift);
                    return chain.proceed();
                } finally { canvas.restoreToCount(save); }
            });
            installed = true;
        } catch (Throwable error) { Xp.w(Main.TAG + "compact player transition unavailable: " + error); }
    }

    private static boolean eligible(View view) {
        return installed && view != null && Main.keyguardLocked() && !Main.bouncerShown()
                && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway()
                && MediaCardRuntime.heightPercent(CompactPlayerMetrics.innerLandscape(
                    view.getRootView().getWidth(), view.getRootView().getHeight(),
                    view.getResources().getDisplayMetrics().density)) < 88;
    }

    private static boolean matches(View header) {
        for (View node = card; node != null; node = node.getParent() instanceof View ? (View) node.getParent() : null)
            if (node == header) return true;
        return false;
    }

    static void nativeLayoutApplied(Object owner) {
        Object holder = Xp.getObjectField(owner, "mediaViewHolder");
        View player = holder == null ? null : (View) Xp.getObjectField(holder, "player");
        if (player != null && player.isAttachedToWindow()) update(player.getRootView(), player);
    }

    static View currentCard() { return card; }
    static void restore() {
        restoreProperties();
        card = background = album = image = title = artist = seamless = action = seek = null;
        java.util.Arrays.fill(actionButtons, null);
        animating = false; animatedHeight = 0;
    }
    static String describe() { return "playerHeight=" + appliedPercent + "% leases=" + owned.size(); }
    private CompactPlayerRuntime() {}
}
