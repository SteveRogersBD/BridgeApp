package com.example.bridge;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.example.bridge.databinding.ActivityGameBinding;

public class GameActivity extends AppCompatActivity {

    private ActivityGameBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Handle system window insets
        setupWindowInsets();
        
        // Setup click listeners
        setupClickListeners();
        
        Toast.makeText(this, "Game Session Started!", Toast.LENGTH_SHORT).show();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            
            binding.getRoot().setPadding(0, topInset, 0, bottomInset);
            return insets;
        });
    }

    private void setupClickListeners() {
        // Back button
        binding.backBtn.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });
        
        // Game controls can be added here
        // For example:
        // binding.startGameBtn.setOnClickListener(v -> startGame());
        // binding.pauseGameBtn.setOnClickListener(v -> pauseGame());
    }
}
