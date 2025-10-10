package com.example.bridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

public class FloatingButtonManager {
    
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1001;
    
    public static void startFloatingButton(Context context) {
        if (canDrawOverlays(context)) {
            Intent serviceIntent = new Intent(context, FloatingButtonService.class);
            context.startForegroundService(serviceIntent);
            
            // Save preference that floating button is enabled
            BridgeApplication.setFloatingButtonEnabled(context, true);
        } else if (context instanceof Activity) {
            requestOverlayPermission((Activity) context);
        }
    }
    
    public static void stopFloatingButton(Context context) {
        Intent serviceIntent = new Intent(context, FloatingButtonService.class);
        serviceIntent.putExtra("action", "STOP");
        context.startService(serviceIntent);
        
        // Save preference that floating button is disabled
        BridgeApplication.setFloatingButtonEnabled(context, false);
    }
    
    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Permission not required for older versions
    }
    
    public static void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            
            Toast.makeText(activity, 
                "Please enable 'Display over other apps' permission", 
                Toast.LENGTH_LONG).show();
        }
    }
    
    public static boolean isOverlayPermissionResult(int requestCode) {
        return requestCode == OVERLAY_PERMISSION_REQUEST_CODE;
    }
}