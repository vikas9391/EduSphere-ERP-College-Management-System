// src/pages/ClassesPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { getMyClasses, createSchoolClass, deleteSchoolClass, type SchoolClass, type SchoolClassPayload } from '@/api'
import { Plus, Trash2, Layers, Users, BookMarked } from 'lucide-react'
import { StatCard, PanelError, Badge, STAT_SHADES } from '@/components/PageBits'

const emptyForm = {
  name: '',
  academicYear: '',
  semester: 1,
  maxSubjects: '' as number | '',
}
type FormState = typeof emptyForm

export function ClassesPage() {
  const navigate = useNavigate()
  const [classes, setClasses] = useState<SchoolClass[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setClasses(await getMyClasses())
    } catch {
      setError('Could not load your classes.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function openCreate() {
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    const payload: SchoolClassPayload = {
      name: form.name,
      academicYear: form.academicYear,
      semester: form.semester,
      maxSubjects: form.maxSubjects === '' ? null : Number(form.maxSubjects),
    }
    try {
      await createSchoolClass(payload)
      setModalOpen(false)
      await load()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Could not create this class.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this class? Its roster and subjects will be removed too. This cannot be undone.')) return
    try {
      await deleteSchoolClass(id)
      await load()
    } catch {
      alert('Could not delete this class.')
    }
  }

  const totalStudents = classes.reduce((sum, c) => sum + c.studentCount, 0)
  const totalSubjects = classes.reduce((sum, c) => sum + c.subjectCount, 0)

  return (
    <Layout>
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-medium text-ink">Classes</h1>
          <p className="mt-1 text-sm text-slate-dim">
            Set up a batch/section: name it, build its roster, then add the subjects for the term.
          </p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center justify-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright"
        >
          <Plus size={16} />
          Create Class
        </button>
      </div>

      <StampGrid className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard icon={Layers} label="My Classes" value={classes.length} accent={STAT_SHADES[0]} />
        <StatCard icon={Users} label="Total Students" value={totalStudents} accent={STAT_SHADES[2]} />
        <StatCard icon={BookMarked} label="Total Subjects" value={totalSubjects} accent={STAT_SHADES[4]} />
      </StampGrid>

      {loading ? (
        <p className="text-sm text-slate-dim">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : classes.length === 0 ? (
        <div className="rounded-lg border border-dashed border-parchment-line bg-white/60 p-10 text-center">
          <h3 className="font-display text-xl text-ink">No Classes Yet</h3>
          <p className="mt-2 text-slate-dim">Create your first class to start building a roster and subjects.</p>
          <button onClick={openCreate} className="mt-5 rounded-md bg-brass px-5 py-2 text-sm font-medium text-white hover:bg-brass-bright">
            Create Class
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {classes.map((c) => (
            <div
              key={c.id}
              onClick={() => navigate(`/teacher/classes/${c.id}`)}
              className="cursor-pointer rounded-lg border border-parchment-line bg-white/60 p-5 transition hover:border-brass hover:shadow-[var(--shadow-paper)]"
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-display text-lg font-medium text-ink">{c.name}</p>
                  <p className="mt-0.5 text-xs text-slate-dim">{c.academicYear} · Semester {c.semester}</p>
                </div>
                <button
                  title="Delete"
                  onClick={(e) => {
                    e.stopPropagation()
                    handleDelete(c.id)
                  }}
                  className="rounded p-1.5 hover:bg-brick/10"
                >
                  <Trash2 size={15} />
                </button>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <Badge variant="neutral">{c.studentCount} student{c.studentCount === 1 ? '' : 's'}</Badge>
                <Badge variant="success">
                  {c.subjectCount} subject{c.subjectCount === 1 ? '' : 's'}
                  {c.maxSubjects != null ? ` / ${c.maxSubjects}` : ''}
                </Badge>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalOpen && (
        <Modal title="Create Class" onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Class name">
              <input
                required
                placeholder="e.g. CSE 3rd Year - Section A"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className={inputClass}
              />
            </Field>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Academic year">
                <input
                  required
                  placeholder="e.g. 2025-26"
                  value={form.academicYear}
                  onChange={(e) => setForm({ ...form, academicYear: e.target.value })}
                  className={inputClass}
                />
              </Field>
              <Field label="Semester">
                <input
                  required
                  type="number"
                  min={1}
                  max={12}
                  value={form.semester}
                  onChange={(e) => setForm({ ...form, semester: Number(e.target.value) })}
                  className={inputClass}
                />
              </Field>
            </div>
            <Field label="Max subjects (optional)">
              <input
                type="number"
                min={1}
                placeholder="Leave blank for no limit"
                value={form.maxSubjects}
                onChange={(e) => setForm({ ...form, maxSubjects: e.target.value === '' ? '' : Number(e.target.value) })}
                className={inputClass}
              />
            </Field>
            {formError && <p className="text-sm text-brick">{formError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-md px-4 py-2 text-sm text-slate-dim hover:text-ink">
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright disabled:opacity-60"
              >
                {saving ? 'Saving…' : 'Create Class'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}
