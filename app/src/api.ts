import AsyncStorage from '@react-native-async-storage/async-storage';

const API_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '');

export type LoginResponse = { accessToken: string; refreshToken?: string; role?: string; username?: string; mustChangePassword?: boolean };

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = await AsyncStorage.getItem('accessToken');
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(options.headers ?? {}) },
  });
  const raw = await response.text();
  const data = raw ? JSON.parse(raw) : null;
  if (!response.ok) throw new Error(data?.message ?? data?.error ?? `Request failed (${response.status})`);
  return (data?.data ?? data) as T;
}

export async function login(collegeCode: string, username: string, password: string) {
  const result = await request<LoginResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ collegeCode, username, password }) });
  await AsyncStorage.multiSet([['accessToken', result.accessToken], ['userRole', result.role ?? ''], ['username', result.username ?? username]]);
  if (result.refreshToken) await AsyncStorage.setItem('refreshToken', result.refreshToken);
  return result;
}
export async function logout() { await AsyncStorage.multiRemove(['accessToken', 'refreshToken', 'userRole', 'username']); }
export async function getAccessToken() { return AsyncStorage.getItem('accessToken'); }
export async function getRole() { return AsyncStorage.getItem('userRole'); }
export async function getUsername() { return AsyncStorage.getItem('username'); }

export type ClassEnrollment = { id:number; classSubjectId:number; schoolClassId:number; className?:string; subjectId?:number; subjectCode?:string; subjectName?:string; teacherId?:number; teacherName?:string; academicYear?:string; semester?:number|string; status?:string };
export type AttendanceSummary = { totalClasses?:number; attendedClasses?:number; missedClasses?:number; percentage?:number; [key:string]:any };
export const getMyEnrollments = () => request<ClassEnrollment[]>('/student/enrollments');
export const getMyAttendanceSummary = () => request<AttendanceSummary>('/student/attendance/summary');
export const getMyTimetable = () => request<any[]>('/timetable/mine');
export const getMyAssignments = () => request<any[]>('/student/assignments');
