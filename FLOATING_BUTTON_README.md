# Floating Button Feature

A Messenger-style chat head floating button has been added to your Bridge app that works application-wide.

## Features

- **Auto-Start**: Automatically appears when the app launches
- **Persistent**: Stays active even when the app is closed
- **Draggable**: Move the floating button anywhere on the screen
- **Quick Actions**: Tap to open a floating page with app shortcuts
- **Long Press to Close**: Hold for 1 second to stop the floating button service
- **System Overlay**: Works over other apps (requires permission)

## How to Use

1. **First Launch**: 
   - The floating button will request "Display over other apps" permission
   - Grant the permission to enable the floating button
   - The button will auto-start on subsequent app launches

2. **Using the Floating Button**:
   - **Drag**: Touch and move to reposition anywhere on screen
   - **Tap**: Quick tap to open the floating quick actions page
   - **Long Press**: Hold for 1 second to remove the floating button

3. **Quick Actions Overlay**:
   - **Meeting**: Start a new meeting recording session
   - **Chat**: Open the chat interface
   - **Call**: Access call functionality
   - **Transcript**: View saved transcripts
   - **Click outside or close button**: Dismiss the overlay

## Permissions Required

- `SYSTEM_ALERT_WINDOW`: To display over other apps
- `FOREGROUND_SERVICE`: To keep the button active in background
- `FOREGROUND_SERVICE_SPECIAL_USE`: For the floating button service type

## Files Added/Modified

### New Files:
- `BridgeApplication.java` - Application class for auto-starting floating button
- `FloatingButtonService.java` - Main service handling the floating button
- `FloatingButtonManager.java` - Helper class for permissions and service management
- `FloatingPageOverlay.java` - Quick actions floating overlay manager
- `floating_button_layout.xml` - Layout for the floating button
- `floating_button_background.xml` - Circular background drawable
- `floating_page_overlay.xml` - Layout for the quick actions overlay

### Modified Files:
- `AndroidManifest.xml` - Added permissions, service registration, and Application class
- `SplashActivity.java` - Added overlay permission request on first launch

## Customization

You can customize the floating button by modifying:
- **Size**: Change dimensions in `floating_button_layout.xml`
- **Color**: Update `floating_button_background.xml`
- **Icon**: Replace the microphone icon with any drawable
- **Position**: Modify initial X,Y coordinates in `FloatingButtonService.java`

The floating button will persist across app switches and even when the app is closed, providing quick access to all Bridge app features from anywhere on the device. It automatically starts when the app is launched and remembers its enabled/disabled state. The quick actions appear as a beautiful floating overlay that doesn't interfere with other apps.