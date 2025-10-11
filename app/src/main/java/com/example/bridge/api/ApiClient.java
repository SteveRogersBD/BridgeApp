package com.example.bridge.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Use 10.0.2.2 for Android emulator, or replace with your computer's IP for physical device
    // Alternative URLs to try:
    // "http://localhost:8080/" - for emulator (sometimes works)
    // "http://127.0.0.1:8080/" - localhost IP
    // "http://YOUR_COMPUTER_IP:8080/" - for physical device (replace YOUR_COMPUTER_IP)
    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthApi getAuthApi() {
        return getClient().create(AuthApi.class);
    }
}