// src/pages/student/StudentClassesPage.tsx
import { useEffect, useState } from 'react'
import { Layout } from '@/components/Layout'
import {
  getMyClassesAsStudent,
  getClassSubjectsForStudent,
  selfEnrollInClassSubject,
  selfDropClassSubject,
  type SchoolClass,
  type ClassSubject,
} from '@/api'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { PanelError, Badge } from '@/components/PageBits'

export function StudentClassesPage() {
  const [classes, setClasses] = useState<SchoolClass[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [expandedClassId, setExpandedClassId] = useState<number | null>(null)
  const [subjects, setSubjects] = useState<ClassSubject[]>([])
  const [subjectsLoading, setSubjectsLoading] = useState(false)
  const [busySubjectId, setBusySubjectId] = useState<number | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setClasses(await getMyClassesAsStudent())
    } catch {
      setError('Could not load your classes.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function toggleClass(classId: number) {
    if (expandedClassId === classId) {
      setExpandedClassId(null)
      return
    }
    setExpandedClassId(classId)
    setSubjectsLoading(true)
    try {
      setSubjects(await getClassSubjectsForStudent(classId))
    } catch {
      setSubjects([])
    } finally {
      setSubjectsLoading(false)
    }
  }

  async function handleEnroll(subjectId: number) {
    setBusySubjectId(subjectId)
    try {
      await selfEnrollInClassSubject(subjectId)
      if (expandedClassId != null) setSubjects(await getClassSubjectsForStudent(expandedClassId))
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Could not enroll in this subject.')
    } finally {
      setBusySubjectId(null)
    }
  }

  async function handleDrop(subjectId: number) {
    if (!confirm('Drop this elective subject?')) return
    setBusySubjectId(subjectId)
    try {
      await selfDropClassSubject(subjectId)
      if (expandedClassId != null) setSubjects(await getClassSubjectsForStudent(expandedClassId))
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Could not drop this subject.')
    } finally {
      setBusySubjectId(null)
    }
  }

  return (
    <Layout>
      <div className="mb-6">
        <h1 className="font-heading text-2xl font-medium text-text">My Classes</h1>
        <p className="mt-1 text-sm text-muted">
          Classes your teachers have added you to. Mandatory subjects are automatic - elective ones you choose yourself.
        </p>
      </div>

      {loading ? (
        <p className="text-sm text-muted">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : classes.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border bg-white/60 p-10 text-center">
          <h3 className="font-heading text-xl text-text">No Classes Yet</h3>
          <p className="mt-2 text-muted">You haven't been added to a class by a teacher yet.</p>
        </div>
      ) : (
        <ul className="space-y-3">
          {classes.map((c) => (
            <li key={c.id} className="rounded-lg border border-border bg-white/60 p-5">
              <button onClick={() => toggleClass(c.id)} className="flex w-full items-center justify-between text-left">
                <div>
                  <p className="font-heading text-lg font-medium text-text">{c.name}</p>
                  <p className="mt-0.5 text-xs text-muted">
                    {c.academicYear} · Semester {c.semester} · {c.subjectCount} subject{c.subjectCount === 1 ? '' : 's'}
                  </p>
                </div>
                {expandedClassId === c.id ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
              </button>

              {expandedClassId === c.id && (
                <div className="mt-4 border-t border-border pt-4">
                  {subjectsLoading ? (
                    <p className="text-sm text-muted">Loading subjects…</p>
                  ) : subjects.length === 0 ? (
                    <p className="text-sm text-muted">No subjects have been added to this class yet.</p>
                  ) : (
                    <ul className="space-y-2">
                      {subjects.map((s) => (
                        <li key={s.id} className="flex items-center justify-between rounded-md border border-border bg-white/60 px-3 py-2">
                          <div>
                            <p className="text-sm font-medium text-text">{s.subjectName}</p>
                            <p className="text-xs text-muted">{s.subjectCode} · {s.teacherName} · {s.credits} credits</p>
                          </div>
                          <div className="flex items-center gap-2">
                            <Badge variant={s.enrollmentMode === 'MANDATORY' ? 'success' : 'neutral'}>
                              {s.enrollmentMode === 'MANDATORY' ? 'Mandatory' : 'Elective'}
                            </Badge>
                            {s.enrollmentMode === 'ELECTIVE' && (
                              s.enrolledByMe ? (
                                <button
                                  disabled={busySubjectId === s.id}
                                  onClick={() => handleDrop(s.id)}
                                  className="rounded-md border border-border px-3 py-1 text-xs text-muted hover:text-danger disabled:opacity-60"
                                >
                                  Drop
                                </button>
                              ) : (
                                <button
                                  disabled={busySubjectId === s.id}
                                  onClick={() => handleEnroll(s.id)}
                                  className="rounded-md bg-primary px-3 py-1 text-xs font-medium text-white hover:bg-secondary disabled:opacity-60"
                                >
                                  Enroll
                                </button>
                              )
                            )}
                          </div>
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
    </Layout>
  )
}
