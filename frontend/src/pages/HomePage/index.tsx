import { useMapStore } from '@/stores/mapStore'
import MapSection from './MapSection'
import ModeToggle from './ModeToggle'
import SearchBar from './SearchBar'
import FoodTagPicker from './FoodTagPicker'
import EateryPanel from './EateryPanel'

export default function HomePage() {
  const { mode } = useMapStore()

  return (
    <div className="relative h-[calc(100vh-57px)] w-full overflow-hidden">
      <MapSection />

      <div className="pointer-events-none absolute inset-x-0 top-0 z-10 flex flex-col items-center gap-3 px-4 pt-4">
        <div className="pointer-events-auto">
          <ModeToggle />
        </div>
        <div className="pointer-events-auto w-full max-w-md">
          {mode === 'eatery' ? <SearchBar /> : <FoodTagPicker />}
        </div>
      </div>

      <EateryPanel />
    </div>
  )
}
