// src/pages/TeachersPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import { getTeachers, createTeacher, updateTeacher, deleteTeacher, type Teacher, type TeacherPayload } from '@/api'
import { Plus, Pencil, Trash2, Loader2, Search, GraduationCap, Users } from 'lucide-react'

const emptyForm: TeacherPayload = {
  employeeId: '',
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  phone: '',
  gender: '',
  qualification: '',
  specialization: '',
  experience: undefined,
  joiningDate: '',
}

function genderBadge(gender?: string) {
  if (gender === 'Male') return 'bg-blue-100 text-blue-700'
  if (gender === 'Female') return 'bg-pink-100 text-pink-700'
  return 'bg-gray-100 text-gray-700'
}

export function TeachersPage() {
  const [teachers, setTeachers] = useState<Teacher[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Teacher | null>(null)
  const [form, setForm] = useState<TeacherPayload>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setTeachers(await getTeachers())
    } catch {
      setError('Could not load teachers.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filteredTeachers = teachers.filter((teacher) => {
    const value = search.toLowerCase()
    return (
      teacher.employeeId.toLowerCase().includes(value) ||
      teacher.firstName.toLowerCase().includes(value) ||
      (teacher.lastName ?? '').toLowerCase().includes(value) ||
      teacher.email.toLowerCase().includes(value) ||
      teacher.specialization?.toLowerCase().includes(value)
    )
  })

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(t: Teacher) {
    setEditing(t)
    setForm({
      employeeId: t.employeeId,
      firstName: t.firstName,
      lastName: t.lastName ?? '',
      email: t.email,
      // Never returned by the backend (security) - blank here means "leave
      // unchanged" on save, not "clear the password".
      password: '',
      phone: t.phone ?? '',
      gender: t.gender ?? '',
      qualification: t.qualification ?? '',
      specialization: t.specialization ?? '',
      experience: t.experience,
      joiningDate: t.joiningDate ?? '',
    })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!editing && !form.password) {
      setFormError('Please enter a password for this teacher.')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      if (editing) await updateTeacher(editing.id, form)
      else await createTeacher(form)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save this teacher. Check the fields and try again.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this teacher? This cannot be undone.')) return
    try {
      await deleteTeacher(id)
      await load()
    } catch {
      alert('Could not delete this teacher.')
    }
  }

  return (
    <Layout>
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-medium text-ink">Teachers</h1>
          <p className="mt-1 text-sm text-slate-dim">Faculty members across all departments.</p>
        </div>
      </div>

      <StampGrid className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard icon={GraduationCap} label="Total Teachers" value={teachers.length} accent={STAT_SHADES[0]} />
        <StatCard icon={Users} label="Male" value={teachers.filter((t) => t.gender === 'Male').length} accent={STAT_SHADES[2]} />
        <StatCard icon={Users} label="Female" value={teachers.filter((t) => t.gender === 'Female').length} accent={STAT_SHADES[4]} />
        <StatCard
          icon={GraduationCap}
          label="Specializations"
          value={new Set(teachers.map((t) => t.specialization).filter(Boolean)).size}
          accent={STAT_SHADES[6]}
        />
      </StampGrid>

      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative w-full sm:max-w-md">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-dim" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search teachers..."
            className="w-full rounded-md border border-parchment-line bg-white/60 py-2 pl-9 pr-3 text-sm text-ink placeholder:text-slate-dim focus:border-brass focus:outline-none"
          />
        </div>

        <button
          onClick={openCreate}
          className="flex items-center justify-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright"
        >
          <Plus size={16} />
          Add Teacher
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-slate-dim">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : filteredTeachers.length === 0 ? (
        <div className="rounded-lg border border-dashed border-parchment-line bg-white/60 p-10 text-center">
          <h3 className="font-display text-xl text-ink">No Teachers Found</h3>
          <p className="mt-2 text-slate-dim">Add your first faculty member.</p>
          <button onClick={openCreate} className="mt-5 rounded-md bg-brass px-5 py-2 text-sm font-medium text-white hover:bg-brass-bright">
            Add Teacher
          </button>
        </div>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-lg border border-parchment-line bg-white/50 sm:block">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-parchment-line text-xs uppercase tracking-wide text-slate-dim">
                  <th className="px-4 py-3 font-medium">Teacher</th>
                  <th className="px-4 py-3 font-medium">Employee ID</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Gender</th>
                  <th className="px-4 py-3 font-medium">Qualification</th>
                  <th className="px-4 py-3 font-medium">Experience</th>
                  <th className="px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredTeachers.map((t) => (
                  <tr key={t.id} className="border-b border-parchment-line last:border-0">
                    <td className="px-4 py-3">
                      <div>
                        <p className="font-medium text-ink">
                          {t.firstName} {t.lastName}
                        </p>
                        <p className="text-xs text-slate-dim">{t.specialization || '—'}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-ink">{t.employeeId}</td>
                    <td className="px-4 py-3 text-slate-dim">{t.email}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded px-2 py-1 text-xs ${genderBadge(t.gender)}`}>{t.gender || '—'}</span>
                    </td>
                    <td className="px-4 py-3 text-slate-dim">{t.qualification || '—'}</td>
                    <td className="px-4 py-3 text-slate-dim">{t.experience ? `${t.experience} Years` : '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-3">
                        <button onClick={() => openEdit(t)} className="text-slate hover:text-ink">
                          <Pencil size={15} />
                        </button>
                        <button onClick={() => handleDelete(t.id)} className="text-slate hover:text-brick">
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="space-y-3 sm:hidden">
            {filteredTeachers.map((t) => (
              <li key={t.id} className="rounded-lg border border-parchment-line bg-white/60 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-display text-sm font-medium text-ink">{t.firstName} {t.lastName}</p>
                    <p className="font-mono text-xs text-slate-dim">{t.employeeId}</p>
                  </div>
                  <div className="flex shrink-0 gap-3">
                    <button onClick={() => openEdit(t)} className="text-slate hover:text-ink">
                      <Pencil size={15} />
                    </button>
                    <button onClick={() => handleDelete(t.id)} className="text-slate hover:text-brick">
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
                <p className="mt-2 text-xs text-slate-dim">{t.email}</p>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  <span className={`rounded px-2 py-0.5 text-xs ${genderBadge(t.gender)}`}>{t.gender || '—'}</span>
                  <span className="text-xs text-slate-dim">{t.qualification || '—'}</span>
                  {t.experience ? <span className="text-xs text-slate-dim">· {t.experience} yrs exp</span> : null}
                </div>
              </li>
            ))}
          </ul>
        </>
      )}

      {modalOpen && (
        <Modal title={editing ? 'Edit teacher' : 'Add teacher'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Employee ID">
                <input required value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Email">
                <input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Password">
                <input
                  type="password"
                  required={!editing}
                  minLength={8}
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  placeholder={editing ? 'Leave blank to keep unchanged' : 'Minimum 8 characters'}
                  className={inputClass}
                />
              </Field>
              <Field label="First name">
                <input required value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Last name">
                <input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Phone">
                <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Gender">
                <select value={form.gender} onChange={(e) => setForm({ ...form, gender: e.target.value })} className={inputClass}>
                  <option value="">Select</option>
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Other</option>
                </select>
              </Field>
              <Field label="Qualification">
                <select
                  value={form.qualification}
                  onChange={(e) => setForm({ ...form, qualification: e.target.value })}
                  className={inputClass}
                >
                  <option value="">Select</option>
                  <option>B.Tech</option>
                  <option>M.Tech</option>
                  <option>MCA</option>
                  <option>MSc</option>
                  <option>PhD</option>
                  <option>MBA</option>
                  <option>Other</option>
                </select>
              </Field>
              <Field label="Specialization">
                <input value={form.specialization} onChange={(e) => setForm({ ...form, specialization: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Experience (years)">
                <select
                  value={form.experience ?? ''}
                  onChange={(e) => setForm({ ...form, experience: e.target.value ? Number(e.target.value) : undefined })}
                  className={inputClass}
                >
                  <option value="">Select</option>
                  {Array.from({ length: 41 }, (_, i) => (
                    <option key={i} value={i}>
                      {i} Years
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Joining date">
                <input type="date" value={form.joiningDate} onChange={(e) => setForm({ ...form, joiningDate: e.target.value })} className={inputClass} />
              </Field>
            </div>
            {formError && <p className="text-sm text-brick">{formError}</p>}
            <div className="flex flex-col-reverse justify-end gap-3 pt-2 sm:flex-row">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-md border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:text-ink">
                Cancel
              </button>
              <button type="submit" disabled={saving} className="flex items-center justify-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright disabled:opacity-60">
                {saving && <Loader2 size={14} className="animate-spin" />}
                Save
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}