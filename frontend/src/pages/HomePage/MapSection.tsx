import { MapContainer, TileLayer, useMapEvents } from 'react-leaflet'
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

function BoundsTracker() {
  const { setMapBounds, setMapCenter, setMapZoom } = useMapStore()

  useMapEvents({
    moveend: (e) => {
      const map = e.target
      const b = map.getBounds()
      const center = map.getCenter()
      setMapBounds({
        minLat: b.getSouthWest().lat,
        maxLat: b.getNorthEast().lat,
        minLon: b.getSouthWest().lng,
        maxLon: b.getNorthEast().lng,
      })
      setMapCenter([center.lat, center.lng])
      setMapZoom(map.getZoom())
    },
  })

  return null
}

export default function MapSection() {
  const { mode } = useMapStore()

  return (
    <div className="absolute inset-0">
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
