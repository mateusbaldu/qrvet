import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi, type User } from '../services/api'
import { AuthContext, type AuthContextValue } from './useAuth'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    authApi.restore()
      .then((restoredUser) => { if (active) setUser(restoredUser) })
      .catch(() => { if (active) setUser(null) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!user) return
    const sendHeartbeat = () => { void authApi.heartbeat().catch(() => undefined) }
    sendHeartbeat()
    const interval = window.setInterval(sendHeartbeat, 60_000)
    const onVisibilityChange = () => { if (document.visibilityState === 'visible') sendHeartbeat() }
    document.addEventListener('visibilitychange', onVisibilityChange)
    window.addEventListener('focus', sendHeartbeat)
    return () => {
      window.clearInterval(interval)
      document.removeEventListener('visibilitychange', onVisibilityChange)
      window.removeEventListener('focus', sendHeartbeat)
    }
  }, [user])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    async login(email, password) {
      await authApi.login(email, password)
      const authenticatedUser = await authApi.me()
      setUser(authenticatedUser)
      return authenticatedUser
    },
    async logout() {
      await authApi.logout()
      setUser(null)
    },
    clearSession() {
      authApi.clearLocalSession()
      setUser(null)
    },
    async reloadUser() {
      setUser(await authApi.me())
    },
  }), [loading, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
