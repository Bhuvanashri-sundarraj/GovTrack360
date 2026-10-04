import React from 'react'
import { scoreColor, fmtNum } from '../utils/performance'

// Circular gauge for a 0-100 score. Shows "—" when the score is null (not enough data).
export default function ScoreRing({ score, size = 140, label = 'Overall score' }) {
  const stroke = 12
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const clamped = score == null ? 0 : Math.max(0, Math.min(100, score))
  const offset = circumference * (1 - clamped / 100)

  return (
    <div className="flex flex-col items-center">
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} role="img" aria-label={`${label}: ${score == null ? 'not rated' : fmtNum(score)}`}>
          <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="#e2e8f0" strokeWidth={stroke} />
          <circle
            cx={size / 2} cy={size / 2} r={radius} fill="none"
            stroke={scoreColor(score)} strokeWidth={stroke} strokeLinecap="round"
            strokeDasharray={circumference} strokeDashoffset={offset}
            transform={`rotate(-90 ${size / 2} ${size / 2})`}
            style={{ transition: 'stroke-dashoffset 0.5s ease' }}
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-3xl font-semibold text-slate-800">{score == null ? '—' : fmtNum(score)}</span>
          <span className="text-[11px] text-slate-400">out of 100</span>
        </div>
      </div>
      <p className="text-xs text-slate-500 mt-2">{label}</p>
    </div>
  )
}
