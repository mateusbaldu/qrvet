import { useEffect, useState, type ReactNode } from 'react'
import { apiRequest, loginRequest, logoutRequest, refreshSession, type User } from '../services/api'
import { AuthContext } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [checkingSession, setCheckingSession] = useState(true)

  useEffect(() => {
    let active = true
    refreshSession()
      .then((response) => { if (active) setUser(response.user) })
      .catch(() => { if (active) setUser(null) })
      .finally(() => { if (active) setCheckingSession(false) })

    const clearSession = () => setUser(null)
    window.addEventListener('qrvet:unauthorized', clearSession)
    return () => {
      active = false
      window.removeEventListener('qrvet:unauthorized', clearSession)
    }
  }, [])

  useEffect(() => {
    if (!user) return

    const sendHeartbeat = () => {
      if (document.visibilityState === 'visible' && document.hasFocus()) {
        void apiRequest('/users/me/heartbeat', { method: 'POST' }).catch(() => undefined)
      }
    }
    const onVisibilityChange = () => sendHeartbeat()
    sendHeartbeat()
    const interval = window.setInterval(sendHeartbeat, 120_000)
    window.addEventListener('focus', sendHeartbeat)
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => {
      window.clearInterval(interval)
      window.removeEventListener('focus', sendHeartbeat)
      document.removeEventListener('visibilitychange', onVisibilityChange)
    }
  }, [user])

  async function login(email: string, password: string) {
    const response = await loginRequest(email, password)
    setUser(response.user)
    return response.user
  }

  async function logout() {
    await logoutRequest()
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, checkingSession, login, logout, updateUser: setUser }}>{children}</AuthContext.Provider>
}
