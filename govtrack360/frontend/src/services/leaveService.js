import axiosClient from '../api/axiosClient'

export const applyLeave = async (payload) => {
  const response = await axiosClient.post('/leave-requests', payload)
  return response.data
}

export const getMyLeaveRequests = async () => {
  const response = await axiosClient.get('/leave-requests/my')
  return response.data
}

export const cancelLeaveRequest = async (id) => {
  const response = await axiosClient.patch(`/leave-requests/${id}/cancel`)
  return response.data
}

export const searchLeaveRequests = async (params = {}) => {
  const response = await axiosClient.get('/leave-requests', { params })
  return response.data
}

export const reviewLeaveRequest = async (id, payload) => {
  const response = await axiosClient.patch(`/leave-requests/${id}/review`, payload)
  return response.data
}
