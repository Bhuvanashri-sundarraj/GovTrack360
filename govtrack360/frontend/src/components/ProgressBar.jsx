import React from 'react'

export default function ProgressBar({ value = 0 }) {
  const clamped = Math.max(0, Math.min(100, value))
  const color = clamped >= 100 ? 'bg-emerald-500' : clamped >= 50 ? 'bg-navy-600' : 'bg-amber-500'
  return (
    <div className="w-full">
      <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
        <div className={`h-full ${color} transition-all duration-300`} style={{ width: `${clamped}%` }} />
      </div>
    </div>
  )
}
