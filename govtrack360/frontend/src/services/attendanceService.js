import axiosClient from '../api/axiosClient'

export const checkIn = async () => {
  const response = await axiosClient.post('/attendance/check-in')
  return response.data
}

export const checkOut = async () => {
  const response = await axiosClient.patch('/attendance/check-out')
  return response.data
}

export const getMyAttendanceHistory = async (params = {}) => {
  const response = await axiosClient.get('/attendance/my', { params })
  return response.data
}

export const getMyAttendanceSummary = async (params = {}) => {
  const response = await axiosClient.get('/attendance/my/summary', { params })
  return response.data
}

export const searchAttendance = async (params = {}) => {
  const response = await axiosClient.get('/attendance', { params })
  return response.data
}

export const getTodayAttendanceSummary = async () => {
  const response = await axiosClient.get('/attendance/summary/today')
  return response.data
}
