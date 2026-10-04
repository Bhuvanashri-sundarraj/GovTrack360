import React from 'react'
import { BarList } from './Charts'
import { SCORE_COMPONENTS, scoreColor, fmtNum } from '../utils/performance'

// Shows each component of the overall score with its weight, plus the raw counts behind them.
export default function ScoreBreakdown({ score }) {
  const items = SCORE_COMPONENTS.map((c) => ({
    label: `${c.label} (weight ${c.weight})`,
    value: score[c.key],
    color: scoreColor(score[c.key]),
  }))

  return (
    <div>
      <BarList items={items} max={100} suffix="%" />
      <p className="text-xs text-slate-400 mt-3">
        A component with no data in this period (for example, no tasks fell due) is left out and the remaining weights are
        rescaled, so nobody is penalised for something that could not be measured.
      </p>
      <dl className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-4 text-sm">
        <div className="rounded-md bg-slate-50 p-3"><dt className="text-xs text-slate-500">Tasks due</dt><dd className="font-semibold text-slate-800">{score.tasksTotal}</dd></div>
        <div className="rounded-md bg-slate-50 p-3"><dt className="text-xs text-slate-500">Delivered</dt><dd className="font-semibold text-slate-800">{score.tasksDelivered} <span className="text-xs font-normal text-slate-400">({score.tasksApproved} approved)</span></dd></div>
        <div className="rounded-md bg-slate-50 p-3"><dt className="text-xs text-slate-500">Overdue / rejections</dt><dd className="font-semibold text-slate-800">{score.tasksOverdue} / {score.rejectionCount}</dd></div>
        <div className="rounded-md bg-slate-50 p-3"><dt className="text-xs text-slate-500">Working days</dt><dd className="font-semibold text-slate-800">{fmtNum(score.expectedWorkingDays)}</dd></div>
        <div className="rounded-md bg-slate-50 p-3"><dt className="text-xs text-slate-500">Present</dt><dd className="font-semibold text-slate-800">{score.presentDays}</dd></div>
        <div className="rounded-md bg-slate-50 p-3"><dt className="text-xs text-slate-500">Late</dt><dd className="font-semibold text-slate-800">{score.lateDays}</dd></div>
        <div className="rounded-md bg-slate-50 p-3"><dt className="text-xs text-slate-500">Half days</dt><dd className="font-semibold text-slate-800">{score.halfDays}</dd></div>
        <div className="rounded-md bg-slate-50 p-3"><dt className="text-xs text-slate-500">Leave days</dt><dd className="font-semibold text-slate-800">{score.leaveDays}</dd></div>
      </dl>
    </div>
  )
}
