// src/pages/UsersPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { PanelError, Badge } from '@/components/PageBits'
import { getUsers, createUser, getRoles, type StaffUser, type UserCreatePayload, type Role } from '@/api'
import { Plus, Loader2, UserCircle2 } from 'lucide-react'

const emptyForm: UserCreatePayload = { firstName: '', lastName: '', email: '', password: '', roleId: 0 }

export function UsersPage() {
  const [users, setUsers] = useState<StaffUser[]>([])
  const [roles, setRoles] = useState<Role[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<UserCreatePayload>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [userList, roleList] = await Promise.all([getUsers(), getRoles()])
      setUsers(userList)
      setRoles(roleList)
    } catch {
      setError('Could not load staff accounts.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function openCreate() {
    setForm({ ...emptyForm, roleId: roles[0]?.id ?? 0 })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      await createUser(form)
      setModalOpen(false)
      await load()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Could not create this account. Check the fields and try again.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Layout>
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-heading text-2xl font-medium text-text">Staff accounts</h1>
          <p className="mt-1 text-sm text-muted">
            Admins, HODs, supervisors, and any other role you've built.
          </p>
        </div>
        <button
          onClick={openCreate}
          disabled={roles.length === 0}
          className="flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-secondary disabled:cursor-not-allowed disabled:opacity-60"
        >
          <Plus size={16} /> Add user
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-muted">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : users.length === 0 ? (
        <p className="text-sm text-muted">No staff accounts yet.</p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-border bg-white/50">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-xs uppercase tracking-wide text-muted">
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Email</th>
                <th className="px-4 py-3 font-medium">Role</th>
                <th className="px-4 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 text-text">
                    <div className="flex items-center gap-2">
                      <UserCircle2 size={16} className="text-muted" />
                      {u.firstName} {u.lastName}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-muted">{u.email}</td>
                  <td className="px-4 py-3 font-numbers text-xs text-text">{u.roleName}</td>
                  <td className="px-4 py-3">
                    {u.mustChangePassword ? (
                      <Badge variant="warning">Password not yet set</Badge>
                    ) : u.isActive ? (
                      <Badge variant="success">Active</Badge>
                    ) : (
                      <Badge variant="neutral">Disabled</Badge>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modalOpen && (
        <Modal title="Add user" onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="First name">
                <input
                  required
                  value={form.firstName}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                  className={inputClass}
                />
              </Field>
              <Field label="Last name">
                <input
                  required
                  value={form.lastName}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                  className={inputClass}
                />
              </Field>
            </div>
            <Field label="Email">
              <input
                type="email"
                required
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                className={inputClass}
              />
            </Field>
            <Field label="Initial password">
              <input
                type="text"
                required
                minLength={8}
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="At least 8 characters"
                className={inputClass}
              />
            </Field>
            <Field label="Role">
              <select
                required
                value={form.roleId || ''}
                onChange={(e) => setForm({ ...form, roleId: Number(e.target.value) })}
                className={inputClass}
              >
                <option value="" disabled>
                  Select a role…
                </option>
                {roles.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </Field>
            <p className="text-xs text-muted">
              The user will be prompted to change this password the first time they sign in.
            </p>

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
                Create user
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}
