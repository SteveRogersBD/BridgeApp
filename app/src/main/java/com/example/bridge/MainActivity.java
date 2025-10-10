package com.example.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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
        itemList.add(new PagerItem(R.drawable.chat,"Conversation",
                R.color.primary,R.color.button_bg));
        itemList.add(new PagerItem(R.drawable.trans,"Transcription",
                R.color.stroke_green,R.color.stroke_green));
        itemList.add(new PagerItem(R.drawable.call,"Call",
                R.color.stroke_red,R.color.stroke_red));
        itemList.add(new PagerItem(R.drawable.mic,"Transcription",
                R.color.stroke_orange,R.color.stroke_orange));

        adapter = new PagerAdapter(MainActivity.this, itemList);

        binding.recyclerView.setAdapter(adapter);

        binding.recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this,2));

        topBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topBar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        
        // Update menu item state based on current floating button status
        MenuItem floatingButtonItem = menu.findItem(R.id.action_floating_button);
        boolean isEnabled = BridgeApplication.isFloatingButtonEnabled(this) && 
                           FloatingButtonManager.canDrawOverlays(this);
        floatingButtonItem.setChecked(isEnabled);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_floating_button) {
            toggleFloatingButton();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void toggleFloatingButton() {
        if (!FloatingButtonManager.canDrawOverlays(this)) {
            // Permission not granted, show dialog and request
            showFloatingButtonPermissionDialog();
        } else {
            // Permission granted, toggle the floating button
            boolean isCurrentlyEnabled = BridgeApplication.isFloatingButtonEnabled(this);
            if (isCurrentlyEnabled) {
                // Disable floating button
                FloatingButtonManager.stopFloatingButton(this);
                BridgeApplication.setFloatingButtonEnabled(this, false);
                Toast.makeText(this, "Floating chat disabled", Toast.LENGTH_SHORT).show();
            } else {
                // Enable floating button
                FloatingButtonManager.startFloatingButton(this);
                BridgeApplication.setFloatingButtonEnabled(this, true);
                Toast.makeText(this, "Floating chat enabled", Toast.LENGTH_SHORT).show();
            }
            // Refresh menu to update icon state
            invalidateOptionsMenu();
        }
    }
    
    private void showFloatingButtonPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Enable Floating Chat")
            .setMessage("To use the floating chat feature, Bridge needs permission to display over other apps. This allows you to access chat from anywhere on your device.")
            .setPositiveButton("Grant Permission", (dialog, which) -> {
                FloatingButtonManager.requestOverlayPermission(this);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FloatingButtonManager.isOverlayPermissionResult(requestCode)) {
            if (FloatingButtonManager.canDrawOverlays(this)) {
                // Permission granted, enable floating button
                FloatingButtonManager.startFloatingButton(this);
                BridgeApplication.setFloatingButtonEnabled(this, true);
                Toast.makeText(this, "Floating chat enabled!", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied. Floating chat is disabled.", Toast.LENGTH_LONG).show();
            }
            // Refresh menu to update icon state
            invalidateOptionsMenu();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh menu when returning to activity
        invalidateOptionsMenu();
    }
}