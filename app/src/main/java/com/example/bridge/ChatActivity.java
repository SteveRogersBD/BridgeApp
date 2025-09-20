package com.example.bridge;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bridge.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;
    private boolean listening = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.fabMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleListening();
            }
        });

    }

    private void toggleListening() {
        if(listening) {
            stopListening();
        }
        else startListening();
    }

    private void startListening() {
        listening = true;
        binding.micOverlay.setVisibility(View.VISIBLE);
        binding.micLottie.setProgress(0f);
        binding.micLottie.playAnimation();
        binding.fabMic.setImageResource(R.drawable.block); // or a pause/stop icon
    }

    private void stopListening() {
        listening = false;
        binding.micLottie.cancelAnimation();
        binding.micOverlay.setVisibility(View.GONE);
        binding.fabMic.setImageResource(R.drawable.mic);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // be safe: stop animation if activity goes background
        if (binding.micLottie.isAnimating()) {
            binding.micLottie.cancelAnimation();
        }
        binding.micOverlay.setVisibility(View.GONE);
        listening = false;
        binding.fabMic.setImageResource(R.drawable.mic);
    }
}