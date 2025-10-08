package com.example.bridge.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimationController provides smooth, optimized animations for the transcription UI
 * with performance considerations and battery optimization support.
 */
public class AnimationController {
    private static final String TAG = "AnimationController";
    
    // Animation durations (can be adjusted based on performance mode)
    private static final long FAST_ANIMATION_DURATION = 150;
    private static final long NORMAL_ANIMATION_DURATION = 300;
    private static final long SLOW_ANIMATION_DURATION = 500;
    private static final long PULSE_ANIMATION_DURATION = 800;
    
    // Animation scales for different performance modes
    private float performanceFactor = 1.0f;
    private boolean animationsEnabled = true;
    
    // Active animations tracking
    private final List<Animator> activeAnimators = new ArrayList<>();
    
    public interface AnimationCallback {
        void onAnimationStart();
        void onAnimationEnd();
        void onAnimationCancel();
    }
    
    /**
     * Sets the performance factor for animations (0.5 = half speed, 1.0 = normal, 2.0 = double speed)
     */
    public void setPerformanceFactor(float factor) {
        this.performanceFactor = Math.max(0.1f, Math.min(2.0f, factor));
    }
    
    /**
     * Enables or disables animations globally
     */
    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
        if (!enabled) {
            cancelAllAnimations();
        }
    }
    
    /**
     * Creates a smooth fade in animation
     */
    public Animator createFadeIn(View view, AnimationCallback callback) {
        if (!animationsEnabled) {
            view.setAlpha(1.0f);
            return null;
        }
        
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setDuration(getScaledDuration(NORMAL_ANIMATION_DURATION));
        fadeIn.setInterpolator(new DecelerateInterpolator());
        
        if (callback != null) {
            fadeIn.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    callback.onAnimationStart();
                }
                
                @Override
                public void onAnimationEnd(Animator animation) {
                    activeAnimators.remove(animation);
                    callback.onAnimationEnd();
                }
                
                @Override
                public void onAnimationCancel(Animator animation) {
                    activeAnimators.remove(animation);
                    callback.onAnimationCancel();
                }
            });
        }
        
        trackAnimator(fadeIn);
        return fadeIn;
    }
    
    /**
     * Creates a smooth fade out animation
     */
    public Animator createFadeOut(View view, AnimationCallback callback) {
        if (!animationsEnabled) {
            view.setAlpha(0f);
            return null;
        }
        
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        fadeOut.setDuration(getScaledDuration(NORMAL_ANIMATION_DURATION));
        fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
        
        if (callback != null) {
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    callback.onAnimationStart();
                }
                
                @Override
                public void onAnimationEnd(Animator animation) {
                    activeAnimators.remove(animation);
                    callback.onAnimationEnd();
                }
                
                @Override
                public void onAnimationCancel(Animator animation) {
                    activeAnimators.remove(animation);
                    callback.onAnimationCancel();
                }
            });
        }
        
        trackAnimator(fadeOut);
        return fadeOut;
    }
    
    /**
     * Creates a smooth scale animation (bounce effect)
     */
    public Animator createScaleBounce(View view, float fromScale, float toScale, AnimationCallback callback) {
        if (!animationsEnabled) {
            view.setScaleX(toScale);
            view.setScaleY(toScale);
            return null;
        }
        
        AnimatorSet scaleSet = new AnimatorSet();
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", fromScale, toScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", fromScale, toScale);
        
        scaleX.setDuration(getScaledDuration(NORMAL_ANIMATION_DURATION));
        scaleY.setDuration(getScaledDuration(NORMAL_ANIMATION_DURATION));
        
        scaleX.setInterpolator(new OvershootInterpolator(1.2f));
        scaleY.setInterpolator(new OvershootInterpolator(1.2f));
        
        scaleSet.playTogether(scaleX, scaleY);
        
        if (callback != null) {
            scaleSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    callback.onAnimationStart();
                }
                
                @Override
                public void onAnimationEnd(Animator animation) {
                    activeAnimators.remove(animation);
                    callback.onAnimationEnd();
                }
                
                @Override
                public void onAnimationCancel(Animator animation) {
                    activeAnimators.remove(animation);
                    callback.onAnimationCancel();
                }
            });
        }
        
        trackAnimator(scaleSet);
        return scaleSet;
    }
    
    /**
     * Creates a continuous pulse animation for recording indication
     */
    public Animator createPulseAnimation(View view, float minScale, float maxScale) {
        if (!animationsEnabled) {
            return null;
        }
        
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(view, "scaleX", minScale, maxScale);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(view, "scaleY", minScale, maxScale);
        
        pulseX.setDuration(getScaledDuration(PULSE_ANIMATION_DURATION));
        pulseY.setDuration(getScaledDuration(PULSE_ANIMATION_DURATION));
        
        pulseX.setRepeatMode(ValueAnimator.REVERSE);
        pulseY.setRepeatMode(ValueAnimator.REVERSE);
        pulseX.setRepeatCount(ValueAnimator.INFINITE);
        pulseY.setRepeatCount(ValueAnimator.INFINITE);
        
        pulseX.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(pulseX, pulseY);
        
        pulseSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimators.remove(animation);
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimators.remove(animation);
            }
        });
        
        trackAnimator(pulseSet);
        return pulseSet;
    }
    
    /**
     * Creates a smooth rotation animation
     */
    public Animator createRotationAnimation(View view, float fromRotation, float toRotation, boolean continuous) {
        if (!animationsEnabled) {
            view.setRotation(toRotation);
            return null;
        }
        
        ObjectAnimator rotation = ObjectAnimator.ofFloat(view, "rotation", fromRotation, toRotation);
        rotation.setDuration(getScaledDuration(continuous ? 3000 : NORMAL_ANIMATION_DURATION));
        rotation.setInterpolator(new LinearInterpolator());
        
        if (continuous) {
            rotation.setRepeatCount(ValueAnimator.INFINITE);
        }
        
        rotation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimators.remove(animation);
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimators.remove(animation);
            }
        });
        
        trackAnimator(rotation);
        return rotation;
    }
    
    /**
     * Creates a smooth text change animation with fade effect
     */
    public void animateTextChange(TextView textView, String newText, AnimationCallback callback) {
        if (!animationsEnabled) {
            textView.setText(newText);
            if (callback != null) callback.onAnimationEnd();
            return;
        }
        
        // Fade out current text
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f);
        fadeOut.setDuration(getScaledDuration(FAST_ANIMATION_DURATION));
        fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
        
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Change text and fade in
                textView.setText(newText);
                
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
                fadeIn.setDuration(getScaledDuration(FAST_ANIMATION_DURATION));
                fadeIn.setInterpolator(new DecelerateInterpolator());
                
                fadeIn.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        activeAnimators.remove(animation);
                        if (callback != null) callback.onAnimationEnd();
                    }
                });
                
                trackAnimator(fadeIn);
                fadeIn.start();
            }
        });
        
        trackAnimator(fadeOut);
        fadeOut.start();
    }
    
    /**
     * Creates a smooth slide animation
     */
    public Animator createSlideAnimation(View view, float fromX, float toX, float fromY, float toY) {
        if (!animationsEnabled) {
            view.setTranslationX(toX);
            view.setTranslationY(toY);
            return null;
        }
        
        AnimatorSet slideSet = new AnimatorSet();
        
        ObjectAnimator slideX = ObjectAnimator.ofFloat(view, "translationX", fromX, toX);
        ObjectAnimator slideY = ObjectAnimator.ofFloat(view, "translationY", fromY, toY);
        
        slideX.setDuration(getScaledDuration(NORMAL_ANIMATION_DURATION));
        slideY.setDuration(getScaledDuration(NORMAL_ANIMATION_DURATION));
        
        slideX.setInterpolator(new DecelerateInterpolator());
        slideY.setInterpolator(new DecelerateInterpolator());
        
        slideSet.playTogether(slideX, slideY);
        
        slideSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimators.remove(animation);
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimators.remove(animation);
            }
        });
        
        trackAnimator(slideSet);
        return slideSet;
    }
    
    /**
     * Creates a subtle glow effect animation
     */
    public Animator createGlowEffect(View view, float intensity) {
        if (!animationsEnabled) {
            return null;
        }
        
        // Create a subtle alpha animation to simulate glow
        ObjectAnimator glow = ObjectAnimator.ofFloat(view, "alpha", 0.7f, intensity);
        glow.setDuration(getScaledDuration(PULSE_ANIMATION_DURATION / 2));
        glow.setRepeatMode(ValueAnimator.REVERSE);
        glow.setRepeatCount(ValueAnimator.INFINITE);
        glow.setInterpolator(new AccelerateDecelerateInterpolator());
        
        glow.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimators.remove(animation);
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimators.remove(animation);
            }
        });
        
        trackAnimator(glow);
        return glow;
    }
    
    /**
     * Cancels all active animations
     */
    public void cancelAllAnimations() {
        for (Animator animator : new ArrayList<>(activeAnimators)) {
            if (animator.isRunning()) {
                animator.cancel();
            }
        }
        activeAnimators.clear();
    }
    
    /**
     * Pauses all active animations (for battery optimization)
     */
    public void pauseAllAnimations() {
        for (Animator animator : activeAnimators) {
            if (animator.isRunning()) {
                animator.pause();
            }
        }
    }
    
    /**
     * Resumes all paused animations
     */
    public void resumeAllAnimations() {
        for (Animator animator : activeAnimators) {
            if (animator.isPaused()) {
                animator.resume();
            }
        }
    }
    
    /**
     * Gets the number of currently active animations
     */
    public int getActiveAnimationCount() {
        return activeAnimators.size();
    }
    
    /**
     * Tracks an animator for management
     */
    private void trackAnimator(Animator animator) {
        activeAnimators.add(animator);
    }
    
    /**
     * Gets scaled duration based on performance factor
     */
    private long getScaledDuration(long baseDuration) {
        return (long) (baseDuration / performanceFactor);
    }
    
    /**
     * Creates a chain of animations that play sequentially
     */
    public Animator createAnimationChain(Animator... animations) {
        if (!animationsEnabled || animations.length == 0) {
            return null;
        }
        
        AnimatorSet chain = new AnimatorSet();
        chain.playSequentially(animations);
        
        chain.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimators.remove(animation);
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimators.remove(animation);
            }
        });
        
        trackAnimator(chain);
        return chain;
    }
    
    /**
     * Creates a set of animations that play together
     */
    public Animator createAnimationSet(Animator... animations) {
        if (!animationsEnabled || animations.length == 0) {
            return null;
        }
        
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animations);
        
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimators.remove(animation);
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimators.remove(animation);
            }
        });
        
        trackAnimator(set);
        return set;
    }
}