// src/pages/EnrollmentsPage.tsx
import { useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import {
  ClipboardCheck,
  Search,
  Plus,
  Pencil,
  Trash2,
  Users,
  Layers,
  BookOpen,
  Filter,
  X,
} from 'lucide-react'
import {
  getEnrollments,
  createEnrollment,
  updateEnrollment,
  deleteEnrollment,
  type Enrollment,
  type EnrollmentPayload,
} from '@/api'
import { getStudents, type Student } from '@/api'
import { getSubjects, type Subject } from '@/api'

const emptyForm: EnrollmentPayload = {
  studentId: 0,
  subjectId: 0,
  academicYear: '',
  semester: 1,
  enrollmentDate: new Date().toISOString().slice(0, 10),
}

const EASE_STAMP = [0.16, 1, 0.3, 1] as const

const panelIn = {
  hidden: { opacity: 0, y: 14 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: EASE_STAMP } },
}

export function EnrollmentsPage() {
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [students, setStudents] = useState<Student[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [search, setSearch] = useState('')
  const [subjectFilter, setSubjectFilter] = useState<string>('')
  const [courseFilter, setCourseFilter] = useState<string>('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<EnrollmentPayload>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function loadAll() {
    setLoading(true)
    setError(null)
    try {
      const [e, s, sub] = await Promise.all([getEnrollments(), getStudents(), getSubjects()])
      setEnrollments(e)
      setStudents(s)
      setSubjects(sub)
    } catch {
      setError('Failed to load enrollments. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  const courseOptions = useMemo(
    () => Array.from(new Set(subjects.map((s) => s.courseName))).sort(),
    [subjects],
  )

  const filtered = useMemo(() => {
    return enrollments.filter((en) => {
      const matchesSearch =
        !search ||
        en.studentName.toLowerCase().includes(search.toLowerCase()) ||
        en.admissionNo.toLowerCase().includes(search.toLowerCase()) ||
        en.subjectName.toLowerCase().includes(search.toLowerCase())
      const matchesSubject = !subjectFilter || String(en.subjectId) === subjectFilter
      const matchesCourse = !courseFilter || en.courseName === courseFilter
      return matchesSearch && matchesSubject && matchesCourse
    })
  }, [enrollments, search, subjectFilter, courseFilter])

  const stats = useMemo(() => {
    const uniqueStudents = new Set(enrollments.map((e) => e.studentId)).size
    const uniqueSubjects = new Set(enrollments.map((e) => e.subjectId)).size
    const active = enrollments.filter((e) => e.status?.toLowerCase() === 'active').length
    return { total: enrollments.length, uniqueStudents, uniqueSubjects, active }
  }, [enrollments])

  function openAddModal() {
    setEditingId(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEditModal(en: Enrollment) {
    setEditingId(en.id)
    setForm({
      studentId: en.studentId,
      subjectId: en.subjectId,
      academicYear: en.academicYear,
      semester: en.semester,
      enrollmentDate: en.enrollmentDate?.slice(0, 10) ?? '',
    })
    setFormError(null)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditingId(null)
    setForm(emptyForm)
    setFormError(null)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.studentId) return setFormError('Please select a student.')
    if (!form.subjectId) return setFormError('Please select a subject.')
    if (!form.academicYear.trim()) return setFormError('Academic year is required.')
    if (!form.enrollmentDate) return setFormError('Enrollment date is required.')

    setSaving(true)
    setFormError(null)
    try {
      if (editingId) {
        await updateEnrollment(editingId, form)
      } else {
        await createEnrollment(form)
      }
      await loadAll()
      closeModal()
    } catch {
      setFormError('Could not save enrollment. Please check the details and try again.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!window.confirm('Remove this enrollment? This action cannot be undone.')) return
    try {
      await deleteEnrollment(id)
      setEnrollments((prev) => prev.filter((e) => e.id !== id))
    } catch {
      window.alert('Failed to delete enrollment. Please try again.')
    }
  }

  const hasFilters = !!search || !!subjectFilter || !!courseFilter

  return (
    <Layout>
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-medium text-ink">Enrollments</h1>
          <p className="mt-1 text-sm text-slate-dim">Manage student subject enrollments</p>
        </div>
        <button
          onClick={openAddModal}
          className="mt-4 inline-flex items-center gap-2 rounded-lg bg-brass px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brass-bright sm:mt-0"
        >
          <Plus size={16} />
          Add Enrollment
        </button>
      </div>

      {/* Dashboard cards */}
      <StampGrid className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard icon={ClipboardCheck} label="Total Enrollments" value={stats.total} accent={STAT_SHADES[0]} />
        <StatCard icon={Users} label="Students Enrolled" value={stats.uniqueStudents} accent={STAT_SHADES[2]} />
        <StatCard icon={Layers} label="Subjects Covered" value={stats.uniqueSubjects} accent={STAT_SHADES[4]} />
        <StatCard icon={BookOpen} label="Active" value={stats.active} accent={STAT_SHADES[6]} />
      </StampGrid>

      {/* Search + Filters */}
      <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-dim" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by student, admission no. or subject..."
            className="w-full rounded-lg border border-parchment-line bg-white/60 py-2 pl-9 pr-3 text-sm text-ink placeholder:text-slate-dim focus:border-brass focus:outline-none"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter size={16} className="text-slate-dim" />
          <select
            value={subjectFilter}
            onChange={(e) => setSubjectFilter(e.target.value)}
            className="rounded-lg border border-parchment-line bg-white/60 px-3 py-2 text-sm text-ink focus:border-brass focus:outline-none"
          >
            <option value="">All Subjects</option>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.subjectName}
              </option>
            ))}
          </select>

          <select
            value={courseFilter}
            onChange={(e) => setCourseFilter(e.target.value)}
            className="rounded-lg border border-parchment-line bg-white/60 px-3 py-2 text-sm text-ink focus:border-brass focus:outline-none"
          >
            <option value="">All Courses</option>
            {courseOptions.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>

          {hasFilters && (
            <button
              onClick={() => {
                setSearch('')
                setSubjectFilter('')
                setCourseFilter('')
              }}
              className="inline-flex items-center gap-1 rounded-lg border border-parchment-line px-3 py-2 text-sm text-slate-dim hover:border-brass hover:text-ink"
            >
              <X size={14} />
              Clear
            </button>
          )}
        </div>
      </div>

      {/* Table */}
      <motion.div
        className="paper mt-6 overflow-hidden rounded-lg border border-parchment-line bg-white/60 shadow-[var(--shadow-paper-lift)]"
        variants={panelIn}
        initial="hidden"
        animate="show"
      >
        {loading ? (
          <div className="p-10 text-center text-sm text-slate-dim">Loading enrollments...</div>
        ) : error ? (
          <div className="p-6">
            <PanelError message={error} />
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 p-12 text-center">
            <ClipboardCheck size={32} className="text-slate-dim/50" />
            <p className="font-display text-base font-medium text-ink">
              {hasFilters ? 'No enrollments match your filters' : 'No enrollments yet'}
            </p>
            <p className="text-sm text-slate-dim">
              {hasFilters
                ? 'Try adjusting your search or filters.'
                : 'Get started by adding your first student enrollment.'}
            </p>
            {!hasFilters && (
              <button
                onClick={openAddModal}
                className="mt-2 inline-flex items-center gap-2 rounded-lg bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright"
              >
                <Plus size={16} />
                Add Enrollment
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-200 text-left text-sm">
              <thead>
                <tr className="border-b border-parchment-line text-xs uppercase tracking-wide text-slate-dim">
                  <th className="px-5 py-3 font-medium">Student</th>
                  <th className="px-5 py-3 font-medium">Admission No.</th>
                  <th className="px-5 py-3 font-medium">Subject</th>
                  <th className="px-5 py-3 font-medium">Course</th>
                  <th className="px-5 py-3 font-medium">Semester</th>
                  <th className="px-5 py-3 font-medium">Academic Year</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((en) => (
                  <tr key={en.id} className="border-b border-parchment-line last:border-0 hover:bg-white/80">
                    <td className="px-5 py-3 text-ink">{en.studentName}</td>
                    <td className="px-5 py-3 text-slate-dim">{en.admissionNo}</td>
                    <td className="px-5 py-3 text-ink">{en.subjectName}</td>
                    <td className="px-5 py-3 text-slate-dim">{en.courseName}</td>
                    <td className="px-5 py-3 text-slate-dim">{en.semester}</td>
                    <td className="px-5 py-3 text-slate-dim">{en.academicYear}</td>
                    <td className="px-5 py-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          en.status?.toLowerCase() === 'active'
                            ? 'bg-green-100 text-green-700'
                            : 'bg-slate-100 text-slate-dim'
                        }`}
                      >
                        {en.status || 'Unknown'}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => openEditModal(en)}
                          className="rounded-md p-1.5 text-slate-dim hover:bg-parchment-line/50 hover:text-brass"
                          aria-label="Edit enrollment"
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          onClick={() => handleDelete(en.id)}
                          className="rounded-md p-1.5 text-slate-dim hover:bg-brick/10 hover:text-brick"
                          aria-label="Delete enrollment"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </motion.div>

      {/* Add / Edit Modal */}
      {modalOpen && (
        <Modal onClose={closeModal} title={editingId ? 'Edit Enrollment' : 'Add Enrollment'}>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Field label="Student">
            <select
              value={form.studentId || ''}
              onChange={(e) => setForm({ ...form, studentId: Number(e.target.value) })}
              className={inputClass}
            >
              <option value="">Select a student</option>
              {students.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.firstName} {s.lastName} ({s.admissionNo})
                </option>
              ))}
            </select>
          </Field>

          <Field label="Subject">
            <select
              value={form.subjectId || ''}
              onChange={(e) => setForm({ ...form, subjectId: Number(e.target.value) })}
              className={inputClass}
            >
              <option value="">Select a subject</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.subjectName} · {s.courseName}
                </option>
              ))}
            </select>
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Academic Year">
              <input
                value={form.academicYear}
                onChange={(e) => setForm({ ...form, academicYear: e.target.value })}
                placeholder="e.g. 2025-2026"
                className={inputClass}
              />
            </Field>

            <Field label="Semester">
              <input
                type="number"
                min={1}
                max={12}
                value={form.semester}
                onChange={(e) => setForm({ ...form, semester: Number(e.target.value) })}
                className={inputClass}
              />
            </Field>
          </div>

          <Field label="Enrollment Date">
            <input
              type="date"
              value={form.enrollmentDate}
              onChange={(e) => setForm({ ...form, enrollmentDate: e.target.value })}
              className={inputClass}
            />
          </Field>

          {formError && <p className="text-sm text-brick">{formError}</p>}

          <div className="mt-2 flex justify-end gap-3">
            <button
              type="button"
              onClick={closeModal}
              className="rounded-lg border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:border-brass hover:text-ink"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright disabled:opacity-60"
            >
              {saving ? 'Saving...' : editingId ? 'Save Changes' : 'Add Enrollment'}
            </button>
          </div>
        </form>
        </Modal>
      )}
    </Layout>
  )
}