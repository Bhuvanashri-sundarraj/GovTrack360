import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getEmployeeDashboard } from '../services/dashboardService'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import Badge from '../components/Badge'
import AttendanceStatusBadge from '../components/AttendanceStatusBadge'
import StatCard from '../components/StatCard'
import ScoreBadge from '../components/ScoreBadge'
import { TrendingUp, Award, ListTodo, AlertTriangle, ClipboardCheck, CalendarClock } from 'lucide-react'

export default function EmployeeDashboard() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const res = await getEmployeeDashboard()
      setData(res)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  if (loading) return <Spinner label="Loading your dashboard..." />
  if (error) return <ErrorState message="Could not load your dashboard." onRetry={load} />

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-800">Welcome, {data.fullName}</h1>
        <p className="text-sm text-slate-500 mt-1">Here is your profile summary.</p>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        <Link to="/my-tasks"><StatCard label="My Active Tasks" value={data.myActiveTasks} icon={ListTodo} accent="navy" /></Link>
        <Link to="/my-tasks"><StatCard label="Due Soon" value={data.tasksDueSoon} icon={AlertTriangle} accent="amber" /></Link>
        <Link to="/my-tasks"><StatCard label="Awaiting Review" value={data.tasksAwaitingReview} icon={ClipboardCheck} accent="slate" /></Link>
        <Link to="/leave"><StatCard label="Pending Leave" value={data.pendingLeaveRequests} icon={CalendarClock} accent="slate" /></Link>
      </div>

      <div className="card p-6 mb-6">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-700">Today's Attendance</p>
            <p className="text-xs text-slate-400 mt-0.5">Check in from the Attendance page.</p>
          </div>
          {data.todayAttendanceStatus === 'NOT_CHECKED_IN' ? (
            <span className="badge bg-slate-100 text-slate-500">Not Checked In</span>
          ) : (
            <AttendanceStatusBadge status={data.todayAttendanceStatus} />
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <div className="card p-6">
          <h2 className="text-sm font-semibold text-slate-800 mb-4">Profile Summary</h2>
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between"><dt className="text-slate-500">Employee ID</dt><dd className="font-medium text-slate-800">{data.employeeId}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Designation</dt><dd className="font-medium text-slate-800">{data.designation}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Department</dt><dd className="font-medium text-slate-800">{data.departmentName}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Email</dt><dd className="font-medium text-slate-800">{data.email}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Joining Date</dt><dd className="font-medium text-slate-800">{data.joiningDate}</dd></div>
            <div className="flex justify-between items-center"><dt className="text-slate-500">Account Status</dt><dd><Badge status={data.status} /></dd></div>
          </dl>
        </div>

        <div className="card p-6">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp className="w-4 h-4 text-navy-600" />
            <h2 className="text-sm font-semibold text-slate-800">Performance &amp; Rewards</h2>
          </div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="text-3xl font-semibold text-slate-800">{data.performanceScore ?? '—'}</p>
              <p className="text-xs text-slate-400">Score, last 90 days</p>
            </div>
            <ScoreBadge band={data.performanceBand} />
          </div>
          <div className="flex items-center justify-between text-sm border-t border-slate-100 pt-3">
            <span className="text-slate-500 flex items-center gap-1.5"><Award className="w-4 h-4 text-amber-500" /> {data.rewardCount} reward(s)</span>
            <span className="font-semibold text-amber-600">{data.rewardPoints} pts</span>
          </div>
          <div className="flex gap-4 mt-4">
            <Link to="/performance" className="text-xs font-medium text-navy-600 hover:underline">View performance</Link>
            <Link to="/rewards" className="text-xs font-medium text-navy-600 hover:underline">View rewards</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
