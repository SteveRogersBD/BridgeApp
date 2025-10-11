package com.example.bridge.api;

import com.example.bridge.models.ActivityRequest;
import com.example.bridge.models.ActivityResponse;
import com.example.bridge.models.LoginRequest;
import com.example.bridge.models.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AuthApi {
    @POST("users/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);
    
    @POST("users/{userId}/activity")
    Call<ActivityResponse> logActivity(@Path("userId") String userId,
                                       @Body ActivityRequest activityRequest);

    @GET("users/{userId}/activities")
    Call<ActivityResponse> getAllActivities(@Path("userId") String userId);

}

