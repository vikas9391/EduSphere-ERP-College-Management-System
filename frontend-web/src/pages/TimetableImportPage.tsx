import { useEffect, useMemo, useState, type ChangeEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { Field, inputClass } from '@/components/FormField'
import { PanelError } from '@/components/PageBits'
import { getMyTeachingClassSubjects, type ClassSubject } from '@/api/schoolClass'
import { createTimetableEntry, type TimetableDay, type TimetableEntryPayload } from '@/api/timetable'
import { ArrowLeft, Check, FileText, Loader2, Upload, WandSparkles } from 'lucide-react'
import * as pdfjsLib from 'pdfjs-dist'
import { createWorker } from 'tesseract.js'

pdfjsLib.GlobalWorkerOptions.workerSrc = new URL('pdfjs-dist/build/pdf.worker.min.mjs', import.meta.url).toString()

const DAYS: TimetableDay[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const DAY_LABELS: Record<TimetableDay, string> = {
  MONDAY: 'Monday', TUESDAY: 'Tuesday', WEDNESDAY: 'Wednesday', THURSDAY: 'Thursday',
  FRIDAY: 'Friday', SATURDAY: 'Saturday', SUNDAY: 'Sunday',
}

type Draft = TimetableEntryPayload & { id: string; subjectText: string; confidence: 'high' | 'medium' | 'low' }

function normaliseTime(value: string): string | null {
  const match = value.trim().match(/(\d{1,2})(?::|\.)(\d{2})\s*(am|pm)?/i) || value.trim().match(/\b(\d{1,2})\s*(am|pm)\b/i)
  if (!match) return null
  let hour = Number(match[1]); const minute = Number(match[2] || 0); const meridiem = match[3]?.toLowerCase()
  if (meridiem === 'pm' && hour < 12) hour += 12
  if (meridiem === 'am' && hour === 12) hour = 0
  if (hour > 23 || minute > 59) return null
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

function detectDay(text: string): TimetableDay | null {
  const value = text.toLowerCase()
  const aliases: Array<[TimetableDay, string[]]> = [
    ['MONDAY', ['monday', 'mon']], ['TUESDAY', ['tuesday', 'tue', 'tues']], ['WEDNESDAY', ['wednesday', 'wed']],
    ['THURSDAY', ['thursday', 'thu', 'thur', 'thurs']], ['FRIDAY', ['friday', 'fri']], ['SATURDAY', ['saturday', 'sat']], ['SUNDAY', ['sunday', 'sun']],
  ]
  return aliases.find(([, names]) => names.some(name => new RegExp(`\\b${name}\\b`, 'i').test(value)))?.[0] ?? null
}

function parseRows(text: string, subjects: ClassSubject[]): Draft[] {
  const lines = text.split(/\r?\n/).map(line => line.replace(/\s+/g, ' ').trim()).filter(Boolean)
  const drafts: Draft[] = []
  const timePattern = /(\d{1,2}(?::|\.)\d{2}\s*(?:am|pm)?|\d{1,2}\s*(?:am|pm))\s*(?:-|–|—|to)\s*(\d{1,2}(?::|\.)\d{2}\s*(?:am|pm)?|\d{1,2}\s*(?:am|pm))/i
  lines.forEach((line, index) => {
    const day = detectDay(line) || DAYS[index % 6]
    const match = line.match(timePattern)
    if (!match) return
    const start = normaliseTime(match[1]); const end = normaliseTime(match[2])
    if (!start || !end || end <= start) return
    const remainder = line.replace(match[0], '').replace(/\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)\b/ig, '').replace(/[|,;]+/g, ' ').trim()
    const matched = subjects.find(subject => subject.subjectName && remainder.toLowerCase().includes(subject.subjectName.toLowerCase()))
    drafts.push({ id: `${Date.now()}-${index}`, classSubjectId: matched?.id || subjects[0]?.id || 0, dayOfWeek: day, startTime: start, endTime: end, room: '', subjectText: remainder || 'Review subject', confidence: matched ? 'high' : 'low' })
  })
  return drafts
}

async function extractPdf(file: File): Promise<string> {
  const data = new Uint8Array(await file.arrayBuffer())
  const pdf = await pdfjsLib.getDocument({ data }).promise
  const pages: string[] = []
  for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber += 1) {
    const page = await pdf.getPage(pageNumber)
    const content = await page.getTextContent()
    pages.push(content.items.map(item => ('str' in item ? item.str : '')).join(' '))
  }
  return pages.join('\n')
}

async function extractImage(file: File, onProgress: (value: number) => void): Promise<string> {
  const worker = await createWorker('eng', 1, { logger: message => { if (message.status === 'recognizing text') onProgress(Math.round(message.progress * 100)) } })
  try { return (await worker.recognize(await file.arrayBuffer())).data.text } finally { await worker.terminate() }
}

export function TimetableImportPage() {
  const navigate = useNavigate()
  const [subjects, setSubjects] = useState<ClassSubject[]>([])
  const [drafts, setDrafts] = useState<Draft[]>([])
  const [loading, setLoading] = useState(true)
  const [processing, setProcessing] = useState(false)
  const [progress, setProgress] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => { getMyTeachingClassSubjects().then(setSubjects).catch(() => setError('Could not load your class subjects.')).finally(() => setLoading(false)) }, [])

  const selectedCount = useMemo(() => drafts.filter(draft => draft.classSubjectId > 0).length, [drafts])

  async function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]; if (!file) return
    setError(null); setProcessing(true); setProgress(0); setDrafts([])
    try {
      const text = file.type === 'application/pdf' ? await extractPdf(file) : await extractImage(file, setProgress)
      if (!text.trim()) throw new Error('No readable timetable text was found.')
      const parsed = parseRows(text, subjects)
      if (!parsed.length) throw new Error('The timetable was read, but no time slots could be confidently detected. Add the slots manually below.')
      setDrafts(parsed)
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not inspect this timetable.') }
    finally { setProcessing(false) }
  }

  function updateDraft(id: string, patch: Partial<Draft>) { setDrafts(current => current.map(draft => draft.id === id ? { ...draft, ...patch } : draft)) }
  function removeDraft(id: string) { setDrafts(current => current.filter(draft => draft.id !== id)) }

  async function saveAll() {
    const valid = drafts.filter(draft => draft.classSubjectId > 0 && draft.endTime > draft.startTime)
    if (!valid.length) return setError('Add at least one valid timetable slot and select its class subject.')
    setSaving(true); setError(null)
    try {
      for (const draft of valid) await createTimetableEntry({ classSubjectId: draft.classSubjectId, dayOfWeek: draft.dayOfWeek, startTime: draft.startTime, endTime: draft.endTime, room: draft.room.trim() })
      navigate('/teacher/timetable')
    } catch { setError('Some slots could not be saved. Conflict checks may have rejected an overlapping class or teacher period. Review and try again.') }
    finally { setSaving(false) }
  }

  return <Layout>
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div><button onClick={() => navigate('/teacher/timetable')} className="mb-2 inline-flex items-center gap-1 text-xs text-muted hover:text-text"><ArrowLeft size={14} /> Back to timetable</button><h1 className="font-heading text-2xl font-medium text-text">Import Timetable</h1><p className="mt-1 text-sm text-muted">Upload an image or PDF. The system extracts likely slots, then you review and edit everything before saving.</p></div>
      {drafts.length > 0 && <button onClick={saveAll} disabled={saving || selectedCount === 0} className="inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"><Check size={16} /> {saving ? 'Saving...' : `Save ${selectedCount} slots`}</button>}
    </div>

    {loading ? <div className="mt-8 p-10 text-center text-sm text-muted">Loading class subjects...</div> : <div className="mt-8 space-y-5">
      <label className="block cursor-pointer rounded-xl border-2 border-dashed border-border bg-white/60 p-8 text-center hover:border-primary/50">
        <input type="file" accept="image/*,.pdf,application/pdf" onChange={handleFile} className="sr-only" />
        {processing ? <><Loader2 size={34} className="mx-auto animate-spin text-primary" /><p className="mt-3 font-heading text-base font-medium text-text">Inspecting timetable{progress ? ` · ${progress}%` : '...'}</p><p className="mt-1 text-sm text-muted">OCR is running locally in your browser for images.</p></> : <><Upload size={34} className="mx-auto text-primary" /><p className="mt-3 font-heading text-base font-medium text-text">Choose timetable image or PDF</p><p className="mt-1 text-sm text-muted">Nothing is saved automatically. You will review every extracted row first.</p></>}
      </label>

      {error && <PanelError message={error} />}

      {drafts.length > 0 && <div className="rounded-lg border border-border bg-white/60 p-4"><div className="mb-4 flex items-center justify-between"><div><p className="font-heading text-sm font-medium text-text">Review extracted slots</p><p className="text-xs text-muted">Low-confidence rows are highlighted for manual correction. Delete anything that was read incorrectly.</p></div><WandSparkles size={18} className="text-primary" /></div><div className="space-y-3">
        {drafts.map(draft => <div key={draft.id} className="grid gap-3 rounded-lg border border-border bg-bg p-3 lg:grid-cols-[1.2fr_1fr_1fr_1fr_1.4fr_auto] lg:items-end">
          <Field label="Class Subject"><select value={draft.classSubjectId} onChange={e => updateDraft(draft.id, { classSubjectId: Number(e.target.value) })} className={inputClass}><option value={0}>Select class subject</option>{subjects.map(subject => <option key={subject.id} value={subject.id}>{subject.schoolClassName || `Class ${subject.schoolClassId}`} · {subject.subjectName}</option>)}</select></Field>
          <Field label="Day"><select value={draft.dayOfWeek} onChange={e => updateDraft(draft.id, { dayOfWeek: e.target.value as TimetableDay })} className={inputClass}>{DAYS.map(day => <option key={day} value={day}>{DAY_LABELS[day]}</option>)}</select></Field>
          <Field label="Start"><input type="time" value={draft.startTime} onChange={e => updateDraft(draft.id, { startTime: e.target.value })} className={inputClass} /></Field>
          <Field label="End"><input type="time" value={draft.endTime} onChange={e => updateDraft(draft.id, { endTime: e.target.value })} className={inputClass} /></Field>
          <Field label={`Room · read as: ${draft.subjectText.slice(0, 30)}`}><input value={draft.room} onChange={e => updateDraft(draft.id, { room: e.target.value })} className={inputClass} placeholder="Room 204" /></Field>
          <button onClick={() => removeDraft(draft.id)} className="rounded-lg border border-border px-3 py-2 text-sm text-muted hover:text-red-600">Remove</button>
          {draft.confidence !== 'high' && <p className="text-xs text-amber-700 lg:col-span-full"><FileText size={13} className="mr-1 inline" />Review this row carefully; subject matching was not confident.</p>}
        </div>)}
      </div></div>}
    </div>}
  </Layout>
}
