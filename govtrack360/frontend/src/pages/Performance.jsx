import React from 'react'
import { useAuth } from '../context/AuthContext'
import PerformanceManagement from './PerformanceManagement'
import MyPerformance from './MyPerformance'

export default function Performance() {
  const { user } = useAuth()

  if (user?.role === 'ADMIN' || user?.role === 'OFFICER') return <PerformanceManagement />
  return <MyPerformance />
}
