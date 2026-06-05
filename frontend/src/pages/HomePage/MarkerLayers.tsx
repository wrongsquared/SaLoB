import { useNavigate } from "react-router-dom";
import { Marker, Popup } from "react-leaflet";
import L from "leaflet";
import { renderToStaticMarkup } from "react-dom/server";
import { Building2, Coffee, Store, UtensilsCrossed, CakeSlice, Wine, CupSoda, Hamburger } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useEateriesWithinBounds, useFoodEntriesWithinBounds } from "@/shared/api/queries";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { useMapStore } from "@/stores/mapStore";
import MarkerClusterGroup from "react-leaflet-cluster";

const typeColors: Record<string, string> = {
  "Hawker Stall": "rgb(var(--primary-700))",
  "Hawker Centre": "rgb(var(--text-500))",
  Cafe: "#d97706",
  Restaurant: "#dc2626",
  "Food Court": "rgb(var(--accent-500))",
  Bakery: "#ea580c",
  Bistro: "#db2777",
  Kopitiam: "#0d9488",
  "Bubble Tea Shop": "#7c3aed",
  "Dessert Shop": "#e11d48",
  "Fast Food": "#ca8a04",
};

const foodColorPalette = [
  "rgb(var(--primary-700))",
  "rgb(var(--text-500))",
  "#d97706",
  "#dc2626",
  "rgb(var(--accent-500))",
];

const typeIcons: Record<string, LucideIcon> = {
  "Hawker Stall": Store,
  "Hawker Centre": Building2,
  Cafe: Coffee,
  Restaurant: UtensilsCrossed,
  "Food Court": UtensilsCrossed,
  Bakery: CakeSlice,
  Bistro: Wine,
  Kopitiam: Coffee,
  "Bubble Tea Shop": CupSoda,
  "Dessert Shop": CakeSlice,
  "Fast Food": Hamburger,
};

const iconCache = new Map<string, string>();

function lucideToSvg(Icon: LucideIcon, size: number, color: string): string {
  const key = `${Icon.displayName || Icon.name}_${size}_${color}`;
  if (iconCache.has(key)) return iconCache.get(key)!;
  const svg = renderToStaticMarkup(<Icon size={size} color={color} />);
  iconCache.set(key, svg);
  return svg;
}

function coloredCircleIcon(color: string, innerHtml: string, size: number): L.DivIcon {
  return L.divIcon({
    className: "",
    html: `<div style="
      width: ${size}px; height: ${size}px;
      background: ${color};
      border: 2px solid white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 6px rgba(0,0,0,0.3);
    ">${innerHtml}</div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -(size / 2 + 2)],
  });
}

function eateryIcon(typeLabel: string) {
  const color = typeColors[typeLabel] ?? "rgb(var(--secondary-400))";
  const Icon = typeIcons[typeLabel];
  const inner = Icon ? lucideToSvg(Icon, 14, "white") : "";
  return coloredCircleIcon(color, inner, 28);
}

function foodIcon(idx: number) {
  const color = foodColorPalette[idx % foodColorPalette.length];
  const inner = lucideToSvg(Hamburger, 12, "white");
  return coloredCircleIcon(color, inner, 24);
}

export function EateryModeMarkers() {
  const { mapBounds, selectEatery } = useMapStore();
  const debouncedBounds = useDebounce(mapBounds, 400);
  const { data: eateries, isError } = useEateriesWithinBounds(debouncedBounds);

  if (isError) return null;
  if (!Array.isArray(eateries)) return null;

  return (
    <MarkerClusterGroup>
      {eateries.map((e) => (
        <Marker
          key={e.eateryId}
          position={[e.latitude, e.longitude]}
          icon={eateryIcon(e.typeLabel)}
          eventHandlers={{
            click: () => selectEatery(e.eateryId),
          }}
        >
          <Popup autoPan={false}>
            {e.name}
          </Popup>
        </Marker>
      ))}
    </MarkerClusterGroup>
  );
}

export function FoodModeMarkers() {
  const navigate = useNavigate();
  const { mapBounds, selectedFoods } = useMapStore();
  const debouncedBounds = useDebounce(mapBounds, 400);
  const { data: entries, isError } = useFoodEntriesWithinBounds(debouncedBounds);

  if (isError) return null;
  if (!Array.isArray(entries)) return null;

  const visibleEntries = entries.filter((entry) => {
    if (selectedFoods.length === 0) return true;
    return selectedFoods.some((sf) => entry.foodName.toLowerCase().includes(sf.toLowerCase()));
  });

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
            <span className="text-xs text-secondary-400">{entry.eateryName}</span>
          </Popup>
        </Marker>
      ))}
    </MarkerClusterGroup>
  );
}
