package com.example.bridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;

public class FloatingButtonService extends Service {
    
    private static final String CHANNEL_ID = "FloatingButtonChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private FloatingPageOverlay floatingPageOverlay;
    
    // Touch handling variables
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isMoving = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        createFloatingButton();
        floatingPageOverlay = new FloatingPageOverlay(this);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getStringExtra("action") : null;
        if ("STOP".equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Inflate the floating button layout
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.floating_button_layout, null);
        
        // Set up window parameters
        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
            
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        
        // Add touch listener for drag and click
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private long downTime = 0;
            private static final long LONG_PRESS_TIME = 1000; // 1 second
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isMoving = false;
                        downTime = System.currentTimeMillis();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        isMoving = true;
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        long pressDuration = System.currentTimeMillis() - downTime;
                        if (!isMoving) {
                            if (pressDuration >= LONG_PRESS_TIME) {
                                // Long press - stop service
                                stopSelf();
                            } else {
                                // Short press - show floating page overlay
                                if (floatingPageOverlay != null) {
                                    if (floatingPageOverlay.isShowing()) {
                                        floatingPageOverlay.hide();
                                    } else {
                                        floatingPageOverlay.show();
                                    }
                                }
                            }
                        }
                        isMoving = false;
                        return true;
                }
                return false;
            }
        });
        
        // Add the floating button to window
        windowManager.addView(floatingView, params);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Floating Button Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Service for floating button overlay");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MeetingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bridge Floating Button")
            .setContentText("Tap to open quick actions")
            .setSmallIcon(R.drawable.mic)
            .setContentIntent(pendingIntent)
            .build();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        if (floatingPageOverlay != null) {
            floatingPageOverlay.destroy();
        }
        // Save preference that floating button is disabled when service stops
        BridgeApplication.setFloatingButtonEnabled(this, false);
    }
}