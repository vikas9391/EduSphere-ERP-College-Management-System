import { api } from './axios'

export interface Marks {

  id:number

  examScheduleId:number

  examId:number

  examName:string

  subjectId:number

  subjectName:string

  studentId:number

  studentName:string

  internalMarks:number

  externalMarks:number

  totalMarks:number

  maxMarks:number

  grade:string

  gradePoint:number

  status:string

}

export interface MarksPayload{

  examScheduleId:number

  studentId:number

  internalMarks:number

  externalMarks:number

}

export async function getMarksByExamSchedule(examScheduleId:number){

  const res=await api.get<Marks[]>(`/marks/exam-schedule/${examScheduleId}`)

  return res.data

}

export async function getMarks(id:number){

  const res=await api.get<Marks>(`/marks/${id}`)

  return res.data

}

export async function enterMarks(payload:MarksPayload){

  const res=await api.post<Marks>('/marks',payload)

  return res.data

}

export async function updateMarks(id:number,payload:MarksPayload){

  const res=await api.put<Marks>(`/marks/${id}`,payload)

  return res.data

}

export async function publishMarks(id:number){

  const res=await api.put<Marks>(`/marks/${id}/publish`)

  return res.data

}

export async function publishMarksForExamSchedule(examScheduleId:number){

  const res=await api.put<Marks[]>(`/marks/exam-schedule/${examScheduleId}/publish`)

  return res.data

}

export async function deleteMarks(id:number){

  await api.delete(`/marks/${id}`)

}
