import React from 'react'

const PRIORITY_STYLES = {
  LOW: 'bg-slate-100 text-slate-600',
  MEDIUM: 'bg-sky-100 text-sky-700',
  HIGH: 'bg-amber-100 text-amber-800',
  URGENT: 'bg-red-100 text-red-700',
}

export default function TaskPriorityBadge({ priority }) {
  const style = PRIORITY_STYLES[priority] || 'bg-slate-100 text-slate-600'
  return <span className={`badge ${style}`}>{priority}</span>
}
