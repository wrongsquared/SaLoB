import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10_000,
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === 'ECONNABORTED') {
      console.warn('[api] Request timed out — backend may be unavailable')
    } else if (!error.response) {
      console.warn('[api] Network error — backend may be unavailable')
    }
    return Promise.reject(error)
  },
)
