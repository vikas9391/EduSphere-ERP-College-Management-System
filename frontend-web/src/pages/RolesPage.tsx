// src/pages/RolesPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { PanelError } from '@/components/PageBits'
import {
  getRoles,
  createRole,
  updateRole,
  deleteRole,
  getAllPermissions,
  groupPermissionsByCategory,
  type Role,
  type RolePayload,
  type PermissionInfo,
} from '@/api'
import { Plus, Pencil, Trash2, Loader2, ShieldCheck, Lock } from 'lucide-react'

const emptyForm: RolePayload = { name: '', description: '', permissions: [] }

export function RolesPage() {
  const [roles, setRoles] = useState<Role[]>([])
  const [permissions, setPermissions] = useState<PermissionInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Role | null>(null)
  const [form, setForm] = useState<RolePayload>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [roleList, permissionList] = await Promise.all([getRoles(), getAllPermissions()])
      setRoles(roleList)
      setPermissions(permissionList)
    } catch {
      setError('Could not load roles.')
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

  function openEdit(role: Role) {
    setEditing(role)
    setForm({ name: role.name, description: role.description ?? '', permissions: [...role.permissions] })
    setFormError(null)
    setModalOpen(true)
  }

  function togglePermission(name: string) {
    setForm((f) => ({
      ...f,
      permissions: f.permissions.includes(name)
        ? f.permissions.filter((p) => p !== name)
        : [...f.permissions, name],
    }))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      if (editing) await updateRole(editing.id, form)
      else await createRole(form)
      setModalOpen(false)
      await load()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Could not save this role. Check the fields and try again.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(role: Role) {
    if (!confirm(`Delete the "${role.name}" role? This cannot be undone.`)) return
    try {
      await deleteRole(role.id)
      await load()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Could not delete this role.')
    }
  }

  const grouped = groupPermissionsByCategory(permissions)

  return (
    <Layout>
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-heading text-2xl font-medium text-text">Roles</h1>
          <p className="mt-1 text-sm text-muted">
            Build custom staff roles from a fixed set of permissions.
          </p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-secondary"
        >
          <Plus size={16} /> Add role
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-muted">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : roles.length === 0 ? (
        <p className="text-sm text-muted">No roles yet.</p>
      ) : (
        <ul className="space-y-3">
          {roles.map((role) => (
            <li key={role.id} className="rounded-lg border border-border bg-white/60 p-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <ShieldCheck size={15} className="text-primary" />
                    <p className="font-heading text-sm font-medium text-text">{role.name}</p>
                    {role.isSystemRole && (
                      <span className="flex items-center gap-1 rounded-full border border-border px-2 py-0.5 font-numbers text-[10px] uppercase tracking-wide text-muted">
                        <Lock size={10} /> Built-in
                      </span>
                    )}
                  </div>
                  {role.description && <p className="mt-1 text-xs text-muted">{role.description}</p>}
                  <p className="mt-2 text-xs text-muted">
                    {role.permissions.length === 0
                      ? 'No permissions granted'
                      : `${role.permissions.length} permission${role.permissions.length === 1 ? '' : 's'} granted`}
                  </p>
                </div>
                {!role.isSystemRole && (
                  <div className="flex shrink-0 gap-3">
                    <button onClick={() => openEdit(role)} className="text-muted hover:text-text">
                      <Pencil size={15} />
                    </button>
                    <button onClick={() => handleDelete(role)} className="text-muted hover:text-danger">
                      <Trash2 size={15} />
                    </button>
                  </div>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {modalOpen && (
        <Modal title={editing ? 'Edit role' : 'Add role'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Role name">
              <input
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="e.g. Supervisor"
                className={inputClass}
              />
            </Field>
            <Field label="Description">
              <input
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="e.g. Read-only access to teacher progress"
                className={inputClass}
              />
            </Field>

            <div>
              <span className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-muted">
                Permissions
              </span>
              <div className="max-h-72 space-y-4 overflow-y-auto rounded-md border border-border bg-hover/40 p-3">
                {[...grouped.entries()].map(([category, perms]) => (
                  <div key={category}>
                    <p className="mb-1.5 font-numbers text-[10px] uppercase tracking-wide text-muted">
                      {category}
                    </p>
                    <div className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
                      {perms.map((p) => (
                        <label key={p.name} className="flex items-center gap-2 text-sm text-text">
                          <input
                            type="checkbox"
                            checked={form.permissions.includes(p.name)}
                            onChange={() => togglePermission(p.name)}
                            className="rounded border-border text-primary focus:ring-primary"
                          />
                          {p.name}
                        </label>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {formError && <p className="text-sm text-danger">{formError}</p>}

            <div className="flex flex-col-reverse justify-end gap-3 pt-2 sm:flex-row">
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="rounded-md border border-border px-4 py-2 text-sm text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-secondary disabled:opacity-60"
              >
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
