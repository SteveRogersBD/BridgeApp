# Recent Sessions Feature

This document explains the Recent Sessions feature added to the MainActivity.

## Overview

The Recent Sessions section displays a list of recent user activities below the main feature cards. It shows demo data with different session types and timestamps.

## Components

### 1. Models
- `SessionItem.java` - Model class for session data with type, title, description, and timestamp

### 2. Layout Files
- `item_session.xml` - Layout for individual session items with Material Design cards
- `badge_background.xml` - Drawable for session type badges
- Updated `activity_main.xml` - Added Recent Sessions section

### 3. Adapter
- `SessionsAdapter.java` - RecyclerView adapter for displaying session items

### 4. MainActivity Integration
- Added `setupRecentSessions()` method with fake demo data
- Integrated sessions RecyclerView with LinearLayoutManager

## Features

### Session Types
- **Voice Note** - Shows mic icon
- **Chat** - Shows chat icon  
- **Meeting** - Shows transcription icon

### Time Formatting
- "Just now" - Less than 1 minute
- "X min ago" - Minutes
- "X hour(s) ago" - Hours
- "X day(s) ago" - Days
- "1 week ago" - Older than 7 days

### Demo Data
1. **Voice Note** - "Morning Workout Plan" (5 min ago)
2. **Meeting** - "Team Standup Meeting" (2 hours ago)
3. **Chat** - "AI Assistant Conversation" (4 hours ago)
4. **Voice Note** - "Shopping List Reminder" (1 day ago)
5. **Meeting** - "Client Presentation" (2 days ago)

## UI Design

### Session Card Layout
- **Header**: Type badge with icon + timestamp (top right)
- **Title**: Bold session title (max 2 lines)
- **Description**: Session description (max 3 lines)

### Styling
- Material Design cards with rounded corners
- Neon theme colors matching app design
- Type badges with primary color scheme
- Proper spacing and typography

### Badge Design
- Rounded rectangle background
- Icon + text layout
- Primary color scheme
- Different icons per session type

## Layout Structure

```
MainActivity
├── Toolbar
├── ScrollView
    ├── Hero Section (Profile + Welcome)
    ├── Feature Cards (2x2 Grid)
    └── Recent Sessions Section
        ├── Section Title
        └── Sessions RecyclerView (Linear)
```

## Future Enhancements

- Replace demo data with real API data
- Add click handlers for session items
- Implement pull-to-refresh
- Add session filtering/search
- Integrate with activity logging system
- Add session deletion/management