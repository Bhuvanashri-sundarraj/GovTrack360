import React from 'react'
import { useAuth } from '../context/AuthContext'
import LeaveManagement from './LeaveManagement'
import MyLeave from './MyLeave'

export default function Leave() {
  const { user } = useAuth()

  if (user?.role === 'ADMIN' || user?.role === 'OFFICER') return <LeaveManagement />
  return <MyLeave />
}
