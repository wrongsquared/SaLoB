import { useEffect } from 'react'
import { MapContainer, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useMapStore } from '@/stores/mapStore'
import { EateryModeMarkers, FoodModeMarkers } from './MarkerLayers'

L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
})

function syncBounds(map: L.Map) {
  const b = map.getBounds()
  const center = map.getCenter()
  useMapStore.getState().setMapBounds({
    minLat: b.getSouthWest().lat,
    maxLat: b.getNorthEast().lat,
    minLon: b.getSouthWest().lng,
    maxLon: b.getNorthEast().lng,
  })
  useMapStore.getState().setMapCenter([center.lat, center.lng])
  useMapStore.getState().setMapZoom(map.getZoom())
}

function BoundsTracker() {
  const map = useMap()

  useEffect(() => {
    syncBounds(map)
  }, [map])

  useMapEvents({
    moveend: () => {
      syncBounds(map)
    },
  })

  return null
}

export default function MapSection() {
  const { mode } = useMapStore()

  return (
    <div className="absolute inset-0 -z-10">
      <MapContainer
        center={[1.3521, 103.8198]}
        zoom={13}
        className="h-full w-full"
        zoomControl={true}
        keyboard={true}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <BoundsTracker />
        {mode === 'eatery' ? <EateryModeMarkers /> : <FoodModeMarkers />}
      </MapContainer>
    </div>
  )
}
