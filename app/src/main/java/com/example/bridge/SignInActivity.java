package com.example.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.example.bridge.api.ApiClient;
import com.example.bridge.databinding.ActivitySignInBinding;
import com.example.bridge.models.LoginRequest;
import com.example.bridge.models.LoginResponse;
import com.example.bridge.utils.PreferenceManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private static final String TAG = "SignInActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(this);
        
        // Check if user is already logged in
        if (preferenceManager.isLoggedIn()) {
            navigateToMainApp();
            return;
        }
        
        setupWindowInsets();
        setupClickListeners();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            
            binding.scrollView.setPadding(0, topInset, 0, bottomInset);
            return insets;
        });
    }

    private void setupClickListeners() {
        binding.btnSignIn.setOnClickListener(v -> handleSignIn());
        
        binding.btnGoogleSignIn.setOnClickListener(v -> handleGoogleSignIn());
        
        binding.tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, RegisterActivity.class));
            finish();
        });

        binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private void handleSignIn() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (validateInputs(email, password)) {
            performLogin(email, password);
        }
    }

    private void performLogin(String email, String password) {
        // Show loading state
        setLoadingState(true);
        
        LoginRequest loginRequest = new LoginRequest(email, password);
        
        ApiClient.getAuthApi().login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoadingState(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    
                    if (loginResponse.getUser() != null) {
                        // Save user data to SharedPreferences
                        preferenceManager.saveUserId(loginResponse.getUser().getId());
                        preferenceManager.saveUserEmail(loginResponse.getUser().getEmail());
                        preferenceManager.saveUserName(loginResponse.getUser().getFullName());
                        preferenceManager.setLoggedIn(true);
                        
                        showToast("Login successful!");
                        Log.d(TAG, "Login successful. User ID: " + loginResponse.getUser().getId());
                        
                        navigateToMainApp();
                    } else {
                        showToast("Login failed: Invalid response");
                        Log.e(TAG, "Login failed: User data is null");
                    }
                } else {
                    showToast("Login failed: " + response.message());
                    Log.e(TAG, "Login failed: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoadingState(false);
                showToast("Network error: " + t.getMessage());
                Log.e(TAG, "Login network error", t);
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        binding.btnSignIn.setEnabled(!isLoading);
        binding.btnSignIn.setText(isLoading ? "Signing in..." : "Sign In");
        
        // Optionally show/hide progress indicator
        if (isLoading) {
            binding.etEmail.setEnabled(false);
            binding.etPassword.setEnabled(false);
        } else {
            binding.etEmail.setEnabled(true);
            binding.etPassword.setEnabled(true);
        }
    }

    private void navigateToMainApp() {
        startActivity(new Intent(SignInActivity.this, MainActivity.class));
        finish();
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Please enter a valid email");
            binding.etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void handleGoogleSignIn() {
        // TODO: Implement Google Sign-In
        showToast("Google Sign-In coming soon!");
    }

    private void handleForgotPassword() {
        // TODO: Implement forgot password functionality
        showToast("Forgot password feature coming soon!");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}