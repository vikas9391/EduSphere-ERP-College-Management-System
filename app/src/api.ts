import AsyncStorage from '@react-native-async-storage/async-storage';

const API_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '');
export type LoginResponse = { accessToken: string; refreshToken?: string; role?: string; username?: string; mustChangePassword?: boolean };
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = await AsyncStorage.getItem('accessToken');
  const response = await fetch(`${API_URL}${path}`, { ...options, headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(options.headers ?? {}) } });
  const raw = await response.text(); let data: any = null; try { data = raw ? JSON.parse(raw) : null; } catch { data = raw; }
  if (!response.ok) throw new Error(data?.message ?? data?.error ?? `Request failed (${response.status})`);
  return (data?.data ?? data) as T;
}
export async function login(collegeCode:string, username:string, password:string) { const result=await request<LoginResponse>('/auth/login',{method:'POST',body:JSON.stringify({collegeCode,username,password})}); await AsyncStorage.multiSet([['accessToken',result.accessToken],['userRole',result.role??''],['username',result.username??username]]); if(result.refreshToken) await AsyncStorage.setItem('refreshToken',result.refreshToken); return result; }
export async function logout(){await AsyncStorage.multiRemove(['accessToken','refreshToken','userRole','username']);}
export async function getAccessToken(){return AsyncStorage.getItem('accessToken');} export async function getRole(){return AsyncStorage.getItem('userRole');} export async function getUsername(){return AsyncStorage.getItem('username');}
export type DashboardSummary={studentId:number;studentName:string;rollNumber:string|null;department:string|null;course:string|null;semester:number|null;cgpa:number;attendancePercentage:number;totalSubjects:number;pendingAssignments:number;upcomingExams:number;notificationsCount:number};
export type ClassEnrollment={id:number;classSubjectId:number;schoolClassId:number;className?:string;subjectId?:number;subjectCode?:string;subjectName?:string;teacherId?:number;teacherName?:string;academicYear?:string;semester?:number|string;status?:string};
export type AttendanceSummary={totalClasses?:number;classesAttended?:number;classesMissed?:number;attendancePercentage?:number;percentage?:number;[key:string]:any};
export type Assignment={assignmentId:number;title:string;description:string;subjectId:number;subjectName:string;teacherName:string;dueDate:string;maxMarks:number;submissionStatus:string;submittedAt:string|null;submissionUrl:string|null;marksObtained:number|null;feedback:string|null};
export type TimetableEntry={classSubjectId:number;startTime:string;endTime:string;subjectId:number|null;subjectName:string;teacherName:string|null;room:string};
export type StudentTimetable={placeholder:boolean;note:string;schedule:Record<string,TimetableEntry[]>};
export const getStudentDashboard=()=>request<DashboardSummary>('/student/dashboard');
export const getMyEnrollments=()=>request<ClassEnrollment[]>('/student/enrollments');
export const getMyAttendanceSummary=()=>request<AttendanceSummary>('/student/attendance/summary');
export const getMyTimetable=()=>request<StudentTimetable>('/student/timetable');
export const getMyAssignments=()=>request<Assignment[]>('/student/assignments');
