package com.example.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds
    private boolean permissionRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_splash);

        // Check overlay permission after splash delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkOverlayPermission();
        }, SPLASH_DELAY);
    }
    
    private void checkOverlayPermission() {
        if (!FloatingButtonManager.canDrawOverlays(this)) {
            showPermissionDialog();
        } else {
            // Permission already granted, start floating button and proceed
            FloatingButtonManager.startFloatingButton(this);
            proceedToMainActivity();
        }
    }
    
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Enable Floating Chat")
            .setMessage("Bridge needs permission to display over other apps to show the floating chat button. This allows you to access chat from anywhere on your device.\n\nTap 'Allow' on the next screen to enable this feature.")
            .setPositiveButton("Enable", (dialog, which) -> {
                permissionRequested = true;
                FloatingButtonManager.requestOverlayPermission(this);
            })
            .setNegativeButton("Skip", (dialog, which) -> {
                // Proceed without floating button
                proceedToMainActivity();
            })
            .setCancelable(false)
            .show();
    }
    
    private void proceedToMainActivity() {
        Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FloatingButtonManager.isOverlayPermissionResult(requestCode)) {
            if (FloatingButtonManager.canDrawOverlays(this)) {
                // Permission granted
                FloatingButtonManager.startFloatingButton(this);
                showPermissionGrantedMessage();
            } else {
                // Permission denied
                showPermissionDeniedMessage();
            }
            // Always proceed to main activity after permission handling
            new Handler(Looper.getMainLooper()).postDelayed(this::proceedToMainActivity, 1500);
        }
    }
    
    private void showPermissionGrantedMessage() {
        new AlertDialog.Builder(this)
            .setTitle("Floating Chat Enabled!")
            .setMessage("You can now access Bridge chat from anywhere using the floating button. Tap it to open chat, or long-press to remove it.")
            .setPositiveButton("Got it", null)
            .show();
    }
    
    private void showPermissionDeniedMessage() {
        new AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Floating chat is disabled. You can enable it later in Settings > Apps > Bridge > Display over other apps.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // If we requested permission and user came back, check the result
        if (permissionRequested && FloatingButtonManager.canDrawOverlays(this)) {
            FloatingButtonManager.startFloatingButton(this);
            showPermissionGrantedMessage();
            new Handler(Looper.getMainLooper()).postDelayed(this::proceedToMainActivity, 1500);
            permissionRequested = false;
        }
    }
}