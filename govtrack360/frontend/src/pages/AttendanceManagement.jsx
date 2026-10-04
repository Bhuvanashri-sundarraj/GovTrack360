import React, { useEffect, useState } from 'react'
import { Users, UserCheck, UserX, CalendarClock, Clock } from 'lucide-react'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import AttendanceStatusBadge from '../components/AttendanceStatusBadge'
import StatCard from '../components/StatCard'
import { searchAttendance, getTodayAttendanceSummary } from '../services/attendanceService'
import { getDepartments } from '../services/departmentService'

export default function AttendanceManagement() {
  const [records, setRecords] = useState([])
  const [summary, setSummary] = useState(null)
  const [departments, setDepartments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [date, setDate] = useState('')
  const [departmentId, setDepartmentId] = useState('')

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const [recordsData, summaryData, deptData] = await Promise.all([
        searchAttendance({ date: date || undefined, departmentId: departmentId || undefined }),
        getTodayAttendanceSummary(),
        getDepartments(),
      ])
      setRecords(recordsData)
      setSummary(summaryData)
      setDepartments(deptData)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, []) // eslint-disable-line
  useEffect(() => {
    const timer = setTimeout(() => { load() }, 250)
    return () => clearTimeout(timer)
  }, [date, departmentId]) // eslint-disable-line

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-800">Attendance Management</h1>
        <p className="text-sm text-slate-500 mt-1">Monitor employee attendance across departments.</p>
      </div>

      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-4 mb-6">
          <StatCard label="Total Employees" value={summary.totalEmployees} icon={Users} accent="navy" />
          <StatCard label="Present Today" value={summary.presentToday} icon={UserCheck} accent="emerald" />
          <StatCard label="Absent Today" value={summary.absentToday} icon={UserX} accent="amber" />
          <StatCard label="On Leave" value={summary.onLeaveToday} icon={CalendarClock} accent="slate" />
          <StatCard label="Late Today" value={summary.lateToday} icon={Clock} accent="amber" />
        </div>
      )}

      <div className="card p-4 mb-4 flex flex-col sm:flex-row gap-3">
        <input type="date" className="input sm:w-56" value={date} onChange={(e) => setDate(e.target.value)} />
        <select className="input sm:w-56" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
          <option value="">All Departments</option>
          {departments.map((d) => <option key={d.id} value={d.id}>{d.departmentName}</option>)}
        </select>
      </div>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Loading attendance records..." />
        ) : error ? (
          <ErrorState message="Could not load attendance records." onRetry={load} />
        ) : records.length === 0 ? (
          <EmptyState title="No attendance records found" description="Try adjusting the date or department filter." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Employee</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Department</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Date</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Check In</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Check Out</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Hours</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {records.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3 text-slate-700">{r.employee.fullName}</td>
                    <td className="px-5 py-3 text-slate-500">{r.department.departmentName}</td>
                    <td className="px-5 py-3 text-slate-500">{r.date}</td>
                    <td className="px-5 py-3 text-slate-500">{r.checkInTime || '—'}</td>
                    <td className="px-5 py-3 text-slate-500">{r.checkOutTime || '—'}</td>
                    <td className="px-5 py-3 text-slate-500">{r.totalWorkingHours ?? '—'}</td>
                    <td className="px-5 py-3"><AttendanceStatusBadge status={r.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
