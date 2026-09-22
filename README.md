# EduSphere ERP — College Management System

> **Smart Campus, Smarter Future**

EduSphere ERP is a multi-tenant college management platform connecting **students, teachers, administrators, academics, attendance, assignments, examinations, marks, timetables and campus communication** in one system.

It includes a **React web application**, a **Flutter mobile application**, and a **Spring Boot REST backend** backed by PostgreSQL.

## ✨ Highlights

- 🏫 Multi-tenant college ERP architecture
- 🔐 JWT authentication and role-based authorization
- 👨‍🎓 Student, teacher, staff and admin workflows
- 📚 Classes, subjects, enrollments and academic management
- 📊 Attendance, marks, exams, results and assignments
- 🗓️ Timetable management with conflict validation
- 🤖 AI-assisted timetable image/PDF inspection
- 📱 Flutter mobile app using the same backend API
- 🌿 Botanical green/white design across web and mobile
- 🔄 Access/refresh-token based sessions
- 🧩 Flyway-managed PostgreSQL migrations
- 🚀 CI validation for backend, web and Flutter

## 📁 Project Structure

```text
EduSphere-ERP-College-Management-System/
├── Backend/              # Spring Boot REST API
├── frontend-web/         # React + TypeScript web application
├── app/                  # Flutter + Dart mobile application
├── docs/                 # Project documentation
└── .github/              # CI workflows
```

## 🛠️ Technology Stack

### Backend
- Java 21
- Spring Boot 3.5.3
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Redis / Spring Cache
- JJWT
- MapStruct
- Lombok
- SpringDoc OpenAPI
- Maven

### Web
- React 19
- TypeScript
- Vite
- React Router
- Axios
- Zustand
- Tailwind CSS
- Framer Motion
- Lucide React
- Recharts

### Mobile
- Flutter
- Dart
- Material 3
- `http`
- `shared_preferences`
- Same Spring Boot REST API as the web application

## 🧭 Architecture

```text
                    ┌─────────────────────┐
                    │   React Web App     │
                    └──────────┬──────────┘
                               │
                               │ REST / JWT
                               ▼
                    ┌─────────────────────┐
                    │ Spring Boot API     │
                    │ Security + Services │
                    └──────────┬──────────┘
                               │
                 ┌─────────────┴─────────────┐
                 ▼                           ▼
        ┌─────────────────┐        ┌─────────────────┐
        │ PostgreSQL      │        │ Redis / Cache   │
        │ Multi-Tenant DB │        │                 │
        └─────────────────┘        └─────────────────┘
                 ▲
                 │ REST / JWT
        ┌────────┴────────┐
        │ Flutter Mobile  │
        │ Android / iOS   │
        └─────────────────┘
```

### Academic relationship model

```text
Department → Course → Subject

Teacher → SchoolClass
SchoolClass → ClassStudent → Student
SchoolClass → ClassSubject → Subject + Teacher
ClassSubject → ClassEnrollment → Student

ClassEnrollment → Attendance
ClassSubject → TimetableEntry
ClassSubject → Assignment
ClassSubject → ExamSchedule
ClassEnrollment → Marks
```

Attendance, assignments, examinations, marks and timetable data use the **class-scoped academic model**.

## 🔐 Authentication & Authorization

- Super-admin, tenant/staff, teacher and student authentication
- JWT access and refresh tokens
- Password change and forgot-password/reset flows
- Role and permission based staff access
- Frontend route protection
- Backend authorization as the final security boundary

## 🏢 Multi-Tenancy

EduSphere uses **schema-based PostgreSQL multi-tenancy**.

- Public tenant metadata is separated from tenant academic data.
- Each tenant operates inside its own PostgreSQL schema.
- Flyway manages public and tenant migrations.
- Hibernate schema generation is disabled.
- Tenant resolution is handled by the backend tenant/security layer.

## 📚 Core Modules

### Students
- Dashboard and profile
- Classes and enrollments
- Attendance and holidays
- Assignments
- Timetable
- Examinations
- Marks and results

### Teachers
- Assigned classes
- Class rosters
- Attendance
- Assignments and submissions
- Timetable
- Examinations
- Marks

### Administration
- College/tenant management
- Users and roles
- Departments, courses and subjects
- Classes
- Class subjects
- Student/teacher relationships
- Academic operations

## 📱 Flutter Mobile App

The `app/` directory is the current mobile client. The previous Expo/React Native implementation has been removed.

### Current foundation

- Material 3
- EduSphere botanical green/white branding
- College code + username + password login
- JWT access/refresh token handling
- Persistent sessions with `shared_preferences`
- Role-aware ERP shell
- Student, teacher and admin dashboard foundation
- Assignment workflows
- Profile and password actions
- Shared backend REST integration
- Flutter static analysis in CI

### Mobile design system

The Flutter app follows the web application's visual language:

| Token | Value |
|---|---|
| Primary green | `#2E7D32` |
| Secondary green | `#4CAF50` |
| Light green | `#E8F5E9` |
| Page background | `#F8F8F2` |
| Main text | `#1F2937` |
| Muted text | `#6B7280` |

The UI uses rounded cards, soft borders, light-green surfaces and botanical/sprout branding.

## 🚀 Run Locally

### Backend

```bash
cd Backend
./mvnw spring-boot:run
```

Windows:

```powershell
cd Backend
./mvnw.cmd spring-boot:run
```

Backend: `http://localhost:8080`

### Web

```bash
cd frontend-web
npm install
npm run dev
```

Web: `http://localhost:5173`

### Flutter

```bash
cd app
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

For a physical device, use a backend URL reachable from that device.

### Android APK

```bash
cd app
flutter build apk --release --dart-define=API_URL=https://your-backend.example.com/api
```

## ⚙️ Environment Variables

### Web

```env
VITE_API_URL=http://localhost:8080/api
```

### Backend

```env
DB_URL=jdbc:postgresql://localhost:5432/college_erp
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=replace_with_a_long_random_secret
FRONTEND_URL=http://localhost:5173
CORS_ALLOWED_ORIGINS=http://localhost:5173
OPENAI_API_KEY=
OPENAI_TIMETABLE_MODEL=gpt-5-mini
```

### Flutter

```bash
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

> **Security:** Never commit real API keys, database passwords, JWT secrets or other credentials.

## 🔌 API Areas

```text
/api/courses
/api/departments
/api/subjects
/api/teachers
/api/students
/api/classes
/api/marks
/api/exams
/api/timetable
/api/assignments
/api/roles
/api/student
/api/teacher
```

Student enrollments:

```text
/api/student/enrollments
```

Swagger/OpenAPI:

```text
/api-docs
/swagger-ui.html
```

## 🤖 AI Timetable Assistance

EduSphere includes an AI-assisted workflow for inspecting timetable images/PDFs and reviewing proposed timetable data before saving it.

The AI workflow is intended as **decision support**: imported timetable information can be reviewed and validated before becoming operational academic data.

## 🧪 CI & Verification

CI validates the main application layers:

- Backend compilation
- Backend relationship/isolation tests
- Frontend production build
- Flutter dependency resolution
- Flutter static analysis

The Flutter mobile project no longer depends on Node/Expo or a TypeScript mobile build.

## 🗄️ Database & Migrations

EduSphere uses PostgreSQL with Flyway.

For a fresh database:

1. Configure PostgreSQL.
2. Set the backend environment variables.
3. Start the Spring Boot backend.
4. Allow Flyway to create the required schemas and tables.
5. Provision tenants through the application flow.

When resetting the ERP database, reset Flyway history together with tenant schemas and recreate the database from the current migration set.

## 📈 Project Status

### Completed

- [x] Spring Boot + React ERP foundation
- [x] Multi-tenant architecture
- [x] JWT authentication and refresh flow
- [x] Role and permission enforcement
- [x] Frontend route protection
- [x] Class/student/teacher relationships
- [x] ClassSubject and ClassEnrollment model
- [x] Class-scoped attendance
- [x] Attendance summaries and holidays
- [x] Class-scoped assignments/submissions
- [x] Class-scoped examinations, marks and results
- [x] Class-scoped timetable and conflict validation
- [x] AI timetable inspection/review UI
- [x] Flutter mobile migration
- [x] Flutter authentication and session persistence
- [x] Flutter role-aware ERP foundation
- [x] Flutter assignment workflows
- [x] Flutter botanical UI aligned with the web application
- [x] CI validation for backend, web and Flutter

### Roadmap

- [ ] Complete remaining Student Flutter modules
- [ ] Complete remaining Teacher Flutter modules
- [ ] Expand Admin/Super-admin mobile views
- [ ] Add robust mobile network/offline states
- [ ] Add mobile push notifications if required
- [ ] Add Android/iOS production icons and release metadata
- [ ] Complete physical-device Android/iOS testing
- [ ] Make AI timetable multi-slot confirmation transactional
- [ ] Finalize production hosting
- [ ] Complete production database/Flyway deployment testing
- [ ] Perform multi-tenant isolation testing with multiple tenants
- [ ] Add production monitoring, logging and database backups
- [ ] Perform final production security review

## 📖 Documentation

- `docs/PROJECT_STATUS.md` — implementation status and mobile roadmap
- `docs/DEPLOYMENT.md` — deployment and production checklist

## 🌐 Repository

https://github.com/vikas9391/EduSphere-ERP-College-Management-System

---

**EduSphere ERP**  
*Smart Campus, Smarter Future*
