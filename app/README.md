# EduSphere ERP — Flutter Mobile App

The `app/` directory is the current Flutter/Dart mobile application for EduSphere ERP. The previous Expo/React Native implementation has been removed.

The mobile app connects to the EduSphere Spring Boot REST API and uses role-aware authentication for students and teachers.

## Requirements

- Flutter stable
- Dart 3.5+
- Android Studio or an Android SDK for Android builds
- A running EduSphere backend/API

## Run

Install dependencies:

```bash
flutter pub get
```

Run against a local backend from an Android emulator:

```bash
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

For a physical phone, use a backend URL reachable from the phone:

```bash
flutter run --dart-define=API_URL=https://your-backend.example.com/api
```

The default API URL is:

```
http://localhost:8080/api
```

## Build

Release APK:

```bash
flutter build apk --release --dart-define=API_URL=https://your-backend.example.com/api
```

## Current Mobile Features

### Authentication & Security

- College-code + email + password login
- JWT access-token authentication
- Refresh-token handling
- Automatic access-token refresh after a 401 response
- Session persistence using `shared_preferences`
- Secure logout/session clearing
- First-login mandatory password-change flow
- Forgot-password request flow using college code + email
- Role-aware routing for authenticated sessions

### Student Experience

- Student dashboard connected to `/student/dashboard`
- Academic profile overview
- Attendance percentage
- Subject count
- Pending assignment count
- Student assignment list from `/student/assignments`
- Assignment due dates and submission status
- Assignment submission using a submission URL
- Submitted marks and teacher feedback display
- Pull-to-refresh for dashboard and assignments

### Teacher Experience

- Teacher dashboard connected to `/teacher/dashboard`
- Teaching overview
- Subject count
- Student count
- Pending review count
- Teacher assignment list from `/teacher/assignments`
- Load assigned classes and class subjects
- Create assignments with:
  - Class/subject
  - Title
  - Description
  - Due date
  - Maximum marks
- View assignment submissions
- Evaluate student submissions with marks and feedback
- Pull-to-refresh for assignment data

### API Integration

The mobile API layer currently supports:

- GET requests
- POST requests
- PUT requests
- DELETE requests
- JSON request/response handling
- API error handling
- Automatic token refresh and retry
- Authenticated REST requests

Important assignment endpoints used by the app include:

```
/student/assignments
/teacher/assignments
/classes/mine
/classes/{id}/subjects
/assignments
/submissions
/submissions/assignment/{id}
/submissions/{id}/evaluate
```

Authentication endpoints used include:

```
/auth/login
/auth/refresh
/auth/forgot-password
```

## UI & Design

- Material 3 Flutter UI
- EduSphere green/white visual system
- Responsive layouts using standard Flutter widgets
- Rounded cards and form controls
- Loading, empty, success and error states
- Refresh interactions for data-heavy screens
- Mobile-first campus experience

## Project Structure

Key mobile files:

```
app/
├── lib/
│   ├── main.dart
│   ├── auth_screens.dart
│   └── assignments.dart
├── android/
├── ios/
├── test/
├── pubspec.yaml
└── README.md
```

### `main.dart`

Contains:

- Application/theme configuration
- API service
- Authentication/session handling
- Login screen
- Root authenticated-session routing
- Student/teacher dashboard
- Logout handling

### `auth_screens.dart`

Contains:

- Forgot-password workflow
- Password reset request UI
- First-login password-change UI

### `assignments.dart`

Contains:

- Student assignments
- Student assignment submission
- Teacher assignments
- Assignment creation
- Assignment submission review
- Submission evaluation

## Backend Compatibility

The Flutter app is designed to work with the EduSphere Spring Boot backend in this repository.

Make sure the backend is running and the configured `API_URL` points to the backend's `/api` base path.

## Validation

Flutter static analysis is validated through the repository's GitHub Actions workflow. Backend and web builds are also checked by CI.

