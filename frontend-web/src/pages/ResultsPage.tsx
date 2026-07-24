// src/pages/ResultsPage.tsx
import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { Field, inputClass } from '@/components/FormField'
import { PanelHeader, PanelError } from '@/components/PageBits'
import {
  getStudents,
  getSemesterResult,
  getOverallResult,
  type Student,
  type SemesterResult,
  type OverallResult,
} from '@/api'
import { Search, Loader2, GraduationCap, FileBarChart } from 'lucide-react'

const EASE_STAMP = [0.16, 1, 0.3, 1] as const

const panelIn = {
  hidden: { opacity: 0, y: 14 },
  show: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.4, ease: EASE_STAMP, delay: 0.06 * i },
  }),
}

export function ResultsPage() {
  const [students, setStudents] = useState<Student[]>([])
  const [studentId, setStudentId] = useState<number | ''>('')
  const [semester, setSemester] = useState(1)
  const [academicYear, setAcademicYear] = useState('')

  const [mode, setMode] = useState<'semester' | 'overall'>('semester')
  const [semesterResult, setSemesterResult] = useState<SemesterResult | null>(null)
  const [overallResult, setOverallResult] = useState<OverallResult | null>(null)

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getStudents().then(setStudents).catch(() => setStudents([]))
  }, [])

  async function handleFetchSemester() {
    if (!studentId) {
      setError('Please select a student.')
      return
    }
    setLoading(true)
    setError(null)
    setOverallResult(null)
    setMode('semester')
    try {
      const res = await getSemesterResult(Number(studentId), semester, academicYear)
      setSemesterResult(res)
    } catch {
      setError('No published result found for this student in that semester.')
      setSemesterResult(null)
    } finally {
      setLoading(false)
    }
  }

  async function handleFetchOverall() {
    if (!studentId) {
      setError('Please select a student.')
      return
    }
    setLoading(true)
    setError(null)
    setSemesterResult(null)
    setMode('overall')
    try {
      const res = await getOverallResult(Number(studentId))
      setOverallResult(res)
    } catch {
      setError('No published results found for this student.')
      setOverallResult(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Layout>
      <div className="mb-8">
        <h1 className="font-display text-2xl font-medium text-ink">Results</h1>
        <p className="mt-1 text-sm text-slate-dim">SGPA and CGPA computed from published marks.</p>
      </div>

      <motion.div
        className="paper mb-8 rounded-lg border border-parchment-line bg-white/50 p-5 shadow-[var(--shadow-paper-lift)]"
        custom={0}
        variants={panelIn}
        initial="hidden"
        animate="show"
      >
        <PanelHeader icon={FileBarChart} title="Look Up a Result" />

        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <div className="col-span-2 lg:col-span-2">
            <Field label="Student">
              <select value={studentId} onChange={(e) => setStudentId(e.target.value ? Number(e.target.value) : '')} className={inputClass}>
                <option value="">Select student</option>
                {students.map((s) => (
                  <option key={s.id} value={s.id}>{s.firstName} {s.lastName} ({s.admissionNo})</option>
                ))}
              </select>
            </Field>
          </div>
          <Field label="Semester">
            <input type="number" min={1} max={12} value={semester} onChange={(e) => setSemester(Number(e.target.value))} className={inputClass} />
          </Field>
          <Field label="Academic year">
            <input placeholder="2025-26" value={academicYear} onChange={(e) => setAcademicYear(e.target.value)} className={inputClass} />
          </Field>
        </div>

        <div className="mt-5 flex gap-3">
          <button
            onClick={handleFetchSemester}
            disabled={loading}
            className="flex items-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass-bright disabled:opacity-60"
          >
            {loading && mode === 'semester' ? <Loader2 size={14} className="animate-spin" /> : <Search size={16} />}
            Semester Result
          </button>
          <button
            onClick={handleFetchOverall}
            disabled={loading}
            className="flex items-center gap-2 rounded-md border border-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass/10 disabled:opacity-60"
          >
            {loading && mode === 'overall' ? <Loader2 size={14} className="animate-spin" /> : <GraduationCap size={16} />}
            Overall Result (CGPA)
          </button>
        </div>

        {error && (
          <div className="mt-4">
            <PanelError message={error} />
          </div>
        )}
      </motion.div>

      {mode === 'semester' && semesterResult && (
        <motion.div
          className="paper rounded-lg border border-parchment-line bg-white/50 p-5 shadow-[var(--shadow-paper-lift)]"
          custom={1}
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="font-display text-lg text-ink">{semesterResult.studentName}</h2>
              <p className="text-sm text-slate-dim">Semester {semesterResult.semester} · {semesterResult.academicYear}</p>
            </div>
            <div className="text-right">
              <p className="text-sm text-slate-dim">SGPA</p>
              <p className="text-2xl font-semibold text-ink">{semesterResult.sgpa.toFixed(2)}</p>
              <span className={`rounded px-2 py-0.5 text-xs ${semesterResult.result === 'PASS' ? 'bg-green-100 text-green-700' : 'bg-brick/10 text-brick'}`}>
                {semesterResult.result}
              </span>
            </div>
          </div>
          <ResultTable subjects={semesterResult.subjects} />
        </motion.div>
      )}

      {mode === 'overall' && overallResult && (
        <div className="space-y-6">
          <motion.div
            className="paper rounded-lg border border-parchment-line bg-white/50 p-5 shadow-[var(--shadow-paper-lift)]"
            custom={1}
            variants={panelIn}
            initial="hidden"
            animate="show"
          >
            <div className="flex items-center justify-between">
              <div>
                <h2 className="font-display text-lg text-ink">{overallResult.studentName}</h2>
                <p className="text-sm text-slate-dim">{overallResult.totalCredits} total credits</p>
              </div>
              <div className="text-right">
                <p className="text-sm text-slate-dim">CGPA</p>
                <p className="text-2xl font-semibold text-ink">{overallResult.cgpa.toFixed(2)}</p>
                <span className={`rounded px-2 py-0.5 text-xs ${overallResult.overallResult === 'PASS' ? 'bg-green-100 text-green-700' : 'bg-brick/10 text-brick'}`}>
                  {overallResult.overallResult}
                </span>
              </div>
            </div>
          </motion.div>

          {overallResult.semesterResults.map((sem, i) => (
            <motion.div
              key={`${sem.semester}-${sem.academicYear}`}
              className="paper rounded-lg border border-parchment-line bg-white/50 p-5 shadow-[var(--shadow-paper-lift)]"
              custom={i + 2}
              variants={panelIn}
              initial="hidden"
              animate="show"
            >
              <div className="mb-4 flex items-center justify-between">
                <p className="font-medium text-ink">Semester {sem.semester} · {sem.academicYear}</p>
                <p className="text-sm text-slate-dim">SGPA {sem.sgpa.toFixed(2)} · {sem.result}</p>
              </div>
              <ResultTable subjects={sem.subjects} />
            </motion.div>
          ))}
        </div>
      )}
    </Layout>
  )
}

function ResultTable({ subjects }: { subjects: SemesterResult['subjects'] }) {
  return (
    <table className="w-full text-left text-sm">
      <thead>
        <tr className="border-b border-parchment-line text-xs uppercase tracking-wide text-slate-dim">
          <th className="px-3 py-2 font-medium">Subject</th>
          <th className="px-3 py-2 font-medium">Credits</th>
          <th className="px-3 py-2 font-medium">Marks</th>
          <th className="px-3 py-2 font-medium">Grade</th>
        </tr>
      </thead>
      <tbody>
        {subjects.map((sub) => (
          <tr key={sub.subjectId} className="border-b border-parchment-line last:border-0">
            <td className="px-3 py-2 text-ink">
              <p className="font-medium">{sub.subjectName}</p>
              <p className="text-xs text-slate-dim">{sub.subjectCode}</p>
            </td>
            <td className="px-3 py-2 text-slate-dim">{sub.credits}</td>
            <td className="px-3 py-2 text-slate-dim">{sub.totalMarks} / {sub.maxMarks}</td>
            <td className="px-3 py-2">
              <span className={`rounded px-2 py-1 text-xs ${sub.grade === 'F' ? 'bg-brick/10 text-brick' : 'bg-green-100 text-green-700'}`}>
                {sub.grade}
              </span>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}