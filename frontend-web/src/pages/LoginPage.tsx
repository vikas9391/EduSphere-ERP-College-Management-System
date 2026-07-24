import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { login } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { SealMark } from '@/components/SealMark'
import { Eye, EyeOff, Loader2 } from 'lucide-react'

const formStagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06, delayChildren: 0.1 } },
}

const formField = {
  hidden: { opacity: 0, y: 8 },
  show: { opacity: 1, y: 0, transition: { duration: 0.28, ease: [0.16, 1, 0.3, 1] as const } },
}

/**
 * Decodes the payload of a JWT without verifying its signature.
 * Verification happens server-side; this is purely so the client can
 * read the claims (id, email, role, schema) to drive routing/UI.
 */
function decodeJwt<T>(token: string): T | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    // JWT uses base64url — normalize to base64 before decoding.
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    )
    return JSON.parse(json) as T
  } catch {
    return null
  }
}

// Single login form for every account type. The backend (POST /api/auth/login) tries
// the staff/admin table, then teacher, then student, and returns whichever matched
// along with the real `role` - routing here is driven entirely by that response,
// never by anything chosen in the UI.
function routeForRole(role: string | undefined) {
  switch (role) {
    case 'SUPER_ADMIN':
      return '/colleges'
    case 'ADMIN':
      return '/admin/dashboard'
    case 'TEACHER':
      return '/teacher/dashboard'
    case 'STUDENT':
      return '/student/dashboard'
    default:
      return '/dashboard'
  }
}

export function LoginPage() {
  const navigate = useNavigate()
  const setToken = useAuthStore((s) => s.setToken)
  const setUser = useAuthStore((s) => s.setUser)

  const [collegeCode, setCollegeCode] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      const response = await login({
        collegeCode: collegeCode.trim(),
        email: email.trim(),
        password,
      })

      setToken(response.accessToken)

      // LoginResponse doesn't include the numeric user id (only email/role/
      // tenantSchema) - that's only present in the JWT's own claims, so it's
      // decoded here purely to fill in `id` for the user store.
      const claims = decodeJwt<{ id?: number }>(response.accessToken)

      setUser({
        id: claims?.id ?? 0,
        email: response.email,
        role: response.role,
        tenantSchema: response.tenantSchema,
      })

      navigate(routeForRole(response.role))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen w-full bg-parchment font-body">
      {/* Left panel — institutional identity */}
      <div className="relative hidden w-[45%] flex-col justify-between overflow-hidden bg-ink px-14 py-12 text-parchment md:flex">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.07]"
          style={{
            backgroundImage:
              'repeating-linear-gradient(0deg, var(--color-brass) 0px, var(--color-brass) 1px, transparent 1px, transparent 64px), repeating-linear-gradient(90deg, var(--color-brass) 0px, var(--color-brass) 1px, transparent 1px, transparent 64px)',
          }}
        />

        <motion.div
          className="relative"
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
        >
          <span className="font-mono text-xs tracking-[0.25em] text-slate uppercase">
            Registrar Access
          </span>
          <h1 className="mt-4 font-display text-4xl font-medium leading-tight tracking-tight">
            College ERP
          </h1>
          <p className="mt-3 max-w-xs text-sm leading-relaxed text-slate">
            One record of truth for every department, from admissions to the exam hall.
          </p>
        </motion.div>

        <div className="relative flex flex-1 items-center justify-center">
          <SealMark />
        </div>

        <div className="relative border-t border-ink-line pt-6">
          <p className="font-mono text-xs leading-relaxed text-slate">
            Accounts are created by your institution's administrator.
            <br />
            There is no self-registration.
          </p>
        </div>
      </div>

      {/* Right panel — the form */}
      <div className="flex flex-1 items-center justify-center px-6 py-16">
        <motion.div
          className="w-full max-w-sm"
          variants={formStagger}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={formField} className="mb-10 md:hidden">
            <span className="font-mono text-xs tracking-[0.25em] text-slate uppercase">
              Registrar Access
            </span>
            <h1 className="mt-2 font-display text-3xl font-medium text-ink">College ERP</h1>
          </motion.div>

          <motion.div variants={formField} className="mb-8">
            <h2 className="font-display text-2xl font-medium text-ink">Sign in</h2>
            <p className="mt-1.5 text-sm text-slate-dim">
              Enter the credentials issued by your institution.
            </p>
          </motion.div>

          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            <motion.div variants={formField}>
              <label
                htmlFor="collegeCode"
                className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-dim"
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
                className="w-full rounded-md border border-parchment-line bg-white/60 px-3.5 py-2.5 font-mono text-sm uppercase tracking-widest text-ink placeholder:text-slate/50 placeholder:tracking-normal placeholder:normal-case transition-shadow focus:border-brass focus:outline-none focus:ring-1 focus:ring-brass"
              />
            </motion.div>

            <motion.div variants={formField}>
              <label
                htmlFor="email"
                className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-dim"
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
                className="w-full rounded-md border border-parchment-line bg-white/60 px-3.5 py-2.5 text-sm text-ink placeholder:text-slate/50 focus:border-brass focus:outline-none focus:ring-1 focus:ring-brass"
              />
            </motion.div>

            <motion.div variants={formField}>
              <div className="mb-1.5 flex items-center justify-between">
                <label
                  htmlFor="password"
                  className="block text-xs font-medium uppercase tracking-wide text-slate-dim"
                >
                  Password
                </label>
                <a href="#" className="text-xs font-medium text-brass hover:text-brass-bright">
                  Forgot password?
                </a>
              </div>
              <div className="relative">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full rounded-md border border-parchment-line bg-white/60 px-3.5 py-2.5 pr-10 text-sm text-ink placeholder:text-slate/50 focus:border-brass focus:outline-none focus:ring-1 focus:ring-brass"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate hover:text-ink"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </motion.div>

            <AnimatePresence>
              {error && (
                <motion.div
                  role="alert"
                  initial={{ opacity: 0, x: 0 }}
                  animate={{ opacity: 1, x: [0, -6, 6, -4, 4, 0] }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.4 }}
                  className="rounded-md border border-brick/30 bg-brick/10 px-3.5 py-2.5 text-sm text-brick"
                >
                  {error}
                </motion.div>
              )}
            </AnimatePresence>

            <motion.button
              variants={formField}
              type="submit"
              disabled={isSubmitting}
              whileHover={isSubmitting ? undefined : { y: -1, boxShadow: 'var(--shadow-brass-glow)' }}
              whileTap={isSubmitting ? undefined : { scale: 0.98 }}
              className="flex w-full items-center justify-center gap-2 rounded-md bg-brass px-4 py-2.5 text-sm font-medium text-ink transition-colors hover:bg-brass-bright disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting && <Loader2 size={16} className="animate-spin" />}
              {isSubmitting ? 'Signing in…' : 'Sign in'}
            </motion.button>
          </form>

          <motion.p variants={formField} className="mt-8 text-center text-xs text-slate">
            Need an account? Contact your college administrator or faculty office.
          </motion.p>
        </motion.div>
      </div>
    </div>
  )
}