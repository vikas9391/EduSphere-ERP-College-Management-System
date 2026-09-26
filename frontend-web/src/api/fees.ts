import { api } from './axios'
export type FeeStatus = 'PENDING'|'PARTIALLY_PAID'|'PAID'|'OVERDUE'
export interface Fee { id:number; studentId:number; studentName:string; feeStructureId:number; feeName:string; academicYear:string; semester:number|null; discount:number; totalAmount:number; amountPaid:number; balance:number; status:FeeStatus; dueDate:string|null }
export interface FeeStructure { id:number; academicYear:string; semester:number|null; name:string; tuitionFee:number; examinationFee:number; libraryFee:number; otherFee:number; dueDate:string|null }
export interface Payment { id:number; studentFeeId:number; amount:number; paymentMethod:'CASH'|'BANK_TRANSFER'|'UPI'|'CARD'|'OTHER'; transactionReference:string|null; receiptNumber:string; paidAt:string; notes:string|null }
export async function getFeeStructures(){return (await api.get<FeeStructure[]>('/fees/structures')).data}
export async function assignFee(data:{studentId:number;feeStructureId:number;discount:number}){return (await api.post<Fee>('/fees/assign',data)).data}
export async function getStudentFees(studentId:number){return (await api.get<Fee[]>('/fees/student/'+studentId)).data}
export async function getMyFees(){return (await api.get<Fee[]>('/fees/mine')).data}
export async function recordPayment(id:number,data:{amount:number;paymentMethod:Payment['paymentMethod'];transactionReference?:string}){return (await api.post<Payment>('/fees/'+id+'/payments',data)).data}
