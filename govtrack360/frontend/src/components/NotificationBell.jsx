import React, { useEffect, useRef, useState } from 'react'
import { Bell, CheckCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getNotifications, getUnreadCount, markNotificationRead, markAllNotificationsRead } from '../services/notificationService'

function timeAgo(dateString) {
  const diffMs = Date.now() - new Date(dateString).getTime()
  const mins = Math.floor(diffMs / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  return `${days}d ago`
}

export default function NotificationBell() {
  const [open, setOpen] = useState(false)
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const containerRef = useRef(null)

  const loadUnreadCount = async () => {
    try {
      const res = await getUnreadCount()
      setUnreadCount(res.unreadCount)
    } catch (err) {
      // silent fail — the bell just won't show a count this cycle
    }
  }

  const loadNotifications = async () => {
    setLoading(true)
    try {
      const res = await getNotifications()
      setNotifications(res)
    } catch (err) {
      // silent fail
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadUnreadCount()
    const interval = setInterval(loadUnreadCount, 30000)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    if (open) loadNotifications()
  }, [open])

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleMarkRead = async (id) => {
    try {
      await markNotificationRead(id)
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)))
      setUnreadCount((c) => Math.max(0, c - 1))
    } catch (err) {
      // silent fail
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsRead()
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
      setUnreadCount(0)
    } catch (err) {
      // silent fail
    }
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={() => setOpen((o) => !o)}
        className="relative p-2 rounded-md hover:bg-slate-100 text-slate-500"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-[16px] h-4 px-1 rounded-full bg-red-500 text-white text-[10px] font-semibold flex items-center justify-center">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 card shadow-lg py-1 max-h-96 overflow-y-auto">
          <div className="flex items-center justify-between px-4 py-2 border-b border-slate-100">
            <p className="text-sm font-semibold text-slate-800">Notifications</p>
            {unreadCount > 0 && (
              <button onClick={handleMarkAllRead} className="text-xs text-navy-600 hover:underline flex items-center gap-1">
                <CheckCheck className="w-3.5 h-3.5" /> Mark all read
              </button>
            )}
          </div>

          {loading ? (
            <p className="px-4 py-6 text-sm text-slate-400 text-center">Loading...</p>
          ) : notifications.length === 0 ? (
            <p className="px-4 py-6 text-sm text-slate-400 text-center">No notifications yet.</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {notifications.slice(0, 8).map((n) => (
                <button
                  key={n.id}
                  onClick={() => !n.isRead && handleMarkRead(n.id)}
                  className={`w-full text-left px-4 py-3 hover:bg-slate-50 ${!n.isRead ? 'bg-navy-50/50' : ''}`}
                >
                  <div className="flex items-start gap-2">
                    {!n.isRead && <span className="w-1.5 h-1.5 rounded-full bg-navy-600 mt-1.5 flex-shrink-0" />}
                    <div className={!n.isRead ? '' : 'pl-3.5'}>
                      <p className="text-sm font-medium text-slate-800">{n.title}</p>
                      <p className="text-xs text-slate-500 mt-0.5">{n.message}</p>
                      <p className="text-[11px] text-slate-400 mt-1">{timeAgo(n.createdAt)}</p>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          )}

          <div className="px-4 py-2 border-t border-slate-100">
            <Link to="/notifications" onClick={() => setOpen(false)} className="text-xs font-medium text-navy-600 hover:underline">
              View all notifications
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
