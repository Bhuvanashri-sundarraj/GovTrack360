import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'

import Login from './pages/Login'
import Unauthorized from './pages/Unauthorized'
import NotFound from './pages/NotFound'
import Dashboard from './pages/Dashboard'
import Employees from './pages/Employees'
import Departments from './pages/Departments'
import Profile from './pages/Profile'
import Tasks from './pages/Tasks'
import MyTasks from './pages/MyTasks'
import TaskDetails from './pages/TaskDetails'
import Attendance from './pages/Attendance'
import Leave from './pages/Leave'
import Notifications from './pages/Notifications'
import AuditLogs from './pages/AuditLogs'
import Performance from './pages/Performance'
import Rewards from './pages/Rewards'
import Reports from './pages/Reports'
import Analytics from './pages/Analytics'

import DashboardLayout from './layouts/DashboardLayout'
import ProtectedRoute from './routes/ProtectedRoute'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/unauthorized" element={<Unauthorized />} />

      {/* Any authenticated user (ADMIN, OFFICER, EMPLOYEE) */}
      <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'OFFICER', 'EMPLOYEE']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/departments" element={<Departments />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/tasks/:id" element={<TaskDetails />} />
          <Route path="/attendance" element={<Attendance />} />
          <Route path="/leave" element={<Leave />} />
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/performance" element={<Performance />} />
          <Route path="/rewards" element={<Rewards />} />

          {/* ADMIN + OFFICER only */}
          <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'OFFICER']} />}>
            <Route path="/employees" element={<Employees />} />
            <Route path="/tasks" element={<Tasks />} />
            <Route path="/reports" element={<Reports />} />
            <Route path="/analytics" element={<Analytics />} />
          </Route>

          {/* EMPLOYEE only */}
          <Route element={<ProtectedRoute allowedRoles={['EMPLOYEE']} />}>
            <Route path="/my-tasks" element={<MyTasks />} />
          </Route>

          {/* ADMIN only */}
          <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
            <Route path="/audit-logs" element={<AuditLogs />} />
          </Route>
        </Route>
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
