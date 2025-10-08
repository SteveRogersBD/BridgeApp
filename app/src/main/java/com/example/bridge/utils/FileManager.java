package com.example.bridge.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for managing file operations for audio recordings and transcripts
 */
public class FileManager {
    private static final String TAG = "FileManager";
    private static final String RECORDINGS_DIR = "BridgeRecordings";
    private static final String AUDIO_EXTENSION = ".wav";
    private static final String TRANSCRIPT_EXTENSION = ".txt";
    
    private final Context context;
    
    public FileManager(Context context) {
        this.context = context;
    }
    
    /**
     * Result class for save operations
     */
    public static class SaveResult {
        public final boolean success;
        public final String message;
        public final String audioFilePath;
        public final String transcriptFilePath;
        public final Exception error;
        
        public SaveResult(boolean success, String message, String audioFilePath, String transcriptFilePath, Exception error) {
            this.success = success;
            this.message = message;
            this.audioFilePath = audioFilePath;
            this.transcriptFilePath = transcriptFilePath;
            this.error = error;
        }
        
        public static SaveResult success(String message, String audioPath, String transcriptPath) {
            return new SaveResult(true, message, audioPath, transcriptPath, null);
        }
        
        public static SaveResult failure(String message, Exception error) {
            return new SaveResult(false, message, null, null, error);
        }
    }
    
    /**
     * Saves both audio and transcript files
     * @param transcriptText The transcript text to save (can be empty)
     * @param hasAudioData Whether there is actual audio data to save
     * @return SaveResult with operation details
     */
    public SaveResult saveRecording(String transcriptText, boolean hasAudioData) {
        try {
            // Create recordings directory if it doesn't exist
            File recordingsDir = getRecordingsDirectory();
            if (!recordingsDir.exists() && !recordingsDir.mkdirs()) {
                return SaveResult.failure("Failed to create recordings directory", 
                    new IOException("Could not create directory: " + recordingsDir.getAbsolutePath()));
            }
            
            // Generate timestamp-based filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String baseFileName = "recording_" + timestamp;
            
            String audioFilePath = null;
            String transcriptFilePath = null;
            
            // Save audio file (placeholder for actual audio data)
            if (hasAudioData) {
                File audioFile = new File(recordingsDir, baseFileName + AUDIO_EXTENSION);
                audioFilePath = audioFile.getAbsolutePath();
                
                // TODO: Implement actual audio file saving when audio recording is integrated
                // For now, create a placeholder file to simulate the save operation
                try {
                    if (!audioFile.createNewFile()) {
                        Log.w(TAG, "Audio file already exists: " + audioFilePath);
                    }
                } catch (IOException e) {
                    return SaveResult.failure("Failed to create audio file", e);
                }
            }
            
            // Save transcript file if there's content
            if (transcriptText != null && !transcriptText.trim().isEmpty()) {
                File transcriptFile = new File(recordingsDir, baseFileName + TRANSCRIPT_EXTENSION);
                transcriptFilePath = transcriptFile.getAbsolutePath();
                
                try (FileWriter writer = new FileWriter(transcriptFile)) {
                    // Add metadata header
                    writer.write("Recording Transcript\n");
                    writer.write("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n");
                    writer.write("Audio file: " + (audioFilePath != null ? new File(audioFilePath).getName() : "N/A") + "\n");
                    writer.write("Word count: " + countWords(transcriptText) + "\n");
                    writer.write("\n--- TRANSCRIPT ---\n\n");
                    writer.write(transcriptText);
                } catch (IOException e) {
                    return SaveResult.failure("Failed to save transcript file", e);
                }
            }
            
            // Generate success message
            String message = generateSuccessMessage(transcriptText, audioFilePath != null, transcriptFilePath != null);
            return SaveResult.success(message, audioFilePath, transcriptFilePath);
            
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during save operation", e);
            return SaveResult.failure("Unexpected error occurred while saving", e);
        }
    }
    
    /**
     * Gets or creates the recordings directory
     */
    private File getRecordingsDirectory() {
        // Use app-specific external storage directory
        File externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        return new File(externalDir, RECORDINGS_DIR);
    }
    
    /**
     * Counts words in the transcript text
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
    
    /**
     * Generates appropriate success message based on what was saved
     */
    private String generateSuccessMessage(String transcriptText, boolean audioSaved, boolean transcriptSaved) {
        if (audioSaved && transcriptSaved) {
            int wordCount = countWords(transcriptText);
            return "Recording and transcript saved! (" + wordCount + " words)";
        } else if (audioSaved && !transcriptSaved) {
            return "Audio recording saved! (No transcript available)";
        } else if (!audioSaved && transcriptSaved) {
            int wordCount = countWords(transcriptText);
            return "Transcript saved! (" + wordCount + " words)";
        } else {
            return "Save completed";
        }
    }
    
    /**
     * Checks if external storage is available for writing
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    
    /**
     * Gets the recordings directory path for display purposes
     */
    public String getRecordingsDirectoryPath() {
        return getRecordingsDirectory().getAbsolutePath();
    }
}