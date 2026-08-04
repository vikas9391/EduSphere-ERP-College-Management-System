import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowLeft, Loader2, Mail, MailCheck } from 'lucide-react'
import { forgotPassword } from '@/api'

/**
 * Reachable from LoginPage's "Forgot password?" link. Same institution-code +
 * email shape as LoginPage — the backend needs collegeCode to know which tenant
 * schema (or the public schema, for the reserved super-admin code) to look the
 * email up in, exactly like the login form does.
 * <p>
 * Always ends in the same "check your email" state regardless of whether the email
 * actually matched an account — the backend intentionally returns the same generic
 * message either way (see ForgotPasswordRequest/AuthController#forgotPassword) so
 * this page never reveals whether a given email is registered.
 */
export function ForgotPasswordPage() {
  const [collegeCode, setCollegeCode] = useState('')
  const [email, setEmail] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await forgotPassword({ collegeCode: collegeCode.trim(), email: email.trim() })
      setSubmitted(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong. Please try again.')
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
        {submitted ? (
          <div className="text-center">
            <span className="mx-auto mb-4 flex h-10 w-10 items-center justify-center rounded-full bg-primary/15 text-primary">
              <MailCheck size={18} />
            </span>
            <h1 className="font-heading text-xl font-medium text-text">Check your email</h1>
            <p className="mt-2 text-sm leading-relaxed text-muted">
              If an account exists for <span className="text-text">{email.trim()}</span>, we've sent a
              link to reset the password. The link expires soon, so use it shortly.
            </p>
            <Link
              to="/login"
              className="mt-6 inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:text-secondary"
            >
              <ArrowLeft size={14} />
              Back to sign in
            </Link>
          </div>
        ) : (
          <>
            <div className="mb-6 flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/15 text-primary">
                <Mail size={18} />
              </span>
              <div>
                <h1 className="font-heading text-xl font-medium text-text">Forgot your password?</h1>
                <p className="mt-0.5 text-xs text-muted">
                  We'll email a link to reset it.
                </p>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              <div>
                <label
                  htmlFor="collegeCode"
                  className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-muted"
                >
                  Institution code
                </label>
                <input
                  id="collegeCode"
                  name="collegeCode"
                  type="text"
                  autoComplete="organization"
                  required
                  value={collegeCode}
                  onChange={(e) => setCollegeCode(e.target.value)}
                  placeholder="e.g. STXAVIERS"
                  className="w-full rounded-md border border-border bg-white/60 px-3.5 py-2.5 font-numbers text-sm uppercase tracking-widest text-text placeholder:text-muted/50 placeholder:tracking-normal placeholder:normal-case focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              <div>
                <label
                  htmlFor="email"
                  className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-muted"
                >
                  Email
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@institution.edu"
                  className="w-full rounded-md border border-border bg-white/60 px-3.5 py-2.5 text-sm text-text placeholder:text-muted/50 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
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
                {isSubmitting ? 'Sending…' : 'Send reset link'}
              </button>
            </form>

            <Link
              to="/login"
              className="mt-6 flex items-center justify-center gap-1.5 text-xs text-muted hover:text-text"
            >
              <ArrowLeft size={13} />
              Back to sign in
            </Link>
          </>
        )}
      </motion.div>
    </div>
  )
}
