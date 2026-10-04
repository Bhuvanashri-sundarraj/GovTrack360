import React from 'react'
import { useAuth } from '../context/AuthContext'
import AttendanceManagement from './AttendanceManagement'
import MyAttendance from './MyAttendance'

export default function Attendance() {
  const { user } = useAuth()

  if (user?.role === 'ADMIN' || user?.role === 'OFFICER') return <AttendanceManagement />
  return <MyAttendance />
}
