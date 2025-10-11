package com.example.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bridge.adapters.PagerAdapter;
import com.example.bridge.adapters.SessionsAdapter;
import com.example.bridge.databinding.ActivityMainBinding;
import com.example.bridge.models.PagerItem;
import com.example.bridge.models.SessionItem;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    List<Integer> list = new ArrayList<>();
    PagerAdapter adapter;
    SessionsAdapter sessionsAdapter;
    ViewPager2 vp;
    List<PagerItem> itemList;
    List<SessionItem> sessionsList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Handle system window insets
        setupWindowInsets();


        itemList = new ArrayList<>();
        itemList.add(new PagerItem(R.drawable.chat,"Conversation","Have a chat",
                R.color.primary,R.color.button_bg));
        itemList.add(new PagerItem(R.drawable.trans,"Transcription","Attend a meeting",
                R.color.stroke_green,R.color.stroke_green));
        itemList.add(new PagerItem(R.drawable.call," Emergency","Call for safety",
                R.color.stroke_red,R.color.stroke_red));
        itemList.add(new PagerItem(R.drawable.mic,"Voice Note","Create a voice note",
                R.color.stroke_orange,R.color.stroke_orange));

        adapter = new PagerAdapter(MainActivity.this, itemList);

        binding.recyclerView.setAdapter(adapter);

        binding.recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this,2));

        // Setup recent sessions
        setupRecentSessions();

        // Setup toolbar
        setSupportActionBar(binding.toolBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
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
    
    private void setupRecentSessions() {
        sessionsList = new ArrayList<>();
        
        // Create fake demo data with different timestamps
        long now = System.currentTimeMillis();
        
        sessionsList.add(new SessionItem(
            "Voice Note",
            "Morning Workout Plan", 
            "Discussed 5km morning run routine and gym schedule for the week",
            now - (5 * 60 * 1000) // 5 minutes ago
        ));
        
        sessionsList.add(new SessionItem(
            "Meeting",
            "Team Standup Meeting",
            "Weekly progress review with development team and project updates",
            now - (2 * 60 * 60 * 1000) // 2 hours ago
        ));
        
        sessionsList.add(new SessionItem(
            "Chat",
            "AI Assistant Conversation",
            "Asked about healthy meal prep ideas and got personalized recipes",
            now - (4 * 60 * 60 * 1000) // 4 hours ago
        ));
        
        sessionsList.add(new SessionItem(
            "Voice Note",
            "Shopping List Reminder",
            "Created voice note for grocery shopping including organic vegetables",
            now - (1 * 24 * 60 * 60 * 1000) // 1 day ago
        ));
        
        sessionsList.add(new SessionItem(
            "Meeting",
            "Client Presentation",
            "Presented quarterly results and discussed future project roadmap",
            now - (2 * 24 * 60 * 60 * 1000) // 2 days ago
        ));

        sessionsAdapter = new SessionsAdapter(this, sessionsList);
        binding.sessionsRecyclerView.setAdapter(sessionsAdapter);
        binding.sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            // Get system bars insets
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            
            // Apply top padding to toolbar to avoid status bar overlap
            binding.toolBar.setPadding(
                binding.toolBar.getPaddingLeft(),
                topInset,
                binding.toolBar.getPaddingRight(),
                binding.toolBar.getPaddingBottom()
            );
            
            return insets;
        });
    }
}