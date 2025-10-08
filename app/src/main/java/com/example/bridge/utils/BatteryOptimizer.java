package com.example.bridge.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

/**
 * BatteryOptimizer manages power consumption during continuous transcription
 * by implementing adaptive strategies based on battery level and charging state.
 */
public class BatteryOptimizer {
    private static final String TAG = "BatteryOptimizer";
    
    // Battery thresholds for optimization strategies
    private static final int LOW_BATTERY_THRESHOLD = 20; // 20%
    private static final int CRITICAL_BATTERY_THRESHOLD = 10; // 10%
    private static final int VERY_LOW_BATTERY_THRESHOLD = 5; // 5%
    
    // Optimization intervals
    private static final long BATTERY_CHECK_INTERVAL_MS = 30000; // 30 seconds
    private static final long AGGRESSIVE_OPTIMIZATION_INTERVAL_MS = 15000; // 15 seconds
    
    // Performance adjustment factors
    private static final float NORMAL_PERFORMANCE_FACTOR = 1.0f;
    private static final float POWER_SAVE_PERFORMANCE_FACTOR = 0.7f;
    private static final float AGGRESSIVE_SAVE_PERFORMANCE_FACTOR = 0.5f;
    
    public enum PowerMode {
        NORMAL,
        POWER_SAVE,
        AGGRESSIVE_SAVE,
        CRITICAL_SAVE
    }
    
    public interface BatteryCallback {
        void onPowerModeChanged(PowerMode newMode, int batteryLevel);
        void onBatteryLevelChanged(int batteryLevel, boolean isCharging);
        void onOptimizationApplied(String optimization);
        void onCriticalBatteryWarning(int batteryLevel);
    }
    
    private final Context context;
    private final BatteryCallback callback;
    private final Handler batteryHandler = new Handler(Looper.getMainLooper());
    private final PowerManager powerManager;
    
    // Current state
    private PowerMode currentPowerMode = PowerMode.NORMAL;
    private int lastBatteryLevel = -1;
    private boolean isCharging = false;
    private boolean isMonitoring = false;
    private long lastOptimizationTime = 0;
    
    // Optimization settings
    private boolean adaptiveTranscriptionEnabled = true;
    private boolean backgroundOptimizationEnabled = true;
    private boolean aggressiveMemoryCleanupEnabled = false;
    
    private final Runnable batteryCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkBatteryStatus();
            
            long interval = currentPowerMode == PowerMode.AGGRESSIVE_SAVE || 
                           currentPowerMode == PowerMode.CRITICAL_SAVE ?
                           AGGRESSIVE_OPTIMIZATION_INTERVAL_MS : BATTERY_CHECK_INTERVAL_MS;
            
            batteryHandler.postDelayed(this, interval);
        }
    };
    
    public BatteryOptimizer(Context context, BatteryCallback callback) {
        this.context = context;
        this.callback = callback;
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }
    
    /**
     * Starts battery monitoring and optimization
     */
    public void startOptimization() {
        if (!isMonitoring) {
            isMonitoring = true;
            checkBatteryStatus(); // Initial check
            batteryHandler.post(batteryCheckRunnable);
            Log.d(TAG, "Battery optimization started");
        }
    }
    
    /**
     * Stops battery monitoring
     */
    public void stopOptimization() {
        if (isMonitoring) {
            isMonitoring = false;
            batteryHandler.removeCallbacks(batteryCheckRunnable);
            Log.d(TAG, "Battery optimization stopped");
        }
    }
    
    /**
     * Checks current battery status and applies optimizations
     */
    private void checkBatteryStatus() {
        BatteryInfo batteryInfo = getBatteryInfo();
        
        boolean batteryLevelChanged = batteryInfo.level != lastBatteryLevel;
        boolean chargingStateChanged = batteryInfo.isCharging != isCharging;
        
        if (batteryLevelChanged || chargingStateChanged) {
            lastBatteryLevel = batteryInfo.level;
            isCharging = batteryInfo.isCharging;
            
            // Notify callback of battery changes
            if (callback != null) {
                callback.onBatteryLevelChanged(batteryInfo.level, batteryInfo.isCharging);
            }
            
            // Determine appropriate power mode
            PowerMode newPowerMode = determinePowerMode(batteryInfo);
            
            if (newPowerMode != currentPowerMode) {
                applyPowerMode(newPowerMode, batteryInfo.level);
            }
        }
        
        // Apply periodic optimizations based on current mode
        applyPeriodicOptimizations();
    }
    
    /**
     * Gets current battery information
     */
    private BatteryInfo getBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        
        if (batteryStatus == null) {
            return new BatteryInfo(50, false, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        }
        
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryPct = (int) ((level / (float) scale) * 100);
        
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL;
        
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, 
                                             BatteryManager.BATTERY_HEALTH_UNKNOWN);
        
        return new BatteryInfo(batteryPct, isCharging, health);
    }
    
    /**
     * Determines appropriate power mode based on battery status
     */
    private PowerMode determinePowerMode(BatteryInfo batteryInfo) {
        // If charging, use normal mode unless battery is very low
        if (batteryInfo.isCharging) {
            return batteryInfo.level < VERY_LOW_BATTERY_THRESHOLD ? 
                   PowerMode.POWER_SAVE : PowerMode.NORMAL;
        }
        
        // Not charging - determine mode based on battery level
        if (batteryInfo.level <= VERY_LOW_BATTERY_THRESHOLD) {
            return PowerMode.CRITICAL_SAVE;
        } else if (batteryInfo.level <= CRITICAL_BATTERY_THRESHOLD) {
            return PowerMode.AGGRESSIVE_SAVE;
        } else if (batteryInfo.level <= LOW_BATTERY_THRESHOLD) {
            return PowerMode.POWER_SAVE;
        } else {
            return PowerMode.NORMAL;
        }
    }
    
    /**
     * Applies the specified power mode and its optimizations
     */
    private void applyPowerMode(PowerMode newMode, int batteryLevel) {
        PowerMode previousMode = currentPowerMode;
        currentPowerMode = newMode;
        
        // Apply mode-specific optimizations
        switch (newMode) {
            case NORMAL:
                applyNormalModeOptimizations();
                break;
            case POWER_SAVE:
                applyPowerSaveModeOptimizations();
                break;
            case AGGRESSIVE_SAVE:
                applyAggressiveSaveModeOptimizations();
                break;
            case CRITICAL_SAVE:
                applyCriticalSaveModeOptimizations();
                break;
        }
        
        // Notify callback of mode change
        if (callback != null) {
            callback.onPowerModeChanged(newMode, batteryLevel);
            
            if (newMode == PowerMode.CRITICAL_SAVE) {
                callback.onCriticalBatteryWarning(batteryLevel);
            }
        }
        
        Log.d(TAG, String.format("Power mode changed from %s to %s (battery: %d%%)", 
                previousMode, newMode, batteryLevel));
    }
    
    /**
     * Applies normal mode optimizations (full performance)
     */
    private void applyNormalModeOptimizations() {
        adaptiveTranscriptionEnabled = true;
        backgroundOptimizationEnabled = false;
        aggressiveMemoryCleanupEnabled = false;
        
        notifyOptimization("Normal performance mode - all features enabled");
    }
    
    /**
     * Applies power save mode optimizations
     */
    private void applyPowerSaveModeOptimizations() {
        adaptiveTranscriptionEnabled = true;
        backgroundOptimizationEnabled = true;
        aggressiveMemoryCleanupEnabled = false;
        
        notifyOptimization("Power save mode - reduced background activity");
    }
    
    /**
     * Applies aggressive save mode optimizations
     */
    private void applyAggressiveSaveModeOptimizations() {
        adaptiveTranscriptionEnabled = true;
        backgroundOptimizationEnabled = true;
        aggressiveMemoryCleanupEnabled = true;
        
        notifyOptimization("Aggressive save mode - reduced transcription frequency");
    }
    
    /**
     * Applies critical save mode optimizations
     */
    private void applyCriticalSaveModeOptimizations() {
        adaptiveTranscriptionEnabled = false; // Minimal transcription
        backgroundOptimizationEnabled = true;
        aggressiveMemoryCleanupEnabled = true;
        
        notifyOptimization("Critical save mode - minimal transcription, audio recording only");
    }
    
    /**
     * Applies periodic optimizations based on current power mode
     */
    private void applyPeriodicOptimizations() {
        long currentTime = System.currentTimeMillis();
        
        // Skip if we've optimized recently
        if (currentTime - lastOptimizationTime < 10000) { // 10 seconds
            return;
        }
        
        switch (currentPowerMode) {
            case POWER_SAVE:
                // Reduce animation frequency
                notifyOptimization("Reduced animation frequency for power saving");
                break;
                
            case AGGRESSIVE_SAVE:
                // Force memory cleanup
                System.gc();
                notifyOptimization("Forced memory cleanup for power saving");
                break;
                
            case CRITICAL_SAVE:
                // Minimal operations only
                System.gc();
                notifyOptimization("Critical power mode - minimal operations only");
                break;
        }
        
        lastOptimizationTime = currentTime;
    }
    
    /**
     * Gets the performance factor for current power mode
     */
    public float getPerformanceFactor() {
        switch (currentPowerMode) {
            case NORMAL:
                return NORMAL_PERFORMANCE_FACTOR;
            case POWER_SAVE:
                return POWER_SAVE_PERFORMANCE_FACTOR;
            case AGGRESSIVE_SAVE:
                return AGGRESSIVE_SAVE_PERFORMANCE_FACTOR;
            case CRITICAL_SAVE:
                return AGGRESSIVE_SAVE_PERFORMANCE_FACTOR * 0.8f; // Even more aggressive
            default:
                return NORMAL_PERFORMANCE_FACTOR;
        }
    }
    
    /**
     * Checks if transcription should be throttled based on power mode
     */
    public boolean shouldThrottleTranscription() {
        return currentPowerMode == PowerMode.AGGRESSIVE_SAVE || 
               currentPowerMode == PowerMode.CRITICAL_SAVE;
    }
    
    /**
     * Checks if background optimizations should be enabled
     */
    public boolean shouldOptimizeBackground() {
        return backgroundOptimizationEnabled;
    }
    
    /**
     * Checks if aggressive memory cleanup should be performed
     */
    public boolean shouldPerformAggressiveCleanup() {
        return aggressiveMemoryCleanupEnabled;
    }
    
    /**
     * Gets current power mode
     */
    public PowerMode getCurrentPowerMode() {
        return currentPowerMode;
    }
    
    /**
     * Gets current battery level
     */
    public int getCurrentBatteryLevel() {
        return lastBatteryLevel;
    }
    
    /**
     * Checks if device is currently charging
     */
    public boolean isCharging() {
        return isCharging;
    }
    
    /**
     * Notifies callback of optimization applied
     */
    private void notifyOptimization(String optimization) {
        if (callback != null) {
            callback.onOptimizationApplied(optimization);
        }
    }
    
    /**
     * Battery information data class
     */
    private static class BatteryInfo {
        final int level;
        final boolean isCharging;
        final int health;
        
        BatteryInfo(int level, boolean isCharging, int health) {
            this.level = level;
            this.isCharging = isCharging;
            this.health = health;
        }
    }
}