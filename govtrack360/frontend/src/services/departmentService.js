import axiosClient from '../api/axiosClient'

export const getDepartments = async (params = {}) => {
  const response = await axiosClient.get('/departments', { params })
  return response.data
}

export const getDepartment = async (id) => {
  const response = await axiosClient.get(`/departments/${id}`)
  return response.data
}

export const createDepartment = async (payload) => {
  const response = await axiosClient.post('/departments', payload)
  return response.data
}

export const updateDepartment = async (id, payload) => {
  const response = await axiosClient.put(`/departments/${id}`, payload)
  return response.data
}

export const updateDepartmentStatus = async (id, status) => {
  const response = await axiosClient.patch(`/departments/${id}/status`, { status })
  return response.data
}

export const deleteDepartment = async (id) => {
  await axiosClient.delete(`/departments/${id}`)
}
