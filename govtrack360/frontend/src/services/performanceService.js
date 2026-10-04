import axiosClient from '../api/axiosClient'

export const getMyPerformance = async (params = {}) => {
  const response = await axiosClient.get('/performance/my', { params })
  return response.data
}

export const getMyPerformanceTrend = async (months = 6) => {
  const response = await axiosClient.get('/performance/my/trend', { params: { months } })
  return response.data
}

export const getMyReviews = async () => {
  const response = await axiosClient.get('/performance/reviews/my')
  return response.data
}

export const getPerformanceScope = async () => {
  const response = await axiosClient.get('/performance/scope')
  return response.data
}

export const getEligibleEmployees = async () => {
  const response = await axiosClient.get('/performance/eligible-employees')
  return response.data
}

export const getLeaderboard = async (params = {}) => {
  const response = await axiosClient.get('/performance/leaderboard', { params })
  return response.data
}

export const getEmployeePerformanceTrend = async (employeeId, months = 6) => {
  const response = await axiosClient.get(`/performance/employees/${employeeId}/trend`, { params: { months } })
  return response.data
}

export const getReviews = async (params = {}) => {
  const response = await axiosClient.get('/performance/reviews', { params })
  return response.data
}

export const createReview = async (payload) => {
  const response = await axiosClient.post('/performance/reviews', payload)
  return response.data
}

export const updateReview = async (id, payload) => {
  const response = await axiosClient.put(`/performance/reviews/${id}`, payload)
  return response.data
}

export const deleteReview = async (id) => {
  await axiosClient.delete(`/performance/reviews/${id}`)
}
