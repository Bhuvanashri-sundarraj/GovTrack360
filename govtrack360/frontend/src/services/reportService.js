import axiosClient from '../api/axiosClient'

export const getReportTypes = async () => {
  const response = await axiosClient.get('/reports/types')
  return response.data
}

export const generateReport = async (type, params = {}) => {
  const response = await axiosClient.get(`/reports/${type}`, { params })
  return response.data
}

// Returns a Blob. The request goes through axios so the JWT header is attached
// (a plain <a href> download could not send it).
export const exportReportCsv = async (type, params = {}) => {
  const response = await axiosClient.get(`/reports/${type}/export`, { params, responseType: 'blob' })
  return response.data
}
