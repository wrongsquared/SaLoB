import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { GoogleLogin } from "@react-oauth/google"
import { useLoginMutation, useGoogleLoginMutation, useRegisterMutation } from "@/shared/api/queries"

type AuthMode = "signin" | "register"

const Login = () => {
  const navigate = useNavigate()
  const [mode, setMode] = useState<AuthMode>("signin")

  const [email, setEmail] = useState("")
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")

  const loginMutation = useLoginMutation()
  const googleMutation = useGoogleLoginMutation()
  const registerMutation = useRegisterMutation()

  const isLoading = loginMutation.isPending || googleMutation.isPending || registerMutation.isPending
  const error = loginMutation.error || googleMutation.error || registerMutation.error

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      if (mode === "signin") {
        await loginMutation.mutateAsync({ usernameOrEmail: email, password })
      } else {
        await registerMutation.mutateAsync({ email, username, password })
      }
      navigate("/")
    } catch {
      // error handled via error state
    }
  }

  const handleGoogleSuccess = async (credentialResponse: { credential?: string }) => {
    if (!credentialResponse.credential) return
    try {
      await googleMutation.mutateAsync(credentialResponse.credential)
      navigate("/")
    } catch {
      // error handled via error state
    }
  }

  return (
    <div className="min-h-screen w-full overflow-hidden">
      <div className="flex min-h-screen w-full flex-col md:flex-row">

        <div className="flex flex-1 flex-col justify-center gap-4 bg-gradient-to-br from-primary-800 to-primary-600 p-10 text-text-50 md:p-16">
          <div className="text-4xl font-extrabold tracking-tight">SaLoB</div>
          <h1 className="text-3xl font-bold leading-tight md:text-4xl">
            Keep Tabs on the Price of Living
          </h1>
          <p className="max-w-md text-lg text-primary-200">
            Join the community tracking Singapore hawker prices. See trends,
            compare costs, and make informed choices.
          </p>
        </div>

        <div className="flex flex-1 flex-col justify-center bg-gray-100 p-10 md:p-16">
          <div className="mx-auto w-full max-w-sm">
            <div className="mb-8 flex gap-4 border-b border-gray-200">
              <button
                type="button"
                onClick={() => setMode("signin")}
                className={`pb-2 text-sm font-medium transition-colors ${
                  mode === "signin"
                    ? "border-b-2 border-primary-700 text-primary-700"
                    : "text-gray-500 hover:text-primary-700"
                }`}
              >
                Sign In
              </button>
              <button
                type="button"
                onClick={() => setMode("register")}
                className={`pb-2 text-sm font-medium transition-colors ${
                  mode === "register"
                    ? "border-b-2 border-primary-700 text-primary-700"
                    : "text-gray-500 hover:text-primary-700"
                }`}
              >
                Register
              </button>
            </div>

            {error && (
              <div className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">
                {(error as { response?: { data?: { error?: string } } }).response?.data?.error ??
                  "An error occurred. Please try again."}
              </div>
            )}

            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full rounded-md border border-gray-300 px-4 py-2 text-sm outline-none transition-colors focus:border-primary-700"
              />

              {mode === "register" && (
                <input
                  type="text"
                  placeholder="Username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  minLength={3}
                  className="w-full rounded-md border border-gray-300 px-4 py-2 text-sm outline-none transition-colors focus:border-primary-700"
                />
              )}

              <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={6}
                className="w-full rounded-md border border-gray-300 px-4 py-2 text-sm outline-none transition-colors focus:border-primary-700"
              />

              <button
                type="submit"
                disabled={isLoading}
                className="w-full rounded-md bg-primary-700 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-800 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isLoading
                  ? "Please wait..."
                  : mode === "signin"
                    ? "Sign In"
                    : "Create Account"}
              </button>
            </form>

            <div className="my-6 flex items-center gap-3">
              <div className="flex-1 border-t border-gray-300" />
              <span className="text-xs text-gray-400">OR</span>
              <div className="flex-1 border-t border-gray-300" />
            </div>

            <div className="flex justify-center">
              <GoogleLogin
                onSuccess={handleGoogleSuccess}
                onError={() => {}}
                size="large"
                text={mode === "register" ? "signup_with" : "signin_with"}
              />
            </div>
          </div>
        </div>

      </div>
    </div>
  )
}

export default Login
