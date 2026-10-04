import React, { useEffect, useState } from 'react'
import { CheckCheck, Bell } from 'lucide-react'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import { getNotifications, markNotificationRead, markAllNotificationsRead } from '../services/notificationService'

const TYPE_STYLES = {
  TASK: 'bg-navy-50 text-navy-700',
  LEAVE: 'bg-purple-50 text-purple-700',
  ATTENDANCE: 'bg-amber-50 text-amber-700',
  SYSTEM: 'bg-slate-100 text-slate-600',
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleString()
}

export default function Notifications() {
  const toast = useToast()
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [filter, setFilter] = useState('ALL') // ALL | UNREAD

  const load = async () => {
    setLoading(true)
    setError(false)
    try {
      const res = await getNotifications()
      setNotifications(res)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleMarkRead = async (id) => {
    try {
      await markNotificationRead(id)
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)))
    } catch (err) {
      toast.error('Failed to mark notification as read.')
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsRead()
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
      toast.success('All notifications marked as read.')
    } catch (err) {
      toast.error('Failed to mark all as read.')
    }
  }

  const filtered = filter === 'UNREAD' ? notifications.filter((n) => !n.isRead) : notifications
  const unreadCount = notifications.filter((n) => !n.isRead).length

  if (loading) return <Spinner label="Loading notifications..." />
  if (error) return <ErrorState message="Could not load notifications." onRetry={load} />

  return (
    <div className="max-w-2xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">Notifications</h1>
          <p className="text-sm text-slate-500 mt-1">{unreadCount} unread</p>
        </div>
        {unreadCount > 0 && (
          <button className="btn-secondary" onClick={handleMarkAllRead}>
            <CheckCheck className="w-4 h-4" /> Mark all as read
          </button>
        )}
      </div>

      <div className="flex gap-2 mb-4">
        <button
          className={`px-3 py-1.5 rounded-md text-sm font-medium ${filter === 'ALL' ? 'bg-navy-700 text-white' : 'bg-white border border-slate-300 text-slate-600'}`}
          onClick={() => setFilter('ALL')}
        >
          All
        </button>
        <button
          className={`px-3 py-1.5 rounded-md text-sm font-medium ${filter === 'UNREAD' ? 'bg-navy-700 text-white' : 'bg-white border border-slate-300 text-slate-600'}`}
          onClick={() => setFilter('UNREAD')}
        >
          Unread
        </button>
      </div>

      <div className="card overflow-hidden">
        {filtered.length === 0 ? (
          <EmptyState title="No notifications" description={filter === 'UNREAD' ? "You're all caught up." : 'Nothing here yet.'} />
        ) : (
          <div className="divide-y divide-slate-100">
            {filtered.map((n) => (
              <div key={n.id} className={`px-5 py-4 flex items-start gap-3 ${!n.isRead ? 'bg-navy-50/40' : ''}`}>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${TYPE_STYLES[n.type] || 'bg-slate-100 text-slate-600'}`}>
                  <Bell className="w-4 h-4" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-3">
                    <p className={`text-sm ${!n.isRead ? 'font-semibold text-slate-800' : 'font-medium text-slate-600'}`}>{n.title}</p>
                    {!n.isRead && (
                      <button onClick={() => handleMarkRead(n.id)} className="text-xs text-navy-600 hover:underline flex-shrink-0">
                        Mark read
                      </button>
                    )}
                  </div>
                  <p className="text-sm text-slate-500 mt-0.5">{n.message}</p>
                  <p className="text-[11px] text-slate-400 mt-1">{formatDate(n.createdAt)}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
