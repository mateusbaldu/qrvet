import { useEffect, useState, type ReactNode } from 'react'
import { loginRequest, logoutRequest, refreshSession, type User } from '../services/api'
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
