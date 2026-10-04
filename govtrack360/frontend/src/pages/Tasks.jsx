import React, { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Plus, Search, Pencil, Trash2, Ban, UserPlus, Eye } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import TaskStatusBadge from '../components/TaskStatusBadge'
import TaskPriorityBadge from '../components/TaskPriorityBadge'
import ProgressBar from '../components/ProgressBar'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import FormField from '../components/FormField'
import { getTasks, createTask, updateTask, assignTask, cancelTask, deleteTask } from '../services/taskService'
import { getDepartments } from '../services/departmentService'
import { getEmployees } from '../services/employeeService'

const emptyForm = {
  title: '', description: '', departmentId: '', assignedToEmployeeId: '',
  priority: 'MEDIUM', startDate: '', dueDate: '',
}

const STATUS_OPTIONS = ['PENDING', 'ASSIGNED', 'IN_PROGRESS', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED']
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

export default function Tasks() {
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const isAdmin = user?.role === 'ADMIN'

  const [tasks, setTasks] = useState([])
  const [departments, setDepartments] = useState([])
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [deptFilter, setDeptFilter] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formErrors, setFormErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const [assignTarget, setAssignTarget] = useState(null)
  const [assignEmployeeId, setAssignEmployeeId] = useState('')
  const [assigning, setAssigning] = useState(false)

  const [confirmTarget, setConfirmTarget] = useState(null) // { type: 'cancel'|'delete', task }
  const [confirmLoading, setConfirmLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const [taskList, depts, emps] = await Promise.all([
        getTasks({
          search: search || undefined,
          status: statusFilter || undefined,
          priority: priorityFilter || undefined,
          departmentId: deptFilter || undefined,
        }),
        getDepartments(),
        getEmployees(),
      ])
      setTasks(taskList)
      setDepartments(depts)
      setEmployees(emps)
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
  }, [search, statusFilter, priorityFilter, deptFilter]) // eslint-disable-line

  const activeDepartments = useMemo(() => departments.filter((d) => d.status === 'ACTIVE'), [departments])
  const employeesInDept = useMemo(
    () => employees.filter((e) => String(e.department.id) === String(form.departmentId) && e.status === 'ACTIVE'),
    [employees, form.departmentId]
  )
  const assignEligibleEmployees = useMemo(
    () => employees.filter((e) => assignTarget && String(e.department.id) === String(assignTarget.department.id) && e.status === 'ACTIVE'),
    [employees, assignTarget]
  )

  const openCreate = () => {
    setEditingId(null)
    setForm(emptyForm)
    setFormErrors({})
    setModalOpen(true)
  }

  const openEdit = (task) => {
    setEditingId(task.id)
    setForm({
      title: task.title,
      description: task.description || '',
      departmentId: String(task.department.id),
      assignedToEmployeeId: '',
      priority: task.priority,
      startDate: task.startDate,
      dueDate: task.dueDate,
    })
    setFormErrors({})
    setModalOpen(true)
  }

  const validate = () => {
    const errs = {}
    if (!form.title.trim()) errs.title = 'Title is required.'
    if (!form.departmentId) errs.departmentId = 'Department is required.'
    if (!form.startDate) errs.startDate = 'Start date is required.'
    if (!form.dueDate) errs.dueDate = 'Due date is required.'
    if (form.startDate && form.dueDate && form.startDate > form.dueDate) errs.dueDate = 'Due date cannot be before the start date.'
    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSave = async (e) => {
    e.preventDefault()
    if (!validate()) return

    setSaving(true)
    try {
      if (editingId) {
        await updateTask(editingId, {
          title: form.title, description: form.description, departmentId: Number(form.departmentId),
          priority: form.priority, startDate: form.startDate, dueDate: form.dueDate,
        })
        toast.success('Task updated successfully.')
      } else {
        await createTask({
          title: form.title, description: form.description, departmentId: Number(form.departmentId),
          assignedToEmployeeId: form.assignedToEmployeeId ? Number(form.assignedToEmployeeId) : null,
          priority: form.priority, startDate: form.startDate, dueDate: form.dueDate,
        })
        toast.success('Task created successfully.')
      }
      setModalOpen(false)
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save task.')
    } finally {
      setSaving(false)
    }
  }

  const openAssign = (task) => {
    setAssignTarget(task)
    setAssignEmployeeId(task.assignedTo?.id ? String(task.assignedTo.id) : '')
  }

  const handleAssign = async (e) => {
    e.preventDefault()
    if (!assignEmployeeId) {
      toast.error('Please select an employee.')
      return
    }
    setAssigning(true)
    try {
      await assignTask(assignTarget.id, { employeeId: Number(assignEmployeeId) })
      toast.success('Task assigned successfully.')
      setAssignTarget(null)
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to assign task.')
    } finally {
      setAssigning(false)
    }
  }

  const handleConfirm = async () => {
    if (!confirmTarget) return
    setConfirmLoading(true)
    try {
      if (confirmTarget.type === 'cancel') {
        await cancelTask(confirmTarget.task.id, 'Cancelled by ' + user.role.toLowerCase())
        toast.success('Task cancelled.')
      } else {
        await deleteTask(confirmTarget.task.id)
        toast.success('Task deleted.')
      }
      setConfirmTarget(null)
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed.')
    } finally {
      setConfirmLoading(false)
    }
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">Task Management</h1>
          <p className="text-sm text-slate-500 mt-1">Create, assign and track tasks across departments.</p>
        </div>
        <button className="btn-primary" onClick={openCreate}>
          <Plus className="w-4 h-4" /> Create Task
        </button>
      </div>

      <div className="card p-4 mb-4 flex flex-col sm:flex-row gap-3 flex-wrap">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input className="input pl-9" placeholder="Search by title or task ID..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <select className="input sm:w-44" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All Status</option>
          {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
        </select>
        <select className="input sm:w-36" value={priorityFilter} onChange={(e) => setPriorityFilter(e.target.value)}>
          <option value="">All Priority</option>
          {PRIORITY_OPTIONS.map((p) => <option key={p} value={p}>{p}</option>)}
        </select>
        <select className="input sm:w-52" value={deptFilter} onChange={(e) => setDeptFilter(e.target.value)}>
          <option value="">All Departments</option>
          {departments.map((d) => <option key={d.id} value={d.id}>{d.departmentName}</option>)}
        </select>
      </div>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Loading tasks..." />
        ) : error ? (
          <ErrorState message="Could not load tasks." onRetry={load} />
        ) : tasks.length === 0 ? (
          <EmptyState title="No tasks found" description="Try adjusting your filters, or create a new task." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Task ID</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Title</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Assigned To</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Department</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Priority</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Due Date</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500 w-32">Progress</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Status</th>
                  <th className="text-right px-5 py-3 font-medium text-slate-500">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {tasks.map((task) => (
                  <tr key={task.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3 font-medium text-slate-700">{task.taskCode}</td>
                    <td className="px-5 py-3 text-slate-700 max-w-[220px] truncate">{task.title}</td>
                    <td className="px-5 py-3 text-slate-500">{task.assignedTo?.fullName || '—'}</td>
                    <td className="px-5 py-3 text-slate-500">{task.department.departmentName}</td>
                    <td className="px-5 py-3"><TaskPriorityBadge priority={task.priority} /></td>
                    <td className={`px-5 py-3 ${task.overdue ? 'text-red-600 font-medium' : 'text-slate-500'}`}>{task.dueDate}</td>
                    <td className="px-5 py-3"><ProgressBar value={task.progressPercentage} /></td>
                    <td className="px-5 py-3"><TaskStatusBadge status={task.overdue ? 'OVERDUE' : task.status} /></td>
                    <td className="px-5 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button className="p-1.5 text-slate-400 hover:text-navy-600" title="View" onClick={() => navigate(`/tasks/${task.id}`)}>
                          <Eye className="w-4 h-4" />
                        </button>
                        {['PENDING', 'ASSIGNED', 'IN_PROGRESS'].includes(task.status) && (
                          <button className="p-1.5 text-slate-400 hover:text-navy-600" title="Assign / Reassign" onClick={() => openAssign(task)}>
                            <UserPlus className="w-4 h-4" />
                          </button>
                        )}
                        {['PENDING', 'ASSIGNED', 'IN_PROGRESS', 'REJECTED'].includes(task.status) && (
                          <button className="p-1.5 text-slate-400 hover:text-navy-600" title="Edit" onClick={() => openEdit(task)}>
                            <Pencil className="w-4 h-4" />
                          </button>
                        )}
                        {!['APPROVED', 'CANCELLED'].includes(task.status) && (
                          <button className="p-1.5 text-slate-400 hover:text-amber-600" title="Cancel" onClick={() => setConfirmTarget({ type: 'cancel', task })}>
                            <Ban className="w-4 h-4" />
                          </button>
                        )}
                        {isAdmin && ['PENDING', 'CANCELLED'].includes(task.status) && (
                          <button className="p-1.5 text-slate-400 hover:text-red-600" title="Delete" onClick={() => setConfirmTarget({ type: 'delete', task })}>
                            <Trash2 className="w-4 h-4" />
                          </button>
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

      {/* Create / Edit modal */}
      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editingId ? 'Edit Task' : 'Create Task'} size="lg">
        <form onSubmit={handleSave} noValidate>
          <FormField label="Title" error={formErrors.title} required>
            <input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          </FormField>
          <FormField label="Description">
            <textarea className="input" rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </FormField>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4">
            <FormField label="Department" error={formErrors.departmentId} required>
              <select className="input" value={form.departmentId} onChange={(e) => setForm({ ...form, departmentId: e.target.value, assignedToEmployeeId: '' })} disabled={!!editingId}>
                <option value="">Select department</option>
                {activeDepartments.map((d) => <option key={d.id} value={d.id}>{d.departmentName}</option>)}
              </select>
            </FormField>
            <FormField label="Priority" required>
              <select className="input" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
                {PRIORITY_OPTIONS.map((p) => <option key={p} value={p}>{p}</option>)}
              </select>
            </FormField>
            <FormField label="Start Date" error={formErrors.startDate} required>
              <input className="input" type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
            </FormField>
            <FormField label="Due Date" error={formErrors.dueDate} required>
              <input className="input" type="date" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
            </FormField>
          </div>
          {!editingId && (
            <FormField label="Assign To (optional)">
              <select className="input" value={form.assignedToEmployeeId} onChange={(e) => setForm({ ...form, assignedToEmployeeId: e.target.value })} disabled={!form.departmentId}>
                <option value="">Leave unassigned for now</option>
                {employeesInDept.map((emp) => <option key={emp.id} value={emp.id}>{emp.fullName}</option>)}
              </select>
            </FormField>
          )}
          <div className="flex justify-end gap-3 mt-6">
            <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)} disabled={saving}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Saving...' : editingId ? 'Save Changes' : 'Create Task'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Assign modal */}
      <Modal open={!!assignTarget} onClose={() => setAssignTarget(null)} title={`Assign: ${assignTarget?.taskCode || ''}`}>
        <form onSubmit={handleAssign} noValidate>
          <FormField label="Employee" required>
            <select className="input" value={assignEmployeeId} onChange={(e) => setAssignEmployeeId(e.target.value)}>
              <option value="">Select employee</option>
              {assignEligibleEmployees.map((emp) => <option key={emp.id} value={emp.id}>{emp.fullName}</option>)}
            </select>
          </FormField>
          <p className="text-xs text-slate-400 mb-4">Only active employees within this task's department are shown.</p>
          <div className="flex justify-end gap-3">
            <button type="button" className="btn-secondary" onClick={() => setAssignTarget(null)} disabled={assigning}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={assigning}>{assigning ? 'Assigning...' : 'Assign'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!confirmTarget}
        onClose={() => setConfirmTarget(null)}
        onConfirm={handleConfirm}
        loading={confirmLoading}
        title={confirmTarget?.type === 'delete' ? 'Delete Task' : 'Cancel Task'}
        message={
          confirmTarget?.type === 'delete'
            ? `This will permanently delete ${confirmTarget?.task.taskCode}.`
            : `This will cancel ${confirmTarget?.task.taskCode}. This cannot be undone.`
        }
        confirmLabel={confirmTarget?.type === 'delete' ? 'Delete' : 'Cancel Task'}
      />
    </div>
  )
}
