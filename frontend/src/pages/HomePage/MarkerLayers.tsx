import { useNavigate } from 'react-router-dom'
import { Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import {
  useAllEateryDetails,
  useEateriesWithinBounds,
} from '@/shared/api/queries'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { useMapStore } from '@/stores/mapStore'
import MarkerClusterGroup from 'react-leaflet-cluster'

const typeColors: Record<string, string> = {
  'Hawker Stall': '#2563eb',
  'Hawker Centre': '#7c3aed',
  Cafe: '#d97706',
  Restaurant: '#dc2626',
}

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
      background: ${color.replace(/[^#\w]/g, '')};
      border: 2px solid white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 6px rgba(0,0,0,0.3);
      font-size: ${Math.round(size * 0.5)}px;
      color: white;
    ">${safeEmoji}</div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -(size / 2 + 2)],
  })
}

const foodColorPalette = [
  '#2563eb', '#7c3aed', '#d97706', '#dc2626', '#059669',
]

function eateryIcon(typeLabel: string) {
  return coloredCircleIcon(typeColors[typeLabel] ?? '#6b7280', '🏪', 28)
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
  const { data: eateries } = useEateriesWithinBounds(debouncedBounds)
  const { data: eateryDetails, isError } =
    useAllEateryDetails(debouncedBounds)

  const positionMap = new Map<string, [number, number]>()
  eateries?.forEach((e) => {
    positionMap.set(e.eateryId, [e.latitude, e.longitude])
  })

  const visibleEntries =
    eateryDetails
      ?.flatMap((ed) => ed.foodPreviews)
      .filter((fp) => {
        if (selectedFoods.length === 0) return true
        return selectedFoods.some(
          (sf) => fp.name.toLowerCase().includes(sf.toLowerCase()),
        )
      }) ?? []

  if (isError) return null

  return (
    <MarkerClusterGroup>
      {visibleEntries.map((entry, idx) => {
        const eatery = eateryDetails?.find((ed) =>
          ed.foodPreviews.some(
            (fp) => fp.foodEntryId === entry.foodEntryId,
          ),
        )
        const pos = eatery ? positionMap.get(eatery.eateryId) : undefined
        if (!pos) return null

        return (
          <Marker
            key={entry.foodEntryId}
            position={pos}
            icon={foodIcon(idx)}
            eventHandlers={{
              click: () => navigate(`/food-entry/${entry.foodEntryId}`),
            }}
          >
            <Popup>
              {entry.name} &middot; ${(entry.sgCents / 100).toFixed(2)}
              <br />
              <span className="text-xs text-secondary-400">
                {eatery?.name}
              </span>
            </Popup>
          </Marker>
        )
      })}
    </MarkerClusterGroup>
  )
}
