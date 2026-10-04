import React from 'react'
import { Star } from 'lucide-react'

// 1-5 star selector. Read-only mode renders the same stars without buttons.
export default function RatingInput({ value, onChange, readOnly = false, label }) {
  return (
    <div className="flex items-center gap-1" role={readOnly ? 'img' : 'radiogroup'} aria-label={label}>
      {[1, 2, 3, 4, 5].map((n) => {
        const filled = n <= (value || 0)
        const star = <Star className={`w-5 h-5 ${filled ? 'text-amber-400 fill-amber-400' : 'text-slate-300'}`} />
        return readOnly ? (
          <span key={n}>{star}</span>
        ) : (
          <button
            type="button" key={n} onClick={() => onChange(n)}
            className="p-0.5 rounded focus:outline-none focus:ring-2 focus:ring-navy-500"
            role="radio" aria-checked={value === n} aria-label={`${n} star${n > 1 ? 's' : ''}`}
          >
            {star}
          </button>
        )
      })}
    </div>
  )
}
