const defaultApiUrl =
  typeof window !== 'undefined' ? window.location.origin : 'http://localhost:8080'

export const env = {
  apiUrl: import.meta.env.VITE_API_URL?.trim() || defaultApiUrl,
}
