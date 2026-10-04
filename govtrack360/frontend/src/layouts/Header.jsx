import React, { useState } from 'react'
import { Menu, LogOut, ChevronDown } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import NotificationBell from '../components/NotificationBell'

export default function Header({ onMenuClick }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const initials = (user?.fullName || user?.email || '?')
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  return (
    <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 lg:px-6 sticky top-0 z-30">
      <div className="flex items-center gap-3">
        <button className="lg:hidden text-slate-500" onClick={onMenuClick}>
          <Menu className="w-6 h-6" />
        </button>
        <h1 className="text-sm font-semibold text-slate-700 hidden sm:block">
          Government Productivity &amp; Performance Management System
        </h1>
      </div>

      <div className="flex items-center gap-2">
        <NotificationBell />
        <div className="relative">
        <button
          onClick={() => setMenuOpen((o) => !o)}
          className="flex items-center gap-2 px-2 py-1.5 rounded-md hover:bg-slate-100"
        >
          <div className="w-8 h-8 rounded-full bg-navy-700 text-white flex items-center justify-center text-xs font-semibold">
            {initials}
          </div>
          <div className="text-left hidden sm:block">
            <p className="text-sm font-medium text-slate-700 leading-tight">{user?.fullName || user?.email}</p>
            <p className="text-[11px] text-slate-500 leading-tight">{user?.role}</p>
          </div>
          <ChevronDown className="w-4 h-4 text-slate-400" />
        </button>

        {menuOpen && (
          <div className="absolute right-0 mt-2 w-48 card py-1 shadow-lg">
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50"
            >
              <LogOut className="w-4 h-4" /> Logout
            </button>
          </div>
        )}
        </div>
      </div>
    </header>
  )
}
