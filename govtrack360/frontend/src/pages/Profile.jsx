import React, { useEffect, useState } from 'react'
import { Pencil, Save, X } from 'lucide-react'
import { getProfile, updateProfile } from '../services/profileService'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import Badge from '../components/Badge'
import FormField from '../components/FormField'

export default function Profile() {
  const toast = useToast()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({ firstName: '', lastName: '', phone: '' })

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const data = await getProfile()
      setProfile(data)
      setForm({ firstName: data.firstName || '', lastName: data.lastName || '', phone: data.phone || '' })
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const updated = await updateProfile(form)
      setProfile(updated)
      setEditing(false)
      toast.success('Profile updated successfully.')
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to update profile.'
      toast.error(message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Spinner label="Loading profile..." />
  if (error) return <ErrorState message="Could not load your profile." onRetry={load} />

  return (
    <div className="max-w-2xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">My Profile</h1>
          <p className="text-sm text-slate-500 mt-1">View and update your personal information.</p>
        </div>
        {!editing && profile.employeeRecordId && (
          <button className="btn-secondary" onClick={() => setEditing(true)}>
            <Pencil className="w-4 h-4" /> Edit
          </button>
        )}
      </div>

      <div className="card p-6">
        {editing ? (
          <form onSubmit={handleSave} noValidate>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4">
              <FormField label="First Name">
                <input className="input" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
              </FormField>
              <FormField label="Last Name">
                <input className="input" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
              </FormField>
              <FormField label="Phone">
                <input className="input" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              </FormField>
            </div>
            <p className="text-xs text-slate-400 mb-4">
              Your email, employee ID, department and role cannot be changed here. Contact an administrator for those updates.
            </p>
            <div className="flex justify-end gap-3">
              <button type="button" className="btn-secondary" onClick={() => setEditing(false)} disabled={saving}>
                <X className="w-4 h-4" /> Cancel
              </button>
              <button type="submit" className="btn-primary" disabled={saving}>
                <Save className="w-4 h-4" /> {saving ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        ) : (
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-y-4 gap-x-6 text-sm">
            <div><dt className="text-slate-500">Full Name</dt><dd className="font-medium text-slate-800 mt-0.5">{profile.firstName ? `${profile.firstName} ${profile.lastName}` : '—'}</dd></div>
            <div><dt className="text-slate-500">Employee ID</dt><dd className="font-medium text-slate-800 mt-0.5">{profile.employeeId || '—'}</dd></div>
            <div><dt className="text-slate-500">Email</dt><dd className="font-medium text-slate-800 mt-0.5">{profile.email}</dd></div>
            <div><dt className="text-slate-500">Phone</dt><dd className="font-medium text-slate-800 mt-0.5">{profile.phone || '—'}</dd></div>
            <div><dt className="text-slate-500">Designation</dt><dd className="font-medium text-slate-800 mt-0.5">{profile.designation || '—'}</dd></div>
            <div><dt className="text-slate-500">Department</dt><dd className="font-medium text-slate-800 mt-0.5">{profile.departmentName || '—'}</dd></div>
            <div><dt className="text-slate-500">Role</dt><dd className="font-medium text-slate-800 mt-0.5">{profile.role}</dd></div>
            <div><dt className="text-slate-500">Joining Date</dt><dd className="font-medium text-slate-800 mt-0.5">{profile.joiningDate || '—'}</dd></div>
            <div><dt className="text-slate-500">Account Status</dt><dd className="mt-0.5"><Badge status={profile.accountEnabled ? 'ACTIVE' : 'INACTIVE'} /></dd></div>
            {profile.employeeRecordId && (
              <div><dt className="text-slate-500">Employment Status</dt><dd className="mt-0.5"><Badge status={profile.employmentStatus} /></dd></div>
            )}
          </dl>
        )}
      </div>
    </div>
  )
}
