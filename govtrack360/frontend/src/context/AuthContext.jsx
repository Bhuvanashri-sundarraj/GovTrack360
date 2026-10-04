import React, { createContext, useContext, useEffect, useState } from 'react'
import * as authService from '../services/authService'

const AuthContext = createContext(null)

const STORAGE_TOKEN_KEY = 'govtrack360_token'
const STORAGE_USER_KEY = 'govtrack360_user'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const bootstrap = async () => {
      const token = localStorage.getItem(STORAGE_TOKEN_KEY)
      if (!token) {
        setLoading(false)
        return
      }
      try {
        // Validate the stored token against the backend and refresh the user profile.
        const current = await authService.getCurrentUser()
        setUser(current)
        localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(current))
      } catch (err) {
        localStorage.removeItem(STORAGE_TOKEN_KEY)
        localStorage.removeItem(STORAGE_USER_KEY)
        setUser(null)
      } finally {
        setLoading(false)
      }
    }
    bootstrap()
  }, [])

  const login = async (email, password) => {
    const data = await authService.login(email, password)
    localStorage.setItem(STORAGE_TOKEN_KEY, data.token)
    const current = await authService.getCurrentUser()
    localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(current))
    setUser(current)
    return current
  }

  const logout = () => {
    localStorage.removeItem(STORAGE_TOKEN_KEY)
    localStorage.removeItem(STORAGE_USER_KEY)
    setUser(null)
  }

  const value = { user, setUser, loading, login, logout, isAuthenticated: !!user }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
