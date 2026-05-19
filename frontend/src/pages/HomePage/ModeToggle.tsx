import { useMapStore } from '@/stores/mapStore'

const modes = [
  { value: 'eatery' as const, label: 'Eatery' },
  { value: 'food' as const, label: 'Food' },
]

export default function ModeToggle() {
  const { mode, setMode } = useMapStore()

  return (
    <div
      className="inline-flex rounded-lg border border-white/30 bg-white/80 p-0.5 backdrop-blur-sm"
      role="radiogroup"
      aria-label="Map mode"
    >
      {modes.map((m) => (
        <button
          key={m.value}
          type="button"
          role="radio"
          aria-checked={mode === m.value}
          onClick={() => setMode(m.value)}
          className={`rounded-md px-4 py-1.5 text-sm font-medium transition-all duration-200 ${
            mode === m.value
              ? 'bg-primary-700 text-primary-50 shadow-sm'
              : 'text-secondary-500 hover:text-primary-700'
          }`}
        >
          {m.label}
        </button>
      ))}
    </div>
  )
}
