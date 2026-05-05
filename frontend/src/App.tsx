import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import LeLayout from './components/layout'
import Dashboard from './pages/Dashboard'
import MapView from './pages/MapView'
import Analytics from './pages/Analytics'
import Reports from './pages/Reports'
import Login from './pages/Login'

const router = createBrowserRouter([
  {
    path: '/',
    element: <LeLayout />,
    children: [
      { index: true, element: <MapView /> },
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'analytics', element: <Analytics /> },
      { path: 'reports', element: <Reports /> },
    ],
  },
  {
    path: '/login',
    element: <Login />,
  },
])

function App() {
  return <RouterProvider router={router} />
}

export default App
