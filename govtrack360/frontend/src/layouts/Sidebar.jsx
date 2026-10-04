import React from 'react'
import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard, Users, Building2, UserCircle, ShieldCheck,
  ClipboardList, CalendarCheck, CalendarClock, Bell, ScrollText,
  TrendingUp, Award, FileText, BarChart3,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'

const NAV_BY_ROLE = {
  ADMIN: [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/employees', label: 'Employees', icon: Users },
    { to: '/departments', label: 'Departments', icon: Building2 },
    { to: '/tasks', label: 'Tasks', icon: ClipboardList },
    { to: '/attendance', label: 'Attendance', icon: CalendarCheck },
    { to: '/leave', label: 'Leave', icon: CalendarClock },
    { to: '/performance', label: 'Performance', icon: TrendingUp },
    { to: '/rewards', label: 'Rewards', icon: Award },
    { to: '/reports', label: 'Reports', icon: FileText },
    { to: '/analytics', label: 'Analytics', icon: BarChart3 },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    { to: '/audit-logs', label: 'Audit Logs', icon: ScrollText },
    { to: '/profile', label: 'Profile', icon: UserCircle },
  ],
  OFFICER: [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/employees', label: 'Employees', icon: Users },
    { to: '/departments', label: 'Departments', icon: Building2 },
    { to: '/tasks', label: 'Tasks', icon: ClipboardList },
    { to: '/attendance', label: 'Attendance', icon: CalendarCheck },
    { to: '/leave', label: 'Leave', icon: CalendarClock },
    { to: '/performance', label: 'Performance', icon: TrendingUp },
    { to: '/rewards', label: 'Rewards', icon: Award },
    { to: '/reports', label: 'Reports', icon: FileText },
    { to: '/analytics', label: 'Analytics', icon: BarChart3 },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    { to: '/profile', label: 'Profile', icon: UserCircle },
  ],
  EMPLOYEE: [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/my-tasks', label: 'My Tasks', icon: ClipboardList },
    { to: '/attendance', label: 'My Attendance', icon: CalendarCheck },
    { to: '/leave', label: 'My Leave', icon: CalendarClock },
    { to: '/performance', label: 'My Performance', icon: TrendingUp },
    { to: '/rewards', label: 'My Rewards', icon: Award },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    { to: '/profile', label: 'My Profile', icon: UserCircle },
    { to: '/departments', label: 'Departments', icon: Building2 },
  ],
}

export default function Sidebar({ open }) {
  const { user } = useAuth()
  const items = NAV_BY_ROLE[user?.role] || []

  return (
    <aside className={`bg-navy-800 text-navy-100 w-64 flex-shrink-0 fixed lg:static inset-y-0 left-0 z-40 transform transition-transform duration-200 overflow-y-auto ${open ? 'translate-x-0' : '-translate-x-full'} lg:translate-x-0`}>
      <div className="h-16 flex items-center gap-2 px-5 border-b border-navy-700 sticky top-0 bg-navy-800 z-10">
        <ShieldCheck className="w-6 h-6 text-white" />
        <div>
          <p className="text-white font-semibold leading-tight">GovTrack 360</p>
          <p className="text-[11px] text-navy-300 leading-tight">e-Office Platform</p>
        </div>
      </div>
      <nav className="px-3 py-4 space-y-1 pb-16">
        {items.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors ${
                isActive ? 'bg-navy-600 text-white' : 'text-navy-200 hover:bg-navy-700 hover:text-white'
              }`
            }
          >
            <Icon className="w-4 h-4" />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="px-5 py-4 border-t border-navy-700">
        <p className="text-[11px] text-navy-400">Phase 3 of 3</p>
        <p className="text-[11px] text-navy-500">Performance, Rewards, Reports & Analytics are live.</p>
      </div>
    </aside>
  )
}
