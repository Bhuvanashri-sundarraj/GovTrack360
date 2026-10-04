import React, { useEffect, useState } from 'react'
import { CheckCircle2, XCircle } from 'lucide-react'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import LeaveStatusBadge from '../components/LeaveStatusBadge'
import Modal from '../components/Modal'
import { searchLeaveRequests, reviewLeaveRequest } from '../services/leaveService'
import { getDepartments } from '../services/departmentService'

export default function LeaveManagement() {
  const toast = useToast()
  const [requests, setRequests] = useState([])
  const [departments, setDepartments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [statusFilter, setStatusFilter] = useState('PENDING')
  const [departmentId, setDepartmentId] = useState('')

  const [reviewTarget, setReviewTarget] = useState(null)
  const [reviewDecision, setReviewDecision] = useState(null)
  const [reviewComment, setReviewComment] = useState('')
  const [reviewing, setReviewing] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const [res, deptData] = await Promise.all([
        searchLeaveRequests({ status: statusFilter || undefined, departmentId: departmentId || undefined }),
        getDepartments(),
      ])
      setRequests(res)
      setDepartments(deptData)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [statusFilter, departmentId]) // eslint-disable-line

  const openReview = (request, decision) => {
    setReviewTarget(request)
    setReviewDecision(decision)
    setReviewComment('')
  }

  const handleReview = async (e) => {
    e.preventDefault()
    if (reviewDecision === 'REJECT' && !reviewComment.trim()) {
      toast.error('A comment is required when rejecting a leave request.')
      return
    }
    setReviewing(true)
    try {
      await reviewLeaveRequest(reviewTarget.id, { decision: reviewDecision, comment: reviewComment || undefined })
      toast.success(reviewDecision === 'APPROVE' ? 'Leave approved.' : 'Leave rejected.')
      setReviewTarget(null)
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to review leave request.')
    } finally {
      setReviewing(false)
    }
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-800">Leave Management</h1>
        <p className="text-sm text-slate-500 mt-1">Review and act on employee leave requests.</p>
      </div>

      <div className="card p-4 mb-4 flex flex-col sm:flex-row gap-3">
        <select className="input sm:w-48" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All Status</option>
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <select className="input sm:w-56" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
          <option value="">All Departments</option>
          {departments.map((d) => <option key={d.id} value={d.id}>{d.departmentName}</option>)}
        </select>
      </div>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Loading leave requests..." />
        ) : error ? (
          <ErrorState message="Could not load leave requests." onRetry={load} />
        ) : requests.length === 0 ? (
          <EmptyState title="No leave requests found" description="Try adjusting your filters." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Employee</th>
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
                    <td className="px-5 py-3 text-slate-700">{r.employee.fullName}</td>
                    <td className="px-5 py-3 text-slate-500">{r.leaveType.replace('_', ' ')}</td>
                    <td className="px-5 py-3 text-slate-500">{r.startDate}</td>
                    <td className="px-5 py-3 text-slate-500">{r.endDate}</td>
                    <td className="px-5 py-3 text-slate-500">{r.numberOfDays}</td>
                    <td className="px-5 py-3 text-slate-500 max-w-[200px] truncate">{r.reason}</td>
                    <td className="px-5 py-3"><LeaveStatusBadge status={r.status} /></td>
                    <td className="px-5 py-3">
                      {r.status === 'PENDING' && (
                        <div className="flex items-center justify-end gap-1">
                          <button className="p-1.5 text-slate-400 hover:text-emerald-600" title="Approve" onClick={() => openReview(r, 'APPROVE')}>
                            <CheckCircle2 className="w-4 h-4" />
                          </button>
                          <button className="p-1.5 text-slate-400 hover:text-red-600" title="Reject" onClick={() => openReview(r, 'REJECT')}>
                            <XCircle className="w-4 h-4" />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal open={!!reviewTarget} onClose={() => setReviewTarget(null)} title={reviewDecision === 'APPROVE' ? 'Approve Leave Request' : 'Reject Leave Request'}>
        <form onSubmit={handleReview}>
          <p className="text-sm text-slate-600 mb-4">
            {reviewDecision === 'APPROVE'
              ? `Approve ${reviewTarget?.employee.fullName}'s leave request for ${reviewTarget?.numberOfDays} day(s)?`
              : `Reject ${reviewTarget?.employee.fullName}'s leave request? A reason is required.`}
          </p>
          <textarea
            className="input mb-4"
            rows={3}
            placeholder={reviewDecision === 'REJECT' ? 'Explain why this is being rejected...' : 'Optional comment...'}
            value={reviewComment}
            onChange={(e) => setReviewComment(e.target.value)}
          />
          <div className="flex justify-end gap-3">
            <button type="button" className="btn-secondary" onClick={() => setReviewTarget(null)} disabled={reviewing}>Cancel</button>
            <button type="submit" className={reviewDecision === 'APPROVE' ? 'btn-primary' : 'btn-danger'} disabled={reviewing}>
              {reviewing ? 'Saving...' : reviewDecision === 'APPROVE' ? 'Confirm Approval' : 'Confirm Rejection'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
