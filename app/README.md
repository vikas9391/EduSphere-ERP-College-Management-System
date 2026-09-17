# EduSphere ERP — Flutter Mobile App

The `app/` directory is now a Flutter/Dart mobile application. The previous Expo/React Native implementation has been removed.

## Requirements
- Flutter stable
- Dart 3.5+
- Android Studio or an Android SDK for Android builds

## Run

```bash
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

For a physical phone, replace `API_URL` with the reachable backend URL, for example:

```bash
flutter run --dart-define=API_URL=https://your-backend.example.com/api
```

The default API URL is `http://localhost:8080/api`.

## Build

```bash
flutter build apk --release --dart-define=API_URL=https://your-backend.example.com/api
```

## Included
- College-code, username and password login
- Access/refresh token handling
- Student, teacher and admin role dashboards
- Student academic overview
- Teacher teaching overview
- Session persistence and logout
- Material 3 green/white EduSphere design system
- Backend REST integration
