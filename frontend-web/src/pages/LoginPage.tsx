import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { login } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { dashboardForRole } from '@/constants/roles'
import { Building2, Eye, EyeOff, Loader2, Lock, Mail } from 'lucide-react'
import { BrandMark } from '@/components/BrandMark'
import { GoogleIcon, MicrosoftIcon } from '@/components/OAuthIcons'
import campusIllustration from '@/assets/illustrations/campus-illustration.webp'

const REMEMBERED_EMAIL_KEY = 'erp_remembered_email'

const formStagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.05, delayChildren: 0.1 } },
}

const formField = {
  hidden: { opacity: 0, y: 10 },
  show: { opacity: 1, y: 0, transition: { duration: 0.3, ease: [0.16, 1, 0.3, 1] as const } },
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

export function LoginPage() {
  const navigate = useNavigate()
  const setToken = useAuthStore((s) => s.setToken)
  const setRefreshToken = useAuthStore((s) => s.setRefreshToken)
  const setUser = useAuthStore((s) => s.setUser)

  const [collegeCode, setCollegeCode] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Prefill from a previously "remembered" sign-in (email only - never the password).
  useEffect(() => {
    const remembered = localStorage.getItem(REMEMBERED_EMAIL_KEY)
    if (remembered) {
      setEmail(remembered)
      setRememberMe(true)
    }
  }, [])

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
      setRefreshToken(response.refreshToken)

      // LoginResponse doesn't include the numeric user id or permissions (only
      // email/role/tenantSchema/mustChangePassword) - id and permissions only live in
      // the JWT's own claims, so they're decoded here purely to fill in the user store.
      const claims = decodeJwt<{ id?: number; permissions?: string[] }>(response.accessToken)

      setUser({
        id: claims?.id ?? 0,
        email: response.email,
        role: response.role,
        tenantSchema: response.tenantSchema,
        mustChangePassword: response.mustChangePassword,
        permissions: claims?.permissions ?? [],
      })

      if (rememberMe) {
        localStorage.setItem(REMEMBERED_EMAIL_KEY, response.email)
      } else {
        localStorage.removeItem(REMEMBERED_EMAIL_KEY)
      }

      navigate(response.mustChangePassword ? '/change-password' : dashboardForRole(response.role))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="relative flex h-screen w-full overflow-hidden bg-white">
      {/* Left column: illustration. object-cover fills its own column
          completely (no empty gaps); object-position centers the crop.
          The brand mark is overlaid on top of the illustration itself
          (top-left corner) instead of living in the form column. */}
      <div className="relative hidden h-full w-[53%] overflow-hidden lg:block">
        <img
          src={campusIllustration}
          alt="Students on campus outside the college building"
          draggable={false}
          className="h-full w-full select-none"
          style={{
            objectFit: 'cover',
            objectPosition: '12% center',
            transform: 'scale(1)',
          }}
        />

        {/* Brand mark overlay - top left of the illustration */}
        <div className="absolute left-8 top-8 flex items-end gap-3">
          <BrandMark />
          <span className="pt-1.5 text-xl font-semibold text-white drop-shadow-md">
            EduSphere <span style={{ color: '#4ADE80' }}>ERP</span>
          </span>
        </div>
      </div>

      {/* Right column: form fields on a plain white background.
          overflow-hidden (no scrollbar) - content is expected to fit
          within the viewport height. */}
      <div className="flex h-screen w-full flex-col overflow-hidden p-6 sm:p-10 lg:flex lg:h-full lg:w-[45%] lg:items-center lg:justify-center lg:p-12">
        <motion.div
          className="w-full max-w-[420px]"
          variants={formStagger}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={formField} className="mb-8">
            <h1 className="text-[36px] font-bold leading-tight text-[#1F2937] sm:text-[40px]">
              Welcome Back!
            </h1>
            <p className="mt-2 text-sm leading-relaxed text-[#6B7280]">
              Sign in to continue managing your institution.
            </p>
          </motion.div>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <motion.div variants={formField} className="relative">
              <Building2
                size={18}
                className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#6B7280]"
              />
              <input
                id="collegeCode"
                name="collegeCode"
                type="text"
                autoComplete="organization"
                required
                value={collegeCode}
                onChange={(e) => setCollegeCode(e.target.value)}
                placeholder="Institution code"
                className="h-14 w-full rounded-2xl border border-[#E5E7EB] bg-white pl-12 pr-4 text-sm uppercase tracking-widest text-[#1F2937] placeholder:text-[#9CA3AF] placeholder:tracking-normal placeholder:normal-case transition-all duration-300 focus:border-[#22C55E] focus:outline-none focus:ring-4 focus:ring-[#22C55E]/15"
              />
            </motion.div>

            <motion.div variants={formField} className="relative">
              <Mail
                size={18}
                className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#6B7280]"
              />
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Email address"
                className="h-14 w-full rounded-2xl border border-[#E5E7EB] bg-white pl-12 pr-4 text-sm text-[#1F2937] placeholder:text-[#9CA3AF] transition-all duration-300 focus:border-[#22C55E] focus:outline-none focus:ring-4 focus:ring-[#22C55E]/15"
              />
            </motion.div>

            <motion.div variants={formField} className="relative">
              <Lock
                size={18}
                className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#6B7280]"
              />
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Password"
                className="h-14 w-full rounded-2xl border border-[#E5E7EB] bg-white pl-12 pr-12 text-sm text-[#1F2937] placeholder:text-[#9CA3AF] transition-all duration-300 focus:border-[#22C55E] focus:outline-none focus:ring-4 focus:ring-[#22C55E]/15"
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-[#6B7280] transition-colors duration-200 hover:text-[#1F2937]"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </motion.div>

            <motion.div variants={formField} className="flex items-center justify-between pt-1">
              <label className="flex select-none items-center gap-2 text-sm text-[#6B7280]">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="h-4 w-4 rounded border-[#E5E7EB] accent-[#22C55E] focus:ring-[#22C55E]"
                />
                Remember me
              </label>
              <Link
                to="/forgot-password"
                className="text-sm font-medium text-[#15803D] transition-colors duration-200 hover:text-[#22C55E]"
              >
                Forgot Password?
              </Link>
            </motion.div>

            <AnimatePresence>
              {error && (
                <motion.div
                  role="alert"
                  initial={{ opacity: 0, x: 0 }}
                  animate={{ opacity: 1, x: [0, -6, 6, -4, 4, 0] }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.4 }}
                  className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600"
                >
                  {error}
                </motion.div>
              )}
            </AnimatePresence>

            <motion.button
              variants={formField}
              type="submit"
              disabled={isSubmitting}
              whileHover={isSubmitting ? undefined : { y: -2 }}
              whileTap={isSubmitting ? undefined : { scale: 0.98 }}
              className="flex h-14 w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-[#22C55E] to-[#15803D] text-sm font-semibold text-white shadow-md transition-all duration-300 hover:from-[#16A34A] hover:to-[#14532D] hover:shadow-lg disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting && <Loader2 size={16} className="animate-spin" />}
              {isSubmitting ? 'Signing in…' : 'Sign In'}
            </motion.button>
          </form>

          <motion.div variants={formField} className="my-7 flex items-center gap-3">
            <span className="h-px flex-1 bg-[#E5E7EB]" />
            <span className="text-xs font-medium uppercase tracking-wider text-[#6B7280]">
              Or continue with
            </span>
            <span className="h-px flex-1 bg-[#E5E7EB]" />
          </motion.div>

          <motion.div variants={formField} className="grid grid-cols-2 gap-3">
            {/* Not wired to a real identity provider yet - visual placeholders
                matching the requested design; see chat for follow-up. */}
            <button
              type="button"
              disabled
              className="flex h-12 items-center justify-center gap-2 rounded-2xl border border-[#E5E7EB] bg-white text-sm font-medium text-[#1F2937] opacity-60 transition-all duration-300 hover:-translate-y-0.5 hover:border-[#22C55E]/40"
              title="Google sign-in isn't connected yet"
            >
              <GoogleIcon />
              Google
            </button>
            <button
              type="button"
              disabled
              className="flex h-12 items-center justify-center gap-2 rounded-2xl border border-[#E5E7EB] bg-white text-sm font-medium text-[#1F2937] opacity-60 transition-all duration-300 hover:-translate-y-0.5 hover:border-[#22C55E]/40"
              title="Microsoft sign-in isn't connected yet"
            >
              <MicrosoftIcon />
              Microsoft
            </button>
          </motion.div>

          <motion.p variants={formField} className="mt-9 text-center text-xs text-[#6B7280]">
            © {new Date().getFullYear()} EduSphere ERP. All rights reserved.
          </motion.p>
        </motion.div>
      </div>
    </div>
  )
}