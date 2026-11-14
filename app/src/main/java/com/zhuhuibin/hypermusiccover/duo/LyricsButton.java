package com.zhuhuibin.hypermusiccover.duo;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

/** A native Notes button beside the track details, owned only by the lock-screen card. */
final class LyricsButton {
    private static ViewGroup host;
    private static ImageView button;
    private static LyricsSheetDrawable notesDrawable;
    private static MusicPageActionDrawable pageDrawable;
    private static boolean railCycle;
    private static ValueAnimator animator;
    private static float progress;
    private static boolean requested;
    /** Off the title: shrink away while the native thumbnail owns the tap. */
    private static boolean besideControls;
    /** The slot it is drawn in: it changes slot at the bottom of a quick shrink-and-fade. */
    private static boolean shownBeside;
    /** How much of it the swap leaves visible: 1 at rest, 0 at the moment it changes slot. */
    private static float swapLook = 1f;
    /** The room the title gives it, eased over the whole swap so the title glides, never jumps. */
    private static float titleShare, titleFrom;
    private static ValueAnimator swap;
    private static boolean swapPastMiddle;
    /** Coming back to the title from nowhere: this swap has only its growing half. */
    private static boolean swapArriving;
    /** Where it rests by the title, kept across unlocks: it grows back there, not on the moving title. */
    private static float restLeft = Float.NaN, titleLeft;
    private static boolean holdLeft;
    /** Where it last rested, and where a jump of its spot holds it until it has faded out. */
    private static float[] restAt, heldAt;
    private static final float JUMP_DP = 6f;
    private static int tint;
    private static int creations, releases;
    private static final float SIZE_DP = 40f;
    private static final float GAP_DP = 6f;
    private static final float APPEAR_SCALE = 0.6f;
    private static final float BLUR_DP = 8f;
    private static final long SWAP_MS = 220L;

    private LyricsButton() {}

    static float update(View card, boolean show, boolean beside) {
        if (!(card instanceof ViewGroup)) return 0f;
        if (host != card) release();
        railCycle = MiniPlayerRuntime.detachedRail();
        // Artwork is already a native click target. The other two actions replace its pixels
        // in that same slot; they never add a second control beside the title.
        if (railCycle && !ArtworkThumbnailPolicy.replaceWithPageAction(true, Main.nextRailMusicPage())) show = false;
        boolean animate = LyricsButtonPolicy.animate(MiniPlayerRuntime.detachedRail());
        if (!animate) {
            if (animator != null) animator.cancel();
            if (swap != null) swap.cancel();
            animator = swap = null;
            besideControls = shownBeside = false;
            swapLook = titleShare = 1f;
            holdLeft = false;
            restAt = heldAt = null;
        }
        // Leaving (the next track has no lyrics), it stays in its slot: swapping back to the title
        // on the way out pushed the title right for a moment as the button went.
        if (show) besideControls = LyricsButtonPolicy.beside(MiniPlayerRuntime.detachedRail(), beside);
        if (button == null || progress <= 0f) {
            // Nothing on screen to swap: it appears where it belongs.
            if (swap != null) swap.cancel();
            swap = null;
            shownBeside = besideControls;
            swapLook = gone() ? 0f : 1f;
            titleShare = besideControls ? 0f : 1f;
        } else if (animate && shownBeside != besideControls && swap == null) {
            // Between the title and the controls it does not travel: it shrinks, fades and blurs
            // away where it was and grows back the same way where it now belongs, quickly.
            startSwap();
        }
        if (button == null && show) {
            host = (ViewGroup) card;
            button = new ImageView(card.getContext());
            creations++;
            button.setId(View.generateViewId());
            notesDrawable = new LyricsSheetDrawable();
            button.setImageDrawable(notesDrawable);
            button.setScaleType(ImageView.ScaleType.FIT_CENTER);
            float density = card.getResources().getDisplayMetrics().density;
            int pad = Math.round(8f * density);
            button.setPadding(pad, pad, pad, pad);
            String language = card.getResources().getConfiguration().getLocales().get(0).getLanguage();
            button.setContentDescription("zh".equals(language) ? "显示歌词" : "Show lyrics");
            button.setOnClickListener(v -> {
                if (railCycle) Main.cycleRailMusicPage();
                else Main.openLyricsFromButton();
            });
            button.setAlpha(0f);
            button.setScaleX(APPEAR_SCALE);
            button.setScaleY(APPEAR_SCALE);
            button.setVisibility(View.INVISIBLE);
            int size = Math.round(SIZE_DP * density);
            host.addView(button, new ViewGroup.LayoutParams(size, size));
        }
        if (button == null) return 0f;
        // Filled only while lyrics are on screen; over the companion it is an outline.
        notesDrawable.setFilled(LockLyrics.lyricsShowing());
        if (railCycle) {
            if (pageDrawable == null) pageDrawable = new MusicPageActionDrawable();
            pageDrawable.setPage(Main.nextRailMusicPage());
            pageDrawable.setLyricsAvailability(LockLyrics.buttonAvailability());
            if (button.getDrawable() != pageDrawable) button.setImageDrawable(pageDrawable);
        } else if (button.getDrawable() != notesDrawable) button.setImageDrawable(notesDrawable);
        if (!animate) {
            float target = show ? 1f : 0f;
            int visibility = show ? View.VISIBLE : View.GONE;
            boolean changed = progress != target || requested != show || button.getVisibility() != visibility
                    || button.getAlpha() != target || show && (button.getScaleX() != 1f || button.getScaleY() != 1f);
            requested = show;
            progress = target;
            boolean interactive = LyricsButtonPolicy.interactive(show, Main.screenOnCached(),
                    MiniPlayerScene.INSTANCE.getAodActive());
            button.setClickable(interactive);
            button.setFocusable(interactive);
            button.setImportantForAccessibility(show ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            if (changed) applyLook();
        } else if (requested != show) {
            requested = show;
            button.setClickable(show);
            button.setFocusable(show);
            button.setImportantForAccessibility(show ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            if (animator != null) animator.cancel();
            button.setVisibility(View.VISIBLE);
            animator = ValueAnimator.ofFloat(progress, show ? 1f : 0f);
            animator.setDuration(show ? 260L : 200L);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(a -> {
                progress = (float) a.getAnimatedValue();
                applyLook();
            });
            animator.start();
        }
        boolean hide = LockLyrics.lyricsShowing();
        boolean chinese = "zh".equals(card.getResources().getConfiguration().getLocales().get(0).getLanguage());
        if (railCycle) {
            int next = Main.nextRailMusicPage();
            String description = next == RailMusicPagePolicy.ARTWORK ? (chinese ? "显示封面" : "Show artwork")
                    : next == RailMusicPagePolicy.LYRICS ? (chinese ? "显示歌词" : "Show lyrics")
                    : next == RailMusicPagePolicy.ARTWORK_AND_LYRICS ? (chinese ? "显示封面和歌词" : "Show artwork and lyrics")
                    : (chinese ? "返回锁屏时钟" : "Return to lock-screen clock");
            int availability = LyricsButtonPolicy.actionAvailability(next, LockLyrics.buttonAvailability());
            if (availability == LyricsButtonPolicy.LOADING)
                description = chinese ? "歌词加载中，点击切换" : "Lyrics loading; tap to switch";
            else if (availability == LyricsButtonPolicy.UNAVAILABLE)
                description = chinese ? "当前歌曲暂无歌词，仍可继续切换" : "No lyrics for this track; page switching remains available";
            button.setContentDescription(description);
            button.setTooltipText(description);
        } else {
            button.setContentDescription(chinese ? (hide ? "隐藏歌词" : "显示歌词") : (hide ? "Hide lyrics" : "Show lyrics"));
            button.setTooltipText(null);
        }
        // The title gives the button room only while it is drawn by the title, eased across the
        // whole swap so the title glides rather than jumping.
        return railCycle ? 0f : (SIZE_DP + GAP_DP) * card.getResources().getDisplayMetrics().density * progress * titleShare;
    }

    private static void startSwap() {
        swapPastMiddle = false;
        swapArriving = shownBeside && !besideControls;
        // It grows by the title from the start, not from the slot it left at the middle.
        if (swapArriving) {
            shownBeside = false;
            holdLeft = !Float.isNaN(restLeft);
        }
        titleFrom = titleShare;
        swap = ValueAnimator.ofFloat(0f, 1f);
        swap.setDuration(SWAP_MS);
        swap.setInterpolator(new LinearInterpolator());
        swap.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            if (t >= .5f) {
                swapPastMiddle = true;
                shownBeside = besideControls;
            }
            // To or from nowhere there is one half to see, so it takes the whole swap.
            swapLook = gone() ? 1f - t : swapArriving ? t : Math.abs(1f - 2f * t);
            float eased = 1f - (1f - t) * (1f - t);
            titleShare = titleFrom + ((besideControls ? 0f : 1f) - titleFrom) * eased;
            if (t >= 1f) swap = null;
            applyLook();
            Main.refreshMediaCardForMorph();
        });
        swap.start();
    }

    /**
     * Grows out of its own centre where it rests (place keeps it there), blurred and faded in;
     * going, the same way back. A slot swap uses the very same look, only quicker.
     */
    private static void applyLook() {
        ImageView view = button;
        if (view == null) return;
        float look = progress * swapLook;
        view.setAlpha(look);
        float scale = APPEAR_SCALE + (1f - APPEAR_SCALE) * look;
        view.setScaleX(scale);
        view.setScaleY(scale);
        float radius = (1f - look) * BLUR_DP * view.getResources().getDisplayMetrics().density;
        view.setRenderEffect(radius < 0.5f ? null : RenderEffect.createBlurEffect(
                radius, radius, Shader.TileMode.DECAL));
        // shortcut: gone from under the thumbnail, where it covered other apps' extra controls,
        // until that row can be measured for room. Invisible so it takes no touches there either.
        if (!requested && progress == 0f) view.setVisibility(View.GONE);
        else view.setVisibility(look > 0f ? View.VISIBLE : View.INVISIBLE);
        if (host != null) host.postInvalidateOnAnimation();
    }

    static void place(View card, TextView title, TextView artist, View artwork) {
        if (button == null || host != card || title == null || progress <= 0f || gone()) return;
        if (railCycle) {
            if (artwork == null || artwork.getWidth() <= 0 || artwork.getHeight() <= 0) return;
            float left = position(card, artwork, true);
            float top = position(card, artwork, false);
            if (!Float.isFinite(left) || !Float.isFinite(top)) return;
            int w = artwork.getWidth(), h = artwork.getHeight();
            ViewGroup.LayoutParams lp = button.getLayoutParams();
            if (lp.width != w || lp.height != h) {
                lp.width = w; lp.height = h;
                button.setLayoutParams(lp);
            }
            // This runs before drawing: update both the geometry and click target immediately,
            // without waiting one frame for the native parent to lay out its overlay child.
            button.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
            button.layout(button.getLeft(), button.getTop(), button.getLeft() + w, button.getTop() + h);
            button.setTranslationX(left - button.getLeft());
            button.setTranslationY(top - button.getTop());
            button.setTranslationZ(Math.max(0f, artwork.getZ()) + .01f);
            int color = title.getCurrentTextColor();
            if (tint != color) { tint = color; button.setImageTintList(ColorStateList.valueOf(color)); }
            return;
        }
        Layout layout = title.getLayout();
        if (layout == null || layout.getLineCount() == 0) return;
        float density = card.getResources().getDisplayMetrics().density;
        int size = Math.round(SIZE_DP * density);
        float titleX = position(card, title, true) + title.getPaddingLeft()
                + (railCycle ? 0f : layout.getLineLeft(0));
        float top = position(card, title, false);
        float bottom = artist != null && artist.isShown()
                ? position(card, artist, false) + artist.getHeight() : top + title.getHeight();
        if (!Float.isFinite(titleX) || !Float.isFinite(top) || !Float.isFinite(bottom)) return;
        // Where it rests whatever the progress: the title is pushed right by the inset update()
        // returns (its share of the progress), and following the title it slid in from the left.
        float left = titleX - (SIZE_DP + GAP_DP) * density * progress * titleShare;
        float y = (top + bottom - size) / 2f;
        if (!shownBeside) {
            // The cover's spring outlasts the swap: held until the title has reached it, or has
            // truly stopped elsewhere. Its slow tail still moves, so letting go on that jumped.
            float riding = left;
            if (holdLeft && swap == null && (Math.abs(riding - restLeft) < 1f
                    || Math.abs(riding - titleLeft) < .01f)) holdLeft = false;
            if (holdLeft) left = restLeft;
            else if (swap == null && progress >= 1f) restLeft = riding;
            titleLeft = riding;
        }
        if (shownBeside) {
            float artLeft = artwork == null ? 16f * density : position(card, artwork, true);
            float artWidth = artwork == null ? size : artwork.getWidth();
            float besideLeft = artLeft + (artWidth - size) / 2f;
            View action = null;
            for (String name : new String[]{"actionPlayPause", "actionPrev", "actionNext", "action0", "media_play_pause"}) {
                int id = card.getResources().getIdentifier(name, "id", "com.android.systemui");
                View candidate = id == 0 ? null : card.findViewById(id);
                if (candidate != null && candidate.isShown()) { action = candidate; break; }
            }
            float centre = action == null ? card.getHeight() * .6f
                    : position(card, action, false) + action.getHeight() / 2f;
            left = besideLeft;
            y = centre - size / 2f;
        }
        // Wherever its spot jumps, it does
        // not ride along: it holds where it was, shrinks away and grows back at the new spot.
        float jump = JUMP_DP * density;
        if (!shownBeside) restAt = null;
        if (swap == null && shownBeside && restAt != null && progress >= 1f
                && (Math.abs(left - restAt[0]) > jump || Math.abs(y - restAt[1]) > jump)) {
            heldAt = restAt;
            startSwap();
        }
        if (swap == null) heldAt = null;
        if (heldAt != null && !swapPastMiddle) {
            left = heldAt[0];
            y = heldAt[1];
        } else restAt = new float[]{left, y};
        // Let the OEM parent own layout; transforms also keep the click target aligned.
        button.setTranslationX(Math.round(left) - button.getLeft());
        button.setTranslationY(Math.round(y) - button.getTop());
        int color = title.getCurrentTextColor();
        if (tint != color) {
            tint = color;
            button.setImageTintList(ColorStateList.valueOf(color));
        }
    }

    /** The slot off the title shrinks away where it stands. */
    private static boolean gone() {
        return besideControls;
    }

    /** Shown, or on its way in, beside the title rather than under the thumbnail. */
    static boolean byTitle() {
        return button != null && requested && !railCycle && !besideControls;
    }

    static boolean contains(float x, float y) {
        if (button == null || !requested || !button.isShown() || !button.isEnabled()
                || !button.isClickable() || button.getAlpha() * button.getTransitionAlpha() < .1f) return false;
        float alpha = 1f;
        for (View node = button; node != null;
             node = node.getParent() instanceof View ? (View) node.getParent() : null) {
            alpha *= node.getAlpha() * node.getTransitionAlpha();
            if (alpha < .1f) return false;
        }
        android.graphics.Rect bounds = new android.graphics.Rect();
        return button.getGlobalVisibleRect(bounds) && bounds.contains((int) x, (int) y);
    }

    private static float position(View card, View view, boolean horizontal) {
        float value = 0f;
        while (view != null && view != card) {
            value += horizontal ? view.getLeft() + view.getTranslationX()
                    : view.getTop() + view.getTranslationY();
            if (!(view.getParent() instanceof View)) return Float.NaN;
            view = (View) view.getParent();
        }
        return view == card ? value : Float.NaN;
    }

    static View viewForDiagnostics() { return button; }

    static String describe() {
        return "requested=" + requested + " beside=" + besideControls + " progress=" + progress
                + " created=" + creations + " released=" + releases
                + " host=" + (host == null ? "none" : Integer.toHexString(System.identityHashCode(host)))
                + " visibility=" + (button == null ? -1 : button.getVisibility())
                + (button == null ? "" : " alpha=" + button.getAlpha() + " size=" + button.getWidth() + "x" + button.getHeight());
    }

    static void release() {
        if (button != null) releases++;
        if (animator != null) animator.cancel();
        animator = null;
        if (host != null && button != null) host.removeView(button);
        host = null;
        button = null;
        notesDrawable = null;
        pageDrawable = null;
        railCycle = false;
        requested = false;
        besideControls = false;
        if (swap != null) swap.cancel();
        swap = null;
        shownBeside = false;
        swapLook = 1f;
        swapPastMiddle = false;
        holdLeft = false;
        titleFrom = titleShare;
        restAt = heldAt = null;
        titleShare = titleFrom = 0f;
        progress = 0f;
        tint = 0;
    }
}
