import React, { useEffect, useState } from 'react'
import { Users, UserCheck, Building2, ClipboardList, ClipboardCheck, AlertTriangle, CalendarClock, TrendingUp, Award } from 'lucide-react'
import { getOfficerDashboard } from '../services/dashboardService'
import StatCard from '../components/StatCard'
import Badge from '../components/Badge'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import { Link } from 'react-router-dom'

export default function OfficerDashboard() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const res = await getOfficerDashboard()
      setData(res)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  if (loading) return <Spinner label="Loading dashboard..." />
  if (error) return <ErrorState message="Could not load dashboard data." onRetry={load} />

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-800">Officer Dashboard</h1>
        <p className="text-sm text-slate-500 mt-1">Read-only overview of employees and departments.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
        <StatCard label="Total Employees" value={data.totalEmployees} icon={Users} accent="navy" />
        <StatCard label="Active Employees" value={data.activeEmployees} icon={UserCheck} accent="emerald" />
        <StatCard label="Departments" value={data.totalDepartments} icon={Building2} accent="amber" />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard label="Assigned Tasks" value={data.assignedTasks} icon={ClipboardList} accent="navy" />
        <StatCard label="Awaiting Review" value={data.tasksAwaitingReview} icon={ClipboardCheck} accent="amber" />
        <StatCard label="Overdue Tasks" value={data.overdueTasks} icon={AlertTriangle} accent="amber" />
        <StatCard label="Pending Leave" value={data.pendingLeaveRequests} icon={CalendarClock} accent="slate" />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        <Link to="/performance"><StatCard label="Dept. Avg Performance (90 days)" value={data.averagePerformanceScore ?? '—'} icon={TrendingUp} accent="navy" /></Link>
        <Link to="/rewards"><StatCard label="Dept. Rewards This Month" value={data.rewardsThisMonth} icon={Award} accent="emerald" /></Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <div className="px-5 py-4 border-b border-slate-200">
            <h2 className="text-sm font-semibold text-slate-800">Recent Employees</h2>
          </div>
          <div className="divide-y divide-slate-100">
            {data.recentEmployees.map((emp) => (
              <div key={emp.id} className="px-5 py-3 flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-800">{emp.fullName}</p>
                  <p className="text-xs text-slate-500">{emp.designation} · {emp.department.departmentName}</p>
                </div>
                <Badge status={emp.status} />
              </div>
            ))}
          </div>
        </div>

        <div className="card">
          <div className="px-5 py-4 border-b border-slate-200">
            <h2 className="text-sm font-semibold text-slate-800">Departments</h2>
          </div>
          <div className="divide-y divide-slate-100">
            {data.departments.map((dept) => (
              <div key={dept.id} className="px-5 py-3 flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-800">{dept.departmentName}</p>
                  <p className="text-xs text-slate-500">{dept.employeeCount} employee(s)</p>
                </div>
                <Badge status={dept.status} />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
