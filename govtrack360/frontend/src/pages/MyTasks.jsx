import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ListTodo, Hourglass, PlayCircle, Send, AlertTriangle } from 'lucide-react'
import { getTasks } from '../services/taskService'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import TaskStatusBadge from '../components/TaskStatusBadge'
import TaskPriorityBadge from '../components/TaskPriorityBadge'
import ProgressBar from '../components/ProgressBar'
import StatCard from '../components/StatCard'

export default function MyTasks() {
  const navigate = useNavigate()
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [statusFilter, setStatusFilter] = useState('')

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const res = await getTasks(statusFilter ? { status: statusFilter } : {})
      setTasks(res)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [statusFilter]) // eslint-disable-line

  const summary = useMemo(() => {
    return {
      total: tasks.length,
      pending: tasks.filter((t) => t.status === 'ASSIGNED').length,
      inProgress: tasks.filter((t) => t.status === 'IN_PROGRESS').length,
      submitted: tasks.filter((t) => t.status === 'SUBMITTED' || t.status === 'UNDER_REVIEW').length,
      overdue: tasks.filter((t) => t.overdue).length,
    }
  }, [tasks])

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-800">My Tasks</h1>
        <p className="text-sm text-slate-500 mt-1">Tasks assigned to you.</p>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-5 gap-4 mb-6">
        <StatCard label="Total Tasks" value={summary.total} icon={ListTodo} accent="navy" />
        <StatCard label="Pending" value={summary.pending} icon={Hourglass} accent="slate" />
        <StatCard label="In Progress" value={summary.inProgress} icon={PlayCircle} accent="amber" />
        <StatCard label="Submitted" value={summary.submitted} icon={Send} accent="emerald" />
        <StatCard label="Overdue" value={summary.overdue} icon={AlertTriangle} accent="amber" />
      </div>

      <div className="card p-4 mb-4">
        <select className="input sm:w-56" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All Status</option>
          <option value="ASSIGNED">Assigned</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="SUBMITTED">Submitted</option>
          <option value="UNDER_REVIEW">Under Review</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Loading your tasks..." />
        ) : error ? (
          <ErrorState message="Could not load your tasks." onRetry={load} />
        ) : tasks.length === 0 ? (
          <EmptyState title="No tasks assigned" description="You have no tasks matching this filter yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Task ID</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Title</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Priority</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Due Date</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500 w-32">Progress</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {tasks.map((task) => (
                  <tr key={task.id} className="hover:bg-slate-50 cursor-pointer" onClick={() => navigate(`/tasks/${task.id}`)}>
                    <td className="px-5 py-3 font-medium text-slate-700">{task.taskCode}</td>
                    <td className="px-5 py-3 text-slate-700 max-w-[260px] truncate">{task.title}</td>
                    <td className="px-5 py-3"><TaskPriorityBadge priority={task.priority} /></td>
                    <td className={`px-5 py-3 ${task.overdue ? 'text-red-600 font-medium' : 'text-slate-500'}`}>{task.dueDate}</td>
                    <td className="px-5 py-3"><ProgressBar value={task.progressPercentage} /></td>
                    <td className="px-5 py-3"><TaskStatusBadge status={task.overdue ? 'OVERDUE' : task.status} /></td>
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
