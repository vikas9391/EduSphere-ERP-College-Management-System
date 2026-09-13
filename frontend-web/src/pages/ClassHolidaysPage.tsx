import { useEffect, useState } from 'react'
import { CalendarOff, Plus, Trash2 } from 'lucide-react'
import { Layout } from '@/components/Layout'
import { Field, inputClass } from '@/components/FormField'
import { getMyClasses, type SchoolClass } from '@/api/schoolClass'
import { createClassHoliday, deleteClassHoliday, getClassHolidays, type ClassHoliday } from '@/api/attendance'

export function ClassHolidaysPage() {
  const [classes, setClasses] = useState<SchoolClass[]>([])
  const [classId, setClassId] = useState('')
  const [holidays, setHolidays] = useState<ClassHoliday[]>([])
  const [date, setDate] = useState('')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getMyClasses().then(items => {
      setClasses(items)
      if (items[0]) setClassId(String(items[0].id))
    }).catch(() => setError('Failed to load classes.')).finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!classId) return
    getClassHolidays(Number(classId)).then(setHolidays).catch(() => setError('Failed to load holidays.'))
  }, [classId])

  async function addHoliday() {
    if (!classId || !date) return
    setSaving(true); setError(null)
    try {
      const holiday = await createClassHoliday({ classId: Number(classId), holidayDate: date, reason: reason.trim() || undefined })
      setHolidays(prev => [holiday, ...prev].sort((a, b) => b.holidayDate.localeCompare(a.holidayDate)))
      setDate(''); setReason('')
    } catch { setError('Failed to create holiday. It may already exist for this date.') }
    finally { setSaving(false) }
  }

  async function removeHoliday(id: number) {
    if (!window.confirm('Remove this holiday?')) return
    try { await deleteClassHoliday(id); setHolidays(prev => prev.filter(h => h.id !== id)) }
    catch { setError('Failed to remove holiday.') }
  }

  return <Layout>
    <div className="flex items-start justify-between gap-4">
      <div><h1 className="font-heading text-2xl font-medium text-text">Class Holidays</h1><p className="mt-1 text-sm text-muted">Mark non-working days so they are shown as Not Counted in attendance.</p></div>
      <CalendarOff className="text-primary" size={26} />
    </div>

    <div className="mt-6 rounded-lg border border-border bg-white/60 p-5">
      {loading ? <p className="text-sm text-muted">Loading classes...</p> : <>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <Field label="Class"><select value={classId} onChange={e => setClassId(e.target.value)} className={inputClass}>{classes.map(c => <option key={c.id} value={c.id}>{c.name} · {c.academicYear} · Sem {c.semester}</option>)}</select></Field>
          <Field label="Holiday date"><input type="date" value={date} onChange={e => setDate(e.target.value)} className={inputClass} /></Field>
          <Field label="Reason (optional)"><input value={reason} onChange={e => setReason(e.target.value)} placeholder="Festival, college closure..." className={inputClass} /></Field>
        </div>
        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        <button onClick={addHoliday} disabled={!classId || !date || saving} className="mt-4 inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"><Plus size={16} />{saving ? 'Saving...' : 'Add Holiday'}</button>
      </>}
    </div>

    <div className="mt-6 overflow-hidden rounded-lg border border-border bg-white/60">
      {holidays.length === 0 ? <div className="p-10 text-center text-sm text-muted">No holidays configured for this class.</div> : <div className="divide-y divide-border">{holidays.map(h => <div key={h.id} className="flex items-center justify-between gap-4 p-4"><div><p className="font-medium text-text">{new Date(`${h.holidayDate}T00:00:00`).toLocaleDateString()}</p><p className="text-sm text-muted">{h.reason || 'Holiday'} · <span className="font-medium">Not Counted</span></p></div><button onClick={() => removeHoliday(h.id)} className="rounded-md p-2 text-muted hover:bg-red-50 hover:text-red-600" aria-label="Remove holiday"><Trash2 size={16} /></button></div>)}</div>}
    </div>
  </Layout>
}
