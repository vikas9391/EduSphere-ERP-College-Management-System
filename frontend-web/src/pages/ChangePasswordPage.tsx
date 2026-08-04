import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Eye, EyeOff, Loader2, KeyRound } from 'lucide-react'
import { changeMyPassword } from '@/api'
import { useAuthStore } from '@/store/authStore'

/** Mirrors routeForRole() in LoginPage / dashboardForRole() in ProtectedRoute. */
function dashboardForRole(role: string | undefined) {
  switch (role) {
    case 'SUPER_ADMIN':
      return '/colleges'
    case 'TEACHER':
      return '/teacher/dashboard'
    case 'STUDENT':
      return '/student/dashboard'
    case 'ADMIN':
      return '/admin/dashboard'
    default:
      return '/dashboard'
  }
}

/**
 * Two ways to land here:
 *  1. Forced - LoginPage routes here instead of the dashboard when the backend's
 *     `mustChangePassword` flag is set (an admin issued this account's current
 *     password). No way to skip; the sidebar/nav isn't rendered.
 *  2. Voluntary - reachable from Layout's account menu any time, to change a
 *     password the user already knows works. Shows a "Cancel" link back out.
 */
export function ChangePasswordPage() {
  const navigate = useNavigate()
  const { user, setUser } = useAuthStore()
  const forced = user?.mustChangePassword === true

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPasswords, setShowPasswords] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.')
      return
    }
    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters.')
      return
    }

    setIsSubmitting(true)
    try {
      await changeMyPassword({ currentPassword, newPassword })

      if (user) setUser({ ...user, mustChangePassword: false })

      navigate(dashboardForRole(user?.role))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not update your password. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-bg px-6 font-body">
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
        className="w-full max-w-sm rounded-lg border border-border bg-white/70 p-8 shadow-[var(--shadow-card)]"
      >
        <div className="mb-6 flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/15 text-primary">
            <KeyRound size={18} />
          </span>
          <div>
            <h1 className="font-heading text-xl font-medium text-text">
              {forced ? 'Choose a new password' : 'Change your password'}
            </h1>
            {forced && (
              <p className="mt-0.5 text-xs text-muted">
                An administrator created this account. Set a password only you know before continuing.
              </p>
            )}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <div>
            <label className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-muted">
              Current password
            </label>
            <input
              type={showPasswords ? 'text' : 'password'}
              autoComplete="current-password"
              required
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className="w-full rounded-md border border-border bg-white/60 px-3.5 py-2.5 text-sm text-text focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          <div>
            <label className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-muted">
              New password
            </label>
            <input
              type={showPasswords ? 'text' : 'password'}
              autoComplete="new-password"
              required
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full rounded-md border border-border bg-white/60 px-3.5 py-2.5 text-sm text-text focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          <div>
            <div className="mb-1.5 flex items-center justify-between">
              <label className="block text-xs font-medium uppercase tracking-wide text-muted">
                Confirm new password
              </label>
              <button
                type="button"
                onClick={() => setShowPasswords((v) => !v)}
                className="flex items-center gap-1 text-xs text-muted hover:text-text"
              >
                {showPasswords ? <EyeOff size={13} /> : <Eye size={13} />}
                {showPasswords ? 'Hide' : 'Show'}
              </button>
            </div>
            <input
              type={showPasswords ? 'text' : 'password'}
              autoComplete="new-password"
              required
              minLength={8}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full rounded-md border border-border bg-white/60 px-3.5 py-2.5 text-sm text-text focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          {error && (
            <p role="alert" className="rounded-md border border-danger/30 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
              {error}
            </p>
          )}

          <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
            {!forced && (
              <button
                type="button"
                onClick={() => navigate(dashboardForRole(user?.role))}
                className="rounded-md border border-border px-4 py-2 text-sm text-muted hover:text-text"
              >
                Cancel
              </button>
            )}
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-white hover:bg-secondary disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting && <Loader2 size={16} className="animate-spin" />}
              {isSubmitting ? 'Saving…' : 'Save password'}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  )
}
