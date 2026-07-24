// src/pages/StudentsPage.tsx
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import {
  getStudents,
  createStudent,
  updateStudent,
  deleteStudent,
  getCourses,
  type Student,
  type StudentPayload,
  type Course,
} from '@/api'
import { Plus, Pencil, Trash2, Eye, Loader2, Search } from 'lucide-react'

const emptyForm = {
  admissionNo: '',
  rollNumber: '',
  password: '',
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  gender: '',
  dateOfBirth: '',
  admissionDate: '',
  courseId: '' as number | '',
  address: '',
  city: '',
  state: '',
  country: '',
  pincode: '',
  fatherName: '',
  motherName: '',
  parentPhone: '',
  parentEmail: '',
  bloodGroup: '',
  category: '',
  nationality: '',
  aadhaarNumber: '',
  photoUrl: '',
}
type FormState = typeof emptyForm

export function StudentsPage() {
  const [students, setStudents] = useState<Student[]>([])
  const [courses, setCourses] = useState<Course[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [viewOpen, setViewOpen] = useState(false)
  const [editing, setEditing] = useState<Student | null>(null)
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [studentsData, coursesData] = await Promise.all([getStudents(), getCourses()])
      setStudents(studentsData)
      setCourses(coursesData)
    } catch {
      setError('Could not load students.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filteredStudents = useMemo(() => {
    const value = search.toLowerCase()
    return students.filter((s) => {
      return (
        s.firstName.toLowerCase().includes(value) ||
        s.lastName?.toLowerCase().includes(value) ||
        s.admissionNo.toLowerCase().includes(value) ||
        s.rollNumber?.toLowerCase().includes(value) ||
        s.email?.toLowerCase().includes(value) ||
        s.phone?.includes(value)
      )
    })
  }, [students, search])

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(s: Student) {
    setEditing(s)
    setForm({
      admissionNo: s.admissionNo,
      rollNumber: s.rollNumber ?? '',
      password: '',
      firstName: s.firstName,
      lastName: s.lastName ?? '',
      email: s.email ?? '',
      phone: s.phone ?? '',
      gender: s.gender ?? '',
      dateOfBirth: s.dateOfBirth ?? '',
      admissionDate: s.admissionDate ?? '',
      courseId: s.courseId ?? '',
      address: s.address ?? '',
      city: s.city ?? '',
      state: s.state ?? '',
      country: s.country ?? '',
      pincode: s.pincode ?? '',
      fatherName: s.fatherName ?? '',
      motherName: s.motherName ?? '',
      parentPhone: s.parentPhone ?? '',
      parentEmail: s.parentEmail ?? '',
      bloodGroup: s.bloodGroup ?? '',
      category: s.category ?? '',
      nationality: s.nationality ?? '',
      // aadhaarNumber is intentionally never returned by the backend (privacy) - leaving
      // this blank on edit is correct, since a blank value now means "leave unchanged"
      // (matches the existing password convention), not "clear it".
      aadhaarNumber: '',
      photoUrl: s.photoUrl ?? '',
    })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    const payload: StudentPayload = {
      ...form,
      courseId: form.courseId === '' ? undefined : form.courseId,
    }
    try {
      if (editing) await updateStudent(editing.id, payload)
      else await createStudent(payload)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save this student. Check the fields and try again.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (
      !window.confirm(
        `Delete ${students.find((s) => s.id === id)?.firstName} permanently?`,
      )
    )
      return
    try {
      await deleteStudent(id)
      await load()
    } catch {
      alert('Could not delete this student.')
    }
  }

  return (
    <Layout>
      <div className="mb-6">
        <h1 className="font-display text-2xl font-medium text-ink">Students</h1>
        <p className="mt-1 text-sm text-slate-dim">Enrolled students across the institution.</p>
      </div>

      {/* Statistics cards */}
      <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <p className="text-sm text-slate-dim">Total Students</p>
          <p className="mt-2 text-3xl font-semibold text-ink">{students.length}</p>
        </div>
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <p className="text-sm text-slate-dim">Male</p>
          <p className="mt-2 text-3xl font-semibold text-ink">
            {students.filter((s) => s.gender === 'Male').length}
          </p>
        </div>
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <p className="text-sm text-slate-dim">Female</p>
          <p className="mt-2 text-3xl font-semibold text-ink">
            {students.filter((s) => s.gender === 'Female').length}
          </p>
        </div>
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <p className="text-sm text-slate-dim">Others</p>
          <p className="mt-2 text-3xl font-semibold text-ink">
            {students.filter((s) => s.gender === 'Other').length}
          </p>
        </div>
      </div>

      {/* Search + Add */}
      <div className="mb-6 flex items-center justify-between gap-4">
        <div className="relative w-full max-w-md">
          <Search size={18} className="absolute left-3 top-3 text-slate-dim" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by admission, roll number, name, email..."
            className="w-full rounded-md border border-parchment-line bg-white pl-10 pr-4 py-2.5 text-sm focus:border-brass focus:outline-none"
          />
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass-bright"
        >
          <Plus size={16} /> Add Student
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-slate-dim">Loading…</p>
      ) : error ? (
        <p className="text-sm text-brick">{error}</p>
      ) : filteredStudents.length === 0 ? (
        <div className="rounded-lg border border-dashed border-parchment-line bg-white/60 p-10 text-center">
          <h3 className="font-display text-xl text-ink">No Students Found</h3>
          <p className="mt-2 text-slate-dim">Create your first student account to get started.</p>
          <button
            onClick={openCreate}
            className="mt-6 rounded-md bg-brass px-5 py-2 text-sm font-medium text-ink hover:bg-brass-bright"
          >
            Add Student
          </button>
        </div>
      ) : (
        <div className="overflow-hidden rounded-lg border border-parchment-line bg-white/50">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-parchment-line text-xs uppercase tracking-wide text-slate-dim">
                <th className="px-4 py-3 font-medium">Admission</th>
                <th className="px-4 py-3 font-medium">Roll</th>
                <th className="px-4 py-3 font-medium">Student</th>
                <th className="px-4 py-3 font-medium">Course</th>
                <th className="px-4 py-3 font-medium">Phone</th>
                <th className="px-4 py-3 font-medium">Gender</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium"></th>
              </tr>
            </thead>
            <tbody>
              {filteredStudents.map((s) => (
                <tr key={s.id} className="border-b border-parchment-line last:border-0">
                  <td className="px-4 py-3 font-mono text-xs text-ink">{s.admissionNo}</td>
                  <td className="px-4 py-3 text-ink">{s.rollNumber}</td>
                  <td className="px-4 py-3 text-ink">
                    {s.firstName} {s.lastName}
                  </td>
                  <td className="px-4 py-3 text-slate-dim">{s.courseName || '-'}</td>
                  <td className="px-4 py-3 text-slate-dim">{s.phone || '-'}</td>
                  <td className="px-4 py-3 text-slate-dim">{s.gender || '-'}</td>
                  <td className="px-4 py-3">
                    <span className="rounded bg-green-100 px-2 py-1 text-xs text-green-700">
                      Active
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-3">
                      <button
                        onClick={() => {
                          setSelectedStudent(s)
                          setViewOpen(true)
                        }}
                        className="text-slate hover:text-ink"
                      >
                        <Eye size={15} />
                      </button>
                      <button onClick={() => openEdit(s)} className="text-slate hover:text-ink">
                        <Pencil size={15} />
                      </button>
                      <button
                        onClick={() => handleDelete(s.id)}
                        className="text-slate hover:text-brick"
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create / Edit modal */}
      {modalOpen && (
        <Modal title={editing ? 'Edit Student' : 'Add Student'} onClose={() => setModalOpen(false)}>
          <div className="max-h-[75vh] overflow-y-auto pr-2">
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Login Information */}
              <h3 className="border-b pb-2 font-display text-lg">Login Information</h3>
              <div className="grid grid-cols-3 gap-4">
                <Field label="Admission No">
                  <input
                    required
                    value={form.admissionNo}
                    onChange={(e) => setForm({ ...form, admissionNo: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Roll Number">
                  <input
                    required
                    value={form.rollNumber}
                    onChange={(e) => setForm({ ...form, rollNumber: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                {!editing && (
                  <Field label="Initial Password">
                    <input
                      required
                      type="password"
                      value={form.password}
                      onChange={(e) => setForm({ ...form, password: e.target.value })}
                      className={inputClass}
                    />
                  </Field>
                )}
              </div>

              {/* Academic Information */}
              <h3 className="border-b pt-5 pb-2 font-display text-lg">Academic Information</h3>
              <div className="grid grid-cols-3 gap-4">
                <Field label="Course">
                  <select
                    value={form.courseId}
                    onChange={(e) =>
                      setForm({ ...form, courseId: e.target.value === '' ? '' : Number(e.target.value) })
                    }
                    className={inputClass}
                  >
                    <option value="">Not assigned</option>
                    {courses.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.courseName} ({c.courseCode})
                      </option>
                    ))}
                  </select>
                </Field>
              </div>

              {/* Personal Information */}
              <h3 className="border-b pt-5 pb-2 font-display text-lg">Personal Information</h3>
              <div className="grid grid-cols-2 gap-4">
                <Field label="First Name">
                  <input
                    required
                    value={form.firstName}
                    onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Last Name">
                  <input
                    value={form.lastName}
                    onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Email">
                  <input
                    type="email"
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Phone">
                  <input
                    value={form.phone}
                    onChange={(e) => setForm({ ...form, phone: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Gender">
                  <select
                    value={form.gender}
                    onChange={(e) => setForm({ ...form, gender: e.target.value })}
                    className={inputClass}
                  >
                    <option value="">Select Gender</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </Field>
                <Field label="Date of Birth">
                  <input
                    type="date"
                    value={form.dateOfBirth}
                    onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Admission Date">
                  <input
                    type="date"
                    value={form.admissionDate}
                    onChange={(e) => setForm({ ...form, admissionDate: e.target.value })}
                    className={inputClass}
                  />
                </Field>
              </div>

              {/* Address Information */}
              <h3 className="border-b pt-5 pb-2 font-display text-lg">Address Information</h3>
              <Field label="Address">
                <textarea
                  rows={3}
                  value={form.address}
                  onChange={(e) => setForm({ ...form, address: e.target.value })}
                  className={inputClass}
                />
              </Field>
              <div className="grid grid-cols-2 gap-4">
                <Field label="City">
                  <input
                    value={form.city}
                    onChange={(e) => setForm({ ...form, city: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="State">
                  <input
                    value={form.state}
                    onChange={(e) => setForm({ ...form, state: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Country">
                  <input
                    value={form.country}
                    onChange={(e) => setForm({ ...form, country: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Pincode">
                  <input
                    value={form.pincode}
                    onChange={(e) => setForm({ ...form, pincode: e.target.value })}
                    className={inputClass}
                  />
                </Field>
              </div>

              {/* Parent Information */}
              <h3 className="border-b pt-5 pb-2 font-display text-lg">Parent Information</h3>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Father Name">
                  <input
                    value={form.fatherName}
                    onChange={(e) => setForm({ ...form, fatherName: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Mother Name">
                  <input
                    value={form.motherName}
                    onChange={(e) => setForm({ ...form, motherName: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Parent Phone">
                  <input
                    value={form.parentPhone}
                    onChange={(e) => setForm({ ...form, parentPhone: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Parent Email">
                  <input
                    type="email"
                    value={form.parentEmail}
                    onChange={(e) => setForm({ ...form, parentEmail: e.target.value })}
                    className={inputClass}
                  />
                </Field>
              </div>

              {/* Other Details */}
              <h3 className="border-b pt-5 pb-2 font-display text-lg">Other Details</h3>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Blood Group">
                  <select
                    value={form.bloodGroup}
                    onChange={(e) => setForm({ ...form, bloodGroup: e.target.value })}
                    className={inputClass}
                  >
                    <option value="">Select Blood Group</option>
                    <option value="A+">A+</option>
                    <option value="A-">A-</option>
                    <option value="B+">B+</option>
                    <option value="B-">B-</option>
                    <option value="AB+">AB+</option>
                    <option value="AB-">AB-</option>
                    <option value="O+">O+</option>
                    <option value="O-">O-</option>
                  </select>
                </Field>
                <Field label="Category">
                  <select
                    value={form.category}
                    onChange={(e) => setForm({ ...form, category: e.target.value })}
                    className={inputClass}
                  >
                    <option value="">Select Category</option>
                    <option value="General">General</option>
                    <option value="OBC">OBC</option>
                    <option value="SC">SC</option>
                    <option value="ST">ST</option>
                    <option value="EWS">EWS</option>
                  </select>
                </Field>
                <Field label="Nationality">
                  <input
                    value={form.nationality}
                    onChange={(e) => setForm({ ...form, nationality: e.target.value })}
                    className={inputClass}
                  />
                </Field>
                <Field label="Aadhaar Number">
                  <input
                    value={form.aadhaarNumber}
                    onChange={(e) => setForm({ ...form, aadhaarNumber: e.target.value })}
                    placeholder={editing ? 'Leave blank to keep unchanged' : undefined}
                    className={inputClass}
                  />
                </Field>
              </div>
              <Field label="Photo URL">
                <input
                  value={form.photoUrl}
                  onChange={(e) => setForm({ ...form, photoUrl: e.target.value })}
                  className={inputClass}
                />
              </Field>

              {formError && <p className="text-sm text-brick">{formError}</p>}

              <div className="flex justify-end gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="rounded-md border border-parchment-line px-5 py-2 text-sm text-slate-dim hover:text-ink"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="flex items-center gap-2 rounded-md bg-brass px-5 py-2 text-sm font-medium text-ink hover:bg-brass-bright disabled:opacity-60"
                >
                  {saving && <Loader2 size={16} className="animate-spin" />}
                  {editing ? 'Update Student' : 'Create Student'}
                </button>
              </div>
            </form>
          </div>
        </Modal>
      )}

      {/* View details modal */}
      {viewOpen && selectedStudent && (
        <Modal title="Student Details" onClose={() => setViewOpen(false)}>
          <div className="space-y-6">
            <section>
              <h3 className="mb-2 border-b pb-2 font-semibold">Login Information</h3>
              <div className="grid grid-cols-2 gap-3">
                <p>
                  <strong>Admission No:</strong> {selectedStudent.admissionNo}
                </p>
                <p>
                  <strong>Roll Number:</strong> {selectedStudent.rollNumber}
                </p>
              </div>
            </section>

            <section>
              <h3 className="mb-2 border-b pb-2 font-semibold">Academic Information</h3>
              <div className="grid grid-cols-2 gap-3">
                <p>
                  <strong>Course:</strong> {selectedStudent.courseName || 'Not assigned'}
                </p>
              </div>
            </section>

            <section>
              <h3 className="mb-2 border-b pb-2 font-semibold">Personal Information</h3>
              <div className="grid grid-cols-2 gap-3">
                <p>
                  <strong>Name:</strong> {selectedStudent.firstName} {selectedStudent.lastName}
                </p>
                <p>
                  <strong>Email:</strong> {selectedStudent.email || '-'}
                </p>
                <p>
                  <strong>Phone:</strong> {selectedStudent.phone || '-'}
                </p>
                <p>
                  <strong>Gender:</strong> {selectedStudent.gender || '-'}
                </p>
                <p>
                  <strong>DOB:</strong> {selectedStudent.dateOfBirth || '-'}
                </p>
                <p>
                  <strong>Admission Date:</strong> {selectedStudent.admissionDate || '-'}
                </p>
              </div>
            </section>

            <section>
              <h3 className="mb-2 border-b pb-2 font-semibold">Address</h3>
              <div className="grid grid-cols-2 gap-3">
                <p>
                  <strong>Address:</strong> {selectedStudent.address || '-'}
                </p>
                <p>
                  <strong>City:</strong> {selectedStudent.city || '-'}
                </p>
                <p>
                  <strong>State:</strong> {selectedStudent.state || '-'}
                </p>
                <p>
                  <strong>Country:</strong> {selectedStudent.country || '-'}
                </p>
                <p>
                  <strong>Pincode:</strong> {selectedStudent.pincode || '-'}
                </p>
              </div>
            </section>

            <section>
              <h3 className="mb-2 border-b pb-2 font-semibold">Parent Information</h3>
              <div className="grid grid-cols-2 gap-3">
                <p>
                  <strong>Father:</strong> {selectedStudent.fatherName || '-'}
                </p>
                <p>
                  <strong>Mother:</strong> {selectedStudent.motherName || '-'}
                </p>
                <p>
                  <strong>Parent Phone:</strong> {selectedStudent.parentPhone || '-'}
                </p>
                <p>
                  <strong>Parent Email:</strong> {selectedStudent.parentEmail || '-'}
                </p>
              </div>
            </section>

            <section>
              <h3 className="mb-2 border-b pb-2 font-semibold">Other Details</h3>
              <div className="grid grid-cols-2 gap-3">
                <p>
                  <strong>Blood Group:</strong> {selectedStudent.bloodGroup || '-'}
                </p>
                <p>
                  <strong>Category:</strong> {selectedStudent.category || '-'}
                </p>
                <p>
                  <strong>Nationality:</strong> {selectedStudent.nationality || '-'}
                </p>
              </div>
            </section>
          </div>
        </Modal>
      )}
    </Layout>
  )
}