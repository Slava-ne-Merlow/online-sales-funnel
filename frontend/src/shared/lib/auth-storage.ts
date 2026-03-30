const ACCESS_TOKEN_KEY = 'crm.access-token'

export function getStoredToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setStoredToken(token: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function removeStoredToken() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
}
