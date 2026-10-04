import React, { useEffect, useState } from 'react'
import { LogIn, LogOut, CalendarCheck, Clock } from 'lucide-react'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import AttendanceStatusBadge from '../components/AttendanceStatusBadge'
import StatCard from '../components/StatCard'
import { checkIn, checkOut, getMyAttendanceHistory, getMyAttendanceSummary } from '../services/attendanceService'

export default function MyAttendance() {
  const toast = useToast()
  const [history, setHistory] = useState([])
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  const today = new Date().toISOString().slice(0, 10)
  const todayRecord = history.find((h) => h.date === today)

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const [historyData, summaryData] = await Promise.all([getMyAttendanceHistory(), getMyAttendanceSummary()])
      setHistory(historyData)
      setSummary(summaryData)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleCheckIn = async () => {
    setActionLoading(true)
    try {
      await checkIn()
      toast.success('Checked in successfully.')
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to check in.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleCheckOut = async () => {
    setActionLoading(true)
    try {
      await checkOut()
      toast.success('Checked out successfully.')
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to check out.')
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) return <Spinner label="Loading your attendance..." />
  if (error) return <ErrorState message="Could not load your attendance." onRetry={load} />

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-800">My Attendance</h1>
        <p className="text-sm text-slate-500 mt-1">Check in, check out, and review your attendance history.</p>
      </div>

      <div className="card p-6 mb-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-slate-700 mb-2">Today's Attendance</p>
            {todayRecord ? (
              <div className="flex flex-wrap items-center gap-4 text-sm text-slate-600">
                <span className="flex items-center gap-1.5"><LogIn className="w-4 h-4 text-slate-400" /> Check-in: {todayRecord.checkInTime || '—'}</span>
                <span className="flex items-center gap-1.5"><LogOut className="w-4 h-4 text-slate-400" /> Check-out: {todayRecord.checkOutTime || '—'}</span>
                <span className="flex items-center gap-1.5"><Clock className="w-4 h-4 text-slate-400" /> Hours: {todayRecord.totalWorkingHours ?? '—'}</span>
                <AttendanceStatusBadge status={todayRecord.status} />
              </div>
            ) : (
              <p className="text-sm text-slate-400">You have not checked in today.</p>
            )}
          </div>
          <div className="flex gap-3">
            <button className="btn-primary" onClick={handleCheckIn} disabled={actionLoading || !!todayRecord}>
              <LogIn className="w-4 h-4" /> Check In
            </button>
            <button className="btn-secondary" onClick={handleCheckOut} disabled={actionLoading || !todayRecord || !!todayRecord?.checkOutTime}>
              <LogOut className="w-4 h-4" /> Check Out
            </button>
          </div>
        </div>
      </div>

      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
          <StatCard label="Present Days" value={summary.presentDays} icon={CalendarCheck} accent="emerald" />
          <StatCard label="Late Days" value={summary.lateDays} icon={Clock} accent="amber" />
          <StatCard label="Leave Days" value={summary.leaveDays} icon={CalendarCheck} accent="slate" />
          <StatCard label="Absent Days" value={summary.absentDays} icon={CalendarCheck} accent="navy" />
        </div>
      )}

      <div className="card overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-200">
          <h2 className="text-sm font-semibold text-slate-800">This Month's History</h2>
        </div>
        {history.length === 0 ? (
          <EmptyState title="No attendance records yet" description="Check in for the first time to start your history." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Date</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Check In</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Check Out</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Hours</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {history.map((h) => (
                  <tr key={h.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3 text-slate-700">{h.date}</td>
                    <td className="px-5 py-3 text-slate-500">{h.checkInTime || '—'}</td>
                    <td className="px-5 py-3 text-slate-500">{h.checkOutTime || '—'}</td>
                    <td className="px-5 py-3 text-slate-500">{h.totalWorkingHours ?? '—'}</td>
                    <td className="px-5 py-3"><AttendanceStatusBadge status={h.status} /></td>
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
