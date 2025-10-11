package com.example.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.example.bridge.databinding.ActivitySignInBinding;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
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
            // TODO: Implement actual sign-in logic
            showToast("Sign in successful!");
            
            // Navigate to main app
            startActivity(new Intent(SignInActivity.this, OnboardingActivity.class));
            finish();
        }
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