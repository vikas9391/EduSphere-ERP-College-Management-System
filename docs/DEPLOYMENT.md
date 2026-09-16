# EduSphere ERP — Deployment Guide

## Architecture

```text
Web Browser ───────┐
                   ├──> Spring Boot API ───> PostgreSQL
Mobile App ────────┘             │
                                 ├──> Redis (where enabled)
                                 └──> OpenAI (AI timetable import, optional)
```

The web and mobile applications use the same backend API. Do not create a separate mobile backend.

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

Never put `OPENAI_API_KEY` in the web or mobile application.

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

## Mobile application

Directory: `app/`

The mobile app is Expo/React Native and uses the same backend.

Install and run:

```bash
cd app
npm install
npm start
```

Backend URL:

```env
EXPO_PUBLIC_API_URL=https://your-api-domain.example/api
```

For a physical device, do not use `localhost` unless the backend is running on the device itself. During local development, use the computer's LAN IP or an accessible development URL.

## Mobile release readiness

Before publishing Android/iOS builds:

- [ ] Complete Student/Teacher/Admin navigation.
- [ ] Complete academic modules.
- [ ] Add refresh-token handling.
- [ ] Add robust network/error states.
- [ ] Add icons and splash assets.
- [ ] Configure Android application ID and iOS bundle ID for production.
- [ ] Test against production backend.
- [ ] Test tenant isolation and role restrictions.
- [ ] Run Android/iOS release builds.
- [ ] Test on physical devices.

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
- Full mobile module implementation and mobile CI.
- Provider-specific frontend SPA configuration after hosting selection.
- Production monitoring and backups.
- Final dependency/security review.
