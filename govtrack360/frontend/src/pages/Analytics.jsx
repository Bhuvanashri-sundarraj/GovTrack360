import React, { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { TrendingUp, ClipboardCheck, Clock, CalendarCheck, Users, AlertTriangle, Award, Star } from 'lucide-react'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import StatCard from '../components/StatCard'
import ScoreBadge from '../components/ScoreBadge'
import RewardTypeBadge from '../components/RewardTypeBadge'
import DateRangeFilter from '../components/DateRangeFilter'
import { BarList, DonutChart, LineChart, CHART_COLORS } from '../components/Charts'
import { getAnalyticsOverview } from '../services/analyticsService'
import { getPerformanceScope } from '../services/performanceService'
import { getDepartments } from '../services/departmentService'
import { rangeFromDays, fmtNum, fmtPct, formatDate, scoreColor, BAND_COLORS } from '../utils/performance'

const STATUS_COLORS = {
  PENDING: '#94a3b8', ASSIGNED: '#0284c7', IN_PROGRESS: '#d97706', SUBMITTED: '#6366f1',
  UNDER_REVIEW: '#7c3aed', APPROVED: '#059669', REJECTED: '#dc2626', OVERDUE: '#b91c1c', CANCELLED: '#cbd5e1',
}
const PRIORITY_COLORS = { LOW: '#94a3b8', MEDIUM: '#0284c7', HIGH: '#d97706', URGENT: '#dc2626' }

function RankedList({ items, emptyText }) {
  if (items.length === 0) return <p className="text-sm text-slate-400 py-4 text-center">{emptyText}</p>
  return (
    <ul className="divide-y divide-slate-100">
      {items.map((p) => (
        <li key={p.employeeId} className="py-2.5 flex items-center justify-between gap-3">
          <div className="min-w-0">
            <p className="text-sm font-medium text-slate-800 truncate">{p.fullName}</p>
            <p className="text-xs text-slate-400 truncate">{p.departmentName}</p>
          </div>
          <div className="flex items-center gap-2 flex-shrink-0">
            <span className="text-sm font-semibold" style={{ color: scoreColor(p.score) }}>{fmtNum(p.score)}</span>
            <ScoreBadge band={p.ratingBand} />
          </div>
        </li>
      ))}
    </ul>
  )
}

export default function Analytics() {
  const [range, setRange] = useState(rangeFromDays(90))
  const [scope, setScope] = useState(null)
  const [departments, setDepartments] = useState([])
  const [departmentId, setDepartmentId] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    const loadMeta = async () => {
      try {
        const s = await getPerformanceScope()
        setScope(s)
        if (!s.restricted) setDepartments(await getDepartments())
      } catch (err) {
        setScope({ restricted: true })
      }
    }
    loadMeta()
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      setData(await getAnalyticsOverview({ departmentId: departmentId || undefined, from: range.from, to: range.to }))
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [range, departmentId])

  useEffect(() => { load() }, [load])

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-800">Analytics</h1>
        <p className="text-sm text-slate-500 mt-1">
          {scope?.restricted && scope.departmentName
            ? `Performance insights for the ${scope.departmentName} department.`
            : 'Organisation-wide performance insights.'}
        </p>
      </div>

      <div className="card p-4 mb-6 flex flex-col lg:flex-row gap-3">
        <DateRangeFilter value={range} onChange={setRange} />
        {!scope?.restricted && (
          <select className="input lg:w-56" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)} aria-label="Department">
            <option value="">All Departments</option>
            {departments.map((d) => <option key={d.id} value={d.id}>{d.departmentName}</option>)}
          </select>
        )}
      </div>

      {loading ? (
        <Spinner label="Crunching the numbers..." />
      ) : error ? (
        <ErrorState message="Could not load analytics." onRetry={load} />
      ) : (
        <>
          <p className="text-xs text-slate-400 mb-3">{data.scope} · {formatDate(data.from)} – {formatDate(data.to)}</p>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
            <StatCard label="Average score" value={fmtNum(data.kpis.averageScore)} icon={TrendingUp} accent="navy" />
            <StatCard label="Task completion" value={fmtPct(data.kpis.taskCompletionRate)} icon={ClipboardCheck} accent="emerald" />
            <StatCard label="On-time delivery" value={fmtPct(data.kpis.onTimeRate)} icon={Clock} accent="amber" />
            <StatCard label="Attendance" value={fmtPct(data.kpis.attendanceRate)} icon={CalendarCheck} accent="slate" />
          </div>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <StatCard label="Employees evaluated" value={data.kpis.employeesEvaluated} icon={Users} accent="navy" />
            <StatCard label="Overdue tasks" value={data.kpis.overdueTasks} icon={AlertTriangle} accent="amber" />
            <StatCard label="Rewards granted" value={data.kpis.rewardsGiven} icon={Award} accent="emerald" />
            <StatCard label="Reward points" value={data.kpis.rewardPoints} icon={Star} accent="amber" />
          </div>

          <div className="card p-6 mb-6">
            <h2 className="text-sm font-semibold text-slate-800 mb-4">Six-month trend</h2>
            <LineChart
              labels={data.scoreTrend.map((p) => p.label)}
              series={[
                { name: 'Average score', color: '#2f5586', values: data.scoreTrend.map((p) => p.averageScore) },
                { name: 'Task completion', color: '#059669', values: data.scoreTrend.map((p) => p.completionRate) },
                { name: 'Attendance', color: '#d97706', values: data.scoreTrend.map((p) => p.attendanceRate) },
              ]}
            />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="card p-6">
              <h2 className="text-sm font-semibold text-slate-800 mb-4">Department comparison (average score)</h2>
              <BarList
                max={100}
                items={data.departments.map((d, i) => ({
                  label: d.departmentName,
                  value: d.averageScore,
                  sub: `${d.employees} emp.`,
                  color: scoreColor(d.averageScore),
                }))}
              />
            </div>
            <div className="card p-6">
              <h2 className="text-sm font-semibold text-slate-800 mb-4">Rating distribution</h2>
              <DonutChart
                centerLabel="employees"
                data={data.ratingBandDistribution.map((s) => ({ label: s.label, value: s.value, color: BAND_COLORS[s.key] }))}
              />
            </div>
            <div className="card p-6">
              <h2 className="text-sm font-semibold text-slate-800 mb-4">Tasks by status</h2>
              <DonutChart
                centerLabel="tasks"
                data={data.taskStatusDistribution.map((s) => ({ label: s.label, value: s.value, color: STATUS_COLORS[s.key] }))}
              />
            </div>
            <div className="card p-6">
              <h2 className="text-sm font-semibold text-slate-800 mb-4">Tasks by priority</h2>
              <BarList
                items={data.taskPriorityDistribution.map((s) => ({ label: s.label, value: s.value, color: PRIORITY_COLORS[s.key] || CHART_COLORS[0] }))}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="card p-6">
              <h2 className="text-sm font-semibold text-slate-800 mb-2">Top performers</h2>
              <RankedList items={data.topPerformers} emptyText="No rated employees in this period." />
            </div>
            <div className="card p-6">
              <h2 className="text-sm font-semibold text-slate-800 mb-2">Needs attention (below 65)</h2>
              <RankedList items={data.needsAttention} emptyText="Nobody is below 65." />
            </div>
            <div className="card p-6">
              <div className="flex items-center justify-between mb-2">
                <h2 className="text-sm font-semibold text-slate-800">Recent rewards</h2>
                <Link to="/rewards" className="text-xs font-medium text-navy-600 hover:underline">View all</Link>
              </div>
              {data.recentRewards.length === 0 ? (
                <p className="text-sm text-slate-400 py-4 text-center">No rewards in this period.</p>
              ) : (
                <ul className="divide-y divide-slate-100">
                  {data.recentRewards.map((r) => (
                    <li key={r.id} className="py-2.5">
                      <p className="text-sm font-medium text-slate-800">{r.employee.fullName}</p>
                      <div className="flex items-center gap-2 mt-1">
                        <RewardTypeBadge type={r.type} label={r.typeLabel} />
                        <span className="text-xs text-amber-600 font-medium">+{r.points}</span>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
