package com.example.bridge;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.appcompat.view.ContextThemeWrapper;

public class FloatingPageOverlay {
    
    private Context context;
    private WindowManager windowManager;
    private View floatingPageView;
    private WindowManager.LayoutParams params;
    private boolean isShowing = false;
    
    public FloatingPageOverlay(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        createFloatingPage();
    }
    
    private void createFloatingPage() {
        try {
            // Try to use the CardView layout first
            Context themedContext = new androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_Bridge);
            LayoutInflater inflater = LayoutInflater.from(themedContext);
            floatingPageView = inflater.inflate(R.layout.floating_page_overlay, null);
        } catch (Exception e) {
            try {
                // Fallback to simple layout without Material components
                LayoutInflater inflater = LayoutInflater.from(context);
                floatingPageView = inflater.inflate(R.layout.simple_floating_overlay, null);
            } catch (Exception e2) {
                // Last resort: create programmatically
                createSimpleProgrammaticView();
            }
        }
        
        // Set up window parameters
        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
            
        params = new WindowManager.LayoutParams(
            (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9),
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.CENTER;
        
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        // Close button
        ImageView closeBtn = floatingPageView.findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(v -> hide());
        
        // Quick action buttons
        floatingPageView.findViewById(R.id.meeting_btn).setOnClickListener(v -> {
            Intent intent = new Intent(context, MeetingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            hide();
        });
        
        floatingPageView.findViewById(R.id.chat_btn).setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            hide();
        });
        
        floatingPageView.findViewById(R.id.call_btn).setOnClickListener(v -> {
            Intent intent = new Intent(context, CallActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            hide();
        });
        
        floatingPageView.findViewById(R.id.transcript_btn).setOnClickListener(v -> {
            Intent intent = new Intent(context, TranscriptActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            hide();
        });
        
        // Click outside to close
        floatingPageView.setOnClickListener(v -> hide());
    }
    
    public void show() {
        if (!isShowing && floatingPageView != null) {
            try {
                windowManager.addView(floatingPageView, params);
                isShowing = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void hide() {
        if (isShowing && floatingPageView != null) {
            try {
                windowManager.removeView(floatingPageView);
                isShowing = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean isShowing() {
        return isShowing;
    }
    
    private void createSimpleProgrammaticView() {
        // Create a simple programmatic view as last resort
        android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(context);
        frameLayout.setBackgroundColor(0x80000000);
        frameLayout.setClickable(true);
        frameLayout.setFocusable(true);
        
        android.widget.LinearLayout container = new android.widget.LinearLayout(context);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setBackgroundColor(0xFFFFFFFF);
        container.setPadding(48, 48, 48, 48);
        
        android.widget.TextView title = new android.widget.TextView(context);
        title.setText("Bridge Quick Actions");
        title.setTextSize(20);
        title.setTextColor(0xFF333333);
        title.setGravity(android.view.Gravity.CENTER);
        container.addView(title);
        
        // Add some basic buttons
        String[] actions = {"Meeting", "Chat", "Call", "Transcript"};
        for (String action : actions) {
            android.widget.Button btn = new android.widget.Button(context);
            btn.setText(action);
            btn.setId(android.view.View.generateViewId());
            container.addView(btn);
        }
        
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.CENTER;
        params.setMargins(40, 40, 40, 40);
        
        frameLayout.addView(container, params);
        floatingPageView = frameLayout;
    }
    
    public void destroy() {
        hide();
        floatingPageView = null;
    }
}