package com.example.bridge;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;

public class BridgeApplication extends Application {
    
    private static final String PREFS_NAME = "BridgePrefs";
    private static final String KEY_FLOATING_BUTTON_ENABLED = "floating_button_enabled";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Auto-start floating button if it was previously enabled
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean wasEnabled = prefs.getBoolean(KEY_FLOATING_BUTTON_ENABLED, true); // Default to true
        
        if (wasEnabled && FloatingButtonManager.canDrawOverlays(this)) {
            FloatingButtonManager.startFloatingButton(this);
        }
    }
    
    public static void setFloatingButtonEnabled(android.content.Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FLOATING_BUTTON_ENABLED, enabled).apply();
    }
    
    public static boolean isFloatingButtonEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_FLOATING_BUTTON_ENABLED, true);
    }
}