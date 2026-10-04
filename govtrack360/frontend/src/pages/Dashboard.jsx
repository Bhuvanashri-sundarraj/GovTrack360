import React from 'react'
import { useAuth } from '../context/AuthContext'
import AdminDashboard from './AdminDashboard'
import OfficerDashboard from './OfficerDashboard'
import EmployeeDashboard from './EmployeeDashboard'

export default function Dashboard() {
  const { user } = useAuth()

  if (user?.role === 'ADMIN') return <AdminDashboard />
  if (user?.role === 'OFFICER') return <OfficerDashboard />
  return <EmployeeDashboard />
}
