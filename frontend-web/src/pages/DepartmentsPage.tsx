// src/pages/DepartmentsPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import {
  getDepartments,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  type Department,
  type DepartmentPayload,
} from '@/api'
import { Plus, Pencil, Trash2, Loader2, Building2, Search, Layers } from 'lucide-react'

const emptyForm: DepartmentPayload = { code: '', name: '', hod: '', description: '' }

export function DepartmentsPage() {
  const [departments, setDepartments] = useState<Department[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Department | null>(null)
  const [form, setForm] = useState<DepartmentPayload>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const filteredDepartments = departments.filter(
    (d) =>
      d.name.toLowerCase().includes(search.toLowerCase()) ||
      d.code.toLowerCase().includes(search.toLowerCase()),
  )

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setDepartments(await getDepartments())
    } catch {
      setError('Could not load departments.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(dept: Department) {
    setEditing(dept)
    setForm({ code: dept.code, name: dept.name, hod: dept.hod ?? '', description: dept.description ?? '' })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      if (editing) await updateDepartment(editing.id, form)
      else await createDepartment(form)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save this department. Check the fields and try again.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this department? This cannot be undone.')) return
    try {
      await deleteDepartment(id)
      await load()
    } catch {
      alert('Could not delete this department.')
    }
  }

  return (
    <Layout>
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-heading text-2xl font-medium text-text">Departments</h1>
          <p className="mt-1 text-sm text-muted">Academic departments across the institution.</p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-secondary"
        >
          <Plus size={16} /> Add department
        </button>
      </div>

      <StampGrid className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <StatCard icon={Building2} label="Total Departments" value={departments.length} accent={STAT_SHADES[0]} />
        <StatCard icon={Layers} label="Showing" value={filteredDepartments.length} accent={STAT_SHADES[3]} />
      </StampGrid>

      <div className="relative mb-6">
        <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by name or code..."
          className="w-full rounded-lg border border-border bg-white/60 py-2 pl-9 pr-3 text-sm text-text placeholder:text-muted focus:border-primary focus:outline-none sm:max-w-md"
        />
      </div>

      {loading ? (
        <p className="text-sm text-muted">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : filteredDepartments.length === 0 ? (
        <p className="text-sm text-muted">
          {search ? 'No departments match your search.' : 'No departments yet.'}
        </p>
      ) : (
        <>
          {/* Table — sm and up */}
          <div className="hidden overflow-hidden rounded-lg border border-border bg-white/50 sm:block">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border text-xs uppercase tracking-wide text-muted">
                  <th className="px-4 py-3 font-medium">Code</th>
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">HOD</th>
                  <th className="px-4 py-3 font-medium">Description</th>
                  <th className="px-4 py-3 font-medium"></th>
                </tr>
              </thead>
              <tbody>
                {filteredDepartments.map((d) => (
                  <tr key={d.id} className="border-b border-border last:border-0">
                    <td className="px-4 py-3 font-numbers text-xs text-text">{d.code}</td>
                    <td className="px-4 py-3 text-text">{d.name}</td>
                    <td className="px-4 py-3 text-muted">{d.hod || '—'}</td>
                    <td className="px-4 py-3 text-muted">{d.description || '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-3">
                        <button onClick={() => openEdit(d)} className="text-muted hover:text-text">
                          <Pencil size={15} />
                        </button>
                        <button onClick={() => handleDelete(d.id)} className="text-muted hover:text-danger">
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Card list — mobile only */}
          <ul className="space-y-3 sm:hidden">
            {filteredDepartments.map((d) => (
              <li key={d.id} className="rounded-lg border border-border bg-white/60 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-heading text-sm font-medium text-text">{d.name}</p>
                    <p className="font-numbers text-xs text-muted">{d.code}</p>
                  </div>
                  <div className="flex shrink-0 gap-3">
                    <button onClick={() => openEdit(d)} className="text-muted hover:text-text">
                      <Pencil size={15} />
                    </button>
                    <button onClick={() => handleDelete(d.id)} className="text-muted hover:text-danger">
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
                <p className="mt-2 text-xs text-muted">HOD: {d.hod || '—'}</p>
                {d.description && <p className="mt-1 text-xs text-muted">{d.description}</p>}
              </li>
            ))}
          </ul>
        </>
      )}

      {modalOpen && (
        <Modal title={editing ? 'Edit department' : 'Add department'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Code">
              <input required value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} className={inputClass} />
            </Field>
            <Field label="Name">
              <input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className={inputClass} />
            </Field>
            <Field label="HOD">
              <input value={form.hod} onChange={(e) => setForm({ ...form, hod: e.target.value })} className={inputClass} />
            </Field>
            <Field label="Description">
              <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} className={inputClass} />
            </Field>
            {formError && <p className="text-sm text-danger">{formError}</p>}
            <div className="flex flex-col-reverse justify-end gap-3 pt-2 sm:flex-row">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-md border border-border px-4 py-2 text-sm text-muted hover:text-text">
                Cancel
              </button>
              <button type="submit" disabled={saving} className="flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-secondary disabled:opacity-60">
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