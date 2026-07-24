import { api } from './axios'

export interface SubjectResult {

  subjectId:number

  subjectCode:string

  subjectName:string

  credits:number

  internalMarks:number

  externalMarks:number

  totalMarks:number

  maxMarks:number

  grade:string

  gradePoint:number

}

export interface SemesterResult {

  studentId:number

  studentName:string

  semester:number

  academicYear:string

  subjects:SubjectResult[]

  totalCredits:number

  sgpa:number

  result:string

}

export interface OverallResult {

  studentId:number

  studentName:string

  semesterResults:SemesterResult[]

  totalCredits:number

  cgpa:number

  overallResult:string

}

export async function getSemesterResult(studentId:number,semester:number,academicYear:string){

  const res=await api.get<SemesterResult>(`/results/student/${studentId}/semester`,{
    params:{ semester, academicYear },
  })

  return res.data

}

export async function getOverallResult(studentId:number){

  const res=await api.get<OverallResult>(`/results/student/${studentId}/overall`)

  return res.data

}
