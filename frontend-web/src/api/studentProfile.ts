import { api } from './axios'

export interface StudentProfile{

    id:number

    admissionNo:string

    rollNumber:string

    firstName:string

    lastName:string

    email:string

    phone:string

    gender:string

    dateOfBirth:string

    admissionDate:string

    address:string

    city:string

    state:string

    country:string

    pincode:string

    fatherName:string

    motherName:string

    parentPhone:string

    parentEmail:string

    bloodGroup:string

    category:string

    nationality:string

    photoUrl:string

    status:string

}

export interface ChangePassword{

    oldPassword:string

    newPassword:string

}

export async function getMyProfile(){

    const res=await api.get<StudentProfile>(
        '/student/profile'
    )

    return res.data

}

export async function updateMyProfile(

    payload:Partial<StudentProfile>){

    const res=await api.put<StudentProfile>(
        '/student/profile',
        payload
    )

    return res.data

}

export async function changePassword(

    payload:ChangePassword){

    const res=await api.put<string>(
        '/student/change-password',
        payload
    )

    return res.data

}