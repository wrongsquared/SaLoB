import { MapContainer, TileLayer, useMapEvents } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { useMapStore } from "@/stores/mapStore";
import { EateryModeMarkers, FoodModeMarkers } from "./MarkerLayers";

L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png",
  iconUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png",
});

function syncBounds(map: L.Map) {
  const b = map.getBounds();
  const center = map.getCenter();
  const store = useMapStore.getState();
  const newBounds = {
    minLat: Math.round(b.getSouthWest().lat * 1_000_000) / 1_000_000,
    maxLat: Math.round(b.getNorthEast().lat * 1_000_000) / 1_000_000,
    minLon: Math.round(b.getSouthWest().lng * 1_000_000) / 1_000_000,
    maxLon: Math.round(b.getNorthEast().lng * 1_000_000) / 1_000_000,
  };
  const oldBounds = store.mapBounds;
  if (
    !oldBounds ||
    oldBounds.minLat !== newBounds.minLat ||
    oldBounds.maxLat !== newBounds.maxLat ||
    oldBounds.minLon !== newBounds.minLon ||
    oldBounds.maxLon !== newBounds.maxLon
  ) {
    store.setMapBounds(newBounds);
  }
  store.setMapCenter([center.lat, center.lng]);
  store.setMapZoom(map.getZoom());
}

function BoundsTracker() {
  useMapEvents({
    load: (e) => {
      syncBounds(e.target)
    },
    moveend: (e) => {
      syncBounds(e.target)
    },
  })

  return null
}

export default function MapSection() {
  const mode = useMapStore((s) => s.mode);

  return (
    <div className="fixed inset-0 isolate">
      <MapContainer center={[1.3521, 103.8198]} zoom={13} className="h-full w-full" zoomControl={false} keyboard={true} scrollWheelZoom={true} doubleClickZoom={false}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <BoundsTracker />
        {mode === "eatery" ? <EateryModeMarkers /> : <FoodModeMarkers />}
      </MapContainer>
    </div>
  );
}
