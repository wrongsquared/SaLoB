import { useMapStore } from "@/stores/mapStore";
import { Plus } from "lucide-react";
import MapSection from "./MapSection";
import ModeToggle from "./ModeToggle";
import SearchBar from "./SearchBar";
import FoodTagPicker from "./FoodTagPicker";
import EateryPanel from "./EateryPanel";
import SubmissionWizard from "./SubmissionWizard";

export default function HomePage() {
  const { mode, setWizardOpen } = useMapStore();

  return (
    <div className="relative h-[calc(100vh-57px)] w-full overflow-hidden">
      <MapSection />

      <div className="pointer-events-none absolute inset-x-0 top-0 z-10 flex flex-col items-center gap-3 px-4 pt-4">
        <div className="pointer-events-auto">
          <ModeToggle />
        </div>
        <div className="pointer-events-auto w-full max-w-md">
          {mode === "eatery" ? <SearchBar /> : <FoodTagPicker />}
        </div>
      </div>

      <EateryPanel />

      <button
        type="button"
        onClick={() => setWizardOpen(true)}
        className="absolute bottom-6 right-6 z-20 flex h-14 w-14 items-center justify-center rounded-full bg-primary-700 text-white shadow-lg transition-transform hover:scale-105 hover:bg-primary-600"
        aria-label="Submit price"
      >
        <Plus size={24} />
      </button>

      <SubmissionWizard />
    </div>
  );
}
