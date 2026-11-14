package com.zhuhuibin.hypermusiccover.duo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderNode;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/** Owned artwork sources and the CARD foreground; FULL is composed behind native controls. */
final class CoverCardLayer extends View implements Choreographer.FrameCallback {
    private static CoverCardLayer sView;
    private static volatile Prepared sPending;
    private static volatile int sGeneration;
    private static volatile CoverCardStyle sStyle = CoverCardStyle.defaults();
    private static volatile boolean sPlayingState;

    private static final class Prepared {
        final Bitmap art;
        final Bitmap aodBackdrop;
        final Bitmap fullBand;
        final float fullBias;
        final boolean cardComposition;
        final int generation;
        final int pushGeneration;
        final boolean temporaryThumbnail;
        final String trackKey;
        Prepared(Bitmap art, Bitmap aodBackdrop, Bitmap fullBand, float fullBias, boolean cardComposition,
                 int generation, int pushGeneration,
                 boolean temporaryThumbnail, String trackKey) {
            this.art = art;
            this.aodBackdrop = aodBackdrop;
            this.fullBand = fullBand;
            this.fullBias = fullBias;
            this.cardComposition = cardComposition;
            this.generation = generation;
            this.pushGeneration = pushGeneration;
            this.temporaryThumbnail = temporaryThumbnail;
            this.trackKey = trackKey;
        }
        void recycle() {
            art.recycle();
            if (aodBackdrop != null) aodBackdrop.recycle();
            if (fullBand != null) fullBand.recycle();
        }
    }

    private Prepared current, previous;
    private long changedAt;
    private long lastFrame;
    private boolean ticking;
    private float opacity;
    private final CardSpring scale = new CardSpring();
    private boolean playing = sPlayingState;
    private boolean exitWithCard;
    private CoverCardStyle style = sStyle;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF square = new RectF();
    private final RectF aodClockBounds = new RectF();
    private final RectF aodArtworkBounds = new RectF();
    private long aodClockOverlapSince;

    /** Latch only a real, sustained AOD collision; wake restores the user's page choice. */
    private boolean avoidAodClock(long nowNs, ClockCollapse.Phase phase, boolean compact, boolean placing) {
        // Not while the square is still flying to its AOD place: from a list scrolled over the
        // player it starts above the screen and crosses the clock on the way (2026-10-30).
        if (Main.screenOnCached() || phase != ClockCollapse.Phase.AOD || compact || placing
                || opacity <= 0.05f || current == null || Float.isNaN(drawX)
                || !ClockCollapse.clockBoundsOnScreen(aodClockBounds)) {
            aodClockOverlapSince = 0L;
            return false;
        }
        CoverMorphMotion.Box box = CoverMorphMotion.cardBox(drawX, drawY, drawSide,
                artworkScale() * (RISE_FROM + (1f - RISE_FROM) * rise), shownAspect());
        aodArtworkBounds.set(box.x, box.y, box.x + box.w, box.y + box.h);
        ClockCollapse.boundsOnScreen(this, aodArtworkBounds);
        boolean overlaps = AodArtworkPolicy.clockOverlapsArtwork(aodClockBounds.left,
                aodClockBounds.top, aodClockBounds.right, aodClockBounds.bottom,
                aodArtworkBounds.left, aodArtworkBounds.top,
                aodArtworkBounds.right, aodArtworkBounds.bottom);
        if (!overlaps) {
            aodClockOverlapSince = 0L;
            return false;
        }
        if (aodClockOverlapSince == 0L) aodClockOverlapSince = nowNs;
        if (nowNs - aodClockOverlapSince >= 120_000_000L) {
            Main.compactArtworkForAodClock();
            aodClockOverlapSince = 0L;
            return false;
        }
        return true;
    }

    /** Per-frame scratch, kept rather than allocated on every draw. */
    private final Path clipPath = new Path();
    private final int[] tmpLoc = new int[2];
    private ViewTreeObserver geometryObserver;
    private ViewTreeObserver.OnPreDrawListener geometryListener;
    private boolean lastLockEligible;
    private int lastWidth = -1, lastHeight = -1, lastTop = Integer.MIN_VALUE;
    private float lastClock = Float.NaN, lastMedia = Float.NaN;

    private final Wash wash;
    /** Dozed under the OEM's big clock and not yet back to a settled lock screen; see doFrame. */
    private boolean afterBigClock;
    /** 0..1, how far the square has grown out of RISE_FROM; 1 everywhere but the big clock. */
    private float rise = 1f;
    /** The size the square is uncovered at, under the big clock's retreating digits. */
    private static final float RISE_FROM = 0.92f;
    /** How far down the square the big clock has to reach for it to be fully covered. */
    private static final float REVEAL_SPAN = 0.8f;
    /**
     * Falling asleep, the wallpaper goes to black in ~200ms along (1 - t)^2 (read frame by frame
     * off a recording), and the square goes with it. It used to ease at the clock's response and
     * was still half lit when the doze took the layer away, so it hung over black and then cut.
     */
    private static final float SLEEP_FADE_S = 0.2f;
    /** When this fall began (frame time, 0 when not falling) and the opacity it began from. */
    private long fallAt;
    private float fallFrom;
    /**
     * Waking, the wallpaper comes back from black in ~350ms along 1 - (1 - t)^3, read off the
     * same recording. The square was lit in one frame at 3/4 while the screen was still dark.
     */
    private static final float WAKE_FADE_S = 0.35f;
    /** How lit the wallpaper is taken to be, 0..1: what a wake starts from. */
    private float lit = 1f;
    private long wakeAt;
    private float wakeFrom;

    private CoverCardLayer(Context context) {
        super(context);
        setTag("HMCBigCoverArtwork");
        wash = new Wash(context);
        setClickable(false);
        setFocusable(false);
    }

    static void attach(ViewGroup layer) {
        if (layer == null) return;
        final ViewGroup target = layer;
        Main.main().post(new Runnable() {
            @Override public void run() { attachNow(target); }
        });
    }

    private static void attachNow(ViewGroup layer) {
        if (!layer.isAttachedToWindow()) return;
        CoverCardLayer v = sView;
        if (v == null || v.getContext() != layer.getContext()) {
            if (v != null) {
                v.stop();
                if (v.getParent() instanceof ViewGroup) {
                    ((ViewGroup) v.getParent()).removeView(v);
                }
                if (v.wash.getParent() instanceof ViewGroup) {
                    ((ViewGroup) v.wash.getParent()).removeView(v.wash);
                }
                if (v.previous != null && v.previous != sPending) v.previous.recycle();
                if (v.current != null && v.current != sPending) v.current.recycle();
            }
            v = new CoverCardLayer(layer.getContext());
            v.scale.snap(sPlayingState ? CardSpring.PLAYING : CardSpring.PAUSED);
            sView = v;
        }
        if (v.getParent() != layer) {
            if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
            layer.addView(v, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        // Called on every entry; bringToFront lays the layer out again, so only when needed.
        if (layer.getChildAt(layer.getChildCount() - 1) != v) v.bringToFront();
        // Beside keyguard_root_view rather than inside it: the doze zooms that view to 0.95 and
        // its bounds clip after the zoom, so nothing under it reaches the screen edge - a
        // counter-scaled wash in the layer was measured still cut to 95%. Just below it keeps
        // the same stacking the layer gave. The layer is the fallback, zoom and all.
        ViewGroup host = layer;
        View below = v;
        View zoomed = zoomedRoot(layer);
        if (zoomed != null && zoomed.getParent() instanceof ViewGroup) {
            host = (ViewGroup) zoomed.getParent();
            below = zoomed;
        }
        if (v.wash.getParent() != host) {
            if (v.wash.getParent() instanceof ViewGroup) {
                ((ViewGroup) v.wash.getParent()).removeView(v.wash);
            }
            int index = host.indexOfChild(below);
            // Match the awake renderer's ordering: background -> user dim -> foreground.
            // The static AOD background must not bypass an already-created dim layer.
            for (int i = 0; i < index; i++) {
                if ("HMCWallpaperDim".equals(host.getChildAt(i).getTag())) { index = i; break; }
            }
            host.addView(v.wash, index, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        v.style = sStyle;
        v.playing = sPlayingState;
        v.watchGeometry(layer);
        v.adoptPending();
        v.start();
    }

    /** keyguard_root_view, the view the doze zooms, found upwards from the layer. */
    private static View zoomedRoot(View from) {
        int id = from.getResources().getIdentifier("keyguard_root_view", "id",
                "com.android.systemui");
        if (id == 0) return null;
        for (Object p = from; p instanceof View; p = ((View) p).getParent()) {
            if (((View) p).getId() == id) return (View) p;
        }
        return null;
    }

    private void watchGeometry(ViewGroup layer) {
        if (geometryObserver == layer.getViewTreeObserver() && geometryListener != null) return;
        unwatchGeometry();
        geometryObserver = layer.getViewTreeObserver();
        geometryListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() { android.os.Trace.beginSection("MC coverGeom"); try {
                CoverBackdrop.refreshVisibility();
                // This parent is also drawn behind the unlocked notification shade. The card's
                // animation stops when it settles, and ACTION_USER_PRESENT is not guaranteed to
                // arrive before the shade uses this layer. Enforce the lock-screen condition on
                // the parent's draw, even when no card frame has been requested.
                if (!layerVisible()) {
                    lastLockEligible = false;
                    hideOutsideKeyguard("parent pre-draw");
                    return true;
                }
                if (!lastLockEligible) {
                    lastLockEligible = true;
                    hiddenUnlocked = false;
                    start();
                }
                // The pad coming up or going moves nothing of ours, so nothing else asks for the
                // frames the blur eases over.
                if (!ticking && bouncerP != bouncerTarget()) start();
                followSwipeFade();
                // Every frame of the window while the wash is up: the doze zoom it has to undo
                // animates, and nothing else of ours is drawing frames through it.
                if (wash.getVisibility() == VISIBLE) wash.fit();
                int[] loc = tmpLoc;
                getLocationOnScreen(loc);
                float clock = ClockCollapse.contentBottomFor(CoverCardLayer.this);
                float media = ClockCollapse.unzoomY(CoverCardLayer.this, Main.coverCardMediaTop());
                if (lastWidth != getWidth() || lastHeight != getHeight() || lastTop != loc[1]
                        || Float.floatToIntBits(lastClock) != Float.floatToIntBits(clock)
                        || Float.floatToIntBits(lastMedia) != Float.floatToIntBits(media)) {
                    lastWidth = getWidth();
                    lastHeight = getHeight();
                    lastTop = loc[1];
                    lastClock = clock;
                    lastMedia = media;
                    // In this frame, not the next: the card has already been moved for it.
                    if (carries(ClockCollapse.phase()) && opacity > 0f) {
                        CoverCardStyle.Rect goal = placeNow();
                        if (goal != null) {
                            carry(goal, true);
                            invalidate();
                        }
                    }
                    start();
                }
                return true;
            } finally { android.os.Trace.endSection(); } }
        };
        geometryObserver.addOnPreDrawListener(geometryListener);
    }

    private void unwatchGeometry() {
        if (geometryObserver != null && geometryListener != null
                && geometryObserver.isAlive()) {
            geometryObserver.removeOnPreDrawListener(geometryListener);
        }
        geometryObserver = null;
        geometryListener = null;
        lastLockEligible = false;
        lastWidth = lastHeight = -1;
    }

    static void style(CoverCardStyle config) {
        sStyle = config;
        CoverCardLayer v = sView;
        if (v != null) {
            boolean shapeChanged = v.style.mode != config.mode;
            v.style = config;
            // Both shapes share generation ownership, first-frame handoff and unlock guards.
            if (v.getParent() instanceof ViewGroup) v.watchGeometry((ViewGroup) v.getParent());
            if (shapeChanged) {
                v.lockPlace = v.bigCoverPlace = v.restPlace = v.lastGoal = null;
                v.railScreenPlace = null;
            }
            v.start();
        }
    }

    /** Called on the existing art worker. Never retain the player's bitmap. */
    static void publish(Bitmap source) {
        publish(source, false);
    }

    static void publish(Bitmap source, boolean temporaryThumbnail) {
        publish(source, temporaryThumbnail, Main.sPushGen, Main.sTrackKey);
    }

    static void publish(Bitmap source, boolean temporaryThumbnail, int pushGeneration,
                        String expectedKey) {
        publish(source, temporaryThumbnail, pushGeneration, expectedKey, !fullScene());
    }

    static void publish(Bitmap source, boolean temporaryThumbnail, int pushGeneration,
                        String expectedKey, boolean cardComposition) {
        final int generation = ++sGeneration;
        final String trackKey = expectedKey == null ? Main.sTrackKey : expectedKey;
        if (source == null || source.isRecycled()) return;
        Bitmap readable = null, art = null, aodBackdrop = null, fullBand = null;
        final boolean full = Main.effectiveCoverStyle().mode == CoverCardStyle.FULL;
        final float bias = Main.sBias;
        try {
            readable = source.getConfig() == Bitmap.Config.HARDWARE
                    ? source.copy(Bitmap.Config.ARGB_8888, false) : source;
            int w = readable.getWidth(), h = readable.getHeight();
            // The artwork's own shape, cropped only past CoverCardStyle.MAX_ASPECT. It was
            // cropped to a square here, which cut the sides off a video's cover.
            float aspect = full ? w / (float) h : CoverCardStyle.aspect(w, h);
            int cw = Math.min(w, Math.round(h * aspect));
            int ch = Math.min(h, Math.round(w / aspect));
            Rect crop = new Rect((w - cw) / 2, (h - ch) / 2, (w + cw) / 2, (h + ch) / 2);
            float limit = full ? 1024f : 512f;
            float down = Math.min(1f, limit / Math.max(cw, ch));
            art = Bitmap.createBitmap(Math.max(1, Math.round(cw * down)),
                    Math.max(1, Math.round(ch * down)), Bitmap.Config.ARGB_8888);
            new Canvas(art).drawBitmap(readable, crop,
                    new Rect(0, 0, art.getWidth(), art.getHeight()),
                    new Paint(Paint.FILTER_BITMAP_FLAG));
            // Shade only the large cover's copy; wallpaper and AOD backdrops keep their brightness.
            if (temporaryThumbnail) new Canvas(art).drawColor(0x33000000);
            // One owned, generation-matched backdrop serves the local lock-screen layer and
            // the existing faint full-AOD wash. Some Flip builds have no keyguard image engine;
            // preparing locally cannot depend on that engine accepting a wallpaper upload.
            try {
                int screenW = Math.max(1, Main.screenWidth());
                int screenH = Math.max(1, Main.screenHeight());
                float fit = Math.min(1f, (Main.sFullBlur <= 0 ? 1280f : 768f)
                        / Math.max(screenW, screenH));
                int backdropW = Math.max(1, Math.round(screenW * fit));
                int backdropH = Math.max(1, Math.round(screenH * fit));
                aodBackdrop = full && !cardComposition
                        ? CoverCompose.composedBackdrop(readable, backdropW, backdropH, bias, Main.sFullBlur)
                        : CoverCompose.cardBackground(readable, backdropW, backdropH, Main.sFullBlur);
                if (full) {
                    // Keep the original full-width composition separate from its blurred fill.
                    // A native background can then retain the sharp band, and the animated mesh
                    // cannot cover it. The bitmap preserves aspect rather than the card's crop.
                    fullBand = CoverCompose.feathered(art, art.getWidth(), art.getHeight(),
                            CoverFullScenePolicy.feather(art.getWidth(), art.getHeight(), screenW));
                }
            } catch (Throwable t) {
                Xp.w("[MCCard] local/AOD backdrop preparation failed: " + t);
            }
            final Prepared p = new Prepared(art, aodBackdrop, fullBand, bias, cardComposition,
                    generation, pushGeneration,
                    temporaryThumbnail, trackKey);
            art = null;
            aodBackdrop = null;
            fullBand = null;
            final Prepared old = sPending;
            sPending = p;
            Main.main().post(new Runnable() {
                @Override public void run() {
                    CoverCardLayer v = sView;
                    // Decided here, on the thread that adopts: read from the worker, `current`
                    // could become `old` between the check and the recycle, and the next draw
                    // would throw inside SystemUI.
                    if (old != null && old != sPending && (v == null
                            || (old != v.current && old != v.previous))) old.recycle();
                    if (v == null) return;
                    // The first art has nothing to keep pace with. A swap waits for the
                    // wallpaper to start its own - see releaseHeld() - with a ceiling in case
                    // that word never comes. A page's composition change is already visible
                    // locally, so do not hold it on a wallpaper acknowledgement for 400ms.
                    Main.main().removeCallbacks(ADOPT_HELD);
                    if (v.current == null || v.isReadyRectangularUpgrade(sPending)
                            || sPending != null && v.current.cardComposition != sPending.cardComposition)
                        v.adoptPending();
                    else Main.main().postDelayed(ADOPT_HELD, HOLD_MAX_MS);
                }
            });
        } catch (Throwable t) {
            Xp.w("[MCCard] artwork preparation failed: " + t);
        } finally {
            if (readable != null && readable != source) readable.recycle();
            if (art != null) art.recycle();
            if (aodBackdrop != null) aodBackdrop.recycle();
            if (fullBand != null) fullBand.recycle();
        }
    }

    /** Longer than the 70-160ms the wallpaper was measured behind by, short of feeling stuck. */
    private static final long HOLD_MAX_MS = 400L;

    private static final Runnable ADOPT_HELD = new Runnable() {
        @Override public void run() {
            CoverCardLayer v = sView;
            if (v != null) v.adoptPending();
        }
    };

    /**
     * The blurred background under the square has started changing - the wallpaper process
     * says so (op wpart), or the video path put it up itself - so the square turns over with it.
     */
    static void releaseHeld() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Main.main().post(new Runnable() { @Override public void run() { releaseHeld(); } });
            return;
        }
        Main.main().removeCallbacks(ADOPT_HELD);
        ADOPT_HELD.run();
    }

    private static boolean layerVisible() {
        CoverBackdrop.Snapshot scene = CoverBackdrop.snapshot();
        return scene.locked && !scene.goingAway && (Main.coverCardVisible()
                || scene.cover && scene.surface == CoverBackdrop.SURFACE_FULL_AOD);
    }

    static void clear() {
        ++sGeneration;
        Main.main().post(new Runnable() {
            @Override public void run() {
                CoverCardLayer v = sView;
                if (v != null) v.start();
            }
        });
    }

    static void refresh() {
        CoverCardLayer v = sView;
        if (v != null) {
            v.start();
            v.invalidate();
        }
    }

    /** Resting/AOD poses belong to one window shape and must not survive a display handoff. */
    static void geometryChanged() {
        CoverCardLayer v = sView;
        if (v == null) return;
        v.lockPlace = v.bigCoverPlace = v.restPlace = v.lastGoal = v.aodUnshrunkGoal = null;
        v.railScreenPlace = null;
        v.railAodHolding = false;
        v.aodShrinkMedia = Float.NaN;
        v.aodShrinkSeconds = 0f;
        v.drawX = v.goalX = Float.NaN;
        v.lastWidth = v.lastHeight = -1;
        v.start();
    }

    static boolean hiddenUnlocked() {
        return sView != null && sView.hiddenUnlocked;
    }

    static void expandForAod(boolean fromThumbnail) {
        CoverCardLayer v = sView;
        if (v == null) return;
        // Grown out of the player's thumbnail only when that thumbnail was on screen. Asleep
        // straight from an app it was not, its box is stale, and the square popped out of a
        // corner on some sleeps and faded in place on others; it fades in place on all of them.
        v.aodExpandPending = !v.hiddenUnlocked && (fromThumbnail || v.opacity <= 0f);
        if (v.bigCoverPlace != null) v.lockPlace = v.bigCoverPlace;
        v.start();
    }

    static void hideNow() {
        CoverCardLayer v = sView;
        if (v == null) return;
        v.hideImmediately();
    }

    private void hideOutsideKeyguard(String from) {
        if (getVisibility() == VISIBLE) {
            Xp.log("[MCCard] hidden outside keyguard on " + from);
        }
        if (getVisibility() != GONE || ticking || opacity != 0f) hideImmediately();
        // Unlocked: the lit places belong to that lock screen's layout (an expanded player, say).
        // A sleep straight from an app must not hold them through its doze.
        lockPlace = null;
        bigCoverPlace = null;
        restPlace = null;
        lastGoal = null;
        railScreenPlace = null;
        railAodHolding = false;
        // A sleep from an app takes its new place, not the last lock screen's drawn size.
        drawX = goalX = Float.NaN;
        hiddenUnlocked = true;
    }

    /** Hidden by an unlock and not drawn on a lock screen since: no thumbnail is on screen. */
    private boolean hiddenUnlocked;

    /**
     * How far the PIN pad's blur has come over the square, 0..1.
     *
     * The OEM blurs the lock screen under the pad, but this layer is the keyguard's background
     * and is not among what it blurs, so the square stood sharp through the pad. It used to be
     * hidden for the pad instead, which cut it out as the pad came up and popped it back as the
     * pad went. It blurs itself now, eased with the pad - the same as LyricView.followBouncer.
     */
    private float bouncerP;
    private boolean skipWaiting;
    private boolean skipEffectActive;
    private boolean skipContracted;
    private float skipTreatment;
    private final RenderNode currentWaitNode = new RenderNode("Cover skip wait");
    private final RenderNode outgoingWaitNode = new RenderNode("Outgoing cover skip wait");
    private float renderedBlur = -1f;
    private final Runnable skipWaitTimeout = () -> finishSkipEffect();

    /** Arm the treatment for the shrink and rebound of a confirmed transport transition. */
    static void beginSkipWait() {
        beginSkipWait(Main.sTrackKey, Main.sTrackKey);
    }

    static void beginSkipWait(String requestedKey, String expectedTrackKey) {
        Main.main().post(() -> {
            CoverCardLayer v = sView;
            if (v == null || v.current == null || !Main.coverCardVisible()
                    || LockLyrics.wantsCompactArtwork()
                    || !Main.sameTrack(expectedTrackKey, Main.sTrackKey)
                    || !Main.sameTrack(requestedKey, v.current.trackKey)) return;
            v.skipWaiting = true;
            v.skipEffectActive = true;
            v.skipContracted = v.scale.value < CardSpring.PLAYING - 0.001f;
            v.removeCallbacks(v.skipWaitTimeout);
            v.postDelayed(v.skipWaitTimeout, 4000L);
            v.start();
        });
    }

    private void endSkipWait() {
        // Receiving artwork ends the wait, but the treatment follows the remaining rebound.
        skipWaiting = false;
        start();
        invalidate();
    }

    private void finishSkipEffect() {
        skipWaiting = false;
        skipEffectActive = false;
        removeCallbacks(skipWaitTimeout);
        start();
    }

    private boolean followSkipTreatment(float dt) {
        if (skipEffectActive) {
            skipTreatment = CardSpring.shrinkFraction(scale.value);
            return false;
        }
        // Ease out a cancelled or timed-out transition instead of cutting the effect.
        skipTreatment *= (float) Math.exp(-Math.max(0f, dt) / BOUNCER_TAU);
        if (skipTreatment < 0.01f) skipTreatment = 0f;
        return skipTreatment > 0f;
    }
    private static final float BOUNCER_BLUR_DP = 24f;
    /**
     * Time constant of the ease, in seconds. Short: the level is the pad's own fade already
     * (Main.bouncerLevel), so this only smooths it. At 80ms it trailed the OEM's blur both ways.
     */
    private static final float BOUNCER_TAU = 0.03f;

    private static float bouncerTarget() {
        return Main.bouncerLevel();
    }

    /** @return whether the blur is still moving, so the frames keep coming until it lands */
    private boolean followBouncer(float dt) {
        float want = bouncerTarget();
        // The first step of a frame has no dt; it still has to start moving.
        float k = dt <= 0f ? 0.25f : (float) (1.0 - Math.exp(-dt / BOUNCER_TAU));
        bouncerP += (want - bouncerP) * k;
        if (Math.abs(want - bouncerP) < 0.01f) bouncerP = want;
        float r = bouncerP * BOUNCER_BLUR_DP
                * getResources().getDisplayMetrics().density;
        if (Math.abs(r - renderedBlur) > 0.01f) {
            renderedBlur = r;
            setRenderEffect(r < 0.5f ? null : android.graphics.RenderEffect.createBlurEffect(
                    r, r, android.graphics.Shader.TileMode.DECAL));
        }
        return bouncerP != want;
    }

    /**
     * The swipe's fade, taken from the media card's.
     *
     * Swiping up, the OEM fades the keyguard's foreground through transitionAlpha on two
     * containers - shared_notification_container, which holds the media card, and
     * miui_keyguard_clock_container - and on nothing between this layer and the window, so the
     * square stood out bright and whole while the clock and the card went (`op alphasweep`,
     * 2026-09-24: both at 0.63 at the deepest, keyguard_background_layer's chain untouched).
     * Its dim and blur, what the OEM gives its own background views, was tried first and was
     * not it. Read every frame of the window, so it is the finger's own curve.
     *
     * Not in the doze - the square fades with the wallpaper there, see SLEEP_FADE_S. Under the
     * pad it goes as the pad comes, blurring on the way (followBouncer): kept blurred, it stood
     * on the PIN screen, where nothing else of the lock screen is (2026-11-01).
     */
    private View fadeSource;
    private static int sFadeSourceId = -1;

    private void followSwipeFade() {
        float a = 1f;
        if (ClockCollapse.phase() != ClockCollapse.Phase.AOD) {
            View src = fadeSource;
            if (src == null || !src.isAttachedToWindow()) {
                if (sFadeSourceId == -1) {
                    sFadeSourceId = getResources().getIdentifier("shared_notification_container",
                            "id", "com.android.systemui");
                }
                src = sFadeSourceId == 0 ? null : getRootView().findViewById(sFadeSourceId);
                fadeSource = src;
            }
            if (src != null) a = src.getTransitionAlpha();
            a = Math.min(a, 1f - bouncerP);
        }
        if (getTransitionAlpha() != a) setTransitionAlpha(a);
    }

    private void hideImmediately() {
        skipWaiting = false;
        skipEffectActive = false;
        skipTreatment = 0f;
        removeCallbacks(skipWaitTimeout);
        stop();
        opacity = 0f;
        exitWithCard = false;
        if (getVisibility() != GONE) setVisibility(GONE);
        wash.hideNow();
    }

    static void entering() {
        CoverCardLayer v = sView;
        if (v != null) v.exitWithCard = false;
    }

    static void leaving() {
        CoverCardLayer v = sView;
        if (v != null) v.exitWithCard = v.opacity > 0.05f
                && !LockLyrics.wantsCompactArtwork();
    }

    static void playback(boolean on) {
        sPlayingState = on;
        CoverCardLayer v = sView;
        if (v != null) {
            v.playing = on;
            // An invisible card has no scale transition to preserve. Place it at the correct
            // playback size before a later thumbnail morph first asks for its destination.
            if (v.opacity <= 0f && !CoverMorphLayer.cardSuppressed()) {
                v.scale.snap(on ? CardSpring.PLAYING : CardSpring.PAUSED);
            }
            v.start();
        }
    }

    /** The square's current art, for the morph to fly - null when it has none yet. */
    static Bitmap currentArt() {
        CoverCardLayer v = sView;
        Prepared p = v == null ? null : v.current;
        return p == null || p.art.isRecycled() ? null : p.art;
    }

    static Bitmap currentBackdrop() {
        CoverCardLayer v = sView;
        Prepared p = v == null ? null : v.current;
        return p == null || p.aodBackdrop == null || p.aodBackdrop.isRecycled() ? null : p.aodBackdrop;
    }

    /** The background arbiter updates the static renderer before committing any sampler. */
    static void refreshBackdrop(CoverBackdrop.Snapshot scene) {
        CoverCardLayer v = sView;
        if (v == null) return;
        boolean retained = CoverBackdrop.retains(ArtworkBackdropState.STILL, scene);
        boolean fullForeground = !scene.navigation && scene.surface == CoverBackdrop.SURFACE_FULL_AOD
                && fullSceneArtworkVisible();
        boolean on = scene.desiredRenderer == ArtworkBackdropState.STILL || retained || fullForeground;
        if (v.current == null || v.current.aodBackdrop == null
                || !v.wash.isAttachedToWindow() || v.wash.getRootView() != scene.window) {
            v.wash.hideNow();
        } else if (on) {
            v.wash.show(true);
            if (v.wash.lastBackdropGeneration != scene.generation) v.wash.invalidate();
        } else if (scene.backgroundWanted && scene.desiredRenderer == ArtworkBackdropState.MESH) {
            v.wash.show(false);
        } else v.wash.hideNow();
    }

    static int backdropGeneration() {
        CoverCardLayer v = sView;
        return v == null || v.current == null ? -1 : v.current.generation;
    }

    static boolean backdropTransitioning() {
        return sView != null && sView.previous != null;
    }

    /** The explicit landscape combined page owns its columns before they finish measuring. */
    static boolean fullScene() {
        return CoverFullScenePolicy.fullScene(Main.effectiveCoverStyle().mode,
                FoldMusicRuntime.combinedRequested());
    }

    static boolean fullSceneArtworkVisible() {
        if (!fullScene()) return false;
        // AOD keeps its existing artwork/clock handoff. On the lit lock screen FULL is a
        // background, so neither a notification list hiding the native clock root nor its
        // temporary compact-card flag may erase the clear band and leave only blurred fill.
        if (!Main.screenOnCached())
            return Main.coverCardVisible() && !LockLyrics.wantsCompactArtwork();
        boolean sceneVisible = (Main.coverModeOn() || LockHold.exitingFor(LockHold.Owner.COVER)
                        && ClockCollapse.phase() == ClockCollapse.Phase.EXIT) && Main.onKeyguardNow()
                && LockscreenSurfacePolicy.visible(Main.keyguardLocked(),
                        MiniPlayerScene.INSTANCE.getKeyguardGoingAway());
        return CoverFullScenePolicy.sharpBand(sceneVisible, LockLyrics.userWantsCompactArtwork());
    }

    static String fullSceneDescription() {
        CoverCardLayer v = sView;
        Prepared p = v == null ? null : v.current;
        return "ready=" + hasFullSceneArtwork() + " wanted=" + fullSceneArtworkVisible()
                + " lyricsPage=" + LockLyrics.requestedLyricsPage()
                + " playerOnly=" + LockLyrics.playerChosen()
                + " notificationCompact=" + LockLyrics.notificationCompact()
                + " source=" + (p == null ? "none" : p.art.getWidth() + "x" + p.art.getHeight())
                + " composedAsCard=" + (p != null && p.cardComposition);
    }

    static boolean hasFullSceneArtwork() {
        CoverCardLayer v = sView;
        return v != null && v.current != null && v.current.fullBand != null
                && !v.current.fullBand.isRecycled();
    }

    /** Release the thumbnail morph only after this track's actual full scene has reached a frame. */
    static boolean fullSceneHandoffReady() {
        CoverCardLayer v = sView;
        return fullScene() && hasFullSceneArtwork() && !v.current.cardComposition && v.previous == null
                && CoverPush.currentPush(v.current.pushGeneration, v.current.trackKey)
                && VideoBackdropMesh.fullSceneHandoffReady(v.current.generation);
    }

    /** Sharp full-width artwork belongs above either the mirrored blur or the dynamic mesh. */
    static boolean drawFullSceneArtwork(Canvas canvas, RectF bounds, Paint paint, float alpha) {
        CoverCardLayer v = sView;
        if (v == null || !hasFullSceneArtwork() || alpha <= 0f) return false;
        Prepared old = v.previous;
        boolean crossfade = old != null && old.fullBand != null && !old.fullBand.isRecycled();
        float fade = crossfade ? v.fadeFraction() : 1f;
        if (crossfade) drawFullBand(canvas, bounds, paint, old, alpha * (1f - fade));
        drawFullBand(canvas, bounds, paint, v.current, alpha * fade);
        paint.setAlpha(255);
        return true;
    }

    private static void drawFullBand(Canvas canvas, RectF bounds, Paint paint, Prepared source,
                                     float alpha) {
        Bitmap band = source.fullBand;
        CoverFullScenePolicy.Band layout = CoverFullScenePolicy.band(Math.round(bounds.width()),
                Math.round(bounds.height()), band.getWidth(), band.getHeight(), source.fullBias);
        if (layout == null) return;
        paint.setShader(null);
        paint.setAlpha(Math.max(0, Math.min(255, Math.round(255f * alpha))));
        canvas.drawBitmap(band, null, new RectF(bounds.left, bounds.top + layout.top,
                bounds.right, bounds.top + layout.top + layout.height), paint);
    }

    /** Draw only owned copies on the UI thread, sharing foreground track-change timing. */
    static boolean drawBackdrop(Canvas canvas, RectF bounds, Paint paint) {
        CoverCardLayer v = sView;
        Bitmap current = currentBackdrop();
        if (v == null || current == null) return false;
        paint.setShader(null);
        paint.setColor(0xFFFFFFFF);
        Prepared old = v.previous;
        Bitmap previous = old == null ? null : old.aodBackdrop;
        boolean crossfade = previous != null && !previous.isRecycled();
        paint.setAlpha(255);
        if (crossfade) canvas.drawBitmap(previous, null, bounds, paint);
        paint.setAlpha(crossfade ? Math.round(255f * v.fadeFraction()) : 255);
        canvas.drawBitmap(current, null, bounds, paint);
        paint.setAlpha(255);
        return true;
    }

    /** Whether a point on screen falls on the square as it was last drawn, shown and lit. */
    static boolean artworkAt(float x, float y) {
        if (fullScene()) return fullSceneArtworkVisible() && VideoBackdropMesh.artworkAt(x, y);
        CoverCardLayer v = sView;
        if (v == null || !v.isShown() || v.getAlpha() < 0.5f || v.square.isEmpty()) return false;
        int[] at = new int[2];
        v.getLocationOnScreen(at);
        return v.square.contains(x - at[0], y - at[1]);
    }

    /** For `op cardstate`. */
    static String describe() {
        CoverCardLayer v = sView;
        if (v == null) return "view=none";
        // The ancestors' scale and whether each one clips its children: the AOD wash is drawn
        // past this view's bounds, which only reaches the screen edge if nothing clips it.
        StringBuilder chain = new StringBuilder();
        for (Object p = v.getParent(); p instanceof ViewGroup; p = ((ViewGroup) p).getParent()) {
            ViewGroup g = (ViewGroup) p;
            String id = "?";
            try {
                if (g.getId() != View.NO_ID) id = g.getResources().getResourceEntryName(g.getId());
            } catch (Throwable ignored) {
            }
            chain.append(' ').append(id).append("[s=").append(g.getScaleX())
                    .append(g.getClipChildren() ? ",clip" : "").append(']');
        }
        Wash w = v.wash;
        int[] at = new int[2];
        w.getLocationOnScreen(at);
        chain.append(" wash[vis=").append(w.getVisibility() == VISIBLE)
                .append(" want=").append(w.want).append(" level=").append(w.level())
                .append(" ready=").append(v.current != null && v.current.aodBackdrop != null
                    && !v.current.aodBackdrop.isRecycled())
                .append(" draws=").append(w.backdropDraws)
                .append(" lastDraw=").append(w.lastBackdropDraw)
                .append(" drawAlpha=").append(w.lastBackdropAlpha)
                .append(" drawGeneration=").append(w.lastBackdropGeneration)
                .append(' ').append(w.getWidth()).append('x').append(w.getHeight())
                .append(" s=").append(w.getScaleX()).append('/').append(w.getScaleY())
                .append(" pivot=").append(w.getPivotX()).append(',').append(w.getPivotY())
                .append(" at=").append(at[0]).append(',').append(at[1])
                .append(" screen=").append(Main.screenWidth()).append('x')
                .append(Main.screenHeight()).append(']');
        return "aodPlace=[" + v.aodPlaceNote + "] view.playing=" + v.playing
                + " scale=" + v.scale.value
                + " skip=" + v.skipEffectActive + "/" + v.skipWaiting
                + " treatment=" + v.skipTreatment
                + " thumbnail=" + (v.current != null && v.current.temporaryThumbnail)
                + " ticking=" + v.ticking + " opacity=" + v.opacity
                + " attached=" + v.isAttachedToWindow()
                + " fade=" + v.getTransitionAlpha() + " chain=" + chain;
    }

    static float renderedScale(ViewGroup layer) {
        CoverCardLayer v = sView;
        return v != null && v.getParent() == layer ? v.artworkScale()
                : (sPlayingState ? CardSpring.PLAYING : CardSpring.PAUSED);
    }

    private float artworkScale() {
        // Immersive artwork reaches the content edges; the play spring cannot spill into rail.
        return style.mode == CoverCardStyle.FULL ? Math.min(1f, scale.value) : scale.value;
    }

    /** A hidden card is kept drawing internally until it can take the morph without a jump. */
    static boolean morphHandoffReady() {
        CoverCardLayer v = sView;
        if (v == null || v.current == null || v.getParent() == null) return false;
        float target = v.playing ? CardSpring.PLAYING : CardSpring.PAUSED;
        return v.opacity >= 0.995f && v.scale.atRest(target);
    }

    /** A quality reveal of the same rectangular cover need not wait for a background swap. */
    private boolean isReadyRectangularUpgrade(Prepared p) {
        return p != null && current != null && current.temporaryThumbnail
                && !p.temporaryThumbnail && Main.sameTrack(current.trackKey, p.trackKey)
                && Math.abs(aspectOf(p) - 1f) > 0.01f;
    }

    private void adoptPending() {
        Prepared p = sPending;
        if (p == null || p == current || p.generation != sGeneration
                || !CoverPush.currentPush(p.pushGeneration, p.trackKey)) return;
        boolean revealReady = isReadyRectangularUpgrade(p);
        if (previous != null) previous.recycle();
        previous = current;
        current = p;
        boolean newTrack = previous != null && !Main.sameTrack(previous.trackKey, p.trackKey);
        boolean flip = !fullScene() && Main.sCoverFlip && newTrack;
        if (flip) startFlip(CoverPush.recentSkipDir());
        // Reveal ready artwork promptly, but fade out the treatment rather than cutting it.
        // Turning over, the old art is kept till the turn is edge-on (doFrame lets it go).
        trackFadeMs = flip ? FLIP_HOLD_MS : revealReady ? 120L : TRACK_FADE_MS;
        flipSwap = flip;
        // The treatment follows the cover scale across the artwork swap, not its crossfade.
        if (newTrack) {
            endSkipWait();
        }
        ClockCollapse.refreshArtworkSize();
        changedAt = SystemClock.uptimeMillis();
        start();
    }

    private void start() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post(new Runnable() { @Override public void run() { start(); } });
            return;
        }
        if (ticking) return;
        ticking = true;
        lastFrame = 0L;
        // A temporary vote for panels that honour it. The spring is sampled on every VSYNC,
        // and the vote is removed as soon as it settles; some SystemUI windows still cap at 60Hz.
        setRequestedFrameRate(120f);
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void stop() {
        if (ticking) Choreographer.getInstance().removeFrameCallback(this);
        if (ticking) setRequestedFrameRate(0f);
        ticking = false;
        lastFrame = 0L;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // A detached/re-attached keyguard layer gets a new ViewTreeObserver. Keep the guard even
        // when SystemUI reuses this same card view for the shade.
        if (getParent() instanceof ViewGroup) {
            watchGeometry((ViewGroup) getParent());
        }
    }

    @Override protected void onDetachedFromWindow() {
        railScreenPlace = null;
        railAodHolding = false;
        stop();
        unwatchGeometry();
        super.onDetachedFromWindow();
    }

    /** Where the square is drawn, eased towards placeNow(); NaN until it has a place. */
    private float drawX = Float.NaN, drawY, drawSide;
    /** The goal as last seen, so the part of its move the media card made can be carried over. */
    private float goalX = Float.NaN, goalY, goalSide;
    /**
     * A goal move bigger than this share of the square in one frame is a jump - a rebuilt media
     * card - and is eased. Under a finger the card moves a few dozen px a frame.
     */
    private static final float CARRY_MAX = 0.25f;
    /** The last place the lit lock screen gave it - what the doze keeps. */
    private CoverCardStyle.Rect lockPlace;
    private CoverCardStyle.Rect bigCoverPlace;
    private CoverCardStyle.Rect lastGoal;
    private CoverCardStyle.Rect aodUnshrunkGoal;
    private float aodShrinkMedia = Float.NaN;
    private float aodShrinkSeconds;
    private static final float AOD_SHRINK_SETTLE_S = 0.15f;
    private boolean aodExpandPending;
    /**
     * The place of the landed cover look, Phase.ON only - what the big clock's fall and wake
     * keep. Not lockPlace: that one is only written on the lit, locked lock screen.
     */
    private CoverCardStyle.Rect restPlace;
    /** Screen-space rail artwork, unaffected by the OEM's AOD parent zoom and translation. */
    private FoldMusicLayout.Box railScreenPlace;
    private long railScreenRevision = -1;
    private boolean railAodHolding;

    /**
     * 0..1, how much of the square's place the big clock has left on its way up: 0 while the
     * digits still reach REVEAL_SPAN of the way down it, 1 once they have cleared its top. The square is held
     * at restPlace and grows and brightens with this, so it comes out from under the clock at
     * the clock's own pace - no gap before it, and no place of its own that it has to jump from.
     */
    private float reveal() {
        CoverCardStyle.Rect r = restPlace;
        float clock = ClockCollapse.contentBottomFor(this);
        if (r == null || Float.isNaN(clock) || r.side <= 0f) return 0f;
        int[] loc = tmpLoc;
        getLocationOnScreen(loc);
        // The big clock was measured reaching ~88% of the way down the square, not past it.
        float span = r.side * REVEAL_SPAN;
        float p = (r.y + span - (clock - loc[1])) / span;
        p = Math.max(0f, Math.min(1f, p));
        return p * p * (3f - 2f * p);
    }

    /**
     * The square's place from the live clock and media card, in this view's coordinates.
     *
     * Both as drawn, not as laid out: a swipe zooms the clock's and the card's containers and
     * not this layer, so a place taken from their layout sat still while they came in round it.
     */
    /** Shared with the moving artwork so its landing and the settled renderer use one box. */
    static CoverCardStyle.Rect place(View host, CoverCardStyle config,
                                     float clock, float media, float aspect) {
        if (FoldMusicRuntime.sideBySide()) return FoldMusicRuntime.artwork(host, aspect);
        CoverCardStyle.Rect placed = LockScreenRailRuntime.artwork(host, config, clock, media, aspect);
        return config.mode == CoverCardStyle.FULL ? placed
                : FoldMusicRuntime.capPortraitArtwork(host, placed);
    }

    private CoverCardStyle.Rect placeNow() {
        float clock = ClockCollapse.contentBottomFor(this);
        float media = ClockCollapse.unzoomY(this, Main.coverCardMediaTop());
        getLocationOnScreen(tmpLoc);
        return place(this, style, clock - tmpLoc[1], media - tmpLoc[1], shownAspect());
    }

    /**
     * Moves the drawn place one frame towards where the square belongs.
     *
     * Through the doze it keeps the lock screen's place: asked live, the media card's AOD stand-in
     * (70% of the screen) re-centred it, and it jumped 63px on the way in and back 11px on the
     * wake, while everything around it slid with the doze zoom. It sits inside that zoom, so
     * holding still in its own coordinates is exactly sliding with it. Any other change of
     * place - the wake landing, a rebuilt media card - is eased at the 缩放动画阻尼 response
     * instead of cut. A square that is not showing yet just takes its place.
     *
     * @return whether it is still on its way
     */
    private boolean followPlace(ClockCollapse.Phase phase, float dt, float response,
                                float target) {
        CoverCardStyle.Rect goal;
        if (phase != ClockCollapse.Phase.AOD || (afterBigClock && restPlace != null)) {
            aodShrinkSeconds = 0f;
            aodShrinkMedia = Float.NaN;
            aodUnshrunkGoal = null;
        }
        if (afterBigClock && restPlace != null) {
            // Placed live, the big clock's bottom pushes it small and low against the media card.
            goal = restPlace;
        } else if (phase == ClockCollapse.Phase.AOD) {
            // No lit place since the unlock: take one once and hold it, not the 70% stand-in's
            // re-derivation every frame of the doze zoom.
            if (lockPlace == null && bigCoverPlace == null) lockPlace = placeNow();
            CoverCardStyle.Rect held = Main.aodArtworkExpanded() && bigCoverPlace != null
                    ? bigCoverPlace : lockPlace;
            goal = held == null ? null : aodPlace(held);
            if (goal != null && goal.side < held.side - 0.5f) {
                // A focus card can rise for a few frames while the doze lays out, and the stack
                // glides down for ~400ms after a sleep from an app: only a card at rest shrinks it.
                float media = Main.liveMediaTop();
                aodShrinkSeconds = Math.abs(media - aodShrinkMedia) <= 1f ? aodShrinkSeconds + dt : 0f;
                aodShrinkMedia = media;
                if (aodShrinkSeconds < AOD_SHRINK_SETTLE_S) {
                    goal = aodUnshrunkGoal != null ? aodUnshrunkGoal : held;
                }
            } else {
                aodShrinkSeconds = 0f;
                aodUnshrunkGoal = goal;
            }
        } else {
            goal = placeNow();
            // An unlock shrinks the place frame by frame for 30ms before going-away is set (1106 to
            // 1050px), which a sleep from the home screen would then hold through the whole doze.
            // Only a place that held still since the last frame is remembered.
            boolean still = goal != null && lastGoal != null
                    && Math.abs(goal.side - lastGoal.side) <= 0.5f
                    && Math.abs(goal.y - lastGoal.y) <= 0.5f;
            lastGoal = goal;
            boolean settled = still && phase == ClockCollapse.Phase.ON
                    && AodArtworkPolicy.canRememberPose(
                    Main.keyguardLocked() && Main.onKeyguardNow(),
                    MiniPlayerScene.INSTANCE.getKeyguardGoingAway(), Main.screenOnCached(),
                    goal.y, goal.side, Float.NaN, getHeight());
            if (settled) lockPlace = goal;
            if (goal != null && phase == ClockCollapse.Phase.ON) {
                restPlace = goal;
                if (!LockLyrics.wantsCompactArtwork() && !Main.aodArtworkExpanded()
                        && !LockIslands.INSTANCE.notificationArtworkExpanded()
                        && settled) bigCoverPlace = goal;
            }
        }
        if (goal == null) {
            goalX = Float.NaN;
            return false;
        }
        if (!LockScreenRailRuntime.reflowing() || railScreenRevision != Main.screenGeometry().revision) {
            railScreenPlace = null;
            railAodHolding = false;
        }
        if (MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) railAodHolding = false;
        else if (MiniPlayerScene.INSTANCE.getAodActive() || !Main.screenOnCached()
                || phase == ClockCollapse.Phase.AOD) railAodHolding = true;
        else if (!LockScreenRailRuntime.holdGeometry()) railAodHolding = false;
        if (railAodHolding && LockScreenRailRuntime.reflowing()) {
            FoldMusicLayout.Box fallback = railScreenPlace != null ? railScreenPlace
                    : LockScreenRailRuntime.artworkOnScreen(this, goal);
            FoldMusicLayout.Box fitted = LockScreenRailRuntime.aodArtwork(fallback,
                    ClockCollapse.contentBottomDrawn(), Main.liveMediaTop(), shownAspect());
            CoverCardStyle.Rect fixed = LockScreenRailRuntime.artworkFromScreen(this, fitted);
            fixed = FoldMusicRuntime.capPortraitArtwork(this, fixed);
            if (fixed != null) {
                // Compensate on the same frame as the parent animation. Easing this inverse
                // transform would recreate the drift while the AOD's zoom is running.
                drawX = fixed.x; drawY = fixed.y; drawSide = fixed.side;
                goalX = fixed.x; goalY = fixed.y; goalSide = fixed.side;
                aodPlaceNote = "rail screen frame fitted to actual AOD room";
                return false;
            }
        }
        if (style.mode == CoverCardStyle.CARD)
            goal = FoldMusicRuntime.capPortraitArtwork(this, goal);
        if (goal == null) return false;
        carry(goal, carries(phase));
        if (Float.isNaN(drawX) || opacity <= 0f) {
            drawX = goal.x;
            drawY = goal.y;
            drawSide = goal.side;
            rememberRailScreenPlace();
            return false;
        }
        float k = Math.min(1f, dt * 3f / response);
        drawX += (goal.x - drawX) * k;
        drawY += (goal.y - drawY) * k;
        drawSide += (goal.side - drawSide) * k;
        boolean moving = Math.abs(goal.x - drawX) > 0.5f || Math.abs(goal.y - drawY) > 0.5f
                || Math.abs(goal.side - drawSide) > 0.5f;
        if (!moving) {
            drawX = goal.x;
            drawY = goal.y;
            drawSide = goal.side;
        }
        rememberRailScreenPlace();
        return moving;
    }

    private void rememberRailScreenPlace() {
        if (!Main.screenOnCached() || !Main.keyguardLocked() || LockScreenRailRuntime.holdGeometry()) return;
        railScreenPlace = LockScreenRailRuntime.artworkOnScreen(this,
                new CoverCardStyle.Rect(drawX, drawY, drawSide));
        railScreenRevision = Main.screenGeometry().revision;
    }

    /**
     * How long into a doze the place keeps being re-derived. aodPlace() reads things that are
     * still moving when the doze starts - the zoom (529ms to settle), the held clock, the compact
     * media card swapping in - and the loop stops the first frame the square has caught up with
     * its goal. It stopped at k=0.967 with the clock 32px from where it came to rest, and the
     * gaps came out 158/131 instead of equal (2026-09-24).
     */
    private static final long AOD_SETTLE_NS = 1_500_000_000L;
    /** The gap kept above and below a held place shrunk to fit the doze. ~50px lit on the 17 Pro. */
    private static final float AOD_FIT_GAP_DP = 16f;
    /** When the current doze began, 0 outside one. */
    private long aodSince;

    /** What aodPlace() last worked from, for `op cardstate`. */
    private String aodPlaceNote = "none";

    /**
     * The held place, moved to sit centred between the doze's own clock and its own media card.
     *
     * Held alone it rode the doze zoom exactly, and still came out off-centre, because the two
     * things it sits between do not ride that zoom: the held clock sits higher than the zoomed
     * lock screen's would (59px) and the AOD swaps in a compact media card whose top is lower
     * (135px). Measured 2026-09-24: gaps 48/50px lit, 105/183px in the AOD. Size and x are kept
     * - only the height it sits at follows the doze - and the move goes through followPlace's
     * easing, so it slides there with the dim rather than jumping.
     *
     * The live media card is read here, not coverCardMediaTop()'s stand-in: that one exists for
     * the place the doze is not allowed to re-derive. Anything unreadable, or no room, keeps held.
     */
    private CoverCardStyle.Rect aodPlace(CoverCardStyle.Rect held) {
        // Where the dozing clock is drawn, not the pose: inside the doze the two part by the
        // OEM's zoom and shifts (24px, which was the whole of what was left off-centre).
        float clock = ClockCollapse.contentBottomDrawn();
        float media = Main.liveMediaTop();
        float k = ancestorScale();
        if (!Float.isFinite(clock) || clock < 0f || !Float.isFinite(media)
                || k <= 0f || media <= clock) {
            aodPlaceNote = "held clock=" + clock + " media=" + media;
            return held;
        }
        int[] loc = tmpLoc;
        getLocationOnScreen(loc);
        // Screen distances into this view's own coordinates, which the zoom above it scales.
        float top = (clock - loc[1]) / k, bottom = (media - loc[1]) / k;
        float artHeight = held.side * Math.min(1f, 1f / shownAspect());
        float room = bottom - top - artHeight;
        if (room < 0f) {
            // The card came up after the place was held - the stack un-sinking as the print
            // comes back for the doze (Main.sFodFace). Shrunk to the room left, with the lit
            // screen's gap on each side, rather than drawn across the card.
            float gap = AOD_FIT_GAP_DP * getResources().getDisplayMetrics().density / k;
            float fit = bottom - top - 2f * gap;
            if (fit <= 0f) {
                aodPlaceNote = "no room clock=" + clock + " media=" + media;
                return held;
            }
            float side = fit / Math.min(1f, 1f / shownAspect());
            aodPlaceNote = "shrunk clock=" + clock + " media=" + media + " side="
                    + Math.round(held.side) + "->" + Math.round(side);
            return new CoverCardStyle.Rect(held.x + (held.side - side) / 2f, top + gap
                    - (side - fit) / 2f, side);
        }
        float y = top + room / 2f - (held.side - artHeight) / 2f;
        aodPlaceNote = "centred clock=" + clock + " media=" + media + " k=" + k
                + " y=" + Math.round(held.y) + "->" + Math.round(y) + " side=" + Math.round(held.side);
        return new CoverCardStyle.Rect(held.x, y, held.side);
    }

    /** The product of every ancestor's vertical scale - the doze's 0.95 among them. */
    private float ancestorScale() {
        float k = 1f;
        for (Object p = getParent(); p instanceof View; p = ((View) p).getParent()) {
            k *= ((View) p).getScaleY();
        }
        return k;
    }

    /**
     * On the settled lock screen the goal only moves when the media card does - dragged, or
     * springing back after the finger lets go - and the square has to keep off it frame for
     * frame. Eased there, it lagged its own easing plus a frame and ran over the card.
     */
    private boolean carries(ClockCollapse.Phase phase) {
        return phase == ClockCollapse.Phase.ON && !afterBigClock;
    }

    /** Moves the drawn place by however far the goal moved since it was last seen. */
    private void carry(CoverCardStyle.Rect goal, boolean follow) {
        if (follow && !Float.isNaN(goalX) && !Float.isNaN(drawX)) {
            float dy = goal.y - goalY, ds = goal.side - goalSide;
            if (Math.abs(dy) + Math.abs(ds) < goal.side * CARRY_MAX) {
                drawX += goal.x - goalX;
                drawY += dy;
                drawSide += ds;
            }
        }
        goalX = goal.x;
        goalY = goal.y;
        goalSide = goal.side;
    }

    @Override public void doFrame(long nowNs) { android.os.Trace.beginSection("MC coverCardFrame"); try {
        CoverGlow.kick();
        if (!ticking || !isAttachedToWindow()) { stop(); return; }
        float dt = lastFrame == 0L ? 1f / 60f
                : Math.min(0.05f, Math.max(0f, (nowNs - lastFrame) / 1e9f));
        lastFrame = nowNs;
        boolean blurring = followBouncer(dt);
        ClockCollapse.Phase phase = ClockCollapse.phase();
        if (phase != ClockCollapse.Phase.AOD) aodSince = 0L;
        else if (aodSince == 0L) aodSince = nowNs;
        boolean inAod = Main.coverCardInAod();
        boolean foregroundCard = style.mode == CoverCardStyle.CARD || FoldMusicRuntime.sideBySide();
        boolean visible = foregroundCard && Main.coverCardVisible();
        if (aodExpandPending && inAod && visible) {
            aodExpandPending = false;
            CoverMorphMotion.Box thumbnail = Main.coverMorphThumbnail();
            if (thumbnail != null) {
                getLocationOnScreen(tmpLoc);
                drawX = thumbnail.x - tmpLoc[0];
                drawY = thumbnail.y - tmpLoc[1];
                drawSide = Math.max(thumbnail.w, thumbnail.h);
                goalX = Float.NaN;
                opacity = 0.01f;
                rise = 1f;
            }
        }
        // Lyrics and the lyricless compact page both keep artwork in the media player.
        boolean compactArtwork = LockLyrics.wantsCompactArtwork();
        // A doze under the OEM's big clock hides the square. Its wake holds the square at the
        // place the landed lock screen gave it and lets the collapsing clock uncover it - see
        // reveal(). Shown at once it sat full size across the big digits, and placed live it was
        // squeezed small and low under the shrinking clock, then grew back.
        //
        // The fall counts, not only the doze: pressed again before the big clock has settled, the
        // clock never reaches Phase.AOD - it goes EXIT (into the AOD) straight back to ENTER.
        if (!ClockCollapse.aodHeld() && (phase == ClockCollapse.Phase.AOD
                || (phase == ClockCollapse.Phase.EXIT && !ClockCollapse.exiting()))) {
            afterBigClock = true;
        } else if (phase == ClockCollapse.Phase.ON || phase == ClockCollapse.Phase.OFF) {
            afterBigClock = false;
        }
        boolean bigWake = afterBigClock && phase == ClockCollapse.Phase.ENTER;
        // The other half: the clock growing into the big one covers the square as it comes down.
        boolean bigFall = afterBigClock && phase == ClockCollapse.Phase.EXIT
                && !ClockCollapse.exiting();
        boolean underBig = bigWake || bigFall;
        float target = visible && current != null && (!afterBigClock || underBig)
                && (phase == ClockCollapse.Phase.EXIT && !bigFall ? exitWithCard : !compactArtwork)
                ? (inAod ? 1f : underBig ? reveal() : Main.cardProgress()) : 0f;
        float response = Math.max(0.18f, Main.sClockResponse);
        // Tied to the clock's own flight on the lit screen, and to the big clock's edge on the way
        // into and out of its doze. Otherwise falling asleep it eases.
        if (!bigFall) fallAt = 0L;
        if (!bigWake) wakeAt = 0L;
        if (!afterBigClock) lit = 1f;
        else if (phase == ClockCollapse.Phase.AOD) lit = 0f;
        if (bigFall) {
            // Out with the wallpaper as well, for a big clock that stops short of covering it.
            if (fallAt == 0L) {
                fallAt = nowNs;
                fallFrom = opacity;
            }
            float u = Math.min(1f, (nowNs - fallAt) / 1e9f / SLEEP_FADE_S);
            lit = (1f - u) * (1f - u);
            opacity = Math.min(target, fallFrom * lit);
        } else if (bigWake) {
            // And back in with it - from wherever a fall pressed short of the doze had left it.
            if (wakeAt == 0L) {
                wakeAt = nowNs;
                wakeFrom = lit;
            }
            float u = 1f - Math.min(1f, (nowNs - wakeAt) / 1e9f / WAKE_FADE_S);
            lit = wakeFrom + (1f - wakeFrom) * (1f - u * u * u);
            opacity = target * lit;
        } else if ((inAod && compactArtwork) || phase == ClockCollapse.Phase.ENTER
                || (phase == ClockCollapse.Phase.EXIT && Main.screenOn())) {
            opacity = target;
        } else {
            opacity += (target - opacity) * Math.min(1f, dt * 3f / response);
        }
        // Grows with the reveal on the way up, and shrinks under the digits on the way down.
        if (afterBigClock) rise = underBig ? target : Math.min(rise, opacity);
        else if (opacity <= 0f) rise = 1f;
        else rise += (1f - rise) * Math.min(1f, dt * 3f / response);
        if (rise > 0.998f) rise = 1f;
        if (Math.abs(opacity - target) < 0.002f) opacity = target;
        float scaleTarget = playing ? CardSpring.PLAYING : CardSpring.PAUSED;
        if (phase == ClockCollapse.Phase.AOD) scale.snap(scaleTarget);
        // At the response the app's 缩放动画阻尼 sets, so the card keeps time with the clock,
        // the wallpaper and the morph instead of running on a clock of its own.
        else scale.step(scaleTarget, dt, Main.sClockResponse);
        if (skipEffectActive && scale.value < CardSpring.PLAYING - 0.001f) {
            skipContracted = true;
        }
        // Finish at the first return to full size, so the spring's later bounce cannot reblur.
        if (skipEffectActive && (phase == ClockCollapse.Phase.AOD
                || ((!skipWaiting || skipContracted) && playing
                    && scale.value >= CardSpring.PLAYING - 0.001f))) {
            finishSkipEffect();
        }
        boolean treating = followSkipTreatment(dt);
        boolean placing = followPlace(phase, dt, response, target);
        boolean checkingClock = avoidAodClock(nowNs, phase, compactArtwork, placing);
        if (phase == ClockCollapse.Phase.AOD) {
            flipAngle = flipTarget;
            flipVelocity = 0f;
        }
        boolean turning = stepFlip(dt);
        if (previous != null && (phase == ClockCollapse.Phase.AOD
                || (flipSwap && !flipShowsPrevious())
                || SystemClock.uptimeMillis() - changedAt > trackFadeMs)) {
            previous.recycle();
            previous = null;
        }
        CoverBackdrop.refreshVisibility();
        int visibility = opacity > 0f || target > 0f ? VISIBLE : GONE;
        if (getVisibility() != visibility) setVisibility(visibility);
        if (visibility == VISIBLE) invalidate();
        boolean settling = checkingClock || blurring || treating || turning || Math.abs(target - opacity) > 0.001f
                || (phase != ClockCollapse.Phase.AOD && !scale.atRest(scaleTarget))
                || previous != null || placing || (rise < 1f && opacity > 0f)
                || (aodSince != 0L && nowNs - aodSince < AOD_SETTLE_NS)
                // Still waking from the big clock: keep looking until the clock has landed.
                || (afterBigClock && phase != ClockCollapse.Phase.AOD);
        if (settling) {
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            stop();
        }
    } finally { android.os.Trace.endSection(); } }

    @Override protected void onDraw(Canvas canvas) {
        // Last line of defence if SystemUI replaced the parent's observer during a transition.
        if (!layerVisible()) {
            lastLockEligible = false;
            hideOutsideKeyguard("card draw");
            return;
        }
        if (current == null || fullScene()
                || style.mode == CoverCardStyle.FULL && !FoldMusicRuntime.sideBySide()) return;
        // A doze frame may draw before the queued animation frame clears a stale lock-screen
        // opacity. The lyric page must never show the square underneath it in the full AOD.
        if (opacity <= 0f || CoverMorphLayer.cardSuppressed()
                || (Main.coverCardInAod() && LockLyrics.wantsCompactArtwork())) return;
        float density = getResources().getDisplayMetrics().density;
        if (Float.isNaN(drawX)) return;
        CoverMorphMotion.Box actual = CoverMorphMotion.cardBox(drawX, drawY,
                drawSide, artworkScale() * (RISE_FROM + (1f - RISE_FROM) * rise), shownAspect());
        square.set(actual.x, actual.y, actual.x + actual.w, actual.y + actual.h);
        float radius = style.radius(Math.min(actual.w, actual.h));
        int turned = canvas.save();
        applyFlip(canvas, square.centerX(), square.centerY());
        // The shadow is keyed on the corner at the RESTING side, not the moving one - see
        // CoverCardStyle.cornerShare, and the note on shadowTile below.
        if (style.mode == CoverCardStyle.CARD)
            drawShadow(canvas, square, style.cornerShare(drawSide), opacity, paint);
        int save = canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(square, radius, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        if (flipSwap) {
            // One side at a time: the old art till it is edge-on, the new one after.
            boolean old = flipShowsPrevious();
            drawWaitingArt(canvas, old ? previous : current, 1f, skipTreatment,
                    old ? outgoingWaitNode : currentWaitNode);
        } else {
            drawWaitingArt(canvas, previous, 1f, skipTreatment, outgoingWaitNode);
            drawWaitingArt(canvas, current, fadeFraction(), skipTreatment, currentWaitNode);
        }
        canvas.restoreToCount(save);
        if (style.mode == CoverCardStyle.CARD) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(density);
            paint.setColor(0x40FFFFFF);
            paint.setAlpha(Math.round(opacity * 64f));
            canvas.drawRoundRect(square, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        canvas.restoreToCount(turned);
    }

    /**
     * A soft drop shadow under the square, shared with CoverMorphLayer so the hand-over does not
     * change it. Blurred and pulled in from the edges: an unblurred copy offset downwards reads
     * as a dark slab along the bottom edge rather than as a shadow.
     *
     * Drawn from one small pre-blurred tile, scaled to the square. It was a BlurMaskFilter on the
     * live rect, and a rect that changes size every frame - the morph, the play/pause spring - is
     * a fresh GPU blur every frame: the frames at the start of a tap toggle measured 15-18ms of
     * GPU against an 8.3ms budget, on top of the wallpaper's own crossfade. Scaled, the shadow
     * keeps its proportions to the square: at a 300dp square it is the 16dp blur, 6dp inset and
     * 8dp drop it was drawn with before.
     */
    static void drawShadow(Canvas canvas, RectF box, float corner, float strength, Paint paint) {
        if (strength <= 0f || box.width() <= 0f || box.height() <= 0f) return;
        Bitmap tile = shadowTile(corner);
        float shortSide = Math.min(box.width(), box.height());
        float k = shortSide / SHADOW_UNIT;
        float pad = SHADOW_PAD * k, drop = SHADOW_DROP * shortSide;
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF000000);
        paint.setAlpha(Math.round(strength * 90f));
        // Nine slices rather than one stretch, so a card that is not square keeps the corners and
        // the fall-off of a square one: the quarters at the short side's scale, and the tile's
        // middle row and column - the shadow along a straight edge - stretched across the rest.
        int t = tile.getWidth(), half = t / 2;
        int[] sx = {0, half, t - half, t};
        float q = half * k;
        float l = box.left - pad, r = box.right + pad;
        float top = box.top - pad + drop, bottom = box.bottom + pad + drop;
        float[] dx = {l, Math.min(l + q, (l + r) * 0.5f), Math.max(r - q, (l + r) * 0.5f), r};
        float[] dy = {top, Math.min(top + q, (top + bottom) * 0.5f),
                Math.max(bottom - q, (top + bottom) * 0.5f), bottom};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (dx[j + 1] - dx[j] <= 0f || dy[i + 1] - dy[i] <= 0f) continue;
                sShadowSrc.set(sx[j], sx[i], sx[j + 1], sx[i + 1]);
                sShadowDst.set(dx[j], dy[i], dx[j + 1], dy[i + 1]);
                canvas.drawBitmap(tile, sShadowSrc, sShadowDst, paint);
            }
        }
    }

    /** The tile's square, in its own pixels; the ratios below are the old dp values over 300dp. */
    private static final float SHADOW_UNIT = 200f;
    private static final float SHADOW_BLUR = SHADOW_UNIT * 16f / 300f;
    private static final float SHADOW_INSET = SHADOW_UNIT * 6f / 300f;
    private static final float SHADOW_DROP = 8f / 300f;
    /** Room round the square for the blur to fall off in. */
    private static final float SHADOW_PAD = SHADOW_BLUR * 2f;
    private static Bitmap sShadowTile;
    /** The corner setting, in whole percent, sShadowTile was drawn with. */
    private static int sShadowCorner = -1;
    private static final RectF sShadowDst = new RectF();
    private static final Rect sShadowSrc = new Rect();

    /**
     * Drawn at the card's corner setting, not the live radius: the morph eases the radius every
     * frame, and a tile per frame would be the per-frame blur this replaced. The shadow only
     * shows as the morph lands on the card, where the two agree.
     */
    private static Bitmap shadowTile(float corner) {
        int key = Math.round(corner * 100f);
        Bitmap t = sShadowTile;
        if (t != null && !t.isRecycled() && key == sShadowCorner) return t;
        int side = Math.round(SHADOW_UNIT + 2f * SHADOW_PAD);
        t = Bitmap.createBitmap(side, side, Bitmap.Config.ALPHA_8);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF000000);
        p.setMaskFilter(new BlurMaskFilter(SHADOW_BLUR, BlurMaskFilter.Blur.NORMAL));
        float lo = SHADOW_PAD + SHADOW_INSET, hi = SHADOW_PAD + SHADOW_UNIT - SHADOW_INSET;
        float r = (hi - lo) * 0.5f * key / 100f;
        new Canvas(t).drawRoundRect(lo, lo, hi, hi, r, r, p);
        sShadowTile = t;
        sShadowCorner = key;
        return t;
    }

    /**
     * The full-screen AOD's static artwork background, beside the square.
     *
     * The doze scales keyguard_root_view to 0.95 (`op cardstate` lists the chain). Drawn inside
     * it, the wash came out 5% short and framed by the dark wallpaper, and neither drawing past
     * the card layer's bounds nor scaling this view back out by the inverse of the zoom got past
     * that: the second one was measured landing on the full screen at (0,0) and still cut to
     * 95%. So it lives beside keyguard_root_view, just below it (see attachNow), and fit() is
     * left to undo whatever zoom is still above it - normally none. The zoom itself is right and
     * stays: the square rides it with the rest of the lock screen.
     */
    private static final class Wash extends View {
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final int[] loc = new int[2];
        private final RectF bounds = new RectF();

        Wash(Context context) {
            super(context);
            setTag("HMCCoverWash");
            setClickable(false);
            setFocusable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            setVisibility(GONE);
        }

        /**
         * In over the doze's own dimming and out over the wallpaper's brightening. Put up and
         * taken down in one frame it flashed: brighter for a moment going into the AOD, while the
         * wallpaper had not dimmed yet, and darker for ~100ms coming out, while it was still dim.
         */
        private static final long IN_MS = 500L, OUT_MS = 250L;
        private boolean want;
        private float from;
        private long changedAt;
        private long backdropDraws, lastBackdropDraw;
        private int lastBackdropAlpha, lastBackdropGeneration = -1;

        /** 0..1, how much of the wash is up right now. */
        private float level() {
            float t = Math.min(1f, (SystemClock.uptimeMillis() - changedAt)
                    / (float) (want ? IN_MS : OUT_MS));
            float e = t * t * (3f - 2f * t);
            return from + ((want ? 1f : 0f) - from) * e;
        }

        void show(boolean on) {
            boolean changed = false;
            if (on != want) {
                from = getVisibility() == VISIBLE ? level() : 0f;
                want = on;
                changedAt = SystemClock.uptimeMillis();
                changed = true;
            }
            if (on && getVisibility() != VISIBLE) { setVisibility(VISIBLE); changed = true; }
            if (getVisibility() == VISIBLE) {
                if (on) fit();
                if (changed) invalidate();
            }
        }

        /** Unlocked, or anywhere else the wash has no business: gone at once. */
        void hideNow() {
            want = false;
            from = 0f;
            lastBackdropAlpha = 0;
            lastBackdropGeneration = -1;
            if (getVisibility() != GONE) setVisibility(GONE);
            CoverBackdrop.rendererHidden(ArtworkBackdropState.STILL);
        }

        /** Scales this view so that its bounds land on the whole display. */
        void fit() {
            if (!(getParent() instanceof View) || getWidth() <= 0 || getHeight() <= 0) return;
            View parent = (View) getParent();
            float sx = 1f, sy = 1f;
            for (Object p = parent; p instanceof View; p = ((View) p).getParent()) {
                sx *= ((View) p).getScaleX();
                sy *= ((View) p).getScaleY();
            }
            int sw = Main.screenWidth(), sh = Main.screenHeight();
            if (sw <= 0 || sh <= 0 || !(sx > 0f) || !(sy > 0f)) return;
            parent.getLocationOnScreen(loc);
            fitAxis(true, loc[0], sx, sw / (getWidth() * sx));
            fitAxis(false, loc[1], sy, sh / (getHeight() * sy));
        }

        /**
         * On screen, x in this view lands at origin + s * (pivot + (x - pivot) * k). Solving for
         * x = 0 landing on 0 gives the pivot; k is what makes the far edge land on the far edge.
         */
        private void fitAxis(boolean x, float origin, float s, float k) {
            float pivot = 0f;
            if (Math.abs(1f - k) > 1e-4f) pivot = -origin / (s * (1f - k));
            else k = 1f;
            if (x) {
                if (getScaleX() != k) setScaleX(k);
                if (getPivotX() != pivot) setPivotX(pivot);
            } else {
                if (getScaleY() != k) setScaleY(k);
                if (getPivotY() != pivot) setPivotY(pivot);
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            CoverBackdrop.Snapshot scene = CoverBackdrop.snapshot();
            boolean retained = CoverBackdrop.retains(ArtworkBackdropState.STILL, scene);
            boolean foreground = !scene.navigation && scene.surface == CoverBackdrop.SURFACE_FULL_AOD
                    && fullSceneArtworkVisible();
            boolean fadingOut = scene.backgroundWanted && !want
                    && scene.desiredRenderer == ArtworkBackdropState.MESH;
            if ((!scene.backgroundWanted && !foreground) || scene.window != getRootView()
                    || (scene.desiredRenderer != ArtworkBackdropState.STILL
                        && !retained && !foreground && !fadingOut)) {
                hideNow();
                return;
            }
            CoverCardLayer v = sView;
            Prepared p = v == null ? null : v.current;
            float level = level();
            boolean moving = SystemClock.uptimeMillis() - changedAt < (want ? IN_MS : OUT_MS);
            if (!want && !moving) {
                // Faded all the way out: off the draw list until it is wanted again.
                post(new Runnable() {
                    @Override public void run() { if (!want) setVisibility(GONE); }
                });
                return;
            }
            if (moving) postInvalidateOnAnimation();
            if (p == null || p.aodBackdrop == null || p.aodBackdrop.isRecycled()) return;
            bounds.set(0f, 0f, getWidth(), getHeight());
            // The system AOD and the existing user dim layer own darkening. Do not turn
            // the chosen blur into a separate quarter-opacity tint when entering AOD.
            int alpha = Math.round(255f * level);
            int saved = canvas.saveLayerAlpha(bounds, alpha);
            paint.setAlpha(255);
            if (scene.backgroundWanted) {
                canvas.drawBitmap(p.aodBackdrop, null, bounds, paint);
                // Record only when pixels enter the display list; VISIBLE alone is not proof.
                backdropDraws++;
                lastBackdropDraw = SystemClock.uptimeMillis();
                lastBackdropAlpha = alpha;
                lastBackdropGeneration = p.generation;
                CoverBackdrop.drawn(ArtworkBackdropState.STILL, scene, this, p.generation, alpha / 255f);
            }
            if (foreground) drawFullSceneArtwork(canvas, bounds, paint, 1f);
            canvas.restoreToCount(saved);
        }
    }

    /**
     * The same crossfade the wallpaper runs under it on a track change - WallpaperProbe's
     * sTrackFadeMs and its cubic ease-out - so the square and its blurred backdrop turn over
     * together instead of the art lagging the background by 400ms.
     */
    private static final long TRACK_FADE_MS = 180L;
    private long trackFadeMs = TRACK_FADE_MS;

    /**
     * A new song's art turns the square over as the media card's thumbnail does - HyperOS's own
     * flip (MiuiMediaAlbumAnimationUtils + miuix FlipAnimation, as HyperImmersive's MusicPage):
     * half a turn about the vertical axis on a Folme spring (damping 0.72, response 0.5s),
     * forwards for next and backwards for previous, the new art taking the face as it passes
     * edge-on. Shrunk by a skip or grown through a song ending on its own, alike: grown it only
     * crossfaded, and a song ending showed no turn at all (the maintainer, 2026-10-24).
     */
    private static final float FLIP_DAMPING = 0.72f;
    private static final float FLIP_RESPONSE = 0.5f;
    /** The old art held until this long at most, should the spring never get edge-on. */
    private static final long FLIP_HOLD_MS = 600L;
    private float flipAngle;
    private float flipTarget;
    private float flipVelocity;
    /** Where the turn began: the old art shows until it is 90 degrees on from here. */
    private float flipFromAngle;
    /** This swap turns over rather than crossfading. */
    private boolean flipSwap;
    private final android.graphics.Camera flipCamera = new android.graphics.Camera();
    private final android.graphics.Matrix flipMatrix = new android.graphics.Matrix();

    private void startFlip(int dir) {
        // Land on the next multiple of 180 in the direction of travel, like FlipAnimation.
        flipTarget = (Math.round(flipTarget / 180f) + dir) * 180f;
        flipFromAngle = flipAngle;
    }

    /** @return whether the turn is still moving */
    private boolean stepFlip(float dt) {
        if (flipAngle == flipTarget && flipVelocity == 0f) return false;
        float k = (float) Math.pow(2.0 * Math.PI / FLIP_RESPONSE, 2.0);
        float damp = (float) (4.0 * Math.PI * FLIP_DAMPING / FLIP_RESPONSE);
        float t = 0f;
        float total = Math.min(Math.max(dt, 0f), 1f / 30f);
        while (t < total) {
            float h = Math.min(1f / 480f, total - t);
            flipVelocity += (-k * (flipAngle - flipTarget) - damp * flipVelocity) * h;
            flipAngle += flipVelocity * h;
            t += h;
        }
        if (Math.abs(flipAngle - flipTarget) < 0.05f && Math.abs(flipVelocity) < 1f) {
            flipAngle = flipTarget;
            flipVelocity = 0f;
        }
        return true;
    }

    /** The old art still has the face: the turn has not yet gone edge-on. */
    private boolean flipShowsPrevious() {
        return previous != null && Math.abs(flipAngle - flipFromAngle) < 90f;
    }

    private boolean applyFlip(Canvas canvas, float cx, float cy) {
        float a = flipAngle % 360f;
        if (a == 0f) return false;
        float density = getResources().getDisplayMetrics().density;
        flipCamera.save();
        // View's default camera distance: 1280dp, in Camera's 72-per-inch units.
        flipCamera.setLocation(0f, 0f, -1280f * density / 72f);
        flipCamera.rotateY(-a);
        flipCamera.getMatrix(flipMatrix);
        flipCamera.restore();
        float norm = ((a % 360f) + 360f) % 360f;
        // Past edge-on the back is facing: mirror it back (FlipAnimation's scaleX = -1).
        if (norm > 90f && norm < 270f) flipMatrix.preScale(-1f, 1f);
        flipMatrix.preTranslate(-cx, -cy);
        flipMatrix.postTranslate(cx, cy);
        canvas.concat(flipMatrix);
        return true;
    }

    private float fadeFraction() {
        if (previous == null) return 1f;
        float t = Math.min(1f, (SystemClock.uptimeMillis() - changedAt) / (float) trackFadeMs);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static float aspectOf(Prepared p) {
        return p == null ? 1f : CoverCardStyle.aspect(p.art.getWidth(), p.art.getHeight());
    }

    /**
     * The card's shape right now: the current art's, eased from the previous one's over the same
     * crossfade the art itself takes, so a song followed by a video's cover changes shape as it
     * turns over rather than jumping.
     */
    private float shownAspect() {
        float to = aspectOf(current);
        if (previous == null) return to;
        float from = aspectOf(previous);
        return from + (to - from) * fadeFraction();
    }

    private final Rect artSrc = new Rect();

    /** Keep the scale-driven transition treatment inside the artwork, away from the backdrop. */
    private void drawWaitingArt(Canvas canvas, Prepared p, float fraction,
                                float treatment, RenderNode node) {
        if (p == null || fraction <= 0f) return;
        if (treatment <= 0f) {
            drawArt(canvas, p, fraction);
            return;
        }
        Canvas target = canvas;
        boolean hardware = canvas.isHardwareAccelerated();
        if (hardware) {
            int left = (int) Math.floor(square.left), top = (int) Math.floor(square.top);
            int right = (int) Math.ceil(square.right), bottom = (int) Math.ceil(square.bottom);
            node.setPosition(left, top, right, bottom);
            float radius = treatment * 2f * getResources().getDisplayMetrics().density;
            node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
            target = node.beginRecording(right - left, bottom - top);
            target.translate(-left, -top);
        }
        drawArt(target, p, fraction);
        paint.setColor(0xFF000000);
        paint.setAlpha(Math.round(51f * opacity * fraction * treatment));
        target.drawRect(square, paint);
        if (hardware) {
            node.endRecording();
            canvas.drawRenderNode(node);
        }
    }

    private void drawArt(Canvas canvas, Prepared p, float fraction) {
        if (p == null || fraction <= 0f) return;
        paint.setShader(null);
        paint.setColor(0xFFFFFFFF);
        paint.setAlpha(Math.round(255f * opacity * fraction));
        // Centre-cropped to the card's shape, which is only ever off this art's own through a
        // crossfade between two shapes - drawn into it as it is, one of the two would stretch.
        int w = p.art.getWidth(), h = p.art.getHeight();
        float box = square.width() / Math.max(1f, square.height());
        int cw = Math.min(w, Math.round(h * box));
        int ch = Math.min(h, Math.round(w / box));
        artSrc.set((w - cw) / 2, (h - ch) / 2, (w + cw) / 2, (h + ch) / 2);
        canvas.drawBitmap(p.art, artSrc, square, paint);
    }

}
