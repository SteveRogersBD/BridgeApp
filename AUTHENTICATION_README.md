# Bridge App - Authentication System

## Overview
The Bridge app now includes a complete authentication system with Register and Sign In functionality, featuring a professional dark theme with glowing UI elements.

## Features

### Authentication Flow
1. **RegisterActivity** - User registration with comprehensive form validation
2. **SignInActivity** - User sign-in with email and password
3. **OnboardingActivity** - Post-authentication onboarding experience

### Registration Page Features
- **Full Name Field** - User's complete name with person icon
- **Username Field** - Unique username with custom user icon
- **Email Field** - Email address with validation and email icon
- **Password Field** - Secure password with toggle visibility and lock icon
- **Sign Up Button** - Primary action with neon glow effect
- **Google Sign-In** - Alternative authentication method
- **Sign In Link** - Navigation to existing user login

### Sign In Page Features
- **Email Field** - Email address with validation
- **Password Field** - Secure password input with toggle visibility
- **Forgot Password** - Password recovery option
- **Sign In Button** - Primary action with neon glow effect
- **Google Sign-In** - Alternative authentication method
- **Sign Up Link** - Navigation to new user registration

## Design Elements

### Visual Theme
- **Dark Background** - Consistent `#05070A` app background
- **Neon Aqua Accents** - Primary color `#00F0FF` for highlights
- **Glowing Effects** - Shadow and elevation effects for depth
- **Material Design 3** - Modern Material components and styling

### UI Components
- **App Logo** - Prominently displayed at the top of each screen
- **Card Layout** - Form fields contained in elevated cards with neon borders
- **Input Fields** - Material TextInputLayout with custom styling:
  - Filled background with rounded corners
  - Neon stroke colors and hint text
  - Leading icons for visual context
  - Password toggle for security
- **Buttons** - Elevated buttons with glow effects and ripple animations
- **Typography** - Hierarchical text styling with shadow effects

### Form Validation
- **Real-time Validation** - Immediate feedback on input errors
- **Email Validation** - Pattern matching for valid email addresses
- **Password Requirements** - Minimum 6 characters for security
- **Username Requirements** - Minimum 3 characters for uniqueness
- **Required Field Checks** - All fields validated before submission

## Technical Implementation

### Key Components
- `RegisterActivity.java` - Registration form controller with validation
- `SignInActivity.java` - Sign-in form controller with validation
- `activity_register.xml` - Registration page layout
- `activity_sign_in.xml` - Sign-in page layout

### Icon Resources
- `ic_person.xml` - Full name field icon
- `ic_username.xml` - Username field icon
- `ic_email.xml` - Email field icon
- `ic_lock.xml` - Password field icon
- `ic_google.xml` - Google Sign-In button icon

### Navigation Flow
```
App Launch → RegisterActivity ⟷ SignInActivity → OnboardingActivity → MainActivity
```

### Input Field Styling
- **Background Color** - `@color/card_bg` for subtle contrast
- **Stroke Color** - `@color/primary` for neon accent
- **Corner Radius** - 12dp for modern rounded appearance
- **Icon Tint** - `@color/primary` for consistent theming

### Button Styling
- **Primary Buttons** - Neon aqua background with white text
- **Secondary Buttons** - Outlined style with neon stroke
- **Elevation** - 8dp for floating appearance
- **Shadow Effects** - Neon glow using `shadowRadius` and `shadowColor`

## User Experience

### Registration Flow
1. User enters full name, username, email, and password
2. Real-time validation provides immediate feedback
3. Submit button triggers comprehensive validation
4. Successful registration navigates to onboarding
5. Alternative Google Sign-In option available
6. Existing users can navigate to Sign In

### Sign In Flow
1. User enters email and password credentials
2. Form validation ensures proper input format
3. Forgot password option for account recovery
4. Successful sign-in navigates to onboarding
5. Alternative Google Sign-In option available
6. New users can navigate to Registration

### Error Handling
- **Field-level Errors** - Individual input validation with error messages
- **Toast Messages** - Success and error notifications
- **Focus Management** - Automatic focus on error fields
- **Accessibility** - Proper content descriptions and focus handling

## Security Considerations
- **Password Visibility Toggle** - Secure input with optional visibility
- **Input Validation** - Client-side validation for immediate feedback
- **Email Pattern Matching** - Ensures valid email format
- **Minimum Password Length** - 6 characters for basic security

## Future Enhancements
- **Google Sign-In Integration** - OAuth implementation
- **Forgot Password Flow** - Email-based password recovery
- **Biometric Authentication** - Fingerprint/face recognition
- **Social Login Options** - Facebook, Twitter integration
- **Two-Factor Authentication** - Enhanced security options

## Usage
The authentication system is now the entry point for the Bridge app. New users will see the registration screen first, while returning users can navigate to sign in. Both flows lead to the onboarding experience before accessing the main application features.