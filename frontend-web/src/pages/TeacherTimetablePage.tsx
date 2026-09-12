import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { PanelError } from '@/components/PageBits'
import { getMyTeachingClassSubjects, type ClassSubject } from '@/api/schoolClass'
import {
  createTimetableEntry,
  deleteTimetableEntry,
  getMyTimetableEntries,
  updateTimetableEntry,
  type TimetableDay,
  type TimetableEntry,
  type TimetableEntryPayload,
} from '@/api/timetable'
import { CalendarDays, Clock3, MapPin, Pencil, Plus, Trash2, Eye, EyeOff } from 'lucide-react'

const DAYS: TimetableDay[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const DAY_LABELS: Record<TimetableDay, string> = {
  MONDAY: 'Monday', TUESDAY: 'Tuesday', WEDNESDAY: 'Wednesday', THURSDAY: 'Thursday',
  FRIDAY: 'Friday', SATURDAY: 'Saturday', SUNDAY: 'Sunday',
}

const emptyForm = {
  classSubjectId: '',
  dayOfWeek: 'MONDAY' as TimetableDay,
  startTime: '09:00',
  endTime: '10:00',
  room: '',
}

type FormState = typeof emptyForm

export function TeacherTimetablePage() {
  const [entries, setEntries] = useState<TimetableEntry[]>([])
  const [classSubjects, setClassSubjects] = useState<ClassSubject[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<TimetableEntry | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [visibleDays, setVisibleDays] = useState<TimetableDay[]>(DAYS)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [schedule, subjects] = await Promise.all([getMyTimetableEntries(), getMyTeachingClassSubjects()])
      setEntries(schedule)
      setClassSubjects(subjects)
    } catch {
      setError('Could not load your timetable.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const timeRows = useMemo(() => {
    const times = new Set<string>()
    entries.forEach((entry) => times.add(`${entry.startTime.slice(0, 5)}-${entry.endTime.slice(0, 5)}`))
    if (times.size === 0) times.add('09:00-10:00')
    return Array.from(times).sort()
  }, [entries])

  const grid = useMemo(() => {
    return timeRows.map((row) => {
      const [start, end] = row.split('-')
      return {
        row,
        start,
        end,
        cells: visibleDays.map((day) => entries.filter((entry) => entry.dayOfWeek === day && entry.startTime.slice(0, 5) === start && entry.endTime.slice(0, 5) === end)),
      }
    })
  }, [entries, timeRows, visibleDays])

  function toggleDay(day: TimetableDay) {
    setVisibleDays((current) => current.includes(day) ? current.filter((item) => item !== day) : [...current, day].sort((a, b) => DAYS.indexOf(a) - DAYS.indexOf(b)))
  }

  function openCreate(day?: TimetableDay, startTime?: string, endTime?: string) {
    setEditing(null)
    setForm({
      ...emptyForm,
      classSubjectId: classSubjects[0] ? String(classSubjects[0].id) : '',
      dayOfWeek: day ?? 'MONDAY',
      startTime: startTime ?? '09:00',
      endTime: endTime ?? '10:00',
    })
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(entry: TimetableEntry) {
    setEditing(entry)
    setForm({
      classSubjectId: String(entry.classSubjectId),
      dayOfWeek: entry.dayOfWeek,
      startTime: entry.startTime.slice(0, 5),
      endTime: entry.endTime.slice(0, 5),
      room: entry.room || '',
    })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!form.classSubjectId) return setFormError('Select a class subject.')
    if (!form.startTime || !form.endTime || form.endTime <= form.startTime) return setFormError('End time must be after start time.')

    const payload: TimetableEntryPayload = {
      classSubjectId: Number(form.classSubjectId), dayOfWeek: form.dayOfWeek,
      startTime: form.startTime, endTime: form.endTime, room: form.room.trim(),
    }
    setSaving(true); setFormError(null)
    try {
      if (editing) await updateTimetableEntry(editing.id, payload)
      else await createTimetableEntry(payload)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save this slot. The class or teacher may already be scheduled at that time.')
    } finally { setSaving(false) }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this timetable slot?')) return
    try { await deleteTimetableEntry(id); setEntries((current) => current.filter((entry) => entry.id !== id)) }
    catch { alert('Could not delete this timetable slot.') }
  }

  return (
    <Layout>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-heading text-2xl font-medium text-text">Timetable</h1>
          <p className="mt-1 text-sm text-muted">Build and edit your timetable as a flexible grid. Add periods whenever your schedule changes.</p>
        </div>
        <button onClick={() => openCreate()} disabled={classSubjects.length === 0} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary/90 disabled:opacity-50">
          <Plus size={16} /> Add Period / Slot
        </button>
      </div>

      {loading ? <div className="mt-8 p-10 text-center text-sm text-muted">Loading timetable...</div> : error ? <div className="mt-8"><PanelError message={error} /></div> : classSubjects.length === 0 ? (
        <div className="mt-8 rounded-lg border border-dashed border-border bg-white/60 p-10 text-center"><CalendarDays size={32} className="mx-auto text-muted/50" /><p className="mt-3 font-heading text-base font-medium text-text">No class subjects assigned</p><p className="mt-1 text-sm text-muted">Create class subjects before scheduling timetable slots.</p></div>
      ) : (
        <div className="mt-8 space-y-5">
          <div className="rounded-lg border border-border bg-white/60 p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div><p className="font-heading text-sm font-medium text-text">Timetable columns</p><p className="text-xs text-muted">Show or hide days without deleting their scheduled classes.</p></div>
              <div className="flex flex-wrap gap-2">
                {DAYS.map((day) => {
                  const shown = visibleDays.includes(day)
                  return <button key={day} onClick={() => toggleDay(day)} className={`inline-flex items-center gap-1.5 rounded-md border px-2.5 py-1.5 text-xs font-medium ${shown ? 'border-primary/30 bg-primary/10 text-primary' : 'border-border bg-bg text-muted'}`}>
                    {shown ? <Eye size={13} /> : <EyeOff size={13} />}{DAY_LABELS[day]}
                  </button>
                })}
              </div>
            </div>
          </div>

          <div className="overflow-x-auto rounded-lg border border-border bg-white/60">
            <table className="w-full min-w-[900px] border-collapse text-sm">
              <thead><tr className="border-b border-border bg-bg"><th className="w-28 border-r border-border px-3 py-3 text-left text-xs font-medium text-muted">Period</th>{visibleDays.map((day) => <th key={day} className="border-r border-border px-3 py-3 text-left text-xs font-medium text-muted last:border-r-0">{DAY_LABELS[day]}</th>)}</tr></thead>
              <tbody>{grid.map(({ row, start, end, cells }) => <tr key={row} className="border-b border-border last:border-b-0">
                <td className="border-r border-border px-3 py-3 align-top text-xs font-medium text-muted">{start} – {end}</td>
                {cells.map((dayCells, index) => <td key={`${row}:${visibleDays[index]}`} className="min-w-[150px] border-r border-border p-2 align-top last:border-r-0">
                  {dayCells.length === 0 ? <button onClick={() => openCreate(visibleDays[index], start, end)} className="flex min-h-20 w-full items-center justify-center rounded-md border border-dashed border-border text-xs text-muted hover:border-primary hover:text-primary"><Plus size={14} className="mr-1" /> Add slot</button> : <div className="space-y-2">{dayCells.map((entry) => <div key={entry.id} className="rounded-md border border-border bg-bg p-2.5">
                    <div className="flex items-start justify-between gap-2"><div className="min-w-0"><p className="truncate text-xs font-medium text-text">{entry.subjectName}</p><p className="mt-0.5 truncate text-[11px] text-muted">{entry.schoolClassName}</p></div><div className="flex gap-0.5"><button onClick={() => openEdit(entry)} className="rounded p-1 text-muted hover:bg-white hover:text-primary" aria-label="Edit slot"><Pencil size={13} /></button><button onClick={() => handleDelete(entry.id)} className="rounded p-1 text-muted hover:bg-red-50 hover:text-red-600" aria-label="Delete slot"><Trash2 size={13} /></button></div></div>
                    <div className="mt-2 flex flex-wrap gap-2 text-[11px] text-muted"><span className="inline-flex items-center gap-1"><Clock3 size={11} />{entry.startTime.slice(0, 5)}–{entry.endTime.slice(0, 5)}</span><span className="inline-flex items-center gap-1"><MapPin size={11} />{entry.room || 'TBD'}</span></div>
                  </div>)}</div>}
                </td>)}
              </tr>)}</tbody>
            </table>
          </div>

          <div className="rounded-lg border border-border bg-white/60 p-4 text-xs text-muted">
            <strong className="text-text">Flexible editing:</strong> each distinct time range becomes a row automatically. Use <strong className="text-text">Add slot</strong> to introduce a new period, edit any existing cell, or hide/show day columns. Changes continue to use the existing class/teacher conflict checks before saving.
          </div>
        </div>
      )}

      {modalOpen && <Modal title={editing ? 'Edit Timetable Slot' : 'Add Timetable Slot'} onClose={() => setModalOpen(false)}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Field label="Class Subject"><select value={form.classSubjectId} onChange={(e) => setForm((current) => ({ ...current, classSubjectId: e.target.value }))} className={inputClass}><option value="">Select class subject</option>{classSubjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.schoolClassName || `Class ${subject.schoolClassId}`} · {subject.subjectName}</option>)}</select></Field>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Day"><select value={form.dayOfWeek} onChange={(e) => setForm((current) => ({ ...current, dayOfWeek: e.target.value as TimetableDay }))} className={inputClass}>{DAYS.map((day) => <option key={day} value={day}>{DAY_LABELS[day]}</option>)}</select></Field>
            <Field label="Room"><input value={form.room} onChange={(e) => setForm((current) => ({ ...current, room: e.target.value }))} className={inputClass} placeholder="Room 204" /></Field>
            <Field label="Start Time"><input type="time" value={form.startTime} onChange={(e) => setForm((current) => ({ ...current, startTime: e.target.value }))} className={inputClass} /></Field>
            <Field label="End Time"><input type="time" value={form.endTime} onChange={(e) => setForm((current) => ({ ...current, endTime: e.target.value }))} className={inputClass} /></Field>
          </div>
          {formError && <p className="text-sm text-red-600">{formError}</p>}
          <div className="flex justify-end gap-2 pt-2"><button type="button" onClick={() => setModalOpen(false)} className="rounded-lg border border-border px-4 py-2 text-sm text-muted hover:text-text">Cancel</button><button type="submit" disabled={saving} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-60">{saving ? 'Saving...' : editing ? 'Save Changes' : 'Add Slot'}</button></div>
        </form>
      </Modal>}
    </Layout>
  )
}
