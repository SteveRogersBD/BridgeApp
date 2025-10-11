package com.example.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bridge.adapters.OnboardingAdapter;
import com.example.bridge.databinding.ActivityOnboardingBinding;
import com.example.bridge.models.PagerItem;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;
    private OnboardingAdapter adapter;
    private List<PagerItem> onboardingItems;
    private ImageView[] indicators;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupWindowInsets();
        setupOnboardingItems();
        setupViewPager();
        setupIndicators();
        setupClickListeners();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            
            binding.getRoot().setPadding(0, topInset, 0, bottomInset);
            return insets;
        });
    }

    private void setupOnboardingItems() {
        onboardingItems = new ArrayList<>();
        
        onboardingItems.add(new PagerItem(
            R.drawable.chat,
            "Smart Conversations",
            "Engage in intelligent conversations with AI that understands context and provides meaningful responses.",
            R.color.primary,
            R.color.primary
        ));
        
        onboardingItems.add(new PagerItem(
            R.drawable.trans,
            "Meeting Transcription",
            "Automatically transcribe and summarize your meetings with real-time accuracy and smart insights.",
            R.color.stroke_green,
            R.color.stroke_green
        ));
        
        onboardingItems.add(new PagerItem(
            R.drawable.call,
            "Emergency Assistance",
            "Quick access to emergency contacts and safety features when you need help the most.",
            R.color.stroke_red,
            R.color.stroke_red
        ));
        
        onboardingItems.add(new PagerItem(
            R.drawable.mic,
            "Voice Notes",
            "Create, organize, and manage voice recordings with intelligent transcription and search capabilities.",
            R.color.stroke_orange,
            R.color.stroke_orange
        ));
    }

    private void setupViewPager() {
        adapter = new OnboardingAdapter(this, onboardingItems);
        binding.viewPager.setAdapter(adapter);
        
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                updateNavigationButtons(position);
            }
        });
    }

    private void setupIndicators() {
        indicators = new ImageView[onboardingItems.size()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_dot));
            indicators[i].setLayoutParams(layoutParams);
            binding.indicatorContainer.addView(indicators[i]);
        }
        
        updateIndicators(0);
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            if (i == position) {
                indicators[i].setAlpha(1.0f);
                indicators[i].setScaleX(1.2f);
                indicators[i].setScaleY(1.2f);
            } else {
                indicators[i].setAlpha(0.5f);
                indicators[i].setScaleX(1.0f);
                indicators[i].setScaleY(1.0f);
            }
        }
    }

    private void updateNavigationButtons(int position) {
        if (position == 0) {
            binding.btnPrevious.setVisibility(View.INVISIBLE);
        } else {
            binding.btnPrevious.setVisibility(View.VISIBLE);
        }

        if (position == onboardingItems.size() - 1) {
            binding.btnNext.setText("Get Started");
        } else {
            binding.btnNext.setText("Next");
        }
    }

    private void setupClickListeners() {
        binding.btnPrevious.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem > 0) {
                binding.viewPager.setCurrentItem(currentItem - 1);
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem < onboardingItems.size() - 1) {
                binding.viewPager.setCurrentItem(currentItem + 1);
            } else {
                // Navigate to GetStartedActivity
                startActivity(new Intent(OnboardingActivity.this, GetStartedActivity.class));
                finish();
            }
        });

        binding.btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity.this, GetStartedActivity.class));
            finish();
        });
    }
}