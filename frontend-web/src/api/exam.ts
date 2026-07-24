import { api } from './axios'

export interface Exam {

  id:number

  examName:string

  examType:string

  academicYear:string

  semester:number

  courseId:number

  courseName:string

  startDate:string

  endDate:string

}

export interface ExamPayload{

  examName:string

  examType:string

  academicYear:string

  semester:number

  courseId:number

  startDate:string

  endDate:string

}

export async function getExams(){

  const res=await api.get<Exam[]>('/exams')

  return res.data

}

export async function getExam(id:number){

  const res=await api.get<Exam>(`/exams/${id}`)

  return res.data

}

export async function createExam(payload:ExamPayload){

  const res=await api.post<Exam>('/exams',payload)

  return res.data

}

export async function updateExam(id:number,payload:ExamPayload){

  const res=await api.put<Exam>(`/exams/${id}`,payload)

  return res.data

}

export async function deleteExam(id:number){

  await api.delete(`/exams/${id}`)

}
