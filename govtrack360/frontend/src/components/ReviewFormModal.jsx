import React, { useEffect, useState } from 'react'
import Modal from './Modal'
import FormField from './FormField'
import Spinner from './Spinner'
import RatingInput from './RatingInput'
import { useToast } from './ToastContext'
import { getEligibleEmployees, createReview, updateReview } from '../services/performanceService'
import { apiError, toISODate, formatDate } from '../utils/performance'

function previousMonthRange() {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
  const end = new Date(now.getFullYear(), now.getMonth(), 0)
  return { start: toISODate(start), end: toISODate(end) }
}

const buildForm = (review, presetEmployeeId) => {
  if (review) {
    return {
      employeeId: String(review.employee.id),
      periodStart: review.periodStart,
      periodEnd: review.periodEnd,
      qualityRating: review.qualityRating,
      teamworkRating: review.teamworkRating,
      initiativeRating: review.initiativeRating,
      strengths: review.strengths || '',
      areasForImprovement: review.areasForImprovement || '',
      comments: review.comments || '',
    }
  }
  const prev = previousMonthRange()
  return {
    employeeId: presetEmployeeId ? String(presetEmployeeId) : '',
    periodStart: prev.start, periodEnd: prev.end,
    qualityRating: 3, teamworkRating: 3, initiativeRating: 3,
    strengths: '', areasForImprovement: '', comments: '',
  }
}

// Create mode when `review` is null; edit mode otherwise (employee and period are then fixed).
export default function ReviewFormModal({ open, onClose, onSaved, review = null, presetEmployeeId = null }) {
  const toast = useToast()
  const editing = !!review
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [form, setForm] = useState(buildForm(review, presetEmployeeId))
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open) return
    setForm(buildForm(review, presetEmployeeId))
    setErrors({})
    if (editing) return
    let cancelled = false
    const load = async () => {
      setLoading(true)
      setLoadError(false)
      try {
        const emps = await getEligibleEmployees()
        if (!cancelled) setEmployees(emps)
      } catch (err) {
        if (!cancelled) setLoadError(true)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [open, review, presetEmployeeId, editing])

  const validate = () => {
    const errs = {}
    if (!editing) {
      if (!form.employeeId) errs.employeeId = 'Select an employee.'
      if (!form.periodStart) errs.periodStart = 'Start date is required.'
      if (!form.periodEnd) errs.periodEnd = 'End date is required.'
      if (form.periodStart && form.periodEnd && form.periodStart > form.periodEnd) errs.periodEnd = 'End date cannot be before the start date.'
      if (form.periodEnd && form.periodEnd > toISODate(new Date())) errs.periodEnd = 'The period cannot end in the future.'
    }
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return
    setSaving(true)
    const texts = {
      strengths: form.strengths.trim() || null,
      areasForImprovement: form.areasForImprovement.trim() || null,
      comments: form.comments.trim() || null,
    }
    const ratings = {
      qualityRating: form.qualityRating,
      teamworkRating: form.teamworkRating,
      initiativeRating: form.initiativeRating,
    }
    try {
      if (editing) {
        await updateReview(review.id, { ...ratings, ...texts })
        toast.success('Review updated.')
      } else {
        await createReview({
          employeeId: Number(form.employeeId),
          periodStart: form.periodStart,
          periodEnd: form.periodEnd,
          ...ratings, ...texts,
        })
        toast.success('Review saved.')
      }
      onSaved && onSaved()
      onClose()
    } catch (err) {
      toast.error(apiError(err, 'Failed to save review.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={editing ? 'Edit performance review' : 'New performance review'} size="lg">
      {loading ? (
        <Spinner label="Loading..." />
      ) : loadError ? (
        <p className="text-sm text-red-600">Could not load employees. Close this dialog and try again.</p>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          {editing ? (
            <div className="mb-4 rounded-md bg-slate-50 border border-slate-200 px-3 py-2 text-sm text-slate-600">
              <span className="font-medium text-slate-800">{review.employee.fullName}</span>
              {' · '}{formatDate(review.periodStart)} – {formatDate(review.periodEnd)}
            </div>
          ) : (
            <>
              <FormField label="Employee" required error={errors.employeeId}>
                <select className="input" value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
                  <option value="">Select employee...</option>
                  {employees.map((emp) => (
                    <option key={emp.id} value={emp.id}>{emp.fullName} — {emp.departmentName}</option>
                  ))}
                </select>
              </FormField>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4">
                <FormField label="Period start" required error={errors.periodStart}>
                  <input type="date" className="input" value={form.periodStart} onChange={(e) => setForm({ ...form, periodStart: e.target.value })} />
                </FormField>
                <FormField label="Period end" required error={errors.periodEnd}>
                  <input type="date" className="input" max={toISODate(new Date())} value={form.periodEnd} onChange={(e) => setForm({ ...form, periodEnd: e.target.value })} />
                </FormField>
              </div>
            </>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
            {[
              ['qualityRating', 'Quality of work'],
              ['teamworkRating', 'Teamwork'],
              ['initiativeRating', 'Initiative'],
            ].map(([key, label]) => (
              <div key={key}>
                <p className="label">{label}</p>
                <RatingInput label={label} value={form[key]} onChange={(n) => setForm({ ...form, [key]: n })} />
              </div>
            ))}
          </div>
          <p className="text-xs text-slate-400 -mt-2 mb-4">
            The final score is 70% system score (tasks and attendance for the period) and 30% your ratings.
          </p>

          <FormField label="Strengths">
            <textarea className="input" rows={2} maxLength={1000} value={form.strengths} onChange={(e) => setForm({ ...form, strengths: e.target.value })} />
          </FormField>
          <FormField label="Areas for improvement">
            <textarea className="input" rows={2} maxLength={1000} value={form.areasForImprovement} onChange={(e) => setForm({ ...form, areasForImprovement: e.target.value })} />
          </FormField>
          <FormField label="Overall comments">
            <textarea className="input" rows={2} maxLength={1000} value={form.comments} onChange={(e) => setForm({ ...form, comments: e.target.value })} />
          </FormField>

          <div className="flex justify-end gap-3 mt-2">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : editing ? 'Save changes' : 'Save review'}</button>
          </div>
        </form>
      )}
    </Modal>
  )
}
