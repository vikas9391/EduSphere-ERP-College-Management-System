import { api } from './axios'

export interface ExamSchedule {

  id:number

  examId:number

  examName:string

  subjectId:number

  subjectName:string

  invigilatorId?:number

  invigilatorName?:string

  examDate:string

  startTime:string

  endTime:string

  room:string

  maxMarks:number

}

export interface ExamSchedulePayload{

  examId:number

  subjectId:number

  invigilatorId?:number | null

  examDate:string

  startTime:string

  endTime:string

  room:string

  maxMarks:number

}

export async function getScheduleByExam(examId:number){

  const res=await api.get<ExamSchedule[]>(`/exam-schedules/exam/${examId}`)

  return res.data

}

export async function getExamSchedule(id:number){

  const res=await api.get<ExamSchedule>(`/exam-schedules/${id}`)

  return res.data

}

export async function createExamSchedule(payload:ExamSchedulePayload){

  const res=await api.post<ExamSchedule>('/exam-schedules',payload)

  return res.data

}

export async function updateExamSchedule(id:number,payload:ExamSchedulePayload){

  const res=await api.put<ExamSchedule>(`/exam-schedules/${id}`,payload)

  return res.data

}

export async function deleteExamSchedule(id:number){

  await api.delete(`/exam-schedules/${id}`)

}
