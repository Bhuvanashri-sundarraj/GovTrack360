import React from 'react'
import { fmtNum } from '../utils/performance'

export const CHART_COLORS = ['#2f5586', '#059669', '#d97706', '#7c3aed', '#0284c7', '#dc2626', '#64748b']

// ---------------------------------------------------------------------------
// BarList: horizontal bars with labels. items: [{ label, value, sub?, color? }]
// ---------------------------------------------------------------------------
export function BarList({ items, max, suffix = '', emptyText = 'No data for this period.' }) {
  const valid = items.filter((i) => i.value != null)
  if (items.length === 0) return <p className="text-sm text-slate-400 py-6 text-center">{emptyText}</p>
  const ceiling = max || Math.max(1, ...valid.map((i) => i.value))

  return (
    <ul className="space-y-3">
      {items.map((item, idx) => (
        <li key={`${item.label}-${idx}`}>
          <div className="flex items-baseline justify-between text-sm mb-1">
            <span className="text-slate-700 truncate pr-2">{item.label}</span>
            <span className="text-slate-500 flex-shrink-0">
              {item.value == null ? '—' : `${fmtNum(item.value)}${suffix}`}
              {item.sub ? <span className="text-slate-400 text-xs ml-2">{item.sub}</span> : null}
            </span>
          </div>
          <div className="h-2.5 bg-slate-100 rounded-full overflow-hidden">
            <div
              className="h-full rounded-full transition-all duration-500"
              style={{
                width: `${item.value == null ? 0 : Math.max(0, Math.min(100, (item.value / ceiling) * 100))}%`,
                backgroundColor: item.color || CHART_COLORS[0],
              }}
            />
          </div>
        </li>
      ))}
    </ul>
  )
}

// ---------------------------------------------------------------------------
// DonutChart: data [{ label, value, color? }]
// ---------------------------------------------------------------------------
export function DonutChart({ data, size = 160, centerLabel, emptyText = 'No data for this period.' }) {
  const total = data.reduce((sum, d) => sum + d.value, 0)
  if (total === 0) return <p className="text-sm text-slate-400 py-6 text-center">{emptyText}</p>

  const stroke = 22
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  let accumulated = 0

  return (
    <div className="flex flex-col sm:flex-row items-center gap-5">
      <div className="relative flex-shrink-0" style={{ width: size, height: size }}>
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} role="img" aria-label="Distribution chart">
          <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="#f1f5f9" strokeWidth={stroke} />
          {data.map((d, idx) => {
            const length = (d.value / total) * circumference
            const segment = (
              <circle
                key={`${d.label}-${idx}`}
                cx={size / 2} cy={size / 2} r={radius} fill="none"
                stroke={d.color || CHART_COLORS[idx % CHART_COLORS.length]} strokeWidth={stroke}
                strokeDasharray={`${length} ${circumference - length}`}
                strokeDashoffset={-accumulated}
                transform={`rotate(-90 ${size / 2} ${size / 2})`}
              />
            )
            accumulated += length
            return segment
          })}
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-2xl font-semibold text-slate-800">{total}</span>
          {centerLabel && <span className="text-[11px] text-slate-400">{centerLabel}</span>}
        </div>
      </div>
      <ul className="space-y-1.5 text-sm w-full">
        {data.map((d, idx) => (
          <li key={`${d.label}-${idx}`} className="flex items-center justify-between gap-3">
            <span className="flex items-center gap-2 text-slate-600">
              <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: d.color || CHART_COLORS[idx % CHART_COLORS.length] }} />
              {d.label}
            </span>
            <span className="text-slate-500">{d.value} <span className="text-slate-400 text-xs">({Math.round((d.value / total) * 100)}%)</span></span>
          </li>
        ))}
      </ul>
    </div>
  )
}

// ---------------------------------------------------------------------------
// LineChart: labels [..], series [{ name, color?, values: [number|null] }], 0-100 scale
// Null values leave a gap in the line instead of dropping to zero.
// ---------------------------------------------------------------------------
export function LineChart({ labels, series, yMax = 100, height = 220, emptyText = 'Not enough data yet.' }) {
  const hasData = series.some((s) => s.values.some((v) => v != null))
  if (!hasData) return <p className="text-sm text-slate-400 py-10 text-center">{emptyText}</p>

  const width = 600
  const pad = { top: 16, right: 30, bottom: 28, left: 34 }
  const innerW = width - pad.left - pad.right
  const innerH = height - pad.top - pad.bottom
  const count = labels.length
  const x = (i) => pad.left + (count === 1 ? innerW / 2 : (i / (count - 1)) * innerW)
  const y = (v) => pad.top + innerH - (Math.max(0, Math.min(yMax, v)) / yMax) * innerH
  const ticks = [0, 25, 50, 75, 100].map((t) => (t / 100) * yMax)

  const pathFor = (values) => {
    let d = ''
    let penDown = false
    values.forEach((v, i) => {
      if (v == null) {
        penDown = false
        return
      }
      d += `${penDown ? 'L' : 'M'}${x(i).toFixed(1)},${y(v).toFixed(1)} `
      penDown = true
    })
    return d.trim()
  }

  return (
    <div>
      <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-auto" role="img" aria-label="Trend chart">
        {ticks.map((t) => (
          <g key={t}>
            <line x1={pad.left} x2={width - pad.right} y1={y(t)} y2={y(t)} stroke="#e2e8f0" strokeDasharray="3 3" />
            <text x={pad.left - 6} y={y(t) + 3} textAnchor="end" fontSize="10" fill="#94a3b8">{Math.round(t)}</text>
          </g>
        ))}
        {labels.map((label, i) => (
          <text key={`${label}-${i}`} x={x(i)} y={height - 8} textAnchor="middle" fontSize="10" fill="#64748b">{label}</text>
        ))}
        {series.map((s, si) => {
          const color = s.color || CHART_COLORS[si % CHART_COLORS.length]
          return (
            <g key={s.name}>
              <path d={pathFor(s.values)} fill="none" stroke={color} strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round" />
              {s.values.map((v, i) => v == null ? null : (
                <circle key={i} cx={x(i)} cy={y(v)} r="3.5" fill="#fff" stroke={color} strokeWidth="2">
                  <title>{`${s.name} — ${labels[i]}: ${fmtNum(v)}`}</title>
                </circle>
              ))}
            </g>
          )
        })}
      </svg>
      {series.length > 1 && (
        <div className="flex flex-wrap gap-4 justify-center mt-2">
          {series.map((s, si) => (
            <span key={s.name} className="flex items-center gap-1.5 text-xs text-slate-600">
              <span className="w-3 h-0.5 rounded" style={{ backgroundColor: s.color || CHART_COLORS[si % CHART_COLORS.length], height: 3 }} />
              {s.name}
            </span>
          ))}
        </div>
      )}
    </div>
  )
}
