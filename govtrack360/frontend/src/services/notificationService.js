import axiosClient from '../api/axiosClient'

export const getNotifications = async () => {
  const response = await axiosClient.get('/notifications')
  return response.data
}

export const getUnreadCount = async () => {
  const response = await axiosClient.get('/notifications/unread-count')
  return response.data
}

export const markNotificationRead = async (id) => {
  const response = await axiosClient.patch(`/notifications/${id}/read`)
  return response.data
}

export const markAllNotificationsRead = async () => {
  await axiosClient.patch('/notifications/read-all')
}
