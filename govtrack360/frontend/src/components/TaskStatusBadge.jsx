import React from 'react'

const STATUS_STYLES = {
  PENDING: 'bg-slate-200 text-slate-700',
  ASSIGNED: 'bg-sky-100 text-sky-800',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  SUBMITTED: 'bg-indigo-100 text-indigo-800',
  UNDER_REVIEW: 'bg-purple-100 text-purple-800',
  APPROVED: 'bg-emerald-100 text-emerald-800',
  REJECTED: 'bg-red-100 text-red-800',
  OVERDUE: 'bg-red-100 text-red-800',
  CANCELLED: 'bg-slate-200 text-slate-500',
}

const STATUS_LABELS = {
  PENDING: 'Pending',
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In Progress',
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  OVERDUE: 'Overdue',
  CANCELLED: 'Cancelled',
}

export default function TaskStatusBadge({ status }) {
  const style = STATUS_STYLES[status] || 'bg-slate-100 text-slate-600'
  const label = STATUS_LABELS[status] || status
  return <span className={`badge ${style}`}>{label}</span>
}
