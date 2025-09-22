package com.example.bridge.utils;

import android.animation.*;
import android.os.SystemClock;
import android.view.View;

public class GlowPulseController {
    private final View glow;
    private AnimatorSet pulseAnim;

    // hysteresis so it won't flicker on borderline levels
    private static final float THRESH_HIGH = 0.25f; // start pulsing above
    private static final float THRESH_LOW  = 0.12f; // stop pulsing below
    private static final long  HOLD_MS     = 180;

    private boolean pulsing = false;
    private long gateOpenAt = 0L, gateCloseAt = 0L;

    public GlowPulseController(View glow) {
        this.glow = glow;
        setupPulse();
    }

    private void setupPulse() {
        ObjectAnimator sx = ObjectAnimator.ofFloat(glow, View.SCALE_X, 1f, 1.15f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(glow, View.SCALE_Y, 1f, 1.15f);
        ObjectAnimator a  = ObjectAnimator.ofFloat(glow, View.ALPHA,   0.6f, 1f);
        for (ValueAnimator v : new ValueAnimator[]{sx, sy, a}) {
            v.setRepeatMode(ValueAnimator.REVERSE);
            v.setRepeatCount(ValueAnimator.INFINITE);
            v.setDuration(900);
        }
        pulseAnim = new AnimatorSet();
        pulseAnim.playTogether(sx, sy, a);
    }

    public void onRecordingStart() { // wait for voice, do not auto-pulse
        pulsing = false;
        stopPulse();
    }

    public void onRecordingPauseOrStop() {
        pulsing = false;
        stopPulse();
    }

    /** Feed normalized mic level [0..1] continuously while recording */
    public void onLevel(float level01) {
        long now = SystemClock.uptimeMillis();
        if (!pulsing) {
            if (level01 >= THRESH_HIGH) {
                if (gateOpenAt == 0L) gateOpenAt = now;
                if (now - gateOpenAt >= HOLD_MS) {
                    startPulse();
                    pulsing = true;
                    gateCloseAt = 0L;
                }
            } else gateOpenAt = 0L;
        } else {
            if (level01 <= THRESH_LOW) {
                if (gateCloseAt == 0L) gateCloseAt = now;
                if (now - gateCloseAt >= HOLD_MS) {
                    stopPulse();
                    pulsing = false;
                    gateOpenAt = 0L;
                }
            } else gateCloseAt = 0L;
        }
    }

    private void startPulse() {
        if (pulseAnim != null && !pulseAnim.isStarted()) pulseAnim.start();
    }

    private void stopPulse() {
        if (pulseAnim != null) pulseAnim.cancel();
        glow.setScaleX(1f);
        glow.setScaleY(1f);
        glow.setAlpha(0.8f); // baseline
    }
}
