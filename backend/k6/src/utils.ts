//========================================================================
export type CoordBounds = {
    minLat: number;
    maxLat: number;
    minLon: number;
    maxLon: number;
};

export const generateRandomCoordBounds = (): CoordBounds => {
    const MIN_LAT = 1.26;
    const MAX_LAT = 1.45;
    const MIN_LON = 103.7;
    const MAX_LON = 104.0;

    const minLat = MIN_LAT + Math.random() * (MAX_LAT - MIN_LAT);
    const maxLat = minLat + Math.random() * (MAX_LAT - minLat);

    const minLon = MIN_LON + Math.random() * (MAX_LON - MIN_LON);
    const maxLon = minLon + Math.random() * (MAX_LON - minLon);

    return { minLat, maxLat, minLon, maxLon };
};
//========================================================================
export const randFloat = (min: number, max: number): number => {
    return Math.random() * (max - min) + min;
};

export const randomFromArray = <T>(items: T[]): T | null => {
    if (items.length === 0) {
        return null;
    }
    return items[Math.floor(Math.random() * items.length)] ?? null;
};
//========================================================================
type WeightedChoice<T> = {
    item: T;
    weight: number; // Can be a raw weight (e.g., 5, 2, 3) or exact probability (e.g., 0.5, 0.2, 0.3)
};

export const getRandomWeightedItem = <T>(choices: WeightedChoice<T>[]): T | null => {
    if (!choices.length) return null;

    // Calculate total weight
    const totalWeight = choices.reduce((sum, choice) => sum + choice.weight, 0);

    // Generate a random value between 0 and totalWeight
    const randomValue = Math.random() * totalWeight;

    // Find the first choice that meets the random threshold
    let cumulativeWeight = 0;
    for (const choice of choices) {
        cumulativeWeight += choice.weight;
        if (cumulativeWeight >= randomValue) {
            return choice.item;
        }
    }

    return choices[choices.length - 1].item;
};
//========================================================================
