import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { useQuery } from '@tanstack/react-query'
import { authApi, usersApi } from '../../shared/api/services'
import { queryClient } from '../../shared/api/query-client'
import { getStoredToken, removeStoredToken, setStoredToken } from '../../shared/lib/auth-storage'
import type { SignInRequest, User } from '../../shared/api/types'

type AuthContextValue = {
  isAuthenticated: boolean
  isAuthReady: boolean
  token: string | null
  user: User | null
  signIn: (credentials: SignInRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: PropsWithChildren) {
  const [token, setToken] = useState<string | null>(() => getStoredToken())
  const [isBootstrapped, setIsBootstrapped] = useState(false)

  const meQuery = useQuery({
    queryKey: ['auth', 'me', token],
    queryFn: () => usersApi.getMe(),
    enabled: Boolean(token),
    retry: false,
  })

  useEffect(() => {
    if (!token) {
      setIsBootstrapped(true)
    }
  }, [token])

  useEffect(() => {
    if (meQuery.isSuccess || meQuery.isError) {
      setIsBootstrapped(true)
    }
  }, [meQuery.isError, meQuery.isSuccess])

  const logout = useCallback(() => {
    removeStoredToken()
    setToken(null)
    queryClient.removeQueries({ queryKey: ['auth', 'me'] })
    queryClient.removeQueries({ queryKey: ['projects'] })
  }, [])

  useEffect(() => {
    const handleUnauthorized = () => logout()

    window.addEventListener('app:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('app:unauthorized', handleUnauthorized)
  }, [logout])

  const signIn = useCallback(async (credentials: SignInRequest) => {
    const response = await authApi.signIn(credentials)
    setStoredToken(response.accessToken)
    setToken(response.accessToken)
    queryClient.setQueryData(['auth', 'me', response.accessToken], response.user)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      isAuthenticated: Boolean(token),
      isAuthReady: isBootstrapped,
      token,
      user: meQuery.data ?? null,
      signIn,
      logout,
    }),
    [isBootstrapped, logout, meQuery.data, signIn, token],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }

  return context
}
