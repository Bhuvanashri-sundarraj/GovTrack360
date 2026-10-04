import React, { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { ArrowLeft, Send, CheckCircle2, XCircle, MessageSquare, History } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import TaskStatusBadge from '../components/TaskStatusBadge'
import TaskPriorityBadge from '../components/TaskPriorityBadge'
import ProgressBar from '../components/ProgressBar'
import Modal from '../components/Modal'
import {
  getTask, getTaskComments, addTaskComment, getTaskHistory,
  updateTaskProgress, submitTask, reviewTask,
} from '../services/taskService'

const PROGRESS_EDITABLE = ['ASSIGNED', 'IN_PROGRESS', 'REJECTED']

export default function TaskDetails() {
  const { id } = useParams()
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const [task, setTask] = useState(null)
  const [comments, setComments] = useState([])
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [progressValue, setProgressValue] = useState(0)
  const [progressComment, setProgressComment] = useState('')
  const [savingProgress, setSavingProgress] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const [newComment, setNewComment] = useState('')
  const [postingComment, setPostingComment] = useState(false)

  const [reviewModal, setReviewModal] = useState(null) // 'APPROVE' | 'REJECT' | null
  const [reviewComment, setReviewComment] = useState('')
  const [reviewing, setReviewing] = useState(false)

  const isAssignee = task?.assignedTo && user?.employeeCode === task.assignedTo.employeeId;
  const canReview = (user?.role === 'ADMIN' || user?.role === 'OFFICER')
    && (task?.status === 'SUBMITTED' || task?.status === 'UNDER_REVIEW')

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const [taskData, commentData, historyData] = await Promise.all([
        getTask(id), getTaskComments(id), getTaskHistory(id),
      ])
      setTask(taskData)
      setProgressValue(taskData.progressPercentage)
      setComments(commentData)
      setHistory(historyData)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [id]) // eslint-disable-line

  const handleSaveProgress = async () => {
    setSavingProgress(true)
    try {
      const updated = await updateTaskProgress(id, { progressPercentage: Number(progressValue), comment: progressComment || undefined })
      setTask(updated)
      setProgressComment('')
      toast.success('Progress updated.')
      const historyData = await getTaskHistory(id)
      setHistory(historyData)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update progress.')
    } finally {
      setSavingProgress(false)
    }
  }

  const handleSubmit = async () => {
    setSubmitting(true)
    try {
      const updated = await submitTask(id, { comment: undefined })
      setTask(updated)
      toast.success('Task submitted for review.')
      const historyData = await getTaskHistory(id)
      setHistory(historyData)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit task.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleAddComment = async (e) => {
    e.preventDefault()
    if (!newComment.trim()) return
    setPostingComment(true)
    try {
      const created = await addTaskComment(id, newComment.trim())
      setComments((prev) => [...prev, created])
      setNewComment('')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to add comment.')
    } finally {
      setPostingComment(false)
    }
  }

  const handleReview = async (e) => {
    e.preventDefault()
    if (reviewModal === 'REJECT' && !reviewComment.trim()) {
      toast.error('A comment is required when rejecting a task.')
      return
    }
    setReviewing(true)
    try {
      const updated = await reviewTask(id, { decision: reviewModal, comment: reviewComment || undefined })
      setTask(updated)
      toast.success(reviewModal === 'APPROVE' ? 'Task approved.' : 'Task rejected.')
      setReviewModal(null)
      setReviewComment('')
      const historyData = await getTaskHistory(id)
      setHistory(historyData)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to review task.')
    } finally {
      setReviewing(false)
    }
  }

  if (loading) return <Spinner label="Loading task..." />
  if (error || !task) return <ErrorState message="Could not load this task." onRetry={load} />

  const canEditProgress = user?.role === 'EMPLOYEE' && isAssignee && PROGRESS_EDITABLE.includes(task.status)
  const canSubmit = user?.role === 'EMPLOYEE' && isAssignee && task.status === 'IN_PROGRESS' && task.progressPercentage >= 100

  return (
    <div className="max-w-4xl">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 mb-4">
        <ArrowLeft className="w-4 h-4" /> Back
      </button>

      <div className="card p-6 mb-6">
        <div className="flex flex-wrap items-start justify-between gap-3 mb-4">
          <div>
            <p className="text-xs font-medium text-slate-400">{task.taskCode}</p>
            <h1 className="text-xl font-semibold text-slate-800 mt-0.5">{task.title}</h1>
          </div>
          <div className="flex items-center gap-2">
            <TaskPriorityBadge priority={task.priority} />
            <TaskStatusBadge status={task.overdue ? 'OVERDUE' : task.status} />
          </div>
        </div>

        {task.description && <p className="text-sm text-slate-600 mb-5">{task.description}</p>}

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 text-sm mb-5">
          <div><p className="text-slate-400">Department</p><p className="font-medium text-slate-700">{task.department.departmentName}</p></div>
          <div><p className="text-slate-400">Assigned To</p><p className="font-medium text-slate-700">{task.assignedTo?.fullName || 'Unassigned'}</p></div>
          <div><p className="text-slate-400">Assigned By</p><p className="font-medium text-slate-700">{task.assignedBy?.fullName || task.assignedBy?.email}</p></div>
          <div><p className="text-slate-400">Start Date</p><p className="font-medium text-slate-700">{task.startDate}</p></div>
          <div><p className="text-slate-400">Due Date</p><p className={`font-medium ${task.overdue ? 'text-red-600' : 'text-slate-700'}`}>{task.dueDate}</p></div>
          {task.reviewedBy && (
            <div><p className="text-slate-400">Reviewed By</p><p className="font-medium text-slate-700">{task.reviewedBy.fullName || task.reviewedBy.email}</p></div>
          )}
        </div>

        <div className="mb-2 flex items-center justify-between">
          <p className="text-sm font-medium text-slate-700">Progress</p>
          <p className="text-sm text-slate-500">{task.progressPercentage}%</p>
        </div>
        <ProgressBar value={task.progressPercentage} />

        {task.reviewComment && (
          <div className={`mt-4 rounded-md px-3 py-2 text-sm ${task.status === 'REJECTED' ? 'bg-red-50 text-red-700' : 'bg-emerald-50 text-emerald-700'}`}>
            <span className="font-medium">Review note: </span>{task.reviewComment}
          </div>
        )}

        {/* Employee progress controls */}
        {canEditProgress && (
          <div className="mt-6 pt-5 border-t border-slate-200">
            <p className="text-sm font-medium text-slate-700 mb-3">Update Progress</p>
            <div className="flex items-center gap-3 mb-3">
              <input
                type="range" min="0" max="100" value={progressValue}
                onChange={(e) => setProgressValue(e.target.value)}
                className="flex-1"
              />
              <input
                type="number" min="0" max="100" value={progressValue}
                onChange={(e) => setProgressValue(Math.max(0, Math.min(100, Number(e.target.value))))}
                className="input w-20"
              />
            </div>
            <input
              className="input mb-3"
              placeholder="Optional comment about this update..."
              value={progressComment}
              onChange={(e) => setProgressComment(e.target.value)}
            />
            <div className="flex flex-wrap gap-3">
              <button className="btn-secondary" onClick={handleSaveProgress} disabled={savingProgress}>
                {savingProgress ? 'Saving...' : 'Save Progress'}
              </button>
              {canSubmit && (
                <button className="btn-primary" onClick={handleSubmit} disabled={submitting}>
                  <Send className="w-4 h-4" /> {submitting ? 'Submitting...' : 'Submit Task'}
                </button>
              )}
            </div>
            {task.status === 'IN_PROGRESS' && task.progressPercentage < 100 && (
              <p className="text-xs text-slate-400 mt-2">Set progress to 100% to enable submission.</p>
            )}
          </div>
        )}

        {/* Reviewer actions */}
        {canReview && (
          <div className="mt-6 pt-5 border-t border-slate-200">
            <p className="text-sm font-medium text-slate-700 mb-3">Review This Task</p>
            <div className="flex flex-wrap gap-3">
              <button className="btn-primary" onClick={() => setReviewModal('APPROVE')}>
                <CheckCircle2 className="w-4 h-4" /> Approve
              </button>
              <button className="btn-danger" onClick={() => setReviewModal('REJECT')}>
                <XCircle className="w-4 h-4" /> Reject
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Comments */}
      <div className="card p-6 mb-6">
        <div className="flex items-center gap-2 mb-4">
          <MessageSquare className="w-4 h-4 text-navy-600" />
          <h2 className="text-sm font-semibold text-slate-800">Comments</h2>
        </div>
        <div className="space-y-3 mb-4">
          {comments.length === 0 && <p className="text-sm text-slate-400">No comments yet.</p>}
          {comments.map((c) => (
            <div key={c.id} className="bg-slate-50 rounded-md px-3 py-2">
              <div className="flex items-center justify-between mb-0.5">
                <p className="text-sm font-medium text-slate-700">{c.author.fullName || c.author.email}</p>
                <p className="text-[11px] text-slate-400">{new Date(c.createdAt).toLocaleString()}</p>
              </div>
              <p className="text-sm text-slate-600">{c.comment}</p>
            </div>
          ))}
        </div>
        <form onSubmit={handleAddComment} className="flex gap-2">
          <input className="input flex-1" placeholder="Add a comment..." value={newComment} onChange={(e) => setNewComment(e.target.value)} />
          <button type="submit" className="btn-secondary" disabled={postingComment}>{postingComment ? 'Posting...' : 'Post'}</button>
        </form>
      </div>

      {/* Workflow history */}
      <div className="card p-6">
        <div className="flex items-center gap-2 mb-4">
          <History className="w-4 h-4 text-navy-600" />
          <h2 className="text-sm font-semibold text-slate-800">Workflow History</h2>
        </div>
        <ol className="space-y-4">
          {history.map((h) => (
            <li key={h.id} className="flex gap-3">
              <div className="w-2 h-2 rounded-full bg-navy-500 mt-1.5 flex-shrink-0" />
              <div>
                <p className="text-sm font-medium text-slate-700">{h.action.replace(/_/g, ' ')}</p>
                <p className="text-xs text-slate-500">
                  {h.performedBy.fullName || h.performedBy.email} · {new Date(h.timestamp).toLocaleString()}
                </p>
                {h.comment && <p className="text-xs text-slate-500 mt-1">"{h.comment}"</p>}
              </div>
            </li>
          ))}
        </ol>
      </div>

      <Modal open={!!reviewModal} onClose={() => setReviewModal(null)} title={reviewModal === 'APPROVE' ? 'Approve Task' : 'Reject Task'}>
        <form onSubmit={handleReview}>
          <p className="text-sm text-slate-600 mb-4">
            {reviewModal === 'APPROVE'
              ? 'Approving marks this task as complete. This cannot be undone.'
              : 'Rejecting sends this task back to the employee. A comment explaining why is required.'}
          </p>
          <textarea
            className="input mb-4"
            rows={3}
            placeholder={reviewModal === 'REJECT' ? 'Explain what needs to change...' : 'Optional comment...'}
            value={reviewComment}
            onChange={(e) => setReviewComment(e.target.value)}
          />
          <div className="flex justify-end gap-3">
            <button type="button" className="btn-secondary" onClick={() => setReviewModal(null)} disabled={reviewing}>Cancel</button>
            <button type="submit" className={reviewModal === 'APPROVE' ? 'btn-primary' : 'btn-danger'} disabled={reviewing}>
              {reviewing ? 'Saving...' : reviewModal === 'APPROVE' ? 'Confirm Approval' : 'Confirm Rejection'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
