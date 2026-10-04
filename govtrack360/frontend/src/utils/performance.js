// Shared helpers for the Phase 3 pages (performance, rewards, reports, analytics).

export const BAND_LABELS = {
  OUTSTANDING: 'Outstanding',
  VERY_GOOD: 'Very Good',
  GOOD: 'Good',
  NEEDS_IMPROVEMENT: 'Needs Improvement',
  UNSATISFACTORY: 'Unsatisfactory',
  NOT_RATED: 'Not Rated',
}

export const BAND_STYLES = {
  OUTSTANDING: 'bg-emerald-100 text-emerald-800',
  VERY_GOOD: 'bg-teal-100 text-teal-800',
  GOOD: 'bg-sky-100 text-sky-800',
  NEEDS_IMPROVEMENT: 'bg-amber-100 text-amber-800',
  UNSATISFACTORY: 'bg-red-100 text-red-700',
  NOT_RATED: 'bg-slate-100 text-slate-500',
}

export const BAND_COLORS = {
  OUTSTANDING: '#059669',
  VERY_GOOD: '#0d9488',
  GOOD: '#0284c7',
  NEEDS_IMPROVEMENT: '#d97706',
  UNSATISFACTORY: '#dc2626',
  NOT_RATED: '#94a3b8',
}

// Must mirror the weights in PerformanceMetricsService on the backend.
export const SCORE_COMPONENTS = [
  { key: 'completionRate', label: 'Task completion', weight: 25, hint: 'Delivered tasks / tasks that fell due' },
  { key: 'onTimeRate', label: 'On-time delivery', weight: 20, hint: 'Tasks submitted by their due date' },
  { key: 'qualityRate', label: 'Quality', weight: 15, hint: 'Approvals vs. rejections on review' },
  { key: 'attendanceRate', label: 'Attendance', weight: 25, hint: 'Working days attended (leave excluded)' },
  { key: 'punctualityRate', label: 'Punctuality', weight: 15, hint: 'Attended days that were not late' },
]

export function scoreColor(score) {
  if (score == null) return BAND_COLORS.NOT_RATED
  if (score >= 90) return BAND_COLORS.OUTSTANDING
  if (score >= 80) return BAND_COLORS.VERY_GOOD
  if (score >= 65) return BAND_COLORS.GOOD
  if (score >= 50) return BAND_COLORS.NEEDS_IMPROVEMENT
  return BAND_COLORS.UNSATISFACTORY
}

export function fmtNum(value, digits = 1) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  const n = Number(value)
  return Number.isInteger(n) ? String(n) : n.toFixed(digits)
}

export function fmtPct(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return `${fmtNum(value)}%`
}

export function toISODate(date) {
  const offsetMs = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 10)
}

export function rangeFromDays(days) {
  const to = new Date()
  const from = new Date()
  from.setDate(to.getDate() - (days - 1))
  return { from: toISODate(from), to: toISODate(to) }
}

export function daysBetween(from, to) {
  const a = new Date(`${from}T00:00:00`)
  const b = new Date(`${to}T00:00:00`)
  return Math.round((b - a) / 86400000) + 1
}

export function formatDate(value) {
  if (!value) return '—'
  return new Date(`${String(value).slice(0, 10)}T00:00:00`).toLocaleDateString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

export function apiError(err, fallback = 'Something went wrong.') {
  return err?.response?.data?.message || fallback
}

// When a request uses responseType: 'blob', error bodies also arrive as Blobs.
export async function blobError(err, fallback = 'Download failed.') {
  const data = err?.response?.data
  if (data instanceof Blob) {
    try {
      const parsed = JSON.parse(await data.text())
      return parsed.message || fallback
    } catch (e) {
      return fallback
    }
  }
  return apiError(err, fallback)
}

export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
