package com.example.bridge.utils;

import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class GeminiHelper {
    // 10.0.2.2 is the special alias to your host loopback interface (127.0.0.1)
    // from the emulator
    private static final String BASE_URL = "http://10.0.2.2:8000/";

    private final RecommendationService service;

    public GeminiHelper() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(RecommendationService.class);
    }

    public void callGemini(String prompt, final GeminiCallback callback) {
        // We treat the incoming "prompt" as the transcript context for the agent
        RecommendationRequest request = new RecommendationRequest(prompt);

        service.getRecommendations(request).enqueue(new Callback<RecommendationResponse>() {
            @Override
            public void onResponse(Call<RecommendationResponse> call, Response<RecommendationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> recs = response.body().recommendations;
                    StringBuilder sb = new StringBuilder();
                    sb.append("--- AI Recommendations ---\n");
                    if (recs != null) {
                        for (String r : recs) {
                            sb.append("• ").append(r).append("\n");
                        }
                    }
                    callback.onSuccess(sb.toString().trim());
                } else {
                    callback.onFailure(new Exception("Server error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<RecommendationResponse> call, Throwable t) {
                Log.e("GeminiHelper", "API Call failed", t);
                callback.onFailure(t);
            }
        });
    }

    public interface GeminiCallback {
        void onSuccess(String result);

        void onFailure(Throwable t);
    }

    // Retrofit Interface
    public interface RecommendationService {
        @POST("recommend")
        Call<RecommendationResponse> getRecommendations(@Body RecommendationRequest request);
    }

    // DTOs
    public static class RecommendationRequest {
        final List<String> transcript;

        public RecommendationRequest(String transcript) {
            this.transcript = java.util.Collections.singletonList(transcript);
        }
    }

    public static class RecommendationResponse {
        List<String> recommendations;
    }
}
