import React, { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { ShieldCheck, Loader2, Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/ToastContext'

const ROLE_HOME = {
  ADMIN: '/dashboard',
  OFFICER: '/dashboard',
  EMPLOYEE: '/dashboard',
}

export default function Login() {
  const { login, isAuthenticated, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated && user) {
    const redirectTo = location.state?.from?.pathname || ROLE_HOME[user.role] || '/dashboard'
    navigate(redirectTo, { replace: true })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!email || !password) {
      setError('Please enter both email and password.')
      return
    }

    setSubmitting(true)
    try {
      const loggedInUser = await login(email, password)
      toast.success('Logged in successfully.')
      navigate(ROLE_HOME[loggedInUser.role] || '/dashboard', { replace: true })
    } catch (err) {
      const message = err.response?.data?.message || 'Invalid email or password.'
      setError(message)
    } finally {
      setSubmitting(false)
    }
  }

  const fillDemo = (demoEmail, demoPassword) => {
    setEmail(demoEmail)
    setPassword(demoPassword)
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-navy-900 px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-14 h-14 bg-white/10 rounded-xl flex items-center justify-center mx-auto mb-4">
            <ShieldCheck className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white">GovTrack 360</h1>
          <p className="text-navy-300 text-sm mt-1">Government Productivity &amp; Performance Management System</p>
        </div>

        <div className="card p-8">
          <h2 className="text-lg font-semibold text-slate-800 mb-1">Sign in</h2>
          <p className="text-sm text-slate-500 mb-6">Access your role-based dashboard.</p>

          {error && (
            <div className="mb-4 rounded-md bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate>
            <div className="mb-4">
              <label className="label">Email address</label>
              <input
                type="email"
                className="input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@govtrack360.com"
                autoComplete="username"
              />
            </div>
            <div className="mb-6">
              <label className="label">Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  className="input pr-10"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((s) => !s)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button type="submit" className="btn-primary w-full" disabled={submitting}>
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
              {submitting ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-slate-200">
            <p className="text-xs font-medium text-slate-500 mb-2">Demo accounts</p>
            <div className="grid grid-cols-3 gap-2">
              <button className="btn-secondary text-xs py-1.5" onClick={() => fillDemo('admin@govtrack360.com', 'Admin@123')}>Admin</button>
              <button className="btn-secondary text-xs py-1.5" onClick={() => fillDemo('officer@govtrack360.com', 'Officer@123')}>Officer</button>
              <button className="btn-secondary text-xs py-1.5" onClick={() => fillDemo('employee@govtrack360.com', 'Employee@123')}>Employee</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
