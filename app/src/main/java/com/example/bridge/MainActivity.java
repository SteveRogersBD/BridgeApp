package com.example.bridge;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bridge.adapters.PagerAdapter;
import com.example.bridge.databinding.ActivityMainBinding;
import com.example.bridge.models.PagerItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    List<Integer> list = new ArrayList<>();
    PagerAdapter adapter;
    ViewPager2 vp;
    List<PagerItem>itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        list.add(R.drawable.mic);
//        list.add(R.drawable.mic);
//        list.add(R.drawable.mic);
//        list.add(R.drawable.mic);
//        list.add(R.drawable.mic);
//
//        vp = binding.viewPager2;

        itemList = new ArrayList<>();
        itemList.add(new PagerItem(R.drawable.mic,"Conversation",
                R.color.primary,R.color.button_bg));
        itemList.add(new PagerItem(R.drawable.mic,"Transcription",
                R.color.stroke_green,R.color.stroke_green));
        itemList.add(new PagerItem(R.drawable.mic,"Call",
                R.color.stroke_red,R.color.stroke_red));
        adapter = new PagerAdapter(MainActivity.this, itemList);

        // carousel effect with scaling
        vp = binding.viewPager2;
        vp.setAdapter(adapter);
        vp.setPageTransformer((page, position) -> {
            float scale = 0.85f + (1 - Math.abs(position)) * 0.15f;
            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setAlpha(0.5f + (1 - Math.abs(position)) * 0.5f);
            page.setRotationY(position * -30); // optional 3d tilt
        });


    }

}