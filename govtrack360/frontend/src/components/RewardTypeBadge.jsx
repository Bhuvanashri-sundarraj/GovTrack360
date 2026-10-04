import React from 'react'

const STYLES = {
  EMPLOYEE_OF_THE_MONTH: 'bg-amber-100 text-amber-800',
  BEST_PERFORMER: 'bg-emerald-100 text-emerald-800',
  EXCELLENCE_AWARD: 'bg-purple-100 text-purple-800',
  INNOVATION: 'bg-sky-100 text-sky-800',
  TEAM_PLAYER: 'bg-teal-100 text-teal-800',
  PUNCTUALITY: 'bg-indigo-100 text-indigo-800',
  APPRECIATION: 'bg-slate-100 text-slate-700',
}

export default function RewardTypeBadge({ type, label }) {
  return <span className={`badge ${STYLES[type] || 'bg-slate-100 text-slate-600'}`}>{label || type}</span>
}
