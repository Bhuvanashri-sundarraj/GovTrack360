import axiosClient from '../api/axiosClient'

export const getAuditLogs = async (params = {}) => {
  const response = await axiosClient.get('/audit-logs', { params })
  return response.data
}
