// src/pages/ClassDetailPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import {
  getSchoolClass,
  getRoster,
  addStudentsToClass,
  removeStudentFromClass,
  getClassSubjects,
  createClassSubject,
  createClassSubjectsBulk,
  deleteClassSubject,
  getClassSubjectEnrollments,
  getStudents,
  getTeachers,
  type SchoolClass,
  type ClassStudent,
  type ClassSubject,
  type ClassSubjectPayload,
  type ClassEnrollment,
  type Student,
  type Teacher,
  type EnrollmentMode,
} from '@/api'
import { ArrowLeft, Plus, Trash2, ChevronDown, ChevronUp } from 'lucide-react'
import { PanelError, Badge } from '@/components/PageBits'

const emptySubjectRow: { subjectCode: string; subjectName: string; credits: number; teacherId: number | ''; enrollmentMode: EnrollmentMode } = {
  subjectCode: '',
  subjectName: '',
  credits: 4,
  teacherId: '',
  enrollmentMode: 'MANDATORY',
}

export function ClassDetailPage() {
  const { id } = useParams()
  const classId = Number(id)

  const [schoolClass, setSchoolClass] = useState<SchoolClass | null>(null)
  const [roster, setRoster] = useState<ClassStudent[]>([])
  const [subjects, setSubjects] = useState<ClassSubject[]>([])
  const [allStudents, setAllStudents] = useState<Student[]>([])
  const [teachers, setTeachers] = useState<Teacher[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [addStudentsOpen, setAddStudentsOpen] = useState(false)
  const [selectedStudentIds, setSelectedStudentIds] = useState<number[]>([])
  const [rosterSaving, setRosterSaving] = useState(false)

  const [subjectModalOpen, setSubjectModalOpen] = useState(false)
  const [subjectRows, setSubjectRows] = useState([{ ...emptySubjectRow }])
  const [subjectSaving, setSubjectSaving] = useState(false)
  const [subjectError, setSubjectError] = useState<string | null>(null)

  const [expandedSubjectId, setExpandedSubjectId] = useState<number | null>(null)
  const [enrollments, setEnrollments] = useState<ClassEnrollment[]>([])

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [c, r, s, students, t] = await Promise.all([
        getSchoolClass(classId),
        getRoster(classId),
        getClassSubjects(classId),
        getStudents(),
        getTeachers(),
      ])
      setSchoolClass(c)
      setRoster(r)
      setSubjects(s)
      setAllStudents(students)
      setTeachers(t)
    } catch {
      setError('Could not load this class.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId])

  const studentsNotOnRoster = allStudents.filter((s) => !roster.some((r) => r.studentId === s.id))

  function openAddStudents() {
    setSelectedStudentIds([])
    setAddStudentsOpen(true)
  }

  async function handleAddStudents(e: FormEvent) {
    e.preventDefault()
    if (selectedStudentIds.length === 0) return
    setRosterSaving(true)
    try {
      await addStudentsToClass(classId, selectedStudentIds)
      setAddStudentsOpen(false)
      await load()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Could not add students.')
    } finally {
      setRosterSaving(false)
    }
  }

  async function handleRemoveStudent(studentId: number) {
    if (!confirm('Remove this student from the class? They will be dropped from any subjects they were enrolled in through this class.')) return
    try {
      await removeStudentFromClass(classId, studentId)
      await load()
    } catch {
      alert('Could not remove this student.')
    }
  }

  function openAddSubjects() {
    setSubjectRows([{ ...emptySubjectRow }])
    setSubjectError(null)
    setSubjectModalOpen(true)
  }

  function updateSubjectRow(index: number, patch: Partial<(typeof subjectRows)[number]>) {
    setSubjectRows((rows) => rows.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  function addSubjectRow() {
    setSubjectRows((rows) => [...rows, { ...emptySubjectRow }])
  }

  function removeSubjectRow(index: number) {
    setSubjectRows((rows) => rows.filter((_, i) => i !== index))
  }

  async function handleCreateSubjects(e: FormEvent) {
    e.preventDefault()
    setSubjectSaving(true)
    setSubjectError(null)
    try {
      const payloads: ClassSubjectPayload[] = subjectRows.map((row) => ({
        subjectCode: row.subjectCode,
        subjectName: row.subjectName,
        credits: row.credits,
        teacherId: Number(row.teacherId),
        enrollmentMode: row.enrollmentMode,
      }))
      if (payloads.length === 1) {
        await createClassSubject(classId, payloads[0])
      } else {
        await createClassSubjectsBulk(classId, payloads)
      }
      setSubjectModalOpen(false)
      await load()
    } catch (err) {
      setSubjectError(err instanceof Error ? err.message : 'Could not create these subjects.')
    } finally {
      setSubjectSaving(false)
    }
  }

  async function handleDeleteSubject(subjectId: number) {
    if (!confirm('Delete this subject? All enrollment records for it will be removed too.')) return
    try {
      await deleteClassSubject(classId, subjectId)
      await load()
    } catch {
      alert('Could not delete this subject.')
    }
  }

  async function toggleEnrollments(subjectId: number) {
    if (expandedSubjectId === subjectId) {
      setExpandedSubjectId(null)
      return
    }
    setExpandedSubjectId(subjectId)
    try {
      setEnrollments(await getClassSubjectEnrollments(subjectId))
    } catch {
      setEnrollments([])
    }
  }

  if (loading) {
    return (
      <Layout>
        <p className="text-sm text-slate-dim">Loading…</p>
      </Layout>
    )
  }

  if (error || !schoolClass) {
    return (
      <Layout>
        <PanelError message={error ?? 'Class not found.'} />
      </Layout>
    )
  }

  return (
    <Layout>
      <Link to="/teacher/classes" className="mb-4 inline-flex items-center gap-1.5 text-sm text-slate-dim hover:text-ink">
        <ArrowLeft size={15} />
        Back to Classes
      </Link>

      <div className="mb-6">
        <h1 className="font-display text-2xl font-medium text-ink">{schoolClass.name}</h1>
        <p className="mt-1 text-sm text-slate-dim">
          {schoolClass.academicYear} · Semester {schoolClass.semester}
          {schoolClass.maxSubjects != null ? ` · Capped at ${schoolClass.maxSubjects} subject(s)` : ''}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Roster */}
        <section className="rounded-lg border border-parchment-line bg-white/50 p-5">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-display text-lg text-ink">Roster ({roster.length})</h2>
            <button
              onClick={openAddStudents}
              className="flex items-center gap-1.5 rounded-md bg-brass px-3 py-1.5 text-xs font-medium text-white hover:bg-brass-bright"
            >
              <Plus size={14} />
              Add Students
            </button>
          </div>
          {roster.length === 0 ? (
            <p className="text-sm text-slate-dim">No students on the roster yet.</p>
          ) : (
            <ul className="space-y-2">
              {roster.map((s) => (
                <li key={s.studentId} className="flex items-center justify-between rounded-md border border-parchment-line bg-white/60 px-3 py-2">
                  <div>
                    <p className="text-sm font-medium text-ink">{s.studentName}</p>
                    <p className="text-xs text-slate-dim">{s.admissionNo}</p>
                  </div>
                  <button title="Remove" onClick={() => handleRemoveStudent(s.studentId)} className="rounded p-1.5 hover:bg-brick/10">
                    <Trash2 size={14} />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Subjects */}
        <section className="rounded-lg border border-parchment-line bg-white/50 p-5">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-display text-lg text-ink">Subjects ({subjects.length})</h2>
            <button
              onClick={openAddSubjects}
              className="flex items-center gap-1.5 rounded-md bg-brass px-3 py-1.5 text-xs font-medium text-white hover:bg-brass-bright"
            >
              <Plus size={14} />
              Add Subjects
            </button>
          </div>
          {subjects.length === 0 ? (
            <p className="text-sm text-slate-dim">No subjects yet - add all of this term's subjects at once.</p>
          ) : (
            <ul className="space-y-2">
              {subjects.map((s) => (
                <li key={s.id} className="rounded-md border border-parchment-line bg-white/60 px-3 py-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-ink">{s.subjectName}</p>
                      <p className="text-xs text-slate-dim">{s.subjectCode} · {s.teacherName} · {s.credits} credits</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant={s.enrollmentMode === 'MANDATORY' ? 'success' : 'neutral'}>
                        {s.enrollmentMode === 'MANDATORY' ? 'Mandatory' : 'Elective'}
                      </Badge>
                      <button title="Enrollments" onClick={() => toggleEnrollments(s.id)} className="rounded p-1.5 hover:bg-brass/10">
                        {expandedSubjectId === s.id ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                      </button>
                      <button title="Delete" onClick={() => handleDeleteSubject(s.id)} className="rounded p-1.5 hover:bg-brick/10">
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                  {expandedSubjectId === s.id && (
                    <div className="mt-3 border-t border-parchment-line pt-2">
                      <p className="mb-1.5 text-xs font-medium uppercase tracking-wide text-slate-dim">
                        Enrolled ({s.enrolledCount})
                      </p>
                      {enrollments.length === 0 ? (
                        <p className="text-xs text-slate-dim">No one enrolled yet.</p>
                      ) : (
                        <ul className="space-y-1">
                          {enrollments.map((e) => (
                            <li key={e.id} className="flex items-center justify-between text-xs text-slate-dim">
                              <span>{e.studentName}</span>
                              <span className="text-slate-dim/70">{e.source === 'AUTO' ? 'auto-enrolled' : 'self-enrolled'}</span>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {addStudentsOpen && (
        <Modal title="Add Students" onClose={() => setAddStudentsOpen(false)}>
          <form onSubmit={handleAddStudents} className="space-y-4">
            {studentsNotOnRoster.length === 0 ? (
              <p className="text-sm text-slate-dim">Every student is already on this class's roster.</p>
            ) : (
              <div className="max-h-72 space-y-1.5 overflow-y-auto rounded-md border border-parchment-line p-2">
                {studentsNotOnRoster.map((s) => (
                  <label key={s.id} className="flex items-center gap-2 rounded px-2 py-1.5 text-sm hover:bg-brass/5">
                    <input
                      type="checkbox"
                      checked={selectedStudentIds.includes(s.id)}
                      onChange={(e) =>
                        setSelectedStudentIds((ids) =>
                          e.target.checked ? [...ids, s.id] : ids.filter((existing) => existing !== s.id)
                        )
                      }
                    />
                    <span className="text-ink">{s.firstName} {s.lastName ?? ''}</span>
                    <span className="text-xs text-slate-dim">({s.admissionNo})</span>
                  </label>
                ))}
              </div>
            )}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setAddStudentsOpen(false)} className="rounded-md px-4 py-2 text-sm text-slate-dim hover:text-ink">
                Cancel
              </button>
              <button
                type="submit"
                disabled={rosterSaving || selectedStudentIds.length === 0}
                className="rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright disabled:opacity-60"
              >
                {rosterSaving ? 'Adding…' : `Add ${selectedStudentIds.length || ''} Student${selectedStudentIds.length === 1 ? '' : 's'}`}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {subjectModalOpen && (
        <Modal title="Add Subjects" onClose={() => setSubjectModalOpen(false)}>
          <form onSubmit={handleCreateSubjects} className="space-y-4">
            <p className="text-xs text-slate-dim">
              Set up one or more subjects for this term. Mandatory subjects auto-enroll the whole roster; elective
              subjects let students opt in themselves.
              {schoolClass.maxSubjects != null && ` This class is capped at ${schoolClass.maxSubjects} subject(s).`}
            </p>
            <div className="space-y-4">
              {subjectRows.map((row, index) => (
                <div key={index} className="rounded-md border border-parchment-line p-3">
                  <div className="mb-2 flex items-center justify-between">
                    <p className="text-xs font-medium uppercase tracking-wide text-slate-dim">Subject {index + 1}</p>
                    {subjectRows.length > 1 && (
                      <button type="button" onClick={() => removeSubjectRow(index)} className="rounded p-1 hover:bg-brick/10">
                        <Trash2 size={13} />
                      </button>
                    )}
                  </div>
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <Field label="Subject code">
                      <input
                        required
                        value={row.subjectCode}
                        onChange={(e) => updateSubjectRow(index, { subjectCode: e.target.value })}
                        className={inputClass}
                      />
                    </Field>
                    <Field label="Subject name">
                      <input
                        required
                        value={row.subjectName}
                        onChange={(e) => updateSubjectRow(index, { subjectName: e.target.value })}
                        className={inputClass}
                      />
                    </Field>
                    <Field label="Credits">
                      <input
                        required
                        type="number"
                        min={1}
                        max={20}
                        value={row.credits}
                        onChange={(e) => updateSubjectRow(index, { credits: Number(e.target.value) })}
                        className={inputClass}
                      />
                    </Field>
                    <Field label="Teacher">
                      <select
                        required
                        value={row.teacherId}
                        onChange={(e) => updateSubjectRow(index, { teacherId: Number(e.target.value) })}
                        className={inputClass}
                      >
                        <option value="">Select teacher</option>
                        {teachers.map((t) => (
                          <option key={t.id} value={t.id}>{t.firstName} {t.lastName}</option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Enrollment" className="sm:col-span-2">
                      <select
                        value={row.enrollmentMode}
                        onChange={(e) => updateSubjectRow(index, { enrollmentMode: e.target.value as 'MANDATORY' | 'ELECTIVE' })}
                        className={inputClass}
                      >
                        <option value="MANDATORY">Mandatory - auto-enroll the whole roster</option>
                        <option value="ELECTIVE">Elective - students opt in themselves</option>
                      </select>
                    </Field>
                  </div>
                </div>
              ))}
            </div>
            <button
              type="button"
              onClick={addSubjectRow}
              className="flex items-center gap-1.5 text-sm text-brass hover:text-brass-bright"
            >
              <Plus size={14} />
              Add another subject
            </button>
            {subjectError && <p className="text-sm text-brick">{subjectError}</p>}
            <div className="flex justify-end gap-3 border-t border-parchment-line pt-4">
              <button type="button" onClick={() => setSubjectModalOpen(false)} className="rounded-md px-4 py-2 text-sm text-slate-dim hover:text-ink">
                Cancel
              </button>
              <button
                type="submit"
                disabled={subjectSaving}
                className="rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright disabled:opacity-60"
              >
                {subjectSaving ? 'Saving…' : `Create ${subjectRows.length > 1 ? subjectRows.length + ' Subjects' : 'Subject'}`}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}
