import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Users, TrendingUp, Trophy, AlertTriangle, Plus, Pencil, Trash2, Eye, Award, Search } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import StatCard from '../components/StatCard'
import Tabs from '../components/Tabs'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import ScoreBadge from '../components/ScoreBadge'
import ScoreBreakdown from '../components/ScoreBreakdown'
import ReviewDetails from '../components/ReviewDetails'
import ReviewFormModal from '../components/ReviewFormModal'
import GrantRewardModal from '../components/GrantRewardModal'
import DateRangeFilter from '../components/DateRangeFilter'
import { LineChart } from '../components/Charts'
import {
  getLeaderboard, getPerformanceScope, getEmployeePerformanceTrend,
  getReviews, deleteReview,
} from '../services/performanceService'
import { getDepartments } from '../services/departmentService'
import { rangeFromDays, fmtNum, fmtPct, formatDate, scoreColor, apiError } from '../utils/performance'

function ScoreCell({ score }) {
  return (
    <div className="flex items-center gap-2 min-w-[110px]">
      <span className="w-9 text-right font-semibold text-slate-800">{fmtNum(score)}</span>
      <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
        <div className="h-full rounded-full" style={{ width: `${score == null ? 0 : score}%`, backgroundColor: scoreColor(score) }} />
      </div>
    </div>
  )
}

function EmployeeDetailModal({ row, onClose }) {
  const [trend, setTrend] = useState(null)

  useEffect(() => {
    let cancelled = false
    setTrend(null)
    if (row) {
      getEmployeePerformanceTrend(row.employee.id, 6)
        .then((t) => { if (!cancelled) setTrend(t) })
        .catch(() => { if (!cancelled) setTrend([]) })
    }
    return () => { cancelled = true }
  }, [row])

  if (!row) return null
  return (
    <Modal open={!!row} onClose={onClose} title={row.employee.fullName} size="lg">
      <div className="flex items-center justify-between mb-5">
        <div>
          <p className="text-sm text-slate-600">{row.employee.designation} · {row.department.departmentName}</p>
          <p className="text-xs text-slate-400">{formatDate(row.from)} – {formatDate(row.to)}</p>
        </div>
        <div className="text-right">
          <p className="text-3xl font-semibold text-slate-800">{fmtNum(row.overallScore)}</p>
          <ScoreBadge band={row.ratingBand} />
        </div>
      </div>
      <ScoreBreakdown score={row} />
      <h3 className="text-sm font-semibold text-slate-800 mt-6 mb-3">Last 6 months</h3>
      {trend === null ? (
        <Spinner label="Loading trend..." className="py-4" />
      ) : (
        <LineChart
          labels={trend.map((p) => p.label)}
          series={[
            { name: 'Overall score', color: '#2f5586', values: trend.map((p) => p.overallScore) },
            { name: 'Task completion', color: '#059669', values: trend.map((p) => p.completionRate) },
            { name: 'Attendance', color: '#d97706', values: trend.map((p) => p.attendanceRate) },
          ]}
          height={200}
        />
      )}
    </Modal>
  )
}

export default function PerformanceManagement() {
  const { user } = useAuth()
  const toast = useToast()
  const isAdmin = user?.role === 'ADMIN'

  const [range, setRange] = useState(rangeFromDays(90))
  const [scope, setScope] = useState(null)
  const [departments, setDepartments] = useState([])
  const [departmentId, setDepartmentId] = useState('')
  const [tab, setTab] = useState('leaderboard')
  const [search, setSearch] = useState('')

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [reviews, setReviews] = useState([])
  const [reviewsLoading, setReviewsLoading] = useState(false)
  const [reviewsError, setReviewsError] = useState(false)

  const [detailRow, setDetailRow] = useState(null)
  const [reviewModal, setReviewModal] = useState({ open: false, review: null, presetEmployeeId: null })
  const [rewardModal, setRewardModal] = useState({ open: false, presetEmployeeId: null })
  const [viewReview, setViewReview] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  // Scope (is the user locked to one department?) and, for admins, the department list.
  useEffect(() => {
    const loadScope = async () => {
      try {
        const s = await getPerformanceScope()
        setScope(s)
        if (!s.restricted) setDepartments(await getDepartments())
      } catch (err) {
        setScope({ restricted: true })
      }
    }
    loadScope()
  }, [])

  const loadBoard = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      const data = await getLeaderboard({ departmentId: departmentId || undefined, from: range.from, to: range.to })
      setRows(data)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [range, departmentId])

  const loadReviews = useCallback(async () => {
    setReviewsLoading(true)
    setReviewsError(false)
    try {
      setReviews(await getReviews({ departmentId: departmentId || undefined }))
    } catch (err) {
      setReviewsError(true)
    } finally {
      setReviewsLoading(false)
    }
  }, [departmentId])

  useEffect(() => { loadBoard() }, [loadBoard])
  useEffect(() => { if (tab === 'reviews') loadReviews() }, [tab, loadReviews])

  const stats = useMemo(() => {
    const rated = rows.filter((r) => r.overallScore != null)
    const avg = rated.length ? rated.reduce((s, r) => s + r.overallScore, 0) / rated.length : null
    return {
      rated: rated.length,
      avg,
      top: rated.length ? rated[0] : null,
      attention: rated.filter((r) => r.overallScore < 65).length,
    }
  }, [rows])

  const needle = search.trim().toLowerCase()
  const filteredRows = rows.filter((r) => !needle || r.employee.fullName.toLowerCase().includes(needle) || r.employee.employeeId.toLowerCase().includes(needle))
  const filteredReviews = reviews.filter((r) => !needle || r.employee.fullName.toLowerCase().includes(needle) || r.employee.employeeId.toLowerCase().includes(needle))

  const isSelf = (employeeId) => user?.employeeId != null && user.employeeId === employeeId
  const canEditReview = (review) => isAdmin || review.reviewer?.id === user?.id

  const handleDelete = async () => {
    setDeleting(true)
    try {
      await deleteReview(deleteTarget.id)
      toast.success('Review deleted.')
      setDeleteTarget(null)
      loadReviews()
    } catch (err) {
      toast.error(apiError(err, 'Failed to delete review.'))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">Performance</h1>
          <p className="text-sm text-slate-500 mt-1">
            {scope?.restricted && scope.departmentName
              ? `Scores and reviews for the ${scope.departmentName} department.`
              : 'Scores calculated from tasks and attendance, plus formal reviews.'}
          </p>
        </div>
        <button className="btn-primary self-start" onClick={() => setReviewModal({ open: true, review: null, presetEmployeeId: null })}>
          <Plus className="w-4 h-4" /> New review
        </button>
      </div>

      <div className="card p-4 mb-4 flex flex-col lg:flex-row gap-3">
        <DateRangeFilter value={range} onChange={setRange} />
        {!scope?.restricted && (
          <select className="input lg:w-56" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)} aria-label="Department">
            <option value="">All Departments</option>
            {departments.map((d) => <option key={d.id} value={d.id}>{d.departmentName}</option>)}
          </select>
        )}
        <div className="relative lg:flex-1 lg:max-w-xs lg:ml-auto">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input className="input pl-9" placeholder="Search employee..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard label="Employees rated" value={stats.rated} icon={Users} accent="navy" />
        <StatCard label="Average score" value={fmtNum(stats.avg)} icon={TrendingUp} accent="emerald" />
        <StatCard label="Top performer" value={stats.top ? stats.top.employee.fullName.split(' ')[0] : '—'} icon={Trophy} accent="amber" />
        <StatCard label="Below 65" value={stats.attention} icon={AlertTriangle} accent="slate" />
      </div>

      <Tabs tabs={[{ key: 'leaderboard', label: 'Leaderboard' }, { key: 'reviews', label: 'Reviews' }]} active={tab} onChange={setTab} />

      {tab === 'leaderboard' && (
        <div className="card overflow-hidden">
          {loading ? (
            <Spinner label="Calculating scores..." />
          ) : error ? (
            <ErrorState message="Could not load performance scores." onRetry={loadBoard} />
          ) : filteredRows.length === 0 ? (
            <EmptyState title="No employees found" description="Try a different department, period or search." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 border-b border-slate-200">
                  <tr>
                    {['#', 'Employee', 'Department', 'Tasks', 'Completion', 'On-time', 'Attendance', 'Score', 'Rating', ''].map((h) => (
                      <th key={h} className="text-left px-4 py-3 font-medium text-slate-500 whitespace-nowrap">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredRows.map((r) => (
                    <tr key={r.employee.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3 text-slate-500">{r.rank ?? '—'}</td>
                      <td className="px-4 py-3">
                        <p className="font-medium text-slate-800">{r.employee.fullName}</p>
                        <p className="text-xs text-slate-400">{r.employee.employeeId}</p>
                      </td>
                      <td className="px-4 py-3 text-slate-500">{r.department.departmentName}</td>
                      <td className="px-4 py-3 text-slate-500 whitespace-nowrap">{r.tasksDelivered}/{r.tasksTotal}</td>
                      <td className="px-4 py-3 text-slate-500">{fmtPct(r.completionRate)}</td>
                      <td className="px-4 py-3 text-slate-500">{fmtPct(r.onTimeRate)}</td>
                      <td className="px-4 py-3 text-slate-500">{fmtPct(r.attendanceRate)}</td>
                      <td className="px-4 py-3"><ScoreCell score={r.overallScore} /></td>
                      <td className="px-4 py-3"><ScoreBadge band={r.ratingBand} /></td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button className="p-1.5 rounded hover:bg-slate-100 text-slate-500" title="View breakdown" aria-label="View breakdown" onClick={() => setDetailRow(r)}><Eye className="w-4 h-4" /></button>
                          {!isSelf(r.employee.id) && (
                            <>
                              <button className="p-1.5 rounded hover:bg-slate-100 text-slate-500" title="Write review" aria-label="Write review" onClick={() => setReviewModal({ open: true, review: null, presetEmployeeId: r.employee.id })}><Pencil className="w-4 h-4" /></button>
                              <button className="p-1.5 rounded hover:bg-slate-100 text-slate-500" title="Grant reward" aria-label="Grant reward" onClick={() => setRewardModal({ open: true, presetEmployeeId: r.employee.id })}><Award className="w-4 h-4" /></button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {tab === 'reviews' && (
        <div className="card overflow-hidden">
          {reviewsLoading ? (
            <Spinner label="Loading reviews..." />
          ) : reviewsError ? (
            <ErrorState message="Could not load reviews." onRetry={loadReviews} />
          ) : filteredReviews.length === 0 ? (
            <EmptyState title="No reviews yet" description="Write the first review using the New review button." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 border-b border-slate-200">
                  <tr>
                    {['Employee', 'Period', 'Reviewer', 'Q / T / I', 'Final score', 'Rating', ''].map((h) => (
                      <th key={h} className="text-left px-4 py-3 font-medium text-slate-500 whitespace-nowrap">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredReviews.map((r) => (
                    <tr key={r.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3">
                        <p className="font-medium text-slate-800">{r.employee.fullName}</p>
                        <p className="text-xs text-slate-400">{r.department.departmentName}</p>
                      </td>
                      <td className="px-4 py-3 text-slate-500 whitespace-nowrap">{formatDate(r.periodStart)} – {formatDate(r.periodEnd)}</td>
                      <td className="px-4 py-3 text-slate-500">{r.reviewer?.fullName || r.reviewer?.email}</td>
                      <td className="px-4 py-3 text-slate-500 whitespace-nowrap">{r.qualityRating} / {r.teamworkRating} / {r.initiativeRating}</td>
                      <td className="px-4 py-3 font-semibold text-slate-800">{fmtNum(r.finalScore)}</td>
                      <td className="px-4 py-3"><ScoreBadge band={r.ratingBand} /></td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button className="p-1.5 rounded hover:bg-slate-100 text-slate-500" title="View" aria-label="View review" onClick={() => setViewReview(r)}><Eye className="w-4 h-4" /></button>
                          {canEditReview(r) && (
                            <button className="p-1.5 rounded hover:bg-slate-100 text-slate-500" title="Edit" aria-label="Edit review" onClick={() => setReviewModal({ open: true, review: r, presetEmployeeId: null })}><Pencil className="w-4 h-4" /></button>
                          )}
                          {isAdmin && (
                            <button className="p-1.5 rounded hover:bg-red-50 text-red-500" title="Delete" aria-label="Delete review" onClick={() => setDeleteTarget(r)}><Trash2 className="w-4 h-4" /></button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      <EmployeeDetailModal row={detailRow} onClose={() => setDetailRow(null)} />

      <ReviewFormModal
        open={reviewModal.open}
        review={reviewModal.review}
        presetEmployeeId={reviewModal.presetEmployeeId}
        onClose={() => setReviewModal((m) => ({ ...m, open: false }))}
        onSaved={() => { if (tab === 'reviews') loadReviews(); else setTab('reviews') }}
      />

      <GrantRewardModal
        open={rewardModal.open}
        presetEmployeeId={rewardModal.presetEmployeeId}
        onClose={() => setRewardModal((m) => ({ ...m, open: false }))}
      />

      <Modal open={!!viewReview} onClose={() => setViewReview(null)} title="Performance review" size="lg">
        {viewReview && <ReviewDetails review={viewReview} />}
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        title="Delete review?"
        message={deleteTarget ? `This permanently deletes the review for ${deleteTarget.employee.fullName}. It cannot be undone.` : ''}
        confirmLabel="Delete"
      />
    </div>
  )
}
