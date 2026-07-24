import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { superAdminLogin } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { SealMark } from '@/components/SealMark'
import { Eye, EyeOff, Loader2, ShieldCheck } from 'lucide-react'

/**
 * Decodes the payload of a JWT without verifying its signature. Verification happens
 * server-side; this is purely so the client can read `id` out of the claims, since
 * LoginResponse doesn't carry it. Same approach as LoginPage.
 */
function decodeJwt<T>(token: string): T | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
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

// Separate from LoginPage on purpose: a super admin isn't scoped to any college, so
// there's no institution-code field here, and this hits POST /api/auth/super-admin/login
// instead of the tenant-aware POST /api/auth/login.
export function SuperAdminLoginPage() {
  const navigate = useNavigate()
  const setToken = useAuthStore((s) => s.setToken)
  const setUser = useAuthStore((s) => s.setUser)

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
      const response = await superAdminLogin({ email: email.trim(), password })

      setToken(response.accessToken)

      const claims = decodeJwt<{ id?: number }>(response.accessToken)

      setUser({
        id: claims?.id ?? 0,
        email: response.email,
        role: response.role,
        tenantSchema: response.tenantSchema,
      })

      navigate('/colleges')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen w-full bg-parchment font-body">
      <div className="relative hidden w-[45%] flex-col justify-between overflow-hidden bg-ink px-14 py-12 text-parchment md:flex">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.07]"
          style={{
            backgroundImage:
              'repeating-linear-gradient(0deg, var(--color-brass) 0px, var(--color-brass) 1px, transparent 1px, transparent 64px), repeating-linear-gradient(90deg, var(--color-brass) 0px, var(--color-brass) 1px, transparent 1px, transparent 64px)',
          }}
        />

        <div className="relative">
          <span className="flex items-center gap-1.5 font-mono text-xs tracking-[0.25em] text-slate uppercase">
            <ShieldCheck size={13} className="text-brass" />
            Platform Access
          </span>
          <h1 className="mt-4 font-display text-4xl font-medium leading-tight tracking-tight">
            College ERP
          </h1>
          <p className="mt-3 max-w-xs text-sm leading-relaxed text-slate">
            Super admin console — register and oversee every institution on the platform.
          </p>
        </div>

        <div className="relative flex flex-1 items-center justify-center">
          <SealMark />
        </div>

        <div className="relative border-t border-ink-line pt-6">
          <p className="font-mono text-xs leading-relaxed text-slate">
            This account is not scoped to any single college.
            <br />
            Looking for your institution login instead?{' '}
            <a href="/login" className="text-brass hover:text-brass-bright">
              Go there
            </a>
            .
          </p>
        </div>
      </div>

      <div className="flex flex-1 items-center justify-center px-6 py-16">
        <div className="w-full max-w-sm">
          <div className="mb-10 md:hidden">
            <span className="font-mono text-xs tracking-[0.25em] text-slate uppercase">
              Platform Access
            </span>
            <h1 className="mt-2 font-display text-3xl font-medium text-ink">College ERP</h1>
          </div>

          <div className="mb-8">
            <h2 className="font-display text-2xl font-medium text-ink">Super admin sign in</h2>
            <p className="mt-1.5 text-sm text-slate-dim">
              Enter your platform operator credentials.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            <div>
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
                autoFocus
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@platform.com"
                className="w-full rounded-md border border-parchment-line bg-white/60 px-3.5 py-2.5 text-sm text-ink placeholder:text-slate/50 focus:border-brass focus:outline-none focus:ring-1 focus:ring-brass"
              />
            </div>

            <div>
              <label
                htmlFor="password"
                className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-dim"
              >
                Password
              </label>
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
            </div>

            {error && (
              <div
                role="alert"
                className="rounded-md border border-brick/30 bg-brick/10 px-3.5 py-2.5 text-sm text-brick"
              >
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="flex w-full items-center justify-center gap-2 rounded-md bg-brass px-4 py-2.5 text-sm font-medium text-ink transition-colors hover:bg-brass-bright disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting && <Loader2 size={16} className="animate-spin" />}
              {isSubmitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
