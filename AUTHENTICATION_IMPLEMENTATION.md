# Authentication Implementation

This document explains the authentication system implemented in the Bridge app.

## Overview

The authentication system uses Retrofit to communicate with the backend API and SharedPreferences to store user data locally.

## Components

### 1. Models
- `LoginRequest.java` - Request model for login API
- `LoginResponse.java` - Response model from login API  
- `User.java` - User data model

### 2. API Interface
- `AuthApi.java` - Retrofit interface for authentication endpoints
- `ApiClient.java` - Retrofit client configuration

### 3. Utilities
- `PreferenceManager.java` - Handles local storage of user data

## API Endpoint

**Login Endpoint:** `POST http://localhost:8080/users`

**Request Body:**
```json
{
  "email": "aniruddhabiswas106@gmail.com",
  "password": "1234"
}
```

**Response:**
```json
{
  "message": "Login successful",
  "user": {
    "id": "68ea9effc0ececee417bd2fc",
    "username": "aniruddha",
    "fullName": "Aniruddha Biswas Atanu",
    "dp": null,
    "createdAt": "2025-10-11T13:16:31.653",
    "email": "aniruddhabiswas106@gmail.com",
    "password": "1234"
  }
}
```

## Usage

### Login Process
1. User enters email and password in `SignInActivity`
2. App validates input and makes API call using Retrofit
3. On successful login, user data is stored in SharedPreferences
4. User is redirected to the main app

### Accessing User Data
```java
// Get current user ID
String userId = PreferenceManager.getCurrentUserId(context);

// Check if user is logged in
boolean isLoggedIn = PreferenceManager.isUserLoggedIn(context);

// Get user details
PreferenceManager prefManager = new PreferenceManager(context);
String userEmail = prefManager.getUserEmail();
String userName = prefManager.getUserName();
```

### Logout
```java
// Logout user and clear all data
PreferenceManager.logout(context);
```

## Features

- **Auto-login:** App checks if user is already logged in on startup
- **Data persistence:** User data is stored locally using SharedPreferences
- **Error handling:** Network errors and API errors are handled gracefully
- **Loading states:** UI shows loading state during API calls
- **Input validation:** Email and password validation before API call

## Security Notes

- User ID is stored locally for session management
- Consider implementing token-based authentication for production
- Add proper error handling for different HTTP status codes
- Implement secure storage for sensitive data in production