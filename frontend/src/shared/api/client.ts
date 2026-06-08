import axios from 'axios';
import { useAuthStore } from '@/stores/authStore';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api';

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10_000,
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === 'ECONNABORTED') {
      console.warn('[api] Request timed out — backend may be unavailable');
    } else if (!error.response) {
      console.warn('[api] Network error — backend may be unavailable');
    } else if (error.response.status === 401) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  },
);
