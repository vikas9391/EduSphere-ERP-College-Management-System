# EduSphere ERP — Deployment Guide

This guide is provider-neutral. Add provider-specific configuration only after the production hosting target is selected.

## 1. Architecture to deploy

```text
Browser
  ↓
React/Vite frontend
  ↓ HTTPS
Spring Boot backend
  ↓
PostgreSQL
  ↓
Tenant schemas

Optional:
Spring Cache / Redis
OpenAI timetable import
SMTP mail
```

## 2. Frontend deployment

Build from `frontend-web`:

```bash
npm install
npm run build
```

The production output is:

```text
frontend-web/dist
```

Set the production environment variable before building:

```env
VITE_API_URL=https://YOUR-BACKEND-DOMAIN/api
```

The frontend uses this value as the API base URL.

### SPA routing

The frontend uses React Router. The hosting provider must return `index.html` for application routes that are not real files.

Examples:

- Vercel: add a rewrite to `/index.html`.
- Netlify: add an SPA redirect to `/index.html`.
- Apache/Hostinger: configure an `.htaccess` rewrite to `/index.html`.
- Nginx: configure a `try_files` fallback to `/index.html`.

Do not add all of these files to the repository. Add only the configuration required by the selected host.

## 3. Backend deployment

Requirements:

- Java 21 runtime.
- PostgreSQL.
- Port 8080 or the platform's mapped application port.
- Network access from backend to PostgreSQL.
- Optional Redis if the deployed configuration uses it.

Build:

```bash
cd Backend
./mvnw clean package -DskipTests
```

Run:

```bash
java -jar target/*.jar
```

The backend exposes a minimal health endpoint through Actuator. Production monitoring should use the health endpoint rather than exposing configuration/bean details.

## 4. Backend environment

Required/core values:

```env
DB_URL=jdbc:postgresql://HOST:5432/DATABASE
DB_USERNAME=DATABASE_USER
DB_PASSWORD=DATABASE_PASSWORD
JWT_SECRET=LONG_RANDOM_SECRET
FRONTEND_URL=https://YOUR-FRONTEND-DOMAIN
CORS_ALLOWED_ORIGINS=https://YOUR-FRONTEND-DOMAIN
```

AI timetable import, if enabled:

```env
OPENAI_API_KEY=YOUR_BACKEND_ONLY_KEY
OPENAI_TIMETABLE_MODEL=gpt-5-mini
```

Optional mail:

```env
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
PASSWORD_RESET_TOKEN_EXPIRY_MINUTES=30
```

Never place `OPENAI_API_KEY`, `JWT_SECRET`, database passwords or SMTP passwords in the frontend or Git repository.

## 5. Database and Flyway

Hibernate schema generation is disabled:

```text
spring.jpa.hibernate.ddl-auto=none
```

Flyway is responsible for schema creation and migration.

For the current clean-database architecture:

1. Create the PostgreSQL database.
2. Reset/recreate tenant schemas when doing a fresh installation.
3. Start the backend.
4. Allow Flyway to apply public and tenant migrations.
5. Create a tenant through the supported application flow.
6. Verify tenant schema creation and required tables.

Do not manually recreate tables that are owned by Flyway.

## 6. CORS and frontend URL

These values must match the deployed domains:

```env
FRONTEND_URL=https://app.example.com
CORS_ALLOWED_ORIGINS=https://app.example.com
```

If multiple trusted frontend origins are required, configure the allowed-origin value according to the backend's supported delimiter/configuration format. Do not use a wildcard origin when credentials are enabled.

## 7. Production smoke test

After deployment, verify:

### Infrastructure

- [ ] Frontend loads over HTTPS.
- [ ] Backend health endpoint is reachable.
- [ ] Backend connects to PostgreSQL.
- [ ] Flyway migrations complete.
- [ ] Redis works if enabled.
- [ ] SMTP works if password reset/mail is enabled.

### Authentication

- [ ] Login works.
- [ ] Access token works.
- [ ] Refresh token works.
- [ ] Logout/token invalidation behavior is correct.
- [ ] Password reset works if mail is configured.

### Admin

- [ ] Tenant/admin dashboard loads.
- [ ] Create class.
- [ ] Add students.
- [ ] Create ClassSubjects.
- [ ] Assign teachers.
- [ ] Manage holidays.

### Teacher

- [ ] Teacher sees only assigned ClassSubjects.
- [ ] Attendance works.
- [ ] Assignments/submissions work.
- [ ] Exams/marks work.
- [ ] Timetable works.
- [ ] AI timetable import works when configured.

### Student

- [ ] Student sees only their own data.
- [ ] Student classes/enrollments load.
- [ ] Attendance and holiday calendar load.
- [ ] Assignments load and submission works.
- [ ] Timetable loads.
- [ ] Results/marks load.

### Isolation/security

- [ ] Student A cannot access Student B's records.
- [ ] Teacher A cannot manage Teacher B's ClassSubjects.
- [ ] Tenant A cannot access Tenant B's data.
- [ ] CORS accepts only trusted origins.
- [ ] Secrets are not exposed in frontend assets.
- [ ] Public actuator endpoints expose only intended health information.

## 8. Current deployment blockers / remaining work

The application does not currently contain provider-specific deployment configuration because the hosting provider has not been selected.

Before production, also complete:

1. Provider-specific SPA fallback configuration.
2. Production environment setup.
3. Clean database deployment test.
4. Tenant isolation test.
5. Full end-to-end regression test.
6. Transactional/batch-safe confirmation for AI timetable imports to avoid partial saves.
7. Review of frontend npm audit findings.
8. Backup/restore procedure.
9. Production monitoring and alerting.
10. Final security review.

## 9. Recommended deployment order

```text
1. Provision PostgreSQL
2. Configure backend environment
3. Start backend and verify migrations
4. Create/verify tenant
5. Deploy frontend with VITE_API_URL
6. Configure SPA fallback
7. Configure CORS/frontend URL
8. Run authentication smoke tests
9. Run admin/teacher/student end-to-end tests
10. Verify tenant isolation
11. Configure backups and monitoring
12. Go live
```
