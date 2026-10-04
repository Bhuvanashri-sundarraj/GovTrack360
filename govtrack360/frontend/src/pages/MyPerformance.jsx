import React, { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Award } from 'lucide-react'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import ScoreRing from '../components/ScoreRing'
import ScoreBadge from '../components/ScoreBadge'
import ScoreBreakdown from '../components/ScoreBreakdown'
import ReviewDetails from '../components/ReviewDetails'
import DateRangeFilter from '../components/DateRangeFilter'
import { LineChart } from '../components/Charts'
import { getMyPerformance, getMyPerformanceTrend, getMyReviews } from '../services/performanceService'
import { rangeFromDays, formatDate, fmtNum } from '../utils/performance'

export default function MyPerformance() {
  const [range, setRange] = useState(rangeFromDays(90))
  const [score, setScore] = useState(null)
  const [trend, setTrend] = useState([])
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [openReviewId, setOpenReviewId] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      const [s, t, r] = await Promise.all([
        getMyPerformance({ from: range.from, to: range.to }),
        getMyPerformanceTrend(6),
        getMyReviews(),
      ])
      setScore(s)
      setTrend(t)
      setReviews(r)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [range])

  useEffect(() => { load() }, [load])

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">My Performance</h1>
          <p className="text-sm text-slate-500 mt-1">Your score is calculated automatically from your tasks and attendance.</p>
        </div>
        <Link to="/rewards" className="btn-secondary self-start"><Award className="w-4 h-4" /> My rewards</Link>
      </div>

      <div className="card p-4 mb-6">
        <DateRangeFilter value={range} onChange={setRange} />
      </div>

      {loading ? (
        <Spinner label="Calculating your performance..." />
      ) : error ? (
        <ErrorState message="Could not load your performance." onRetry={load} />
      ) : (
        <>
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
            <div className="card p-6 flex flex-col items-center justify-center">
              <ScoreRing score={score.overallScore} />
              <div className="mt-3"><ScoreBadge band={score.ratingBand} /></div>
              <p className="text-xs text-slate-400 mt-3 text-center">
                {formatDate(score.from)} – {formatDate(score.to)}
              </p>
              {score.overallScore == null && (
                <p className="text-xs text-slate-500 mt-2 text-center">
                  Not enough activity in this period to calculate a score yet.
                </p>
              )}
            </div>
            <div className="card p-6 lg:col-span-2">
              <h2 className="text-sm font-semibold text-slate-800 mb-4">How your score is made up</h2>
              <ScoreBreakdown score={score} />
            </div>
          </div>

          <div className="card p-6 mb-6">
            <h2 className="text-sm font-semibold text-slate-800 mb-4">Last 6 months</h2>
            <LineChart
              labels={trend.map((p) => p.label)}
              series={[
                { name: 'Overall score', color: '#2f5586', values: trend.map((p) => p.overallScore) },
                { name: 'Task completion', color: '#059669', values: trend.map((p) => p.completionRate) },
                { name: 'Attendance', color: '#d97706', values: trend.map((p) => p.attendanceRate) },
              ]}
            />
          </div>

          <div className="card">
            <div className="px-5 py-4 border-b border-slate-200">
              <h2 className="text-sm font-semibold text-slate-800">My performance reviews</h2>
            </div>
            {reviews.length === 0 ? (
              <EmptyState title="No reviews yet" description="Formal reviews written by your officer will appear here." />
            ) : (
              <div className="divide-y divide-slate-100">
                {reviews.map((r) => (
                  <div key={r.id} className="px-5 py-4">
                    <button
                      className="w-full flex items-center justify-between text-left"
                      onClick={() => setOpenReviewId(openReviewId === r.id ? null : r.id)}
                      aria-expanded={openReviewId === r.id}
                    >
                      <div>
                        <p className="text-sm font-medium text-slate-800">{formatDate(r.periodStart)} – {formatDate(r.periodEnd)}</p>
                        <p className="text-xs text-slate-500">Reviewed by {r.reviewer?.fullName || r.reviewer?.email}</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-lg font-semibold text-slate-800">{fmtNum(r.finalScore)}</span>
                        <ScoreBadge band={r.ratingBand} />
                      </div>
                    </button>
                    {openReviewId === r.id && <div className="mt-4 pt-4 border-t border-slate-100"><ReviewDetails review={r} /></div>}
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
