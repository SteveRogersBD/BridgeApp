package com.example.bridge;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bridge.adapters.PagerAdapter;
import com.example.bridge.databinding.ActivityMainBinding;
import com.example.bridge.models.PagerItem;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    List<Integer> list = new ArrayList<>();
    PagerAdapter adapter;
    ViewPager2 vp;
    List<PagerItem>itemList;
    MaterialToolbar topBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        itemList = new ArrayList<>();
        itemList.add(new PagerItem(R.drawable.mic,"Conversation",
                R.color.primary,R.color.button_bg));
        itemList.add(new PagerItem(R.drawable.mic,"Transcription",
                R.color.stroke_green,R.color.stroke_green));
        itemList.add(new PagerItem(R.drawable.mic,"Call",
                R.color.stroke_red,R.color.stroke_red));
        itemList.add(new PagerItem(R.drawable.mic,"Call",
                R.color.stroke_orange,R.color.stroke_orange));

        adapter = new PagerAdapter(MainActivity.this, itemList);

        binding.recyclerView.setAdapter(adapter);

        binding.recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this,2));

        topBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topBar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);



    }

}