import axiosClient from '../api/axiosClient'

export const login = async (email, password) => {
  const response = await axiosClient.post('/auth/login', { email, password })
  return response.data
}

export const getCurrentUser = async () => {
  const response = await axiosClient.get('/auth/me')
  return response.data
}
