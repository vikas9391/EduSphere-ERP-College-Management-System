# EduSphere ERP — Deployment Guide

## Architecture

```text
Web Browser ───────┐
                   ├──> Spring Boot API ───> PostgreSQL
Flutter Mobile ────┘             │
                                 ├──> Redis (where enabled)
                                 └──> OpenAI (AI timetable import, optional)
```

The web and Flutter mobile applications use the same backend API. Do not create a separate mobile backend.

## Backend

### Runtime

- Java 21
- Spring Boot 3.5.3
- PostgreSQL
- Flyway
- Port `8080` by default

### Required environment variables

```env
DB_URL=jdbc:postgresql://HOST:5432/college_erp
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
FRONTEND_URL=https://your-web-domain.example
CORS_ALLOWED_ORIGINS=https://your-web-domain.example
```

For AI timetable inspection:

```env
OPENAI_API_KEY=...
OPENAI_TIMETABLE_MODEL=gpt-5-mini
```

Never put `OPENAI_API_KEY` in the web or Flutter application.

### Deployment checks

1. Provision PostgreSQL.
2. Configure the backend environment variables.
3. Start the Spring Boot application.
4. Confirm Flyway public migrations complete.
5. Confirm tenant provisioning works.
6. Confirm the health endpoint is available.
7. Verify authentication and refresh tokens.
8. Verify tenant isolation.

## Web application

Directory: `frontend-web/`

Build:

```bash
npm install
npm run build
```

Output: `frontend-web/dist/`

Production environment:

```env
VITE_API_URL=https://your-api-domain.example/api
```

Because the application uses browser-side routing, configure the selected static host to serve `index.html` for application routes. The exact rewrite configuration depends on the provider and is intentionally not hard-coded until a provider is selected.

## Flutter mobile application

Directory: `app/`

The mobile client is **Flutter/Dart**. The former Expo/React Native implementation has been removed.

Install and run:

```bash
cd app
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

For a physical device, use a backend URL reachable from the device rather than `localhost`.

The API URL is provided through Dart compile-time configuration:

```bash
flutter run --dart-define=API_URL=https://your-api-domain.example/api
```

There is no npm, Expo, React Native, or `EXPO_PUBLIC_API_URL` configuration for the mobile client.

## Flutter release readiness

Before publishing Android/iOS builds:

- [ ] Complete Student/Teacher/Admin Flutter navigation.
- [ ] Complete all required academic modules.
- [x] Access/refresh-token handling foundation implemented.
- [ ] Add robust network/offline states.
- [ ] Add icons and splash assets.
- [ ] Configure Android application ID and iOS bundle ID for production.
- [ ] Test against production backend.
- [ ] Test tenant isolation and role restrictions.
- [ ] Run Android/iOS release builds.
- [ ] Test on physical devices.

Android release example:

```bash
cd app
flutter build apk --release --dart-define=API_URL=https://your-api-domain.example/api
```

For Play Store distribution, use `flutter build appbundle --release` after configuring Android signing.

For iOS distribution on macOS, configure Xcode signing and use `flutter build ipa --release`.

## CI

CI validates the backend, web application and Flutter mobile application. The Flutter job installs Flutter dependencies and runs `flutter analyze`; it does not install Node/Expo dependencies or run a TypeScript mobile check.

## Production smoke test

### Authentication

- [ ] Valid tenant login succeeds.
- [ ] Invalid credentials are rejected.
- [ ] Access token is accepted.
- [ ] Refresh flow works.
- [ ] Logout clears the client session.

### Academic workflow

- [ ] Admin creates a class.
- [ ] Students are added to the class.
- [ ] ClassSubjects are created.
- [ ] Teachers are assigned.
- [ ] ClassEnrollments are created.
- [ ] Attendance works.
- [ ] Holidays are excluded from attendance percentages.
- [ ] Assignments/submissions work.
- [ ] Exams/marks/results work.
- [ ] Timetable creation and conflict checks work.
- [ ] AI timetable import works when configured.

### Security

- [ ] Students cannot access another student's records.
- [ ] Teachers cannot manage another teacher's ClassSubjects.
- [ ] Cross-tenant records cannot be accessed.
- [ ] CORS allows only intended origins.
- [ ] Production secrets are not committed.
- [ ] File upload limits remain enabled.

## Known remaining hardening

- Batch/transactional confirmation for multi-slot AI timetable imports.
- Full Flutter mobile module implementation.
- Provider-specific frontend SPA configuration after hosting selection.
- Production monitoring and backups.
- Final dependency/security review.
