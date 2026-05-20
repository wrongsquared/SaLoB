import { useMapStore } from '@/stores/mapStore'

export default function SearchBar() {
  const { searchQuery, setSearchQuery } = useMapStore()

  return (
    <div className="relative w-full max-w-md">
      <input
        type="text"
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
        placeholder="Search eateries..."
        className="w-full rounded-lg border border-white/30 bg-white/70 px-4 py-2.5 pl-11 text-base text-secondary-900 backdrop-blur-sm placeholder-secondary-400 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
      />
      <svg
        className="absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-secondary-400"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        strokeWidth={2}
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z"
        />
      </svg>
    </div>
  )
}
