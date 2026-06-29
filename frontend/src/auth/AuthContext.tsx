import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { api, tokenStore } from '@/lib/api'
import type { AuthResponse, User } from '@/lib/types'

interface AuthContextValue {
  user: User | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  signup: (name: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => tokenStore.user())

  const login = useCallback(async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/api/v1/auth/login', { email, password })
    tokenStore.set(data)
    setUser(data.user)
  }, [])

  const signup = useCallback(async (name: string, email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/api/v1/auth/signup', { name, email, password })
    tokenStore.set(data)
    setUser(data.user)
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.refresh()
    try {
      if (refreshToken) await api.post('/api/v1/auth/logout', { refreshToken })
    } catch {
      // ignore — clear locally regardless
    }
    tokenStore.clear()
    setUser(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: !!user, login, signup, logout }),
    [user, login, signup, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
