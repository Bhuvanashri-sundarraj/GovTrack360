import React, { useEffect, useState } from 'react'
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
  getDepartments, createDepartment, updateDepartment, updateDepartmentStatus, deleteDepartment,
} from '../services/departmentService'

const emptyForm = { departmentCode: '', departmentName: '', description: '' }

export default function Departments() {
  const { user } = useAuth()
  const toast = useToast()
  const isAdmin = user?.role === 'ADMIN'

  const [departments, setDepartments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [search, setSearch] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formErrors, setFormErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const [viewDept, setViewDept] = useState(null)
  const [confirmTarget, setConfirmTarget] = useState(null)
  const [confirmLoading, setConfirmLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const depts = await getDepartments({ search: search || undefined })
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
  }, [search]) // eslint-disable-line

  const openCreate = () => {
    setEditingId(null)
    setForm(emptyForm)
    setFormErrors({})
    setModalOpen(true)
  }

  const openEdit = (dept) => {
    setEditingId(dept.id)
    setForm({
      departmentCode: dept.departmentCode,
      departmentName: dept.departmentName,
      description: dept.description || '',
    })
    setFormErrors({})
    setModalOpen(true)
  }

  const validate = () => {
    const errs = {}
    if (!form.departmentCode.trim()) errs.departmentCode = 'Department code is required.'
    if (!form.departmentName.trim()) errs.departmentName = 'Department name is required.'
    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSave = async (e) => {
    e.preventDefault()
    if (!validate()) return

    setSaving(true)
    try {
      if (editingId) {
        await updateDepartment(editingId, form)
        toast.success('Department updated successfully.')
      } else {
        await createDepartment(form)
        toast.success('Department created successfully.')
      }
      setModalOpen(false)
      load()
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to save department.'
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
        await deleteDepartment(confirmTarget.department.id)
        toast.success('Department deleted.')
      } else {
        const newStatus = confirmTarget.department.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
        await updateDepartmentStatus(confirmTarget.department.id, newStatus)
        toast.success(`Department marked as ${newStatus.toLowerCase()}.`)
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

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">Department Management</h1>
          <p className="text-sm text-slate-500 mt-1">
            {isAdmin ? 'Create, edit and manage departments.' : 'View department information.'}
          </p>
        </div>
        {isAdmin && (
          <button className="btn-primary" onClick={openCreate}>
            <Plus className="w-4 h-4" /> Add Department
          </button>
        )}
      </div>

      <div className="card p-4 mb-4">
        <div className="relative max-w-sm">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            className="input pl-9"
            placeholder="Search departments..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Loading departments..." />
        ) : error ? (
          <ErrorState message="Could not load departments." onRetry={load} />
        ) : departments.length === 0 ? (
          <EmptyState title="No departments found" description="Add a new department to get started." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Code</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Name</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Description</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Employees</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Status</th>
                  <th className="text-right px-5 py-3 font-medium text-slate-500">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {departments.map((dept) => (
                  <tr key={dept.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3 font-medium text-slate-700">{dept.departmentCode}</td>
                    <td className="px-5 py-3 text-slate-700">{dept.departmentName}</td>
                    <td className="px-5 py-3 text-slate-500 max-w-xs truncate">{dept.description || '—'}</td>
                    <td className="px-5 py-3 text-slate-500">{dept.employeeCount}</td>
                    <td className="px-5 py-3"><Badge status={dept.status} /></td>
                    <td className="px-5 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button className="p-1.5 text-slate-400 hover:text-navy-600" title="View" onClick={() => setViewDept(dept)}>
                          <Eye className="w-4 h-4" />
                        </button>
                        {isAdmin && (
                          <>
                            <button className="p-1.5 text-slate-400 hover:text-navy-600" title="Edit" onClick={() => openEdit(dept)}>
                              <Pencil className="w-4 h-4" />
                            </button>
                            <button
                              className="p-1.5 text-slate-400 hover:text-amber-600"
                              title={dept.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                              onClick={() => setConfirmTarget({ type: 'status', department: dept })}
                            >
                              <Power className="w-4 h-4" />
                            </button>
                            <button
                              className="p-1.5 text-slate-400 hover:text-red-600"
                              title="Delete"
                              onClick={() => setConfirmTarget({ type: 'delete', department: dept })}
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

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editingId ? 'Edit Department' : 'Add Department'}>
        <form onSubmit={handleSave} noValidate>
          <FormField label="Department Code" error={formErrors.departmentCode} required>
            <input className="input" value={form.departmentCode} onChange={(e) => setForm({ ...form, departmentCode: e.target.value })} />
          </FormField>
          <FormField label="Department Name" error={formErrors.departmentName} required>
            <input className="input" value={form.departmentName} onChange={(e) => setForm({ ...form, departmentName: e.target.value })} />
          </FormField>
          <FormField label="Description">
            <textarea className="input" rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </FormField>
          <div className="flex justify-end gap-3 mt-6">
            <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)} disabled={saving}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Saving...' : editingId ? 'Save Changes' : 'Create Department'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={!!viewDept} onClose={() => setViewDept(null)} title="Department Details">
        {viewDept && (
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between"><dt className="text-slate-500">Code</dt><dd className="font-medium">{viewDept.departmentCode}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Name</dt><dd className="font-medium">{viewDept.departmentName}</dd></div>
            <div className="flex justify-between"><dt className="text-slate-500">Employees</dt><dd className="font-medium">{viewDept.employeeCount}</dd></div>
            <div className="flex justify-between items-center"><dt className="text-slate-500">Status</dt><dd><Badge status={viewDept.status} /></dd></div>
            <div>
              <dt className="text-slate-500 mb-1">Description</dt>
              <dd className="font-medium">{viewDept.description || '—'}</dd>
            </div>
          </dl>
        )}
      </Modal>

      <ConfirmDialog
        open={!!confirmTarget}
        onClose={() => setConfirmTarget(null)}
        onConfirm={handleConfirm}
        loading={confirmLoading}
        danger={confirmTarget?.type === 'delete'}
        title={confirmTarget?.type === 'delete' ? 'Delete Department' : 'Change Status'}
        message={
          confirmTarget?.type === 'delete'
            ? `This will permanently delete ${confirmTarget?.department.departmentName}. Departments with employees assigned cannot be deleted.`
            : `This will mark ${confirmTarget?.department.departmentName} as ${confirmTarget?.department.status === 'ACTIVE' ? 'inactive' : 'active'}.`
        }
        confirmLabel={confirmTarget?.type === 'delete' ? 'Delete' : 'Confirm'}
      />
    </div>
  )
}
