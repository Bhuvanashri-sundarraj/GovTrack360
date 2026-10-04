import React from 'react'

const STYLES = {
  PRESENT: 'bg-emerald-100 text-emerald-800',
  LATE: 'bg-amber-100 text-amber-800',
  HALF_DAY: 'bg-sky-100 text-sky-800',
  ON_LEAVE: 'bg-purple-100 text-purple-800',
  ABSENT: 'bg-red-100 text-red-700',
}

const LABELS = {
  PRESENT: 'Present',
  LATE: 'Late',
  HALF_DAY: 'Half Day',
  ON_LEAVE: 'On Leave',
  ABSENT: 'Absent',
}

export default function AttendanceStatusBadge({ status }) {
  return <span className={`badge ${STYLES[status] || 'bg-slate-100 text-slate-600'}`}>{LABELS[status] || status}</span>
}
