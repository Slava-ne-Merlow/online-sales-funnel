const defaultApiUrl =
  typeof window !== 'undefined' ? window.location.origin : 'http://localhost'

export const env = {
  apiUrl: import.meta.env.VITE_API_URL?.trim() || defaultApiUrl,
}
