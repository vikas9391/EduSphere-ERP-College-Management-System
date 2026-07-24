import axios from 'axios'
import { useAuthStore } from '@/store/authStore'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout()
    }
    return Promise.reject(error)
  }
)

/* ------------------------------------------------------------------ */
/* Auth                                                                */
/* ------------------------------------------------------------------ */
/* Matches LoginRequest(collegeCode, email, password) — collegeCode is
   matched against Tenant.subdomain on the backend, not schemaName. */

export interface LoginPayload {
  collegeCode: string
  email: string
  password: string
}

export async function login(payload: LoginPayload): Promise<string> {
  const response = await api.post<string>('/auth/login', payload)
  return response.data
}

/* ------------------------------------------------------------------ */
/* Tenant                                                              */
/* ------------------------------------------------------------------ */
/* Tenant entity: id (UUID), name, schemaName, subdomain, isActive,
   createdAt. The registration controller wasn't in what you sent, so
   this payload is a best guess — send me TenantController /
   the register request DTO to lock it down exactly. */

export interface Tenant {
  id: string
  name: string
  schemaName: string
  subdomain: string
  isActive: boolean
  createdAt: string
}

export interface TenantRegisterPayload {
  name: string
  subdomain: string
  adminEmail: string
  adminPassword: string
  adminFirstName: string
  adminLastName: string
}

export async function registerTenant(payload: TenantRegisterPayload): Promise<void> {
  await api.post('/tenants/register', payload)
}

/* ------------------------------------------------------------------ */
/* Department                                                          */
/* ------------------------------------------------------------------ */

export interface Department {
  id: number
  code: string
  name: string
  hod?: string
  description?: string
  createdAt?: string
}

export type DepartmentPayload = Omit<Department, 'id' | 'createdAt'>

export async function getDepartments(): Promise<Department[]> {
  const res = await api.get<Department[]>('/departments')
  return res.data
}

export async function getDepartment(id: number): Promise<Department> {
  const res = await api.get<Department>(`/departments/${id}`)
  return res.data
}

export async function createDepartment(payload: DepartmentPayload): Promise<Department> {
  const res = await api.post<Department>('/departments', payload)
  return res.data
}

export async function updateDepartment(
  id: number,
  payload: DepartmentPayload
): Promise<Department> {
  const res = await api.put<Department>(`/departments/${id}`, payload)
  return res.data
}

export async function deleteDepartment(id: number): Promise<void> {
  await api.delete(`/departments/${id}`)
}

/* ------------------------------------------------------------------ */
/* Course                                                               */
/* ------------------------------------------------------------------ */
/* Course.department is a @ManyToOne to the full Department entity.
   Since there's no separate DTO, the entity is almost certainly bound
   directly from JSON — so the create/update payload sends a department
   reference as { id }, the standard Jackson pattern for a JPA
   @ManyToOne on an entity with no custom deserializer. */

export interface Course {
  id: number
  courseCode: string
  courseName: string
  duration?: number
  description?: string
  department: Department
  createdAt?: string
}

export interface CoursePayload {
  courseCode: string
  courseName: string
  duration?: number
  description?: string
  department: { id: number }
}

export async function getCourses(): Promise<Course[]> {
  const res = await api.get<Course[]>('/courses')
  return res.data
}

export async function getCourse(id: number): Promise<Course> {
  const res = await api.get<Course>(`/courses/${id}`)
  return res.data
}

export async function createCourse(payload: CoursePayload): Promise<Course> {
  const res = await api.post<Course>('/courses', payload)
  return res.data
}

export async function updateCourse(id: number, payload: CoursePayload): Promise<Course> {
  const res = await api.put<Course>(`/courses/${id}`, payload)
  return res.data
}

export async function deleteCourse(id: number): Promise<void> {
  await api.delete(`/courses/${id}`)
}

/* ------------------------------------------------------------------ */
/* Subject                                                              */
/* ------------------------------------------------------------------ */
/* Subject.course and Subject.teacher are both @ManyToOne and
   nullable = false, so both must be sent on create/update. */

export interface Subject {
  id: number
  subjectCode: string
  subjectName: string
  credits: number
  semester: number
  course: Course
  teacher: Teacher
  createdAt?: string
}

export interface SubjectPayload {
  subjectCode: string
  subjectName: string
  credits: number
  semester: number
  course: { id: number }
  teacher: { id: number }
}

export async function getSubjects(): Promise<Subject[]> {
  const res = await api.get<Subject[]>('/subjects')
  return res.data
}

export async function getSubject(id: number): Promise<Subject> {
  const res = await api.get<Subject>(`/subjects/${id}`)
  return res.data
}

export async function createSubject(payload: SubjectPayload): Promise<Subject> {
  const res = await api.post<Subject>('/subjects', payload)
  return res.data
}

export async function updateSubject(id: number, payload: SubjectPayload): Promise<Subject> {
  const res = await api.put<Subject>(`/subjects/${id}`, payload)
  return res.data
}

export async function deleteSubject(id: number): Promise<void> {
  await api.delete(`/subjects/${id}`)
}

/* ------------------------------------------------------------------ */
/* Teacher                                                              */
/* ------------------------------------------------------------------ */

export interface Teacher {
  id: number
  employeeId: string
  firstName: string
  lastName?: string
  email: string
  phone?: string
  gender?: string
  qualification?: string
  specialization?: string
  experience?: number
  joiningDate?: string
  createdAt?: string
}

export type TeacherPayload = Omit<Teacher, 'id' | 'createdAt'>

export async function getTeachers(): Promise<Teacher[]> {
  const res = await api.get<Teacher[]>('/teachers')
  return res.data
}

export async function getTeacher(id: number): Promise<Teacher> {
  const res = await api.get<Teacher>(`/teachers/${id}`)
  return res.data
}

export async function createTeacher(payload: TeacherPayload): Promise<Teacher> {
  const res = await api.post<Teacher>('/teachers', payload)
  return res.data
}

export async function updateTeacher(id: number, payload: TeacherPayload): Promise<Teacher> {
  const res = await api.put<Teacher>(`/teachers/${id}`, payload)
  return res.data
}

export async function deleteTeacher(id: number): Promise<void> {
  await api.delete(`/teachers/${id}`)
}

/* ------------------------------------------------------------------ */
/* Student                                                              */
/* ------------------------------------------------------------------ */

export interface Student {
  id: number
  admissionNo: string
  firstName: string
  lastName?: string
  email?: string
  phone?: string
  gender?: string
  dateOfBirth?: string
  createdAt?: string
}

export type StudentPayload = Omit<Student, 'id' | 'createdAt'>

export async function getStudents(): Promise<Student[]> {
  const res = await api.get<Student[]>('/students')
  return res.data
}

export async function getStudent(id: number): Promise<Student> {
  const res = await api.get<Student>(`/students/${id}`)
  return res.data
}

export async function createStudent(payload: StudentPayload): Promise<Student> {
  const res = await api.post<Student>('/students', payload)
  return res.data
}

export async function updateStudent(id: number, payload: StudentPayload): Promise<Student> {
  const res = await api.put<Student>(`/students/${id}`, payload)
  return res.data
}

export async function deleteStudent(id: number): Promise<void> {
  await api.delete(`/students/${id}`)
}