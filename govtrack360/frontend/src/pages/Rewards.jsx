import React from 'react'
import { useAuth } from '../context/AuthContext'
import RewardsManagement from './RewardsManagement'
import MyRewards from './MyRewards'

export default function Rewards() {
  const { user } = useAuth()

  if (user?.role === 'ADMIN' || user?.role === 'OFFICER') return <RewardsManagement />
  return <MyRewards />
}
