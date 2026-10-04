import axiosClient from '../api/axiosClient'

export const getAnalyticsOverview = async (params = {}) => {
  const response = await axiosClient.get('/analytics/overview', { params })
  return response.data
}
