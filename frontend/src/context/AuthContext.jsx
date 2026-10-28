import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const [token, setToken] = useState(() => localStorage.getItem('token'))

  function loginUser(userData, tokenValue) {
    setUser(userData)
    setToken(tokenValue)
    localStorage.setItem('user', JSON.stringify(userData))
    localStorage.setItem('token', tokenValue)
  }

  function logoutUser() {
    setUser(null)
    setToken(null)
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  return (
    <AuthContext.Provider value={{ user, token, loginUser, logoutUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}