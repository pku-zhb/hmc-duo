package com.zhuhuibin.hypermusiccover.duo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

/** The OEM Folme adapter passes null; Java preserves that contract without Kotlin null checks. */
final class NativeNotificationAnimatorListener extends AnimatorListenerAdapter {
    private final AnimatorListenerAdapter original;
    private final Runnable cancelled;
    private final Runnable finished;

    NativeNotificationAnimatorListener(AnimatorListenerAdapter original, Runnable cancelled, Runnable finished) {
        this.original = original;
        this.cancelled = cancelled;
        this.finished = finished;
    }

    @Override public void onAnimationStart(Animator animation) {
        if (original != null) original.onAnimationStart(animation);
    }

    @Override public void onAnimationStart(Animator animation, boolean reverse) {
        if (original != null) original.onAnimationStart(animation, reverse);
    }

    @Override public void onAnimationRepeat(Animator animation) {
        if (original != null) original.onAnimationRepeat(animation);
    }

    @Override public void onAnimationCancel(Animator animation) {
        cancelled.run();
        try { if (original != null) original.onAnimationCancel(animation); }
        finally { finished.run(); }
    }

    @Override public void onAnimationEnd(Animator animation) {
        try { if (original != null) original.onAnimationEnd(animation); }
        finally { finished.run(); }
    }

    @Override public void onAnimationEnd(Animator animation, boolean reverse) {
        try { if (original != null) original.onAnimationEnd(animation, reverse); }
        finally { finished.run(); }
    }
}
