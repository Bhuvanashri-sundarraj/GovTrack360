import React from 'react'

export default function StatCard({ label, value, icon: Icon, accent = 'navy' }) {
  const accents = {
    navy: 'bg-navy-50 text-navy-700',
    emerald: 'bg-emerald-50 text-emerald-700',
    amber: 'bg-amber-50 text-amber-700',
    slate: 'bg-slate-100 text-slate-700',
  }
  return (
    <div className="card p-5 flex items-center justify-between">
      <div>
        <p className="text-sm text-slate-500">{label}</p>
        <p className="text-2xl font-semibold text-slate-800 mt-1">{value}</p>
      </div>
      <div className={`w-11 h-11 rounded-lg flex items-center justify-center ${accents[accent]}`}>
        <Icon className="w-5 h-5" />
      </div>
    </div>
  )
}
