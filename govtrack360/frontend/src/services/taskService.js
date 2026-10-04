import axiosClient from '../api/axiosClient'

export const getTasks = async (params = {}) => {
  const response = await axiosClient.get('/tasks', { params })
  return response.data
}

export const getTask = async (id) => {
  const response = await axiosClient.get(`/tasks/${id}`)
  return response.data
}

export const createTask = async (payload) => {
  const response = await axiosClient.post('/tasks', payload)
  return response.data
}

export const updateTask = async (id, payload) => {
  const response = await axiosClient.put(`/tasks/${id}`, payload)
  return response.data
}

export const assignTask = async (id, payload) => {
  const response = await axiosClient.patch(`/tasks/${id}/assign`, payload)
  return response.data
}

export const updateTaskProgress = async (id, payload) => {
  const response = await axiosClient.patch(`/tasks/${id}/progress`, payload)
  return response.data
}

export const submitTask = async (id, payload) => {
  const response = await axiosClient.patch(`/tasks/${id}/submit`, payload)
  return response.data
}

export const reviewTask = async (id, payload) => {
  const response = await axiosClient.patch(`/tasks/${id}/review`, payload)
  return response.data
}

export const cancelTask = async (id, comment) => {
  const response = await axiosClient.patch(`/tasks/${id}/status`, { status: 'CANCELLED', comment })
  return response.data
}

export const deleteTask = async (id) => {
  await axiosClient.delete(`/tasks/${id}`)
}

export const getTaskComments = async (id) => {
  const response = await axiosClient.get(`/tasks/${id}/comments`)
  return response.data
}

export const addTaskComment = async (id, comment) => {
  const response = await axiosClient.post(`/tasks/${id}/comments`, { comment })
  return response.data
}

export const getTaskHistory = async (id) => {
  const response = await axiosClient.get(`/tasks/${id}/history`)
  return response.data
}
