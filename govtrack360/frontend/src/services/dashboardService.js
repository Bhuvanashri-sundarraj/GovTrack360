import axiosClient from '../api/axiosClient'

export const getAdminDashboard = async () => {
  const response = await axiosClient.get('/dashboard/admin')
  return response.data
}

export const getOfficerDashboard = async () => {
  const response = await axiosClient.get('/dashboard/officer')
  return response.data
}

export const getEmployeeDashboard = async () => {
  const response = await axiosClient.get('/dashboard/employee')
  return response.data
}
