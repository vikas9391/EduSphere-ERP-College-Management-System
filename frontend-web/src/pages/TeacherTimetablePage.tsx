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
import { CalendarDays, Clock3, MapPin, Pencil, Plus, Trash2 } from 'lucide-react'

const DAYS: TimetableDay[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

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

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [schedule, subjects] = await Promise.all([
        getMyTimetableEntries(),
        getMyTeachingClassSubjects(),
      ])
      setEntries(schedule)
      setClassSubjects(subjects)
    } catch {
      setError('Could not load your timetable.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const grouped = useMemo(() => {
    return DAYS.map((day) => ({
      day,
      entries: entries
        .filter((entry) => entry.dayOfWeek === day)
        .sort((a, b) => a.startTime.localeCompare(b.startTime)),
    }))
  }, [entries])

  function openCreate() {
    setEditing(null)
    setForm({ ...emptyForm, classSubjectId: classSubjects[0] ? String(classSubjects[0].id) : '' })
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
    if (!form.classSubjectId) {
      setFormError('Select a class subject.')
      return
    }
    if (!form.startTime || !form.endTime || form.endTime <= form.startTime) {
      setFormError('End time must be after start time.')
      return
    }

    const payload: TimetableEntryPayload = {
      classSubjectId: Number(form.classSubjectId),
      dayOfWeek: form.dayOfWeek,
      startTime: form.startTime,
      endTime: form.endTime,
      room: form.room.trim(),
    }

    setSaving(true)
    setFormError(null)
    try {
      if (editing) await updateTimetableEntry(editing.id, payload)
      else await createTimetableEntry(payload)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save this slot. The class or teacher may already be scheduled at that time.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this timetable slot?')) return
    try {
      await deleteTimetableEntry(id)
      setEntries((current) => current.filter((entry) => entry.id !== id))
    } catch {
      alert('Could not delete this timetable slot.')
    }
  }

  return (
    <Layout>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-heading text-2xl font-medium text-text">Timetable</h1>
          <p className="mt-1 text-sm text-muted">Schedule your class subjects with real day, time, and room data</p>
        </div>
        <button
          onClick={openCreate}
          disabled={classSubjects.length === 0}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary/90 disabled:opacity-50"
        >
          <Plus size={16} />
          Add Slot
        </button>
      </div>

      {loading ? (
        <div className="mt-8 p-10 text-center text-sm text-muted">Loading timetable...</div>
      ) : error ? (
        <div className="mt-8"><PanelError message={error} /></div>
      ) : classSubjects.length === 0 ? (
        <div className="mt-8 rounded-lg border border-dashed border-border bg-white/60 p-10 text-center">
          <CalendarDays size={32} className="mx-auto text-muted/50" />
          <p className="mt-3 font-heading text-base font-medium text-text">No class subjects assigned</p>
          <p className="mt-1 text-sm text-muted">Create class subjects before scheduling timetable slots.</p>
        </div>
      ) : (
        <div className="mt-8 space-y-6">
          {grouped.map(({ day, entries: dayEntries }) => (
            <section key={day}>
              <div className="flex items-center gap-2">
                <CalendarDays size={17} className="text-primary" />
                <h2 className="font-heading text-base font-medium text-text">{day.charAt(0) + day.slice(1).toLowerCase()}</h2>
                <span className="rounded-full bg-border/50 px-2 py-0.5 text-xs text-muted">{dayEntries.length}</span>
              </div>

              {dayEntries.length === 0 ? (
                <p className="mt-2 text-sm text-muted">No classes scheduled.</p>
              ) : (
                <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
                  {dayEntries.map((entry) => (
                    <div key={entry.id} className="rounded-lg border border-border bg-white/60 p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="font-heading text-sm font-medium text-text">{entry.subjectName}</p>
                          <p className="mt-1 text-xs text-muted">
                            {entry.schoolClassName} · {entry.academicYear} · Semester {entry.semester}
                          </p>
                        </div>
                        <div className="flex gap-1">
                          <button onClick={() => openEdit(entry)} className="rounded p-1.5 text-muted hover:bg-border/50 hover:text-primary" aria-label="Edit slot">
                            <Pencil size={15} />
                          </button>
                          <button onClick={() => handleDelete(entry.id)} className="rounded p-1.5 text-muted hover:bg-red-50 hover:text-red-600" aria-label="Delete slot">
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </div>
                      <div className="mt-3 flex flex-wrap gap-4 text-xs text-muted">
                        <span className="inline-flex items-center gap-1"><Clock3 size={13} />{entry.startTime.slice(0, 5)} – {entry.endTime.slice(0, 5)}</span>
                        <span className="inline-flex items-center gap-1"><MapPin size={13} />{entry.room || 'TBD'}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
          ))}
        </div>
      )}

      {modalOpen && (
        <Modal title={editing ? 'Edit Timetable Slot' : 'Add Timetable Slot'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Class Subject">
              <select
                value={form.classSubjectId}
                onChange={(e) => setForm((current) => ({ ...current, classSubjectId: e.target.value }))}
                className={inputClass}
              >
                <option value="">Select class subject</option>
                {classSubjects.map((subject) => (
                  <option key={subject.id} value={subject.id}>
                    {subject.schoolClassName || `Class ${subject.schoolClassId}`} · {subject.subjectName}
                  </option>
                ))}
              </select>
            </Field>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Day">
                <select
                  value={form.dayOfWeek}
                  onChange={(e) => setForm((current) => ({ ...current, dayOfWeek: e.target.value as TimetableDay }))}
                  className={inputClass}
                >
                  {DAYS.map((day) => <option key={day} value={day}>{day.charAt(0) + day.slice(1).toLowerCase()}</option>)}
                </select>
              </Field>
              <Field label="Room">
                <input value={form.room} onChange={(e) => setForm((current) => ({ ...current, room: e.target.value }))} className={inputClass} placeholder="Room 204" />
              </Field>
              <Field label="Start Time">
                <input type="time" value={form.startTime} onChange={(e) => setForm((current) => ({ ...current, startTime: e.target.value }))} className={inputClass} />
              </Field>
              <Field label="End Time">
                <input type="time" value={form.endTime} onChange={(e) => setForm((current) => ({ ...current, endTime: e.target.value }))} className={inputClass} />
              </Field>
            </div>

            {formError && <p className="text-sm text-red-600">{formError}</p>}

            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-lg border border-border px-4 py-2 text-sm text-muted hover:text-text">Cancel</button>
              <button type="submit" disabled={saving} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-60">
                {saving ? 'Saving...' : editing ? 'Save Changes' : 'Add Slot'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}
