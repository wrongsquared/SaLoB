import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import LeLayout from './components/layout'
import HomePage from './pages/HomePage'
import Dashboard from './pages/Dashboard'
import Analytics from './pages/Analytics'
import Reports from './pages/Reports'
import Login from './pages/Login'
import FoodEntryDetailPage from './pages/FoodEntryDetailPage'

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
])

function App() {
  const [queryClient] = useState(() => new QueryClient())

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}

export default App
