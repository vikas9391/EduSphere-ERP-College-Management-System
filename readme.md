# 🎓 EduSphere ERP – College Management System (Backend)

> A Multi-Tenant SaaS College ERP System built using Spring Boot, PostgreSQL, Flyway, JWT Authentication, and REST APIs.

---

# 📌 Project Overview

EduSphere ERP is a cloud-based College ERP platform where every college gets its own isolated database schema.

Each college can manage:

- Students
- Teachers
- Departments
- Courses
- Subjects
- (Upcoming) Attendance, Exams, Fees, Timetable, etc.

---

# 🏗 Tech Stack

Backend
- Java 21
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- PostgreSQL (NeonDB)
- Flyway Migration
- JWT Authentication
- Swagger / OpenAPI
- Maven

Database
- PostgreSQL
- Multi-Tenant (Schema Based)

Authentication
- JWT Token
- BCrypt Password Encryption

---

# ✅ Features Completed

## Authentication

✔ Login

✔ JWT Token Generation

✔ Password Encryption

✔ Authorization Filter

---

## Multi-Tenant SaaS

✔ College Registration

✔ Separate PostgreSQL Schema per College

✔ Automatic Schema Creation

✔ Flyway Migration per Tenant

✔ Tenant Context

---

## Student Module

✔ Create Student

✔ View All Students

✔ View Student By ID

✔ Update Student

✔ Delete Student

---

## Teacher Module

✔ Create Teacher

✔ View All Teachers

✔ View Teacher

✔ Update Teacher

✔ Delete Teacher

---

## Department Module

✔ Create Department

✔ View All Departments

✔ View Department

✔ Update Department

✔ Delete Department

---

## Course Module

✔ Create Course

✔ View All Courses

✔ View Course

✔ Update Course

✔ Delete Course

✔ Department Mapping

---

## Subject Module

✔ Create Subject

✔ View All Subjects

✔ View Subject

✔ Update Subject

✔ Delete Subject

✔ Course Mapping

✔ Teacher Mapping

---

# 🔐 Authentication Flow

## 1 Register College

POST

```
/api/tenants/register
```

Creates

- PostgreSQL Schema
- Roles
- Admin User

---

## 2 Login

POST

```
/api/auth/login
```

Returns

```
JWT Token
```

---

## 3 Authorize

Swagger

```
Bearer <JWT_TOKEN>
```

All remaining APIs require JWT.

---

# 📚 API Documentation

---

# Tenant APIs

## Register College

POST

```
/api/tenants/register
```

---

# Authentication APIs

## Login

POST

```
/api/auth/login
```

---

# Student APIs

## Create Student

POST

```
/api/students
```

## Get All Students

GET

```
/api/students
```

## Get Student

GET

```
/api/students/{id}
```

## Update Student

PUT

```
/api/students/{id}
```

## Delete Student

DELETE

```
/api/students/{id}
```

---

# Teacher APIs

## Create Teacher

POST

```
/api/teachers
```

## Get All Teachers

GET

```
/api/teachers
```

## Get Teacher

GET

```
/api/teachers/{id}
```

## Update Teacher

PUT

```
/api/teachers/{id}
```

## Delete Teacher

DELETE

```
/api/teachers/{id}
```

---

# Department APIs

## Create Department

POST

```
/api/departments
```

## Get All Departments

GET

```
/api/departments
```

## Get Department

GET

```
/api/departments/{id}
```

## Update Department

PUT

```
/api/departments/{id}
```

## Delete Department

DELETE

```
/api/departments/{id}
```

---

# Course APIs

## Create Course

POST

```
/api/courses
```

## Get All Courses

GET

```
/api/courses
```

## Get Course

GET

```
/api/courses/{id}
```

## Update Course

PUT

```
/api/courses/{id}
```

## Delete Course

DELETE

```
/api/courses/{id}
```

---

# Subject APIs

## Create Subject

POST

```
/api/subjects
```

## Get All Subjects

GET

```
/api/subjects
```

## Get Subject

GET

```
/api/subjects/{id}
```

## Update Subject

PUT

```
/api/subjects/{id}
```

## Delete Subject

DELETE

```
/api/subjects/{id}
```

---

# 🗄 Database Modules Completed

```
Tenant
Users
Roles
Students
Teachers
Departments
Courses
Subjects
```

---

# 🔄 Entity Relationships

```
Tenant
   │
   ├─────────────► Users

Department
      │
      ▼
Course
      │
      ▼
Subject
      │
      ▼
Teacher

Student
```

---

# 🔐 Security

✔ JWT Authentication

✔ BCrypt Password Encryption

✔ Protected APIs

✔ Multi-Tenant Isolation

✔ Tenant Context Filter

---

# 🧪 API Testing

Swagger UI

```
http://localhost:8080/swagger-ui.html
```

API Docs

```
http://localhost:8080/api-docs
```

---

# 📁 Current Backend Structure

```
auth/
common/
config/
course/
department/
security/
student/
subject/
teacher/
tenant/
```

---

# 🚀 Upcoming Modules (Phase 2)

## Enrollment

- Student Enrollment
- Subject Registration
- Semester Enrollment

---

## Attendance

- Daily Attendance
- Faculty Attendance
- Attendance Percentage

---

## Timetable

- Weekly Timetable
- Faculty Schedule
- Classroom Allocation

---

## Examination

- Mid Exams
- Semester Exams
- Practical Exams

---

## Marks

- Internal Marks
- External Marks
- Grade Calculation
- GPA
- CGPA

---

## Fees

- Fee Structure
- Fee Payment
- Pending Fees
- Online Payment Integration

---

## Dashboard

Admin Dashboard

- Total Students
- Teachers
- Courses
- Revenue
- Attendance
- Reports

---

## Notices

- College Notices
- Student Notices
- Faculty Notices

---

## Leave Management

- Student Leave
- Teacher Leave
- Approval Workflow

---

## Library Management

- Books
- Borrow
- Return
- Fine Calculation

---

## Hostel Management

- Hostel Rooms
- Room Allocation
- Hostel Fees

---

## Transport Management

- Bus Routes
- Drivers
- Student Bus Allocation

---

## Reports

- Student Reports
- Faculty Reports
- Attendance Reports
- Fee Reports
- Exam Reports

---

## Role Based Access Control

Roles

- Super Admin
- College Admin
- HOD
- Teacher
- Student
- Accountant
- Librarian

---

# 🌐 Future Integrations

- React Frontend
- Mobile App (Flutter)
- Email Notifications
- SMS Notifications
- WhatsApp Notifications
- File Uploads
- Cloud Storage
- Docker
- Kubernetes
- CI/CD
- AWS Deployment

---

# 📈 Project Status

| Module | Status |
|---------|--------|
| Multi-Tenant SaaS | ✅ Completed |
| Authentication | ✅ Completed |
| Student | ✅ Completed |
| Teacher | ✅ Completed |
| Department | ✅ Completed |
| Course | ✅ Completed |
| Subject | ✅ Completed |
| Enrollment | ⏳ Planned |
| Attendance | ⏳ Planned |
| Timetable | ⏳ Planned |
| Exams | ⏳ Planned |
| Marks | ⏳ Planned |
| Fees | ⏳ Planned |
| Dashboard | ⏳ Planned |
| Reports | ⏳ Planned |

---

# 👨‍💻 Author

**Vikas**

B.Tech Computer Science Engineering

Malla Reddy University

Java • Spring Boot • PostgreSQL • React • JWT • SaaS ERP

---

**EduSphere ERP** is designed as a scalable, production-ready multi-tenant College ERP platform capable of serving multiple institutions from a single backend while maintaining complete data isolation.