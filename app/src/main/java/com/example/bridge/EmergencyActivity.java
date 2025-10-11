package com.example.bridge;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.example.bridge.databinding.ActivityEmergencyBinding;
import com.example.bridge.utils.GeminiHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class EmergencyActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    ActivityEmergencyBinding binding;
    private TextToSpeech textToSpeech;
    private File audioFile;
    private AlertDialog loadingDialog;
    private GeminiHelper geminiHelper;
    private AlertDialog aiDialog;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentLocationAddress = "";
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityEmergencyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Handle system window insets
        setupWindowInsets();
        
        // Setup toolbar buttons
        setupToolbarButtons();

        setupTextToSpeech();
        setupClickListeners();
        setupLocationServices();
        
        // Initialize GeminiHelper
        geminiHelper = new GeminiHelper();
        
        // Request location permissions and get current location
        requestLocationPermissions();
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                binding.textViewLocationStatus.setText("Location permission denied. Please enable location access for emergency alerts.");
                Toast.makeText(this, "Location access is required for emergency alerts", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        binding.textViewLocationStatus.setText("Getting current location...");
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        getAddressFromLocation(location);
                    } else {
                        binding.textViewLocationStatus.setText("Unable to get current location. Please try again.");
                    }
                })
                .addOnFailureListener(this, e -> {
                    binding.textViewLocationStatus.setText("Failed to get location: " + e.getMessage());
                });
    }

    private void getAddressFromLocation(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressBuilder = new StringBuilder();
                
                // Build a readable address
                if (address.getFeatureName() != null) {
                    addressBuilder.append(address.getFeatureName()).append(", ");
                }
                if (address.getThoroughfare() != null) {
                    addressBuilder.append(address.getThoroughfare()).append(", ");
                }
                if (address.getSubLocality() != null) {
                    addressBuilder.append(address.getSubLocality()).append(", ");
                }
                if (address.getLocality() != null) {
                    addressBuilder.append(address.getLocality()).append(", ");
                }
                if (address.getAdminArea() != null) {
                    addressBuilder.append(address.getAdminArea()).append(", ");
                }
                if (address.getCountryName() != null) {
                    addressBuilder.append(address.getCountryName());
                }
                
                currentLocationAddress = addressBuilder.toString();
                if (currentLocationAddress.endsWith(", ")) {
                    currentLocationAddress = currentLocationAddress.substring(0, currentLocationAddress.length() - 2);
                }
                
                binding.textViewLocationStatus.setText("📍 " + currentLocationAddress);
            } else {
                currentLocationAddress = "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();
                binding.textViewLocationStatus.setText("📍 " + currentLocationAddress);
            }
        } catch (IOException e) {
            currentLocationAddress = "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();
            binding.textViewLocationStatus.setText("📍 " + currentLocationAddress);
        }
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, this);
    }

    private void setupClickListeners() {
        // AI button - show prompt dialog
        binding.buttonAI.setOnClickListener(v -> {
            showAIPromptDialog();
        });

        // Send Emergency button
        binding.buttonSendEmergency.setOnClickListener(v -> {
            String message = binding.editTextEmergencyMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter an emergency message", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (currentLocationAddress.isEmpty()) {
                Toast.makeText(this, "Getting location... Please wait and try again", Toast.LENGTH_SHORT).show();
                getCurrentLocation();
                return;
            }
            
            createEmergencyVoiceNote(message);
        });
    }

    private void createEmergencyVoiceNote(String text) {
        if (textToSpeech != null) {
            // Show loading dialog
            showLoadingDialog();
            
            // Create the full emergency message with location
            String fullMessage = "EMERGENCY ALERT: " + text + ". My current location is: " + currentLocationAddress;
            
            // Create audio file
            audioFile = new File(getExternalFilesDir(null), "emergency_alert_" + System.currentTimeMillis() + ".wav");
            
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "emergency_alert");
            
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    // Loading dialog is already shown
                }

                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        hideLoadingDialog();
                        showEmergencyShareDialog();
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    runOnUiThread(() -> {
                        hideLoadingDialog();
                        Toast.makeText(EmergencyActivity.this, "Error creating emergency voice note", Toast.LENGTH_SHORT).show();
                    });
                }
            });

            int result = textToSpeech.synthesizeToFile(fullMessage, params, audioFile.getAbsolutePath());
            if (result != TextToSpeech.SUCCESS) {
                hideLoadingDialog();
                Toast.makeText(this, "Failed to create emergency voice note", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_loading, null));
        builder.setCancelable(false);
        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void showEmergencyShareDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚨 Emergency Alert Created!")
                .setMessage("Your emergency alert with location has been successfully created. Would you like to send it now?")
                .setPositiveButton("Send Alert", (dialog, which) -> showEmergencyPlatformDialog())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showEmergencyPlatformDialog() {
        String[] platforms = {"WhatsApp", "SMS", "Email", "All Apps"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚨 Send Emergency Alert")
                .setItems(platforms, (dialog, which) -> {
                    switch (which) {
                        case 0: // WhatsApp
                            shareEmergencyToWhatsApp();
                            break;
                        case 1: // SMS
                            shareEmergencyToSMS();
                            break;
                        case 2: // Email
                            shareEmergencyToEmail();
                            break;
                        case 3: // All Apps
                            shareEmergencyToAllApps();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareEmergencyToWhatsApp() {
        try {
            String emergencyText = "🚨 EMERGENCY ALERT 🚨\n\n" + 
                                 binding.editTextEmergencyMessage.getText().toString().trim() + 
                                 "\n\n📍 My Location: " + currentLocationAddress +
                                 "\n\nGoogle Maps: https://maps.google.com/?q=" + currentLatitude + "," + currentLongitude +
                                 "\n\nSent from Bridge Emergency Alert";
            
            Uri audioUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", audioFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("audio/*");
            intent.setPackage("com.whatsapp");
            intent.putExtra(Intent.EXTRA_STREAM, audioUri);
            intent.putExtra(Intent.EXTRA_TEXT, emergencyText);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed or error sharing", Toast.LENGTH_SHORT).show();
            shareEmergencyToAllApps(); // Fallback to general sharing
        }
    }

    private void shareEmergencyToSMS() {
        try {
            String emergencyText = "🚨 EMERGENCY: " + 
                                 binding.editTextEmergencyMessage.getText().toString().trim() + 
                                 " Location: " + currentLocationAddress +
                                 " Maps: https://maps.google.com/?q=" + currentLatitude + "," + currentLongitude;
            
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:"));
            intent.putExtra("sms_body", emergencyText);
            
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening SMS app", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareEmergencyToEmail() {
        try {
            String emergencyText = "🚨 EMERGENCY ALERT 🚨\n\n" + 
                                 binding.editTextEmergencyMessage.getText().toString().trim() + 
                                 "\n\n📍 My Current Location: " + currentLocationAddress +
                                 "\n\nGoogle Maps Link: https://maps.google.com/?q=" + currentLatitude + "," + currentLongitude +
                                 "\n\nThis is an automated emergency alert sent from Bridge app." +
                                 "\nPlease respond immediately or contact emergency services if needed.";
            
            Uri audioUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", audioFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_STREAM, audioUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "🚨 EMERGENCY ALERT - Immediate Response Needed");
            intent.putExtra(Intent.EXTRA_TEXT, emergencyText);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(intent, "Send Emergency Alert via Email"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing via email", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareEmergencyToAllApps() {
        try {
            String emergencyText = "🚨 EMERGENCY ALERT 🚨\n\n" + 
                                 binding.editTextEmergencyMessage.getText().toString().trim() + 
                                 "\n\n📍 Location: " + currentLocationAddress +
                                 "\n\nMaps: https://maps.google.com/?q=" + currentLatitude + "," + currentLongitude +
                                 "\n\nSent from Bridge Emergency Alert";
            
            Uri audioUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", audioFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_STREAM, audioUri);
            intent.putExtra(Intent.EXTRA_TEXT, emergencyText);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(intent, "Send Emergency Alert"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing emergency alert", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAIPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_ai_prompt, null);
        builder.setView(dialogView);
        
        aiDialog = builder.create();
        aiDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Get references to dialog views
        EditText editTextPrompt = dialogView.findViewById(R.id.editTextPrompt);
        LinearLayout buttonSend = dialogView.findViewById(R.id.buttonSend);
        LinearLayout buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        LinearLayout progressLayout = dialogView.findViewById(R.id.aiProgressLayout);
        
        // Set placeholder text for emergency context
        editTextPrompt.setHint("e.g., Make it more urgent, Add medical details, Translate to local language...");
        
        // Set up button click listeners
        buttonCancel.setOnClickListener(v -> aiDialog.dismiss());
        
        buttonSend.setOnClickListener(v -> {
            String prompt = editTextPrompt.getText().toString().trim();
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Process the AI prompt
            processAIPrompt(prompt, progressLayout, buttonSend, buttonCancel);
        });
        
        aiDialog.show();
    }
    
    private void processAIPrompt(String prompt, LinearLayout progressLayout, LinearLayout buttonSend, LinearLayout buttonCancel) {
        String currentMessage = binding.editTextEmergencyMessage.getText().toString().trim();
        
        if (currentMessage.isEmpty()) {
            Toast.makeText(this, "Please enter an emergency message first, then use AI to enhance it", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Show progress bar and hide buttons
        progressLayout.setVisibility(android.view.View.VISIBLE);
        buttonSend.setVisibility(android.view.View.GONE);
        buttonCancel.setVisibility(android.view.View.GONE);
        
        // Start glowing animation
        android.view.animation.Animation glowAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.glow_pulse);
        progressLayout.startAnimation(glowAnimation);
        
        // Create the full prompt for Gemini with emergency context
        String fullPrompt = "Please enhance the following emergency message based on this instruction: \"" + prompt + "\"\n\n" +
                           "Original emergency message: \"" + currentMessage + "\"\n\n" +
                           "Important: This is an emergency message that will be sent with location information. " +
                           "Please keep it clear, urgent, and appropriate for emergency situations. " +
                           "Return only the enhanced emergency message without any additional explanation or formatting.";
        
        // Call Gemini API
        geminiHelper.callGemini(fullPrompt, new GeminiHelper.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    // Hide progress and update the main EditText
                    progressLayout.clearAnimation();
                    progressLayout.setVisibility(android.view.View.GONE);
                    
                    // Clean up the result (remove any extra formatting)
                    String enhancedMessage = result.trim();
                    if (enhancedMessage.startsWith("\"") && enhancedMessage.endsWith("\"")) {
                        enhancedMessage = enhancedMessage.substring(1, enhancedMessage.length() - 1);
                    }
                    
                    // Update the main message EditText
                    binding.editTextEmergencyMessage.setText(enhancedMessage);
                    
                    // Close the dialog
                    aiDialog.dismiss();
                    
                    // Show success message
                    Toast.makeText(EmergencyActivity.this, "Emergency message enhanced successfully!", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    // Hide progress and show buttons again
                    progressLayout.clearAnimation();
                    progressLayout.setVisibility(android.view.View.GONE);
                    buttonSend.setVisibility(android.view.View.VISIBLE);
                    buttonCancel.setVisibility(android.view.View.VISIBLE);
                    
                    // Show error message
                    Toast.makeText(EmergencyActivity.this, "AI processing failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        hideLoadingDialog();
        
        // Dismiss AI dialog if showing
        if (aiDialog != null && aiDialog.isShowing()) {
            aiDialog.dismiss();
        }
        
        super.onDestroy();
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
    
    private void setupToolbarButtons() {
        // Back button click listener
        binding.backBtn.setOnClickListener(v -> {
            finish(); // Close the activity and go back
        });
    }
}