# Floating Button Feature

A Messenger-style chat head floating button has been added to your Bridge app that works application-wide.

## Features

- **Auto-Start**: Automatically appears when the app launches
- **Persistent**: Stays active even when the app is closed
- **Draggable**: Move the floating button anywhere on the screen
- **Direct Chat Access**: Tap to open a floating chat interface
- **Long Press to Close**: Hold for 1 second to stop the floating button service
- **System Overlay**: Works over other apps (requires permission)

## How to Use

1. **First Launch**: 
   - App will show a dialog explaining the floating chat feature
   - Choose "Enable" to grant "Display over other apps" permission
   - Choose "Skip" to use the app without floating chat
   - If enabled, the floating button will appear and auto-start on future launches

2. **Using the Floating Button**:
   - **Drag**: Touch and move to reposition anywhere on screen
   - **Tap**: Quick tap to open the floating chat interface
   - **Long Press**: Hold for 1 second to remove the floating button

3. **Floating Chat Interface**:
   - **Text Input**: Type messages and send them
   - **Voice Input**: Press and hold mic button to record voice messages
   - **Text-to-Speech**: Messages are spoken aloud automatically
   - **Full Chat History**: Scrollable conversation view
   - **Click outside or close button**: Dismiss the chat overlay

4. **Managing Floating Chat**:
   - **Toggle On/Off**: Use the microphone icon in the main app toolbar
   - **Permission Required**: App will guide you through enabling overlay permission
   - **Settings Access**: Can also be managed through Android Settings > Apps > Bridge

## Permissions Required

- `SYSTEM_ALERT_WINDOW`: To display over other apps
- `FOREGROUND_SERVICE`: To keep the button active in background
- `FOREGROUND_SERVICE_SPECIAL_USE`: For the floating button service type

## Files Added/Modified

### New Files:
- `BridgeApplication.java` - Application class for auto-starting floating button
- `FloatingButtonService.java` - Main service handling the floating button
- `FloatingButtonManager.java` - Helper class for permissions and service management
- `FloatingChatOverlay.java` - Floating chat interface manager
- `floating_button_layout.xml` - Layout for the floating button
- `floating_button_background.xml` - Circular background drawable
- `floating_chat_overlay.xml` - Layout for the floating chat interface
- `simple_floating_chat.xml` - Fallback chat layout
- Various drawable backgrounds for chat UI components

### Modified Files:
- `AndroidManifest.xml` - Added permissions, service registration, and Application class
- `SplashActivity.java` - Added proper overlay permission flow with user dialogs
- `MainActivity.java` - Added floating button toggle in toolbar menu
- `BridgeApplication.java` - Updated auto-start logic to respect user preferences

## Customization

You can customize the floating button by modifying:
- **Size**: Change dimensions in `floating_button_layout.xml`
- **Color**: Update `floating_button_background.xml`
- **Icon**: Replace the microphone icon with any drawable
- **Position**: Modify initial X,Y coordinates in `FloatingButtonService.java`

The floating button will persist across app switches and even when the app is closed, providing direct access to the Bridge chat interface from anywhere on the device. It automatically starts when the app is launched and remembers its enabled/disabled state. The chat interface appears as a beautiful floating overlay with full functionality including voice input, text-to-speech, and conversation history.