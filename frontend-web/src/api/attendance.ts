import { api } from './axios'

export interface Attendance{

  id:number

  enrollmentId:number

  studentId:number

  studentName:string

  subjectId:number

  subjectName:string

  attendanceDate:string

  status:string

  remarks:string

}

export interface AttendancePayload{

  enrollmentId:number

  attendanceDate:string

  status:string

  remarks:string

}

export async function getAttendance(){

  const res=await api.get<Attendance[]>('/attendance')

  return res.data

}

export async function getAttendanceRecord(id:number){

  const res=await api.get<Attendance>(`/attendance/${id}`)

  return res.data

}

export async function createAttendance(payload:AttendancePayload){

  const res=await api.post<Attendance>('/attendance',payload)

  return res.data

}

export async function deleteAttendance(id:number){

  await api.delete(`/attendance/${id}`)

}