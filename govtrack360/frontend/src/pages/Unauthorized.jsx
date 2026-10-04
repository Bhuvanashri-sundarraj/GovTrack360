import React from 'react'
import { Link } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'

export default function Unauthorized() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-4 text-center">
      <div className="w-14 h-14 rounded-full bg-red-50 flex items-center justify-center mb-4">
        <ShieldAlert className="w-7 h-7 text-red-500" />
      </div>
      <h1 className="text-xl font-semibold text-slate-800">Access denied</h1>
      <p className="text-sm text-slate-500 mt-2 max-w-sm">
        Your account role does not have permission to view this page.
      </p>
      <Link to="/dashboard" className="btn-primary mt-6">Back to my dashboard</Link>
    </div>
  )
}
