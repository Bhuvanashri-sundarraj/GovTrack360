import React, { useEffect, useState } from 'react'
import { Plus, X } from 'lucide-react'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import LeaveStatusBadge from '../components/LeaveStatusBadge'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import FormField from '../components/FormField'
import { applyLeave, getMyLeaveRequests, cancelLeaveRequest } from '../services/leaveService'

const emptyForm = { leaveType: 'CASUAL_LEAVE', startDate: '', endDate: '', reason: '' }
const LEAVE_TYPES = ['CASUAL_LEAVE', 'SICK_LEAVE', 'EARNED_LEAVE', 'OTHER']

export default function MyLeave() {
  const toast = useToast()
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [formErrors, setFormErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const [cancelTarget, setCancelTarget] = useState(null)
  const [cancelLoading, setCancelLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const res = await getMyLeaveRequests()
      setRequests(res)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const openApply = () => {
    setForm(emptyForm)
    setFormErrors({})
    setModalOpen(true)
  }

  const validate = () => {
    const errs = {}
    if (!form.startDate) errs.startDate = 'Start date is required.'
    if (!form.endDate) errs.endDate = 'End date is required.'
    if (form.startDate && form.endDate && form.startDate > form.endDate) errs.endDate = 'End date cannot be before start date.'
    if (!form.reason.trim()) errs.reason = 'Reason is required.'
    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleApply = async (e) => {
    e.preventDefault()
    if (!validate()) return
    setSaving(true)
    try {
      await applyLeave(form)
      toast.success('Leave request submitted.')
      setModalOpen(false)
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit leave request.')
    } finally {
      setSaving(false)
    }
  }

  const handleCancel = async () => {
    setCancelLoading(true)
    try {
      await cancelLeaveRequest(cancelTarget.id)
      toast.success('Leave request cancelled.')
      setCancelTarget(null)
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to cancel request.')
    } finally {
      setCancelLoading(false)
    }
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">My Leave</h1>
          <p className="text-sm text-slate-500 mt-1">Apply for leave and track your requests.</p>
        </div>
        <button className="btn-primary" onClick={openApply}>
          <Plus className="w-4 h-4" /> Apply Leave
        </button>
      </div>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Loading your leave requests..." />
        ) : error ? (
          <ErrorState message="Could not load your leave requests." onRetry={load} />
        ) : requests.length === 0 ? (
          <EmptyState title="No leave requests yet" description="Apply for leave to see it listed here." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Type</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Start</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">End</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Days</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Reason</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Status</th>
                  <th className="text-right px-5 py-3 font-medium text-slate-500">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {requests.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3 text-slate-700">{r.leaveType.replace('_', ' ')}</td>
                    <td className="px-5 py-3 text-slate-500">{r.startDate}</td>
                    <td className="px-5 py-3 text-slate-500">{r.endDate}</td>
                    <td className="px-5 py-3 text-slate-500">{r.numberOfDays}</td>
                    <td className="px-5 py-3 text-slate-500 max-w-[200px] truncate">{r.reason}</td>
                    <td className="px-5 py-3"><LeaveStatusBadge status={r.status} /></td>
                    <td className="px-5 py-3 text-right">
                      {r.status === 'PENDING' && (
                        <button className="p-1.5 text-slate-400 hover:text-red-600" title="Cancel" onClick={() => setCancelTarget(r)}>
                          <X className="w-4 h-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="Apply for Leave">
        <form onSubmit={handleApply} noValidate>
          <FormField label="Leave Type" required>
            <select className="input" value={form.leaveType} onChange={(e) => setForm({ ...form, leaveType: e.target.value })}>
              {LEAVE_TYPES.map((t) => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
            </select>
          </FormField>
          <div className="grid grid-cols-2 gap-x-4">
            <FormField label="Start Date" error={formErrors.startDate} required>
              <input className="input" type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
            </FormField>
            <FormField label="End Date" error={formErrors.endDate} required>
              <input className="input" type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
            </FormField>
          </div>
          <FormField label="Reason" error={formErrors.reason} required>
            <textarea className="input" rows={3} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
          </FormField>
          <div className="flex justify-end gap-3 mt-6">
            <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)} disabled={saving}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Submitting...' : 'Submit Request'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!cancelTarget}
        onClose={() => setCancelTarget(null)}
        onConfirm={handleCancel}
        loading={cancelLoading}
        title="Cancel Leave Request"
        message="This will cancel your pending leave request."
        confirmLabel="Cancel Request"
      />
    </div>
  )
}
