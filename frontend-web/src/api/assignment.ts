import { api } from './axios'

/* =========================
   Assignment
========================= */

export interface Assignment {

  id:number

  subjectId:number

  subjectName:string

  teacherId:number

  teacherName:string

  title:string

  description:string

  dueDate:string

  maxMarks:number

}

export interface AssignmentPayload{

  subjectId:number

  teacherId:number

  title:string

  description:string

  dueDate:string

  maxMarks:number

}

export async function getAssignments(){

  const res=await api.get<Assignment[]>('/assignments')

  return res.data

}

export async function getAssignment(id:number){

  const res=await api.get<Assignment>(`/assignments/${id}`)

  return res.data

}

export async function createAssignment(payload:AssignmentPayload){

  const res=await api.post<Assignment>('/assignments',payload)

  return res.data

}

export async function updateAssignment(id:number,payload:AssignmentPayload){

  const res=await api.put<Assignment>(`/assignments/${id}`,payload)

  return res.data

}

export async function deleteAssignment(id:number){

  await api.delete(`/assignments/${id}`)

}

/* =========================
   Assignment Submission
========================= */

export interface AssignmentSubmission{

  id:number

  assignmentId:number

  assignmentTitle:string

  studentId:number

  studentName:string

  submissionUrl:string

  submittedAt:string

  marks:number

  feedback:string

  status:string

}

export interface AssignmentSubmissionPayload{

  assignmentId:number

  studentId:number

  submissionUrl:string

}

export async function submitAssignment(
    payload:AssignmentSubmissionPayload){

    const res=await api.post<AssignmentSubmission>(
        '/submissions',
        payload
    )

    return res.data

}

export async function getSubmissions(){

    const res=await api.get<AssignmentSubmission[]>(
        '/submissions'
    )

    return res.data

}

export async function getAssignmentSubmissions(
    assignmentId:number){

    const res=await api.get<AssignmentSubmission[]>(
        `/submissions/assignment/${assignmentId}`
    )

    return res.data

}

export async function evaluateSubmission(

    id:number,

    marks:number,

    feedback:string){

    const res=await api.put<AssignmentSubmission>(
        `/submissions/${id}/evaluate`,
        null,
        {
            params:{
                marks,
                feedback
            }
        }
    )

    return res.data

}