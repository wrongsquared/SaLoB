import { NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

const navItems = [
  { label: 'Dashboard', path: '/dashboard' },
  { label: 'Analytics', path: '/analytics' },
  { label: 'Reports', path: '/reports' },
]

export default function Navbar() {
  const navigate = useNavigate()
  const { isAuthenticated, user, logout } = useAuthStore()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-50 bg-background-50 min-h-full border-b-1 border-black">
      <div className="w-full flex items-center justify-between py-4 px-20">
        <div className="flex items-center gap-10">
          <div className="text-xl font-extrabold text-primary-700">SaLoB</div>
          <nav className="flex items-center space-x-4">
            {navItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `text-sm ${isActive ? 'text-primary-700 underline underline-offset-6 font-bold' : 'text-secondary-400'} hover:text-primary-700`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
        <div>
          {isAuthenticated && user ? (
            <div className="flex items-center gap-3">
              <span className="text-sm font-medium text-gray-700">{user.username}</span>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-full border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 transition-colors duration-250 hover:bg-gray-100"
              >
                Logout
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="rounded-full bg-primary-700 px-4 py-2 text-sm font-medium text-primary-50 transition-colors duration-250 hover:bg-primary-100 hover:text-primary-700 hover:font-weight-bold"
            >
              Login
            </button>
          )}
        </div>
      </div>
    </header>
  )
}
