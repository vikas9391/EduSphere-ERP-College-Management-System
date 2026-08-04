import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CheckCircle2, Eye, EyeOff, KeyRound, Loader2 } from 'lucide-react'
import { resetPassword } from '@/api'

/**
 * Landed on from the link inside the forgot-password email, which carries `token`
 * and `college` as query params (see PasswordResetService#requestReset /
 * AuthController#requestSuperAdminPasswordReset — `college` is the same collegeCode
 * LoginPage sends, including the reserved super-admin code). Both are threaded
 * straight through to POST /auth/reset-password without the user re-entering them.
 */
export function ResetPasswordPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const collegeCode = searchParams.get('college') ?? ''

  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPasswords, setShowPasswords] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const linkIsValid = token.length > 0 && collegeCode.length > 0

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
      await resetPassword({ collegeCode, token, newPassword })
      setSuccess(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'This reset link is invalid or has expired.')
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
        {!linkIsValid ? (
          <div className="text-center">
            <h1 className="font-heading text-xl font-medium text-text">Invalid reset link</h1>
            <p className="mt-2 text-sm leading-relaxed text-muted">
              This link is missing or malformed. Request a new one from the sign-in page.
            </p>
            <Link
              to="/forgot-password"
              className="mt-6 inline-block rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-white hover:bg-secondary"
            >
              Request a new link
            </Link>
          </div>
        ) : success ? (
          <div className="text-center">
            <span className="mx-auto mb-4 flex h-10 w-10 items-center justify-center rounded-full bg-primary/15 text-primary">
              <CheckCircle2 size={18} />
            </span>
            <h1 className="font-heading text-xl font-medium text-text">Password updated</h1>
            <p className="mt-2 text-sm leading-relaxed text-muted">
              Your password has been reset. You can now sign in with your new password.
            </p>
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="mt-6 inline-block rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-white hover:bg-secondary"
            >
              Go to sign in
            </button>
          </div>
        ) : (
          <>
            <div className="mb-6 flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/15 text-primary">
                <KeyRound size={18} />
              </span>
              <div>
                <h1 className="font-heading text-xl font-medium text-text">Choose a new password</h1>
                <p className="mt-0.5 text-xs text-muted">
                  Enter and confirm your new password below.
                </p>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
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

              <button
                type="submit"
                disabled={isSubmitting}
                className="flex w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-secondary disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSubmitting && <Loader2 size={16} className="animate-spin" />}
                {isSubmitting ? 'Saving…' : 'Reset password'}
              </button>
            </form>
          </>
        )}
      </motion.div>
    </div>
  )
}
