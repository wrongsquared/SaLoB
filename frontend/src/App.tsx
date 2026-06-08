import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { apiClient } from '@/shared/api/client';
import type { User } from '@/shared/types/api';
import LeLayout from './components/layout';
import HomePage from './pages/HomePage';
import Dashboard from './pages/Dashboard';
import Analytics from './pages/Analytics';
import Reports from './pages/Reports';
import Login from './pages/Login';
import FoodEntryDetailPage from './pages/FoodEntryDetailPage';

function AuthInitializer({ children }: { children: React.ReactNode }) {
  const { token, user, setUser } = useAuthStore();

  useEffect(() => {
    if (token && !user) {
      apiClient
        .get<User>('/users/me')
        .then(({ data }) => setUser(data))
        .catch(() => {
          // token expired or invalid
        });
    }
  }, [token, user]); // eslint-disable-line react-hooks/exhaustive-deps

  return <>{children}</>;
}

const router = createBrowserRouter([
  {
    path: '/',
    element: <LeLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'analytics', element: <Analytics /> },
      { path: 'reports', element: <Reports /> },
      { path: 'food-entry/:foodEntryId', element: <FoodEntryDetailPage /> },
    ],
  },
  {
    path: '/login',
    element: <Login />,
  },
]);

function App() {
  const [queryClient] = useState(() => new QueryClient());

  return (
    <QueryClientProvider client={queryClient}>
      <AuthInitializer>
        <RouterProvider router={router} />
      </AuthInitializer>
    </QueryClientProvider>
  );
}

export default App;
