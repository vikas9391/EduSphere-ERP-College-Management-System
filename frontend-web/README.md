# EduSphere ERP — Web Application

The `frontend-web/` directory contains the production web client for **EduSphere ERP**, built with React, TypeScript and Vite.

## Stack

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
- Oxlint

## Development

```bash
cd frontend-web
npm install
npm run dev
```

The Vite development server runs on:

```text
http://localhost:5173
```

## Environment

Create `frontend-web/.env.local` when a local API override is needed:

```env
VITE_API_URL=http://localhost:8080/api
```

If `VITE_API_URL` is not provided, the application defaults to the same local API URL.

For production, point the variable at the deployed Spring Boot API:

```env
VITE_API_URL=https://your-api-domain.example/api
```

## Verification

Build the production bundle:

```bash
npm run build
```

Run the linter:

```bash
npm run lint
```

Preview the production bundle:

```bash
npm run preview
```

The web application uses the same Spring Boot backend and tenant/authentication model as the Flutter mobile application. It does not contain a separate backend.

## Main areas

The web client includes role-aware views for:

- Students
- Teachers
- Staff and administrators
- Departments, courses and subjects
- Classes and enrollments
- Attendance and holidays
- Assignments and submissions
- Exams, marks and results
- Timetables
- Announcements
- Profiles and authentication flows

## Related documentation

See the repository root `README.md` for the complete architecture and local setup instructions.

See `docs/DEPLOYMENT.md` for production deployment and smoke-test guidance.
