import React from 'react'
import RatingInput from './RatingInput'
import ScoreBadge from './ScoreBadge'
import { fmtNum, formatDate } from '../utils/performance'

export default function ReviewDetails({ review }) {
  return (
    <div className="space-y-4 text-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-medium text-slate-800">{review.employee.fullName}</p>
          <p className="text-xs text-slate-500">{review.department.departmentName} · {formatDate(review.periodStart)} – {formatDate(review.periodEnd)}</p>
          <p className="text-xs text-slate-400 mt-0.5">Reviewed by {review.reviewer?.fullName || review.reviewer?.email || 'Unknown'}</p>
        </div>
        <div className="text-right">
          <p className="text-2xl font-semibold text-slate-800">{fmtNum(review.finalScore)}</p>
          <ScoreBadge band={review.ratingBand} />
        </div>
      </div>

      <div className="grid grid-cols-3 gap-3">
        {[['Quality', review.qualityRating], ['Teamwork', review.teamworkRating], ['Initiative', review.initiativeRating]].map(([label, value]) => (
          <div key={label} className="rounded-md bg-slate-50 p-3">
            <p className="text-xs text-slate-500 mb-1">{label}</p>
            <RatingInput value={value} readOnly label={`${label} rating`} />
          </div>
        ))}
      </div>

      <p className="text-xs text-slate-500">
        System score {review.systemScore == null ? 'n/a' : fmtNum(review.systemScore)} (70%) ·
        Manager score {fmtNum(review.managerScore)} (30%)
      </p>

      {review.strengths && <div><p className="font-medium text-slate-700">Strengths</p><p className="text-slate-600 whitespace-pre-line">{review.strengths}</p></div>}
      {review.areasForImprovement && <div><p className="font-medium text-slate-700">Areas for improvement</p><p className="text-slate-600 whitespace-pre-line">{review.areasForImprovement}</p></div>}
      {review.comments && <div><p className="font-medium text-slate-700">Comments</p><p className="text-slate-600 whitespace-pre-line">{review.comments}</p></div>}
    </div>
  )
}
