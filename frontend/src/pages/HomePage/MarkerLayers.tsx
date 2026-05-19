import { useNavigate } from 'react-router-dom'
import { Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import {
  useEateriesWithinBounds,
  useFoodEntriesWithinBounds,
} from '@/shared/api/queries'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { useMapStore } from '@/stores/mapStore'
import MarkerClusterGroup from 'react-leaflet-cluster'

const typeColors: Record<string, string> = {
  'Hawker Stall': 'rgb(var(--primary-700))',
  'Hawker Centre': 'rgb(var(--text-500))',
  Cafe: '#d97706',
  Restaurant: '#dc2626',
  'Food Court': 'rgb(var(--accent-500))',
  Bakery: '#ea580c',
  Bistro: '#db2777',
  Kopitiam: '#0d9488',
  'Bubble Tea Shop': '#7c3aed',
  'Dessert Shop': '#e11d48',
  'Fast Food': '#ca8a04',
}

const foodColorPalette = [
  'rgb(var(--primary-700))',
  'rgb(var(--text-500))',
  '#d97706',
  '#dc2626',
  'rgb(var(--accent-500))',
]

function coloredCircleIcon(
  color: string,
  emoji: string,
  size: number,
): L.DivIcon {
  const safeEmoji = emoji.replace(/[<>&"']/g, '')
  return L.divIcon({
    className: '',
    html: `<div style="
      width: ${size}px; height: ${size}px;
      background: ${color};
      border: 2px solid white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 6px rgba(0,0,0,0.3);
      font-size: ${Math.round(size * 0.5)}px;
    ">${safeEmoji}</div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -(size / 2 + 2)],
  })
}

function eateryIcon(typeLabel: string) {
  return coloredCircleIcon(typeColors[typeLabel] ?? 'rgb(var(--secondary-400))', '', 28)
}

function foodIcon(idx: number) {
  return coloredCircleIcon(
    foodColorPalette[idx % foodColorPalette.length],
    '🍽',
    24,
  )
}

export function EateryModeMarkers() {
  const { mapBounds, selectEatery } = useMapStore()
  const debouncedBounds = useDebounce(mapBounds, 300)
  const { data: eateries, isError } =
    useEateriesWithinBounds(debouncedBounds)

  if (isError) return null

  return (
    <MarkerClusterGroup>
      {eateries?.map((e) => (
        <Marker
          key={e.eateryId}
          position={[e.latitude, e.longitude]}
          icon={eateryIcon(e.typeLabel)}
          eventHandlers={{
            click: () => selectEatery(e.eateryId),
          }}
        >
          <Popup>{e.name}</Popup>
        </Marker>
      ))}
    </MarkerClusterGroup>
  )
}

export function FoodModeMarkers() {
  const navigate = useNavigate()
  const { mapBounds, selectedFoods } = useMapStore()
  const debouncedBounds = useDebounce(mapBounds, 300)
  const { data: entries, isError } =
    useFoodEntriesWithinBounds(debouncedBounds)

  const visibleEntries = entries?.filter((entry) => {
    if (selectedFoods.length === 0) return true
    return selectedFoods.some(
      (sf) => entry.foodName.toLowerCase().includes(sf.toLowerCase()),
    )
  }) ?? []

  if (isError) return null

  return (
    <MarkerClusterGroup>
      {visibleEntries.map((entry, idx) => (
        <Marker
          key={entry.foodEntryId}
          position={[entry.latitude, entry.longitude]}
          icon={foodIcon(idx)}
          eventHandlers={{
            click: () => navigate(`/food-entry/${entry.foodEntryId}`),
          }}
        >
          <Popup>
            {entry.foodName} &middot; ${(entry.sgCents / 100).toFixed(2)}
            <br />
            <span className="text-xs text-secondary-400">
              {entry.eateryName}
            </span>
          </Popup>
        </Marker>
      ))}
    </MarkerClusterGroup>
  )
}
