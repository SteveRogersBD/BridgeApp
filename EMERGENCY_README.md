# Emergency Alert Feature

## Overview
The Emergency Alert feature allows users to quickly send emergency messages with their current location via voice notes to multiple platforms including WhatsApp, SMS, Email, and other apps.

## Features

### 1. Emergency Message Input
- Large text input area for describing the emergency situation
- AI-powered message enhancement using Gemini AI
- Real-time location detection and display

### 2. Location Services
- Automatic location detection using GPS
- Address resolution using Geocoder
- Fallback to coordinates if address resolution fails
- Location permission handling

### 3. Voice Note Generation
- Text-to-Speech conversion of emergency message
- Includes location information in the voice note
- Professional emergency alert format

### 4. Multi-Platform Sharing
- **WhatsApp**: Direct sharing with emergency text and voice note
- **SMS**: Text message with location and Google Maps link
- **Email**: Detailed emergency email with voice note attachment
- **All Apps**: Universal sharing via Android's share sheet

### 5. AI Enhancement
- Gemini AI integration for message improvement
- Emergency-specific prompts and suggestions
- Context-aware enhancements

## UI Components

### Layout Structure
- **Toolbar**: Emergency Alert title with back navigation
- **Message Input Card**: Large text area with emergency icon
- **Location Status Card**: Shows current location with GPS icon
- **Action Buttons**: AI enhancement and Send Alert buttons

### Design Elements
- Emergency theme with red accent colors (`stroke_red`)
- Consistent with app's dark theme and neon color scheme
- Material Design cards with proper elevation and corners
- Responsive layout with proper spacing

## Technical Implementation

### Permissions Required
- `ACCESS_FINE_LOCATION`: For precise location detection
- `ACCESS_COARSE_LOCATION`: For approximate location as fallback

### Dependencies
- Google Play Services Location API
- Android Text-to-Speech
- Gemini AI integration
- FileProvider for secure file sharing

### Key Classes
- `EmergencyActivity.java`: Main activity handling all emergency functionality
- `GeminiHelper.java`: AI integration for message enhancement
- Location services integration with `FusedLocationProviderClient`

## Usage Flow

1. **Open Emergency Activity**: User navigates to emergency feature
2. **Location Detection**: App automatically detects and displays current location
3. **Message Input**: User enters emergency description
4. **AI Enhancement** (Optional): User can enhance message using AI
5. **Voice Note Creation**: App converts message to voice note with location
6. **Platform Selection**: User chooses sharing platform
7. **Emergency Alert Sent**: Message and location shared via selected platform

## Emergency Message Format

### Voice Note Content
```
EMERGENCY ALERT: [User's emergency message]. My current location is: [Full address or coordinates]
```

### Text Message Content
```
🚨 EMERGENCY ALERT 🚨

[User's emergency message]

📍 My Location: [Full address]

Google Maps: https://maps.google.com/?q=[latitude],[longitude]

Sent from Bridge Emergency Alert
```

## Error Handling
- Location permission denied: Shows appropriate message and guidance
- Location unavailable: Falls back to coordinates
- TTS failure: Shows error message with retry option
- Sharing app unavailable: Falls back to universal sharing
- AI processing failure: Allows manual message sending

## Security & Privacy
- Location data only used for emergency purposes
- No location data stored permanently
- Secure file sharing using FileProvider
- User consent required for location access

## Future Enhancements
- Emergency contacts integration
- Automatic emergency service calling
- Medical information inclusion
- Multiple language support
- Offline emergency mode