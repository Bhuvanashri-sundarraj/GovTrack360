import React, { useEffect, useState } from 'react'
import { rangeFromDays, daysBetween } from '../utils/performance'

const PRESETS = [
  { key: '30', label: 'Last 30 days', days: 30 },
  { key: '90', label: 'Last 90 days', days: 90 },
  { key: '180', label: 'Last 6 months', days: 180 },
  { key: '365', label: 'Last 12 months', days: 365 },
]

const MAX_DAYS = 366

// Emits { from, to } (YYYY-MM-DD) through onChange. Invalid custom ranges are explained
// inline and never emitted, so callers always receive a range the API will accept.
export default function DateRangeFilter({ value, onChange, className = '' }) {
  const [preset, setPreset] = useState('90')
  const [custom, setCustom] = useState({ from: value?.from || '', to: value?.to || '' })
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (preset !== 'custom') return
    if (!custom.from || !custom.to) {
      setMessage('Pick both dates.')
      return
    }
    if (custom.from > custom.to) {
      setMessage('"From" must be on or before "To".')
      return
    }
    if (daysBetween(custom.from, custom.to) > MAX_DAYS) {
      setMessage(`The range cannot exceed ${MAX_DAYS} days.`)
      return
    }
    setMessage('')
    // Don't re-emit a range the parent already has (e.g. right after switching to "Custom").
    if (custom.from === value?.from && custom.to === value?.to) return
    onChange({ from: custom.from, to: custom.to })
  }, [custom, preset]) // eslint-disable-line

  const handlePreset = (e) => {
    const next = e.target.value
    setPreset(next)
    if (next === 'custom') {
      setCustom({ from: value?.from || '', to: value?.to || '' })
      return
    }
    setMessage('')
    onChange(rangeFromDays(PRESETS.find((p) => p.key === next).days))
  }

  return (
    <div className={className}>
      <div className="flex flex-col sm:flex-row gap-3">
        <select className="input sm:w-48" value={preset} onChange={handlePreset} aria-label="Period">
          {PRESETS.map((p) => <option key={p.key} value={p.key}>{p.label}</option>)}
          <option value="custom">Custom range</option>
        </select>
        {preset === 'custom' && (
          <>
            <input type="date" className="input sm:w-44" value={custom.from}
              onChange={(e) => setCustom((c) => ({ ...c, from: e.target.value }))} aria-label="From date" />
            <input type="date" className="input sm:w-44" value={custom.to}
              onChange={(e) => setCustom((c) => ({ ...c, to: e.target.value }))} aria-label="To date" />
          </>
        )}
      </div>
      {message && <p className="text-xs text-amber-700 mt-1">{message}</p>}
    </div>
  )
}
