import axiosClient from '../api/axiosClient'

export const getRewardTypes = async () => {
  const response = await axiosClient.get('/rewards/types')
  return response.data
}

export const getRewardWall = async (limit = 10) => {
  const response = await axiosClient.get('/rewards/wall', { params: { limit } })
  return response.data
}

export const getMyRewards = async () => {
  const response = await axiosClient.get('/rewards/my')
  return response.data
}

export const getRewards = async (params = {}) => {
  const response = await axiosClient.get('/rewards', { params })
  return response.data
}

export const getRewardLeaderboard = async (params = {}) => {
  const response = await axiosClient.get('/rewards/leaderboard', { params })
  return response.data
}

export const grantReward = async (payload) => {
  const response = await axiosClient.post('/rewards', payload)
  return response.data
}

export const revokeReward = async (id) => {
  await axiosClient.delete(`/rewards/${id}`)
}
