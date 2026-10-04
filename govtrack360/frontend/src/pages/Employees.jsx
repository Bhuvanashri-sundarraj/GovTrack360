import React, { useEffect, useMemo, useState } from 'react'
import { Plus, Search, Pencil, Trash2, Power, Eye } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import Badge from '../components/Badge'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import FormField from '../components/FormField'
import {
  getEmployees, createEmployee, updateEmployee, updateEmployeeStatus, deleteEmployee,
} from '../services/employeeService'
import { getDepartments } from '../services/departmentService'

const emptyForm = {
  employeeId: '', firstName: '', lastName: '', email: '', phone: '',
  designation: '', departmentId: '', joiningDate: '',
}

export default function Employees() {
  const { user } = useAuth()
  const toast = useToast()
  const isAdmin = user?.role === 'ADMIN'

  const [employees, setEmployees] = useState([])
  const [departments, setDepartments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [search, setSearch] = useState('')
  const [deptFilter, setDeptFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formErrors, setFormErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const [viewEmployee, setViewEmployee] = useState(null)
  const [confirmTarget, setConfirmTarget] = useState(null) // { type: 'delete'|'status', employee }
  const [confirmLoading, setConfirmLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const [emps, depts] = await Promise.all([
        getEmployees({
          search: search || undefined,
          departmentId: deptFilter || undefined,
          status: statusFilter || undefined,
        }),
        getDepartments(),
      ])
      setEmployees(emps)
      setDepartments(depts)
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
  }, [search, deptFilter, statusFilter]) // eslint-disable-line

  const openCreate = () => {
    setEditingId(null)
    setForm(emptyForm)
    setFormErrors({})
    setModalOpen(true)
  }

  const openEdit = (emp) => {
    setEditingId(emp.id)
    setForm({
      employeeId: emp.employeeId,
      firstName: emp.firstName,
      lastName: emp.lastName,
      email: emp.email,
      phone: emp.phone || '',
      designation: emp.designation,
      departmentId: String(emp.department.id),
      joiningDate: emp.joiningDate,
    })
    setFormErrors({})
    setModalOpen(true)
  }

  const validate = () => {
    const errs = {}
    if (!form.employeeId.trim()) errs.employeeId = 'Employee ID is required.'
    if (!form.firstName.trim()) errs.firstName = 'First name is required.'
    if (!form.lastName.trim()) errs.lastName = 'Last name is required.'
    if (!form.email.trim()) errs.email = 'Email is required.'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) errs.email = 'Enter a valid email.'
    if (!form.designation.trim()) errs.designation = 'Designation is required.'
    if (!form.departmentId) errs.departmentId = 'Department is required.'
    if (!form.joiningDate) errs.joiningDate = 'Joining date is required.'
    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSave = async (e) => {
    e.preventDefault()
    if (!validate()) return

    setSaving(true)
    const payload = { ...form, departmentId: Number(form.departmentId) }

    try {
      if (editingId) {
        await updateEmployee(editingId, payload)
        toast.success('Employee updated successfully.')
      } else {
        await createEmployee(payload)
        toast.success('Employee created successfully.')
      }
      setModalOpen(false)
      load()
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to save employee.'
      toast.error(message)
    } finally {
      setSaving(false)
    }
  }

  const handleConfirm = async () => {
    if (!confirmTarget) return
    setConfirmLoading(true)
    try {
      if (confirmTarget.type === 'delete') {
        await deleteEmployee(confirmTarget.employee.id)
        toast.success('Employee deleted.')
      } else {
        const newStatus = confirmTarget.employee.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
        await updateEmployeeStatus(confirmTarget.employee.id, newStatus)
        toast.success(`Employee marked as ${newStatus.toLowerCase()}.`)
      }
      setConfirmTarget(null)
      load()
    } catch (err) {
      const message = err.response?.data?.message || 'Action failed.'
      toast.error(message)
    } finally {
      setConfirmLoading(false)
    }
  }

  const activeDepartments = useMemo(
    () => departments.filter((d) => d.status === 'ACTIVE'),
    [departments]
  )

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">Employee Management</h1>
          <p className="text-sm text-slate-500 mt-1">
            {isAdmin ? 'Create, edit and manage employee records.' : 'View employee records.'}
          </p>
        </div>
        {isAdmin && (
          <button className="btn-primary" onClick={openCreate}>
            <Plus className="w-4 h-4" /> Add Employee
          </button>
        )}
      </div>

      <div className="card p-4 mb-4 flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            className="input pl-9"
            placeholder="Search by name, email or employee ID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <select className="input sm:w-56" value={deptFilter} onChange={(e) => setDeptFilter(e.target.value)}>
          <option value="">All Departments</option>
          {departments.map((d) => (
            <option key={d.id} value={d.id}>{d.departmentName}</option>
          ))}
        </select>
        <select className="input sm:w-40" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>
      </div>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Loading employees..." />
        ) : error ? (
          <ErrorState message="Could not load employees." onRetry={load} />
        ) : employees.length === 0 ? (
          <EmptyState
            title="No employees found"
            description="Try adjusting your search or filters, or add a new employee."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Employee ID</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Name</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Email</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Department</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Designation</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Status</th>
                  <th className="text-right px-5 py-3 font-medium text-slate-500">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {employees.map((emp) => (
                  <tr key={emp.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3 font-medium text-slate-700">{emp.employeeId}</td>
                    <td className="px-5 py-3 text-slate-700">{emp.fullName}</td>
                    <td className="px-5 py-3 text-slate-500">{emp.email}</td>
                    <td className="px-5 py-3 text-slate-500">{emp.department.departmentName}</td>
                    <td className="px-5 py-3 text-slate-500">{emp.designation}</td>
                    <td className="px-5 py-3"><Badge status={emp.status} /></td>
                    <td className="px-5 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button className="p-1.5 text-slate-400 hover:text-navy-600" title="View" onClick={() => setViewEmployee(emp)}>
                          <Eye className="w-4 h-4" />
                        </button>
                        {isAdmin && (
                          <>
                            <button className="p-1.5 text-slate-400 hover:text-navy-600" title="Edit" onClick={() => openEdit(emp)}>
                              <Pencil className="w-4 h-4" />
                            </button>
                            <button
                              className="p-1.5 text-slate-400 hover:text-amber-600"
                              title={emp.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                              onClick={() => setConfirmTarget({ type: 'status', employee: emp })}
                            >
                              <Power className="w-4 h-4" />
                            </button>
                            <button
                              className="p-1.5 text-slate-400 hover:text-red-600"
                              title="Delete"
                              onClick={() => setConfirmTarget({ type: 'delete', employee: emp })}
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
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

      {/* Create / Edit modal */}
      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editingId ? 'Edit Employee' : 'Add Employee'} size="lg">
        <form onSubmit={handleSave} noValidate>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4">
            <FormField label="Employee ID" error={formErrors.employeeId} required>
              <input className="input" value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })} />
            </FormField>
            <FormField label="Designation" error={formErrors.designation} required>
              <input className="input" value={form.designation} onChange={(e) => setForm({ ...form, designation: e.target.value })} />
            </FormField>
            <FormField label="First Name" error={formErrors.firstName} required>
              <input className="input" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
            </FormField>
            <FormField label="Last Name" error={formErrors.lastName} required>
              <input className="input" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
            </FormField>
            <FormField label="Email" error={formErrors.email} required>
              <input className="input" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </FormField>
            <FormField label="Phone">
              <input className="input" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </FormField>
            <FormField label="Department" error={formErrors.departmentId} required>
              <select className="input" value={form.departmentId} onChange={(e) => setForm({ ...form, departmentId: e.target.value })}>
                <option value="">Select department</option>
                {activeDepartments.map((d) => (
                  <option key={d.id} value={d.id}>{d.departmentName}</option>
                ))}
              </select>
            </FormField>
            <FormField label="Joining Date" error={formErrors.joiningDate} required>
              <input className="input" type="date" value={form.joiningDate} onChange={(e) => setForm({ ...form, joiningDate: e.target.value })} />
            </FormField>
          </div>
          <div className="flex justify-end gap-3 mt-6">
            <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)} disabled={saving}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Saving...' : editingId ? 'Save Changes' : 'Create Employee'}
            </button>
          </div>
        </form>
      </Modal>

      {/* View details modal */}
      <Modal open={!!viewEmployee} onClose={() => setViewEmployee(null)} title="Employee Details">
        {viewEmployee && (
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between"><dt className="text-slate-500">Employee ID</dt><dd className="font-medium">{viewEmployee.employeeId}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Name</dt><dd className="font-medium">{viewEmployee.fullName}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Email</dt><dd className="font-medium">{viewEmployee.email}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Phone</dt><dd className="font-medium">{viewEmployee.phone || '—'}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Designation</dt><dd className="font-medium">{viewEmployee.designation}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Department</dt><dd className="font-medium">{viewEmployee.department.departmentName}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Joining Date</dt><dd className="font-medium">{viewEmployee.joiningDate}</dd></div>
            <div className="flex justify-between items-center"><dt className="text-slate-500">Status</dt><dd><Badge status={viewEmployee.status} /></dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Login Account</dt><dd className="font-medium">{viewEmployee.hasLoginAccount ? `Yes (${viewEmployee.role})` : 'No'}</dd></div>
          </dl>
        )}
      </Modal>

      <ConfirmDialog
        open={!!confirmTarget}
        onClose={() => setConfirmTarget(null)}
        onConfirm={handleConfirm}
        loading={confirmLoading}
        danger={confirmTarget?.type === 'delete'}
        title={confirmTarget?.type === 'delete' ? 'Delete Employee' : 'Change Status'}
        message={
          confirmTarget?.type === 'delete'
            ? `This will permanently delete ${confirmTarget?.employee.fullName} and cannot be undone.`
            : `This will mark ${confirmTarget?.employee.fullName} as ${confirmTarget?.employee.status === 'ACTIVE' ? 'inactive' : 'active'}.`
        }
        confirmLabel={confirmTarget?.type === 'delete' ? 'Delete' : 'Confirm'}
      />
    </div>
  )
}
