package com.example.bridge.utils;

import android.animation.Animator;
import android.view.View;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AnimationControllerTest {

    @Mock
    private AnimationController.AnimationCallback mockCallback;
    
    @Mock
    private View mockView;
    
    @Mock
    private TextView mockTextView;
    
    private AnimationController animationController;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        animationController = new AnimationController();
    }

    @Test
    public void testSetPerformanceFactor() {
        // Test normal range
        animationController.setPerformanceFactor(0.5f);
        // Should not throw exceptions
        
        animationController.setPerformanceFactor(1.0f);
        animationController.setPerformanceFactor(2.0f);
        
        // Test boundary values
        animationController.setPerformanceFactor(0.0f); // Should be clamped to 0.1f
        animationController.setPerformanceFactor(5.0f); // Should be clamped to 2.0f
    }

    @Test
    public void testSetAnimationsEnabled() {
        // Test enabling animations
        animationController.setAnimationsEnabled(true);
        
        // Test disabling animations
        animationController.setAnimationsEnabled(false);
        
        // Should cancel all animations when disabled
        assertEquals(0, animationController.getActiveAnimationCount());
    }

    @Test
    public void testCreateFadeInWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        Animator fadeIn = animationController.createFadeIn(mockView, mockCallback);
        
        // Should return null when animations are disabled
        assertNull(fadeIn);
        
        // Should set view alpha directly
        verify(mockView).setAlpha(1.0f);
    }

    @Test
    public void testCreateFadeOutWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        Animator fadeOut = animationController.createFadeOut(mockView, mockCallback);
        
        // Should return null when animations are disabled
        assertNull(fadeOut);
        
        // Should set view alpha directly
        verify(mockView).setAlpha(0f);
    }

    @Test
    public void testCreateScaleBounceWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        Animator scaleBounce = animationController.createScaleBounce(mockView, 1.0f, 1.2f, mockCallback);
        
        // Should return null when animations are disabled
        assertNull(scaleBounce);
        
        // Should set view scale directly
        verify(mockView).setScaleX(1.2f);
        verify(mockView).setScaleY(1.2f);
    }

    @Test
    public void testCreatePulseAnimationWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        Animator pulse = animationController.createPulseAnimation(mockView, 0.8f, 1.2f);
        
        // Should return null when animations are disabled
        assertNull(pulse);
    }

    @Test
    public void testCreateRotationAnimationWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        Animator rotation = animationController.createRotationAnimation(mockView, 0f, 360f, false);
        
        // Should return null when animations are disabled
        assertNull(rotation);
        
        // Should set view rotation directly
        verify(mockView).setRotation(360f);
    }

    @Test
    public void testAnimateTextChangeWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        String newText = "New Text";
        animationController.animateTextChange(mockTextView, newText, mockCallback);
        
        // Should set text directly
        verify(mockTextView).setText(newText);
        
        // Should call callback immediately
        verify(mockCallback).onAnimationEnd();
    }

    @Test
    public void testCreateSlideAnimationWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        Animator slide = animationController.createSlideAnimation(mockView, 0f, 100f, 0f, 50f);
        
        // Should return null when animations are disabled
        assertNull(slide);
        
        // Should set view translation directly
        verify(mockView).setTranslationX(100f);
        verify(mockView).setTranslationY(50f);
    }

    @Test
    public void testCreateGlowEffectWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        Animator glow = animationController.createGlowEffect(mockView, 1.0f);
        
        // Should return null when animations are disabled
        assertNull(glow);
    }

    @Test
    public void testCancelAllAnimations() {
        // Should not throw exceptions even with no active animations
        animationController.cancelAllAnimations();
        
        assertEquals(0, animationController.getActiveAnimationCount());
    }

    @Test
    public void testPauseAllAnimations() {
        // Should not throw exceptions even with no active animations
        animationController.pauseAllAnimations();
    }

    @Test
    public void testResumeAllAnimations() {
        // Should not throw exceptions even with no active animations
        animationController.resumeAllAnimations();
    }

    @Test
    public void testGetActiveAnimationCount() {
        // Initial count should be 0
        assertEquals(0, animationController.getActiveAnimationCount());
    }

    @Test
    public void testCreateAnimationChainWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        Animator chain = animationController.createAnimationChain();
        
        // Should return null when animations are disabled or no animations provided
        assertNull(chain);
    }

    @Test
    public void testCreateAnimationSetWithAnimationsDisabled() {
        animationController.setAnimationsEnabled(false);
        
        Animator set = animationController.createAnimationSet();
        
        // Should return null when animations are disabled or no animations provided
        assertNull(set);
    }

    @Test
    public void testCreateAnimationChainWithNoAnimations() {
        animationController.setAnimationsEnabled(true);
        
        Animator chain = animationController.createAnimationChain();
        
        // Should return null when no animations provided
        assertNull(chain);
    }

    @Test
    public void testCreateAnimationSetWithNoAnimations() {
        animationController.setAnimationsEnabled(true);
        
        Animator set = animationController.createAnimationSet();
        
        // Should return null when no animations provided
        assertNull(set);
    }

    @Test
    public void testPerformanceFactorBoundaries() {
        // Test minimum boundary
        animationController.setPerformanceFactor(0.05f); // Below minimum
        // Should be clamped to 0.1f internally
        
        // Test maximum boundary
        animationController.setPerformanceFactor(3.0f); // Above maximum
        // Should be clamped to 2.0f internally
        
        // Should not throw exceptions
    }

    @Test
    public void testAnimationControllerInitialState() {
        AnimationController newController = new AnimationController();
        
        // Initial state should have no active animations
        assertEquals(0, newController.getActiveAnimationCount());
        
        // Should be able to create animations by default (animations enabled)
        // This is tested indirectly through other tests
    }

    @Test
    public void testMultiplePerformanceFactorChanges() {
        // Test multiple rapid changes
        for (int i = 0; i < 10; i++) {
            float factor = 0.5f + (i * 0.1f);
            animationController.setPerformanceFactor(factor);
        }
        
        // Should handle multiple changes without issues
    }

    @Test
    public void testMultipleAnimationEnableDisable() {
        // Test multiple rapid enable/disable cycles
        for (int i = 0; i < 5; i++) {
            animationController.setAnimationsEnabled(true);
            animationController.setAnimationsEnabled(false);
        }
        
        // Should handle multiple cycles without issues
        assertEquals(0, animationController.getActiveAnimationCount());
    }
}