import React from 'react'

const STYLES = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-emerald-100 text-emerald-800',
  REJECTED: 'bg-red-100 text-red-700',
  CANCELLED: 'bg-slate-200 text-slate-500',
}

export default function LeaveStatusBadge({ status }) {
  return <span className={`badge ${STYLES[status] || 'bg-slate-100 text-slate-600'}`}>{status}</span>
}
