import React, { useEffect, useState } from 'react'
import { Search } from 'lucide-react'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import { getAuditLogs } from '../services/auditLogService'

const MODULE_OPTIONS = ['AUTH', 'EMPLOYEE', 'DEPARTMENT', 'TASK', 'ATTENDANCE', 'LEAVE', 'PERFORMANCE', 'REWARD', 'REPORT']

function formatDate(dateString) {
  return new Date(dateString).toLocaleString()
}

export default function AuditLogs() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [search, setSearch] = useState('')
  const [module, setModule] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const res = await getAuditLogs({
        search: search || undefined,
        module: module || undefined,
        from: from ? `${from}T00:00:00` : undefined,
        to: to ? `${to}T23:59:59` : undefined,
      })
      setLogs(res)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, []) // eslint-disable-line
  useEffect(() => {
    const timer = setTimeout(() => { load() }, 300)
    return () => clearTimeout(timer)
  }, [search, module, from, to]) // eslint-disable-line

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-800">Audit Logs</h1>
        <p className="text-sm text-slate-500 mt-1">System-wide activity trail. Read-only.</p>
      </div>

      <div className="card p-4 mb-4 flex flex-col sm:flex-row gap-3 flex-wrap">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input className="input pl-9" placeholder="Search action or description..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <select className="input sm:w-44" value={module} onChange={(e) => setModule(e.target.value)}>
          <option value="">All Modules</option>
          {MODULE_OPTIONS.map((m) => <option key={m} value={m}>{m}</option>)}
        </select>
        <input type="date" className="input sm:w-44" value={from} onChange={(e) => setFrom(e.target.value)} title="From date" />
        <input type="date" className="input sm:w-44" value={to} onChange={(e) => setTo(e.target.value)} title="To date" />
      </div>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Loading audit logs..." />
        ) : error ? (
          <ErrorState message="Could not load audit logs." onRetry={load} />
        ) : logs.length === 0 ? (
          <EmptyState title="No audit log entries found" description="Try adjusting your filters." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Timestamp</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">User</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Role</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Action</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Module</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Description</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3 text-slate-500 whitespace-nowrap">{formatDate(log.timestamp)}</td>
                    <td className="px-5 py-3 text-slate-700">{log.user ? (log.user.fullName || log.user.email) : 'System'}</td>
                    <td className="px-5 py-3 text-slate-500">{log.role || '—'}</td>
                    <td className="px-5 py-3 text-slate-700 font-medium">{log.action.replace(/_/g, ' ')}</td>
                    <td className="px-5 py-3 text-slate-500">{log.module}</td>
                    <td className="px-5 py-3 text-slate-500 max-w-xs truncate">{log.description}</td>
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
