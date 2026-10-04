import React from 'react'

// tabs: [{ key, label }]
export default function Tabs({ tabs, active, onChange }) {
  return (
    <div className="flex gap-2 mb-4 flex-wrap" role="tablist">
      {tabs.map((t) => (
        <button
          key={t.key} role="tab" aria-selected={active === t.key} onClick={() => onChange(t.key)}
          className={`px-3 py-1.5 rounded-md text-sm font-medium ${active === t.key ? 'bg-navy-700 text-white' : 'bg-white border border-slate-300 text-slate-600 hover:bg-slate-50'}`}
        >
          {t.label}
        </button>
      ))}
    </div>
  )
}
