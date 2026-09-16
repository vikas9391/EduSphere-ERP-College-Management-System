# EduSphere ERP Mobile

Expo / React Native mobile client for the EduSphere ERP backend.

## Backend

The mobile app uses the existing Spring Boot API. No second backend is required.

Create `app/.env` from `.env.example`:

```env
EXPO_PUBLIC_API_URL=http://YOUR_COMPUTER_IP:8080/api
```

For a physical phone, do not use `localhost`; use the LAN IP of the machine running the backend, and make sure the phone can reach that machine.

## Run

```bash
cd app
npm install
npm start
```

Then open the project with Expo Go or an Android/iOS emulator.

## Current mobile modules

- JWT login with persisted session
- Student dashboard using live ERP dashboard data
- Enrolled classes
- Attendance summary
- Assignments and submission status
- Weekly timetable
- Profile foundation

The mobile UI follows the web application's green botanical design tokens.
