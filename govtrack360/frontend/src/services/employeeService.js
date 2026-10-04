import axiosClient from '../api/axiosClient'

export const getEmployees = async (params = {}) => {
  const response = await axiosClient.get('/employees', { params })
  return response.data
}

export const getEmployee = async (id) => {
  const response = await axiosClient.get(`/employees/${id}`)
  return response.data
}

export const createEmployee = async (payload) => {
  const response = await axiosClient.post('/employees', payload)
  return response.data
}

export const updateEmployee = async (id, payload) => {
  const response = await axiosClient.put(`/employees/${id}`, payload)
  return response.data
}

export const updateEmployeeStatus = async (id, status) => {
  const response = await axiosClient.patch(`/employees/${id}/status`, { status })
  return response.data
}

export const deleteEmployee = async (id) => {
  await axiosClient.delete(`/employees/${id}`)
}
