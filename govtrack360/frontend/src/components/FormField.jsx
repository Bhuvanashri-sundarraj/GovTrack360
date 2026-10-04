import React from 'react'

export default function FormField({ label, error, children, required = false }) {
  return (
    <div className="mb-4">
      <label className="label">
        {label}{required && <span className="text-red-500"> *</span>}
      </label>
      {children}
      {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
    </div>
  )
}
