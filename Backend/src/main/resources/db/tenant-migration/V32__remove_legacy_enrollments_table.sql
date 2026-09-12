-- ClassEnrollment is now the single operational student-to-subject relationship.
-- The original enrollments table is no longer mapped or queried by the application.
DROP TABLE IF EXISTS enrollments CASCADE;
