package com.example.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Request overlay permission if needed and start floating button
        if (!FloatingButtonManager.canDrawOverlays(this)) {
            FloatingButtonManager.requestOverlayPermission(this);
        }
        
        // Navigate to MainActivity after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FloatingButtonManager.isOverlayPermissionResult(requestCode)) {
            if (FloatingButtonManager.canDrawOverlays(this)) {
                FloatingButtonManager.startFloatingButton(this);
            }
        }
    }
}