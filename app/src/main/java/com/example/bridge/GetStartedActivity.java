package com.example.bridge;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.example.bridge.databinding.ActivityGetStartedBinding;

public class GetStartedActivity extends AppCompatActivity {

    private ActivityGetStartedBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityGetStartedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupWindowInsets();
        setupClickListener();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            
            binding.getRoot().setPadding(0, topInset, 0, bottomInset);
            return insets;
        });
    }

    private void setupClickListener() {
        binding.btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(GetStartedActivity.this, RegisterActivity.class));
            finish();
        });
    }
}