# 🌉 Bridge - AI-Powered Communication Platform

<div align="center">

![Bridge Logo](https://img.shields.io/badge/Bridge-AI%20Communication-00F0FF?style=for-the-badge&logo=android&logoColor=white)

[![Android](https://img.shields.io/badge/Android-API%2026+-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248?style=flat-square&logo=mongodb&logoColor=white)](https://www.mongodb.com)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Gemini AI](https://img.shields.io/badge/Gemini-AI-4285F4?style=flat-square&logo=google&logoColor=white)](https://ai.google.dev/)

</div>

## 📋 Table of Contents
- [Project Overview](#-project-overview)
- [Features](#-features)
- [Technologies Used](#-technologies-used)
- [Architecture Overview](#-architecture-overview)
- [File Structure](#-file-structure)
- [API Documentation](#-api-documentation)
- [Installation Process](#-installation-process)

## 🚀 Project Overview

Bridge is a comprehensive AI-powered communication platform that combines intelligent conversation capabilities with practical features like meeting transcription, emergency assistance, and voice note management. The application consists of an Android mobile client with a modern dark theme and neon UI elements, backed by a robust Spring Boot REST API server with MongoDB integration.

The platform leverages Google's Gemini AI to provide intelligent responses, activity logging, and enhanced user experiences across all features. Bridge is designed to be a complete communication solution for modern users who need smart, reliable, and feature-rich messaging capabilities.

## ✨ Features

### 🤖 AI-Powered Chat
- **Intelligent Conversations**: Context-aware AI responses using Google Gemini
- **Real-time Messaging**: Smooth chat interface with message history
- **Smart Suggestions**: AI-powered conversation enhancements

### 📝 Meeting Transcription
- **Real-time Transcription**: Live meeting transcription with high accuracy
- **Smart Summaries**: AI-generated meeting summaries and insights
- **Export Options**: Save and share meeting transcripts

### 🚨 Emergency Assistance
- **Quick Emergency Alerts**: One-tap emergency message broadcasting
- **Location Integration**: Automatic GPS location sharing
- **Multi-platform Sharing**: WhatsApp, SMS, Email, and universal sharing
- **Voice Emergency Notes**: Text-to-speech emergency messages

### 🎙️ Voice Notes Management
- **Voice Recording**: High-quality voice note recording
- **Intelligent Transcription**: AI-powered voice-to-text conversion
- **Organization Tools**: Search and categorize voice notes

### 🔐 Authentication System
- **Secure Registration**: Complete user registration with validation
- **Email/Password Login**: Traditional authentication flow
- **Google Sign-In**: OAuth integration (planned)
- **Profile Management**: User profile and settings management

### 📊 Activity Logging
- **Automatic Tracking**: Smart activity logging across all features
- **AI Descriptions**: Gemini-generated activity summaries
- **Usage Analytics**: Comprehensive user activity insights

### 🎨 Modern UI/UX
- **Dark Theme**: Professional dark interface with neon accents
- **Material Design 3**: Latest Material Design components
- **Glowing Effects**: Signature neon glow UI elements
- **Responsive Design**: Optimized for various screen sizes
- **Interactive Onboarding**: Guided feature introduction

## 🛠️ Technologies Used

### Frontend (Android)
- **Language**: Java
- **Min SDK**: API 26 (Android 8.0)
- **Target SDK**: API 36
- **UI Framework**: Material Design 3
- **Architecture**: MVVM Pattern
- **Key Libraries**:
  - ViewPager2 for carousel interfaces
  - Lottie for animations
  - Retrofit for API communication
  - Google Generative AI (Gemini)
  - Play Services Location
  - RoundedImageView for profile pictures

### Backend (Spring Boot)
- **Language**: Java 21
- **Framework**: Spring Boot 3.5.6
- **Database**: MongoDB Atlas
- **Architecture**: RESTful API
- **Key Dependencies**:
  - Spring Boot Starter Web
  - Spring Boot Starter Data MongoDB
  - Lombok for boilerplate reduction
  - Maven for dependency management

### AI Integration
- **Google Gemini AI**: For intelligent responses and content generation
- **Text-to-Speech**: Android TTS for voice features
- **Location Services**: Google Play Services Location API

### Development Tools
- **Build System**: Gradle (Android), Maven (Backend)
- **Version Control**: Git
- **IDE**: Android Studio, IntelliJ IDEA

## 🏗️ Architecture Overview

### System Architecture
```
┌─────────────────┐    HTTP/REST    ┌─────────────────┐    MongoDB    ┌─────────────────┐
│                 │ ◄──────────────► │                 │ ◄────────────► │                 │
│  Android Client │                 │ Spring Boot API │               │ MongoDB Atlas   │
│                 │                 │                 │               │                 │
└─────────────────┘                 └─────────────────┘               └─────────────────┘
         │                                   │
         │                                   │
         ▼                                   ▼
┌─────────────────┐                 ┌─────────────────┐
│                 │                 │                 │
│   Gemini AI     │                 │  External APIs  │
│   Integration   │                 │  (Location,     │
│                 │                 │   Sharing)      │
└─────────────────┘                 └─────────────────┘
```

### Android App Architecture
- **Presentation Layer**: Activities and Fragments with Material Design UI
- **Business Logic Layer**: Utility classes and API integration
- **Data Layer**: SharedPreferences for local storage, Retrofit for API calls
- **AI Integration Layer**: Gemini AI helper classes for intelligent features

### Backend Architecture
- **Controller Layer**: REST endpoints for user and activity management
- **Service Layer**: Business logic and data processing
- **Repository Layer**: MongoDB data access with Spring Data
- **Model Layer**: Entity classes with Lombok annotations

## 📁 File Structure

```
Bridge/
├── 📱 Android App (app/)
│   ├── src/main/java/com/example/bridge/
│   │   ├── 🏠 MainActivity.java
│   │   ├── 🔐 Authentication/
│   │   │   ├── SignInActivity.java
│   │   │   └── RegisterActivity.java
│   │   ├── 💬 Chat/
│   │   │   └── ChatActivity.java
│   │   ├── 📝 Meeting/
│   │   │   └── MeetingActivity.java
│   │   ├── 🎙️ Voice/
│   │   │   └── VoiceNoteActivity.java
│   │   ├── 🚨 Emergency/
│   │   │   └── EmergencyActivity.java
│   │   ├── 🎯 Onboarding/
│   │   │   ├── OnboardingActivity.java
│   │   │   └── adapters/OnboardingAdapter.java
│   │   └── 🛠️ Utils/
│   │       ├── ActivityLogger.java
│   │       ├── GeminiHelper.java
│   │       └── ApiClient.java
│   └── src/main/res/
│       ├── layout/ (Activity layouts)
│       ├── values/ (Colors, strings, themes)
│       └── drawable/ (Icons and graphics)
│
├── 🖥️ Backend Server (BridgeServer/)
│   ├── src/main/java/com/example/BridgeServer/
│   │   ├── 🎮 controllers/
│   │   │   ├── UserController.java
│   │   │   └── ActivityController.java
│   │   ├── 📊 models/
│   │   │   ├── User.java
│   │   │   └── Activity.java
│   │   ├── 🗄️ repos/
│   │   │   ├── UserRepo.java
│   │   │   └── ActivityRepo.java
│   │   └── BridgeServerApplication.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── 📚 Documentation/
│   ├── AUTHENTICATION_README.md
│   ├── EMERGENCY_README.md
│   ├── ONBOARDING_README.md
│   ├── ACTIVITY_LOGGING_README.md
│   ├── FLOATING_BUTTON_README.md
│   ├── GRADIENT_BACKGROUNDS_README.md
│   └── RECENT_SESSIONS_README.md
│
└── 🔧 Configuration Files
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    └── .gitignore
```

## 📖 API Documentation

### Base URL
```
http://localhost:8080
```

### Authentication Endpoints

#### Register User
```http
POST /users/register
Content-Type: application/json

{
  "username": "johndoe",
  "password": "password123",
  "email": "john@example.com",
  "fullName": "John Doe"
}
```

**Response:**
```json
{
  "message": "User registered successfully",
  "user": {
    "id": "user_id",
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "createdAt": "2025-10-11T10:30:00"
  }
}
```

#### Login User
```http
POST /users/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "message": "Login successful",
  "user": {
    "id": "user_id",
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe"
  }
}
```

### User Management Endpoints

#### Get All Users
```http
GET /users
```

#### Get User by ID
```http
GET /users/{id}
```

#### Update User
```http
PUT /users/{id}
Content-Type: application/json

{
  "username": "newusername",
  "fullName": "New Full Name",
  "email": "newemail@example.com"
}
```

#### Delete User
```http
DELETE /users/{id}
```

#### Search Users
```http
GET /users/search/username/{username}
GET /users/search/email/{email}
```

### Activity Management Endpoints

#### Get User Activities
```http
GET /users/{userId}/activities
```

#### Create Activity
```http
POST /users/{userId}/activity
Content-Type: application/json

{
  "type": "Chat",
  "title": "Chat Session",
  "description": "Discussion about project requirements"
}
```

**Response:**
```json
{
  "id": "activity_id",
  "createdAt": "2025-10-11T10:30:00",
  "type": "Chat",
  "title": "Chat Session",
  "userId": "user_id",
  "description": "Discussion about project requirements"
}
```

#### Update Activity
```http
PUT /users/{userId}/activity/{id}
Content-Type: application/json

{
  "type": "Meeting",
  "title": "Updated Title",
  "description": "Updated description"
}
```

#### Delete Activity
```http
DELETE /users/{userId}/activity/{id}
```

### Error Responses
All endpoints return appropriate HTTP status codes:
- `200 OK`: Successful operation
- `201 Created`: Resource created successfully
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Authentication failed
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

## 🚀 Installation Process

### Prerequisites
- **Android Development**:
  - Android Studio Arctic Fox or later
  - Android SDK API 26+
  - Java 11 or later
  - Gradle 8.0+

- **Backend Development**:
  - Java 21
  - Maven 3.6+
  - MongoDB Atlas account
  - IntelliJ IDEA or Eclipse (optional)

### Backend Setup

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd Bridge/BridgeServer
   ```

2. **Configure MongoDB**
   - Create a MongoDB Atlas account at [mongodb.com](https://www.mongodb.com)
   - Create a new cluster and database
   - Update `application.properties`:
   ```properties
   spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/bridge
   spring.data.mongodb.database=bridge
   server.port=8080
   ```

3. **Install Dependencies**
   ```bash
   mvn clean install
   ```

4. **Run the Backend Server**
   ```bash
   mvn spring-boot:run
   ```
   
   The server will start at `http://localhost:8080`

### Android App Setup

1. **Open Android Studio**
   - Open the Bridge project folder in Android Studio
   - Wait for Gradle sync to complete

2. **Configure API Endpoints**
   - Update `ApiClient.java` with your backend server URL:
   ```java
   private static final String BASE_URL = "http://10.0.2.2:8080/"; // For emulator
   // or
   private static final String BASE_URL = "http://your-server-ip:8080/"; // For physical device
   ```

3. **Set up Gemini AI**
   - Get a Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
   - Add the API key to your `local.properties`:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

4. **Configure Network Security**
   - For development, the app is configured to allow cleartext traffic
   - For production, ensure HTTPS is used

5. **Build and Run**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio or use:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

### Development Setup

1. **Enable Developer Options**
   - Enable USB debugging on your Android device
   - Allow installation from unknown sources

2. **Testing**
   - Run backend tests: `mvn test`
   - Run Android tests: `./gradlew test`

3. **Database Setup**
   - The MongoDB collections (`users` and `activities`) will be created automatically
   - Ensure proper indexing for username and email fields

### Production Deployment

1. **Backend Deployment**
   - Build production JAR: `mvn clean package`
   - Deploy to cloud service (AWS, Google Cloud, Heroku)
   - Configure production MongoDB connection
   - Set up HTTPS with SSL certificates

2. **Android App Release**
   - Generate signed APK in Android Studio
   - Update API endpoints to production URLs
   - Test thoroughly on various devices
   - Publish to Google Play Store

### Troubleshooting

**Common Issues:**
- **Network Connection**: Ensure backend server is running and accessible
- **MongoDB Connection**: Verify MongoDB Atlas connection string and credentials
- **Gemini API**: Check API key validity and quota limits
- **Permissions**: Ensure location and storage permissions are granted
- **Build Errors**: Clean and rebuild project, sync Gradle files

**Logs and Debugging:**
- Backend logs: Check console output or application logs
- Android logs: Use `adb logcat` or Android Studio Logcat
- Network issues: Use network profiler in Android Studio

For additional support and detailed feature documentation, refer to the individual README files in the project directory.

---

## 🤝 Contributing Guidelines

Pull requests are welcome! For major changes, open an issue first to discuss scope/design.

**Branching**: `feat/*`, `fix/*`, `chore/*`
**Commit style**: Conventional Commits
**PR checklist**: tests, docs, accessible UI, screenshots for UI changes

---
## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### Ways to Contribute
- 🐛 **Bug Reports**: Found a bug? [Open an issue](https://github.com/SteveRogersBD/BridgeApp/issues)
- 💡 **Feature Requests**: Have an idea? [Start a discussion](https://github.com/SteveRogersBD/BridgeApp/discussions)
- 🔧 **Code Contributions**: Submit pull requests for bug fixes or new features
- 📖 **Documentation**: Help improve our docs and tutorials
---

## 👤 Contact / Author Info

**Aniruddha Biswas**

* GitHub: [https://github.com/SteveRogersBD](https://github.com/SteveRogersBD)
* LinkedIn: [https://linkedin.com/in/your-profile](https://www.linkedin.com/in/aniruddha-biswas-atanu-16b708228)
* Email: [cd43641@truman.edu](mailto:cd43641@truman.edu)
</div>
