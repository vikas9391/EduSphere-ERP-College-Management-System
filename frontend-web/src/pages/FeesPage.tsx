import { useEffect, useState } from 'react'
import { IndianRupee, CreditCard } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { getMyFees, getFeeStructures, assignFee, recordPayment, getPayments, type Fee, type FeeStructure, type Payment } from '@/api/fees'
import { getStudents, type Student } from '@/api/student'

export function FeesPage() {
  const user = useAuthStore(s => s.user)
  const isStudent = user?.role === 'STUDENT'
  const [fees,setFees] = useState<Fee[]>([])
  const [structures,setStructures] = useState<FeeStructure[]>([])
  const [students,setStudents] = useState<Student[]>([])
  const [loading,setLoading] = useState(true)
  const [studentId,setStudentId] = useState('')
  const [structureId,setStructureId] = useState('')
  const [discount,setDiscount] = useState('0')
  const [paymentFor,setPaymentFor] = useState<Fee|null>(null)
  const [amount,setAmount] = useState('')
  const [method,setMethod] = useState<Payment['paymentMethod']>('UPI')
  const [reference,setReference] = useState('')
  const [message,setMessage] = useState('')
  const [history,setHistory] = useState<Payment[]>([])
  const [historyFee,setHistoryFee] = useState<Fee|null>(null)

  async function load() {
    setLoading(true)
    try {
      if (isStudent) setFees(await getMyFees())
      else {
        const [s,st] = await Promise.all([getStudents(),getFeeStructures()])
        setStudents(s); setStructures(st)
      }
    } catch (e:any) { setMessage(e?.response?.data?.message || 'Unable to load fees') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [isStudent])

  async function assign() {
    try {
      await assignFee({studentId:Number(studentId),feeStructureId:Number(structureId),discount:Number(discount)})
      setMessage('Fee assigned successfully'); setStudentId(''); setStructureId(''); await load()
    } catch (e:any) { setMessage(e?.response?.data?.message || 'Unable to assign fee') }
  }

  async function pay() {
    if (!paymentFor) return
    try {
      const p = await recordPayment(paymentFor.id,{amount:Number(amount),paymentMethod:method,transactionReference:reference || undefined})
      setMessage('Payment recorded. Receipt: ' + p.receiptNumber)
      setPaymentFor(null); setAmount(''); setReference(''); await load()
    } catch (e:any) { setMessage(e?.response?.data?.message || 'Unable to record payment') }
  }

  return <div>
    <div className="mb-8 flex items-center justify-between">
      <div><p className="text-xs uppercase tracking-[0.18em] text-muted">Finance</p><h1 className="font-heading text-3xl font-semibold text-text">{isStudent ? 'My Fees' : 'Fees & Payments'}</h1><p className="mt-1 text-sm text-muted">{isStudent ? 'View your fee balance and payment history.' : 'Manage student dues and recorded payments.'}</p></div>
      <IndianRupee className="text-primary" size={28}/>
    </div>
    {message && <div className="mb-4 rounded-xl border border-border bg-hover px-4 py-3 text-sm">{message}</div>}
    {!isStudent && <div className="mb-8 rounded-2xl border border-border p-5"><h2 className="mb-4 font-semibold">Assign fee</h2><div className="grid gap-3 md:grid-cols-4">
      <select className="rounded-xl border p-3" value={studentId} onChange={e=>setStudentId(e.target.value)}><option value="">Select student</option>{students.map(s=><option key={s.id} value={s.id}>{s.firstName} {s.lastName || ''}</option>)}</select>
      <select className="rounded-xl border p-3" value={structureId} onChange={e=>setStructureId(e.target.value)}><option value="">Select fee structure</option>{structures.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select>
      <input className="rounded-xl border p-3" type="number" placeholder="Discount" value={discount} onChange={e=>setDiscount(e.target.value)}/>
      <button className="rounded-xl bg-primary px-4 py-3 font-semibold text-white disabled:opacity-50" disabled={!studentId||!structureId} onClick={()=>void assign()}>Assign</button>
    </div></div>}
    {loading ? <p className="text-muted">Loading fees…</p> : <div className="space-y-3">{fees.length === 0 ? <div className="rounded-2xl border border-dashed p-8 text-center text-muted">No fee records yet.</div> : fees.map(f=><div key={f.id} className="rounded-2xl border border-border p-5">
      <div className="flex items-start justify-between gap-4"><div><h3 className="font-semibold">{f.feeName}</h3><p className="text-sm text-muted">{isStudent ? '' : f.studentName + ' · '}Academic year {f.academicYear}{f.semester ? ' · Semester ' + f.semester : ''}</p></div><span className="rounded-full bg-hover px-3 py-1 text-xs font-semibold">{f.status}</span></div>
      <div className="mt-4 grid grid-cols-2 gap-4 md:grid-cols-4"><div><p className="text-xs text-muted">Total</p><p className="font-semibold">₹{f.totalAmount.toFixed(2)}</p></div><div><p className="text-xs text-muted">Paid</p><p className="font-semibold">₹{f.amountPaid.toFixed(2)}</p></div><div><p className="text-xs text-muted">Balance</p><p className="font-semibold">₹{f.balance.toFixed(2)}</p></div><div><p className="text-xs text-muted">Due</p><p className="font-semibold">{f.dueDate || '—'}</p></div></div>
      {<button className="mt-4 ml-2 inline-flex items-center gap-2 rounded-xl border px-4 py-2 text-sm font-semibold" onClick={async()=>{try{setHistory(await getPayments(f.id));setHistoryFee(f)}catch(e:any){setMessage(e?.response?.data?.message||'Unable to load payment history')}}}>View payments</button>}{!isStudent && f.balance > 0 && <button className="mt-4 inline-flex items-center gap-2 rounded-xl border px-4 py-2 text-sm font-semibold" onClick={()=>{setPaymentFor(f);setAmount(String(f.balance))}}><CreditCard size={16}/>Record payment</button>}
    </div>)}</div>}
    {historyFee && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"><div className="w-full max-w-lg rounded-2xl bg-card p-6 shadow-xl"><h2 className="text-xl font-semibold">Payment history</h2><p className="mt-1 text-sm text-muted">{historyFee.feeName} · {historyFee.studentName}</p><div className="mt-5 space-y-2">{history.length===0?<p className="text-sm text-muted">No payments recorded.</p>:history.map(p=><div key={p.id} className="flex items-center justify-between rounded-xl border p-3"><div><p className="font-semibold">₹{p.amount.toFixed(2)} · {p.paymentMethod}</p><p className="text-xs text-muted">{p.receiptNumber} · {new Date(p.paidAt).toLocaleString()}</p></div><span className="text-xs text-muted">{p.transactionReference||'—'}</span></div>)}</div><button className="mt-5 rounded-xl border px-4 py-2" onClick={()=>setHistoryFee(null)}>Close</button></div></div>}
    {paymentFor && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"><div className="w-full max-w-md rounded-2xl bg-card p-6 shadow-xl"><h2 className="text-xl font-semibold">Record payment</h2><p className="mt-1 text-sm text-muted">{paymentFor.feeName} · Balance ₹{paymentFor.balance.toFixed(2)}</p><div className="mt-5 space-y-3"><input className="w-full rounded-xl border p-3" type="number" max={paymentFor.balance} placeholder="Amount" value={amount} onChange={e=>setAmount(e.target.value)}/><select className="w-full rounded-xl border p-3" value={method} onChange={e=>setMethod(e.target.value as Payment['paymentMethod'])}>{['UPI','CASH','BANK_TRANSFER','CARD','OTHER'].map(x=><option key={x}>{x}</option>)}</select><input className="w-full rounded-xl border p-3" placeholder="Transaction reference" value={reference} onChange={e=>setReference(e.target.value)}/></div><div className="mt-5 flex justify-end gap-2"><button className="rounded-xl border px-4 py-2" onClick={()=>setPaymentFor(null)}>Cancel</button><button className="rounded-xl bg-primary px-4 py-2 font-semibold text-white" onClick={()=>void pay()}>Save payment</button></div></div></div>}
  </div>
}
