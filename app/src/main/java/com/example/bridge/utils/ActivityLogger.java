package com.example.bridge.utils;

import android.content.Context;
import android.util.Log;

import com.example.bridge.api.ApiClient;
import com.example.bridge.models.ActivityRequest;
import com.example.bridge.models.ActivityResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityLogger {
    private static final String TAG = "ActivityLogger";
    
    public enum ActivityType {
        CHAT("Chat"),
        MEETING("Meeting"),
        VOICE_NOTE("Voice Note");
        
        private final String value;
        
        ActivityType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public static void logActivity(Context context, ActivityType type, String title, String content) {
        String userId = PreferenceManager.getCurrentUserId(context);
        
        if (userId == null) {
            Log.e(TAG, "Cannot log activity: User ID not found");
            return;
        }
        
        // Generate description using Gemini
        generateDescriptionAndLog(context, userId, type, title, content);
    }
    
    private static void generateDescriptionAndLog(Context context, String userId, ActivityType type, String title, String content) {
        GeminiHelper geminiHelper = new GeminiHelper();
        
        String prompt = createPrompt(type, title, content);
        
        geminiHelper.callGemini(prompt, new GeminiHelper.GeminiCallback() {
            @Override
            public void onSuccess(String description) {
                // Clean up the description (remove quotes, trim, etc.)
                String cleanDescription = description.trim().replaceAll("^\"|\"$", "");
                
                // Log the activity with generated description
                logActivityToServer(userId, type, title, cleanDescription);
            }
            
            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Failed to generate description with Gemini", t);
                // Fallback to a simple description
                String fallbackDescription = "Activity completed: " + type.getValue();
                logActivityToServer(userId, type, title, fallbackDescription);
            }
        });
    }
    
    private static String createPrompt(ActivityType type, String title, String content) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a short, concise description (maximum 50 words) for a ");
        prompt.append(type.getValue().toLowerCase());
        prompt.append(" activity. ");
        
        if (title != null && !title.trim().isEmpty()) {
            prompt.append("Title: ").append(title).append(". ");
        }
        
        if (content != null && !content.trim().isEmpty()) {
            prompt.append("Content summary: ").append(content).append(". ");
        }
        
        prompt.append("Focus on the main topic and key points discussed. Be specific and informative.");
        
        return prompt.toString();
    }
    
    private static void logActivityToServer(String userId, ActivityType type, String title, String description) {
        ActivityRequest request = new ActivityRequest(type.getValue(), title, description);
        
        ApiClient.getAuthApi().logActivity(userId, request).enqueue(new Callback<ActivityResponse>() {
            @Override
            public void onResponse(Call<ActivityResponse> call, Response<ActivityResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Activity logged successfully: " + response.body().getId());
                } else {
                    Log.e(TAG, "Failed to log activity: " + response.code() + " - " + response.message());
                }
            }
            
            @Override
            public void onFailure(Call<ActivityResponse> call, Throwable t) {
                Log.e(TAG, "Network error while logging activity", t);
            }
        });
    }
}