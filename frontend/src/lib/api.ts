import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { AuthResponse } from './types'

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

const ACCESS_KEY = 'pf_access'
const REFRESH_KEY = 'pf_refresh'
const USER_KEY = 'pf_user'

export const tokenStore = {
  access: () => localStorage.getItem(ACCESS_KEY),
  refresh: () => localStorage.getItem(REFRESH_KEY),
  user: () => {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  },
  set(auth: AuthResponse) {
    localStorage.setItem(ACCESS_KEY, auth.accessToken)
    localStorage.setItem(REFRESH_KEY, auth.refreshToken)
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
  },
}

export const api = axios.create({ baseURL: BASE_URL })

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.access()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStore.refresh()
  if (!refreshToken) return null
  try {
    const { data } = await axios.post<AuthResponse>(`${BASE_URL}/api/v1/auth/refresh`, { refreshToken })
    tokenStore.set(data)
    return data.accessToken
  } catch {
    tokenStore.clear()
    return null
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    const isAuthCall = original?.url?.includes('/api/v1/auth/')

    if (error.response?.status === 401 && original && !original._retried && !isAuthCall) {
      original._retried = true
      refreshing = refreshing ?? refreshAccessToken()
      const newToken = await refreshing
      refreshing = null
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      }
      // Refresh failed — force re-login.
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

/** Extracts a human-readable message from an API error envelope. */
export function apiErrorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    return data?.message ?? error.message ?? fallback
  }
  return fallback
}
