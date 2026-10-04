import React, { useEffect, useState } from 'react'
import Modal from './Modal'
import FormField from './FormField'
import Spinner from './Spinner'
import { useToast } from './ToastContext'
import { getEligibleEmployees } from '../services/performanceService'
import { getRewardTypes, grantReward } from '../services/rewardService'
import { apiError, toISODate } from '../utils/performance'

const emptyForm = () => ({
  employeeId: '', type: '', title: '', points: '', awardedDate: toISODate(new Date()), citation: '',
})

export default function GrantRewardModal({ open, onClose, onGranted, presetEmployeeId = null }) {
  const toast = useToast()
  const [employees, setEmployees] = useState([])
  const [types, setTypes] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [form, setForm] = useState(emptyForm())
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open) return
    let cancelled = false
    const load = async () => {
      setLoading(true)
      setLoadError(false)
      try {
        const [emps, rewardTypes] = await Promise.all([getEligibleEmployees(), getRewardTypes()])
        if (cancelled) return
        setEmployees(emps)
        setTypes(rewardTypes)
        const first = rewardTypes[0]
        setForm({
          ...emptyForm(),
          employeeId: presetEmployeeId ? String(presetEmployeeId) : '',
          type: first ? first.value : '',
          title: first ? first.label : '',
          points: first ? String(first.defaultPoints) : '',
        })
        setErrors({})
      } catch (err) {
        if (!cancelled) setLoadError(true)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [open, presetEmployeeId])

  const handleTypeChange = (value) => {
    const previous = types.find((t) => t.value === form.type)
    const next = types.find((t) => t.value === value)
    setForm((f) => ({
      ...f,
      type: value,
      points: next ? String(next.defaultPoints) : f.points,
      // Only replace the title if the user has not customised it.
      title: !f.title.trim() || (previous && f.title === previous.label) ? (next ? next.label : f.title) : f.title,
    }))
  }

  const validate = () => {
    const errs = {}
    if (!form.employeeId) errs.employeeId = 'Select an employee.'
    if (!form.type) errs.type = 'Select a reward type.'
    if (!form.title.trim()) errs.title = 'Title is required.'
    const points = Number(form.points)
    if (!Number.isInteger(points) || points < 1 || points > 100) errs.points = 'Points must be a whole number from 1 to 100.'
    if (!form.awardedDate) errs.awardedDate = 'Award date is required.'
    else if (form.awardedDate > toISODate(new Date())) errs.awardedDate = 'The award date cannot be in the future.'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return
    setSaving(true)
    try {
      await grantReward({
        employeeId: Number(form.employeeId),
        type: form.type,
        title: form.title.trim(),
        citation: form.citation.trim() || null,
        points: Number(form.points),
        awardedDate: form.awardedDate,
      })
      toast.success('Reward granted.')
      onGranted && onGranted()
      onClose()
    } catch (err) {
      toast.error(apiError(err, 'Failed to grant reward.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Grant a reward">
      {loading ? (
        <Spinner label="Loading..." />
      ) : loadError ? (
        <p className="text-sm text-red-600">Could not load employees and reward types. Close this dialog and try again.</p>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          <FormField label="Employee" required error={errors.employeeId}>
            <select className="input" value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
              <option value="">Select employee...</option>
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>{emp.fullName} — {emp.departmentName}</option>
              ))}
            </select>
          </FormField>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4">
            <FormField label="Reward type" required error={errors.type}>
              <select className="input" value={form.type} onChange={(e) => handleTypeChange(e.target.value)}>
                {types.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </FormField>
            <FormField label="Points (1-100)" required error={errors.points}>
              <input type="number" min="1" max="100" className="input" value={form.points}
                onChange={(e) => setForm({ ...form, points: e.target.value })} />
            </FormField>
          </div>

          <FormField label="Title" required error={errors.title}>
            <input className="input" maxLength={150} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          </FormField>

          <FormField label="Award date" required error={errors.awardedDate}>
            <input type="date" className="input" max={toISODate(new Date())} value={form.awardedDate}
              onChange={(e) => setForm({ ...form, awardedDate: e.target.value })} />
          </FormField>

          <FormField label="Citation (why this reward?)">
            <textarea className="input" rows={3} maxLength={1000} value={form.citation}
              onChange={(e) => setForm({ ...form, citation: e.target.value })} />
          </FormField>

          <div className="flex justify-end gap-3 mt-2">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Granting...' : 'Grant reward'}</button>
          </div>
        </form>
      )}
    </Modal>
  )
}
