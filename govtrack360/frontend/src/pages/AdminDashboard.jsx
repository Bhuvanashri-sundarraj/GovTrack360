import React, { useEffect, useState } from 'react'
import { Users, UserCheck, Building2, BuildingIcon, ClipboardList, ClipboardCheck, AlertTriangle, CalendarCheck, CalendarClock, TrendingUp, Award } from 'lucide-react'
import { getAdminDashboard } from '../services/dashboardService'
import StatCard from '../components/StatCard'
import Badge from '../components/Badge'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import { Link } from 'react-router-dom'

export default function AdminDashboard() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const res = await getAdminDashboard()
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
        <h1 className="text-xl font-semibold text-slate-800">Admin Dashboard</h1>
        <p className="text-sm text-slate-500 mt-1">System-wide overview of employees and departments.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
        <StatCard label="Total Employees" value={data.totalEmployees} icon={Users} accent="navy" />
        <StatCard label="Active Employees" value={data.activeEmployees} icon={UserCheck} accent="emerald" />
        <StatCard label="Total Departments" value={data.totalDepartments} icon={Building2} accent="amber" />
        <StatCard label="Active Departments" value={data.activeDepartments} icon={BuildingIcon} accent="slate" />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4 mb-4">
        <StatCard label="Active Tasks" value={data.activeTasks} icon={ClipboardList} accent="navy" />
        <StatCard label="Awaiting Review" value={data.tasksAwaitingReview} icon={ClipboardCheck} accent="amber" />
        <StatCard label="Overdue Tasks" value={data.overdueTasks} icon={AlertTriangle} accent="amber" />
        <StatCard label="Present Today" value={data.presentToday} icon={CalendarCheck} accent="emerald" />
        <StatCard label="Pending Leave" value={data.pendingLeaveRequests} icon={CalendarClock} accent="slate" />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        <Link to="/analytics"><StatCard label="Avg Performance Score (90 days)" value={data.averagePerformanceScore ?? '—'} icon={TrendingUp} accent="navy" /></Link>
        <Link to="/rewards"><StatCard label="Rewards This Month" value={data.rewardsThisMonth} icon={Award} accent="emerald" /></Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 card">
          <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-800">Recent Employees</h2>
            <Link to="/employees" className="text-xs font-medium text-navy-600 hover:underline">View all</Link>
          </div>
          <div className="divide-y divide-slate-100">
            {data.recentEmployees.length === 0 && (
              <p className="px-5 py-6 text-sm text-slate-500">No employees yet.</p>
            )}
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
          <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-800">Department Overview</h2>
            <Link to="/departments" className="text-xs font-medium text-navy-600 hover:underline">Manage</Link>
          </div>
          <div className="divide-y divide-slate-100">
            {data.departmentOverview.map((dept) => (
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

      <div className="card mt-6">
        <div className="px-5 py-4 border-b border-slate-200">
          <h2 className="text-sm font-semibold text-slate-800">Recent System Activity</h2>
        </div>
        <ul className="px-5 py-4 space-y-2">
          {data.recentActivity.map((item, i) => (
            <li key={i} className="text-sm text-slate-600">• {item}</li>
          ))}
        </ul>
      </div>

      <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Link to="/employees" className="card p-5 hover:border-navy-400 transition-colors">
          <p className="text-sm font-semibold text-slate-800">Manage Employees</p>
          <p className="text-xs text-slate-500 mt-1">Add, edit, activate or deactivate employee records.</p>
        </Link>
        <Link to="/departments" className="card p-5 hover:border-navy-400 transition-colors">
          <p className="text-sm font-semibold text-slate-800">Manage Departments</p>
          <p className="text-xs text-slate-500 mt-1">Add, edit, activate or deactivate departments.</p>
        </Link>
      </div>
    </div>
  )
}
