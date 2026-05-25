import { generateRandomCoordBounds, getRandomWeightedItem, randomFromArray, randFloat, CoordBounds } from "./utils";
import type { RefinedResponse } from "k6/http";
import { check, sleep } from "k6";
import http from "k6/http";

const API_BASE_URL = "http://localhost:8081";

type EateryMapItem = {
    eateryId: string;
};

type FoodEntryMapItem = {
    foodEntryId: string;
};

type FoodPreview = {
    foodEntryId: string;
};

enum MODE {
    EATERY,
    FOOD,
}

enum StateName {
    INITIAL = "initial",
    PANNING = "panning",
    SEARCH = "search",
    VIEW_EATERY_DETAIL = "view_eatery_detail",
    VIEW_HISTORICAL_DATA = "view_historical_data",
    PAUSE = "pause",
    EXIT = "exit",
}
//======================================================================================================
class SharedContext {
    public startedBrowsingAt: number;
    public lastEateryIds: string[];
    public lastFoodEntryIds: string[];
    public prevState: StateName = StateName.INITIAL;

    constructor() {
        this.startedBrowsingAt = Date.now();
        this.lastEateryIds = [];
        this.lastFoodEntryIds = [];
    }

    updateEateryIdsFromResponse(res: RefinedResponse<"text">): void {
        if (res.status !== 200) {
            return;
        }
        const data = res.json() as unknown;
        if (!Array.isArray(data)) {
            return;
        }
        const ids = data.filter((item) => this.isEateryMapItem(item)).map((item) => item.eateryId);
        if (ids.length > 0) {
            this.lastEateryIds = ids;
        }
    }

    updateFoodEntryIdsFromMapResponse(res: RefinedResponse<"text">): void {
        if (res.status !== 200) {
            return;
        }
        const data = res.json() as unknown;
        if (!Array.isArray(data)) {
            return;
        }
        const ids = data.filter((item) => this.isFoodEntryMapItem(item)).map((item) => item.foodEntryId);
        if (ids.length > 0) {
            this.lastFoodEntryIds = ids;
        }
    }

    updateFoodEntryIdsFromEateryDetail(res: RefinedResponse<"text">): void {
        if (res.status !== 200) {
            return;
        }
        const data = res.json() as unknown;
        if (typeof data !== "object" || data === null) {
            return;
        }
        const candidate = data as { foodPreviews?: unknown };
        if (!Array.isArray(candidate.foodPreviews)) {
            return;
        }
        const ids = candidate.foodPreviews.filter((item) => this.isFoodPreview(item)).map((item) => item.foodEntryId);
        if (ids.length > 0) {
            this.lastFoodEntryIds = ids;
        }
    }

    private isEateryMapItem(value: unknown): value is EateryMapItem {
        if (typeof value !== "object" || value === null) {
            return false;
        }
        const candidate = value as { eateryId?: unknown };
        return typeof candidate.eateryId === "string";
    }

    private isFoodEntryMapItem(value: unknown): value is FoodEntryMapItem {
        if (typeof value !== "object" || value === null) {
            return false;
        }
        const candidate = value as { foodEntryId?: unknown };
        return typeof candidate.foodEntryId === "string";
    }

    private isFoodPreview(value: unknown): value is FoodPreview {
        if (typeof value !== "object" || value === null) {
            return false;
        }
        const candidate = value as { foodEntryId?: unknown };
        return typeof candidate.foodEntryId === "string";
    }
}

abstract class State {
    _ctx: SharedContext;
    _transitionTo: State | null = null;

    constructor(ctx: SharedContext) {
        this._ctx = ctx;
    }
    abstract onEnter(): void;
    abstract onRun(): void;
    abstract getName(): StateName;
    transitionTo(): State | null {
        return this._transitionTo;
    }
}

class InitialState extends State {
    constructor(ctx: SharedContext) {
        super(ctx);
    }
    override onEnter(): void {}
    override onRun(): void {
        this._transitionTo = new PanningState(this._ctx, randFloat(0, 1) < 0.5 ? MODE.EATERY : MODE.FOOD);
    }
    override getName(): StateName {
        return StateName.INITIAL;
    }
}

class ExitState extends State {
    constructor(ctx: SharedContext) {
        super(ctx);
    }
    override onEnter(): void {}
    override onRun(): void {}
    override getName(): StateName {
        return StateName.EXIT;
    }
}
//======================================================================================================
class PauseState extends State {
    static readonly PAUSE_DURATION_SECONDS = { min: 1.0, max: 3.0 };

    constructor(ctx: SharedContext) {
        super(ctx);
    }
    override onEnter(): void {
        this._transitionTo = new PanningState(this._ctx, randFloat(0, 1) < 0.5 ? MODE.EATERY : MODE.FOOD);

        const desiredPauseDurationSeconds = randFloat(
            PauseState.PAUSE_DURATION_SECONDS.min,
            PauseState.PAUSE_DURATION_SECONDS.max,
        );
        sleep(desiredPauseDurationSeconds);
    }
    override onRun(): void {}
    override getName(): StateName {
        return StateName.PAUSE;
    }
}

class PanningState extends State {
    static readonly TAG_EATERY_QUERY = "/api/eateries/within-bounds";
    static readonly TAG_FOOD_QUERY = "/api/food-entries/within-bounds";

    // In the frontend, while the user is panning, we debounce the query to avoid sending too many requests.
    static readonly QUERY_DEBOUNCE_TIME_SECONDS = 0.3;
    static readonly PANNING_DURATION_SECONDS = { min: 3.0, max: 15.0 };

    _startedPanningAt: number = 0;
    _desiredPanDurationSeconds: number = 0;
    _panMode: MODE = MODE.EATERY;

    constructor(ctx: SharedContext, panMode: MODE) {
        super(ctx);
        this._panMode = panMode;
    }
    override onEnter(): void {
        this._startedPanningAt = Date.now();
        this._desiredPanDurationSeconds = randFloat(
            PanningState.PANNING_DURATION_SECONDS.min,
            PanningState.PANNING_DURATION_SECONDS.max,
        );
    }
    override onRun(): void {
        const timeElapsedSeconds = (Date.now() - this._startedPanningAt) / 1000;
        if (timeElapsedSeconds >= this._desiredPanDurationSeconds) {
            const nextState = getRandomWeightedItem<State>([
                { item: new PauseState(this._ctx), weight: 0.3 },
                { item: new SearchState(this._ctx), weight: 0.4 },
                { item: new ViewEateryDetailState(this._ctx), weight: 0.3 },
            ]);
            this._transitionTo = nextState ?? new PauseState(this._ctx);
            return;
        }

        const bounds = generateRandomCoordBounds();
        if (this._panMode == MODE.EATERY) {
            const refreshRes = this.fetchEateriesWithinBounds(bounds);
            check(refreshRes, { "eateries | within-bounds 200": (r) => r.status === 200 });
            this._ctx.updateEateryIdsFromResponse(refreshRes);
        } else {
            const refreshRes = this.fetchFoodEntriesWithinBounds(bounds);
            check(refreshRes, { "food-entries | within-bounds 200": (r) => r.status === 200 });
            this._ctx.updateFoodEntryIdsFromMapResponse(refreshRes);
        }
        sleep(PanningState.QUERY_DEBOUNCE_TIME_SECONDS);
    }
    override getName(): StateName {
        return StateName.PANNING;
    }

    fetchEateriesWithinBounds(bounds: CoordBounds): RefinedResponse<"text"> {
        const url =
            `${API_BASE_URL}/api/eateries/within-bounds?` +
            `minLat=${bounds.minLat}&maxLat=${bounds.maxLat}&minLon=${bounds.minLon}&maxLon=${bounds.maxLon}`;

        return http.get(url, { tags: { name: PanningState.TAG_EATERY_QUERY } }) as RefinedResponse<"text">;
    }

    fetchFoodEntriesWithinBounds(bounds: CoordBounds): RefinedResponse<"text"> {
        const url =
            `${API_BASE_URL}/api/food-entries/within-bounds?` +
            `minLat=${bounds.minLat}&maxLat=${bounds.maxLat}&minLon=${bounds.minLon}&maxLon=${bounds.maxLon}`;

        return http.get(url, { tags: { name: PanningState.TAG_FOOD_QUERY } }) as RefinedResponse<"text">;
    }
}

class SearchState extends State {
    static readonly SEARCH_KEYSTROKE_DELAY_SECONDS = 0.1;
    static readonly SEARCH_TERMS = ["chicken", "nasi", "laksa", "prata", "mee", "kopi", "teh", "satay", "roti", "rice"];

    _searchTerm: string = "rice";
    _searchMode: MODE = MODE.EATERY;

    constructor(ctx: SharedContext) {
        super(ctx);
    }
    override onEnter(): void {
        this._searchTerm = randomFromArray(SearchState.SEARCH_TERMS) ?? "rice";
        this._searchMode = Math.random() < 0.5 ? MODE.EATERY : MODE.FOOD;
    }
    override onRun(): void {
        for (let i = 1; i <= this._searchTerm.length; i += 1) {
            const query = this._searchTerm.slice(0, i);
            const searchRes = this.fetchSearch(query, this._searchMode);
            check(searchRes, { "search 2XX": (r) => r.status >= 200 && r.status < 300 });
            sleep(SearchState.SEARCH_KEYSTROKE_DELAY_SECONDS);
        }
        this._transitionTo = new PauseState(this._ctx);
    }
    override getName(): StateName {
        return StateName.SEARCH;
    }

    private fetchSearch(query: string, searchMode: MODE): RefinedResponse<"text"> {
        const endpoint = searchMode == MODE.EATERY ? "eateries" : "foods";
        const url = `${API_BASE_URL}/api/${endpoint}/search?search=${encodeURIComponent(query)}`;
        const tagName = searchMode == MODE.EATERY ? "/api/eateries/search" : "/api/foods/search";
        return http.get(url, { tags: { name: tagName } }) as RefinedResponse<"text">;
    }
}

class ViewEateryDetailState extends State {
    static readonly TAG_EATERY_DETAIL = "/api/eateries/:eateryId";
    static readonly DETAIL_VIEW_SECONDS = { min: 5.0, max: 12.0 };
    static readonly DETAIL_TO_HISTORICAL_PROB = 0.35;

    constructor(ctx: SharedContext) {
        super(ctx);
    }
    override onEnter(): void {
        const eateryId = randomFromArray(this._ctx.lastEateryIds);
        if (!eateryId) {
            const nextState = getRandomWeightedItem<State>([
                {
                    item: new PanningState(this._ctx, randFloat(0, 1) < 0.5 ? MODE.EATERY : MODE.FOOD),
                    weight: 0.6,
                },
                { item: new SearchState(this._ctx), weight: 0.4 },
            ]);
            this._transitionTo = nextState;
            sleep(2);
            return;
        }

        const detailRes = this.fetchEateryDetail(eateryId);
        check(detailRes, { "view eatery panel 200": (r) => r.status === 200 });
        this._ctx.updateFoodEntryIdsFromEateryDetail(detailRes);

        const nextState = getRandomWeightedItem<State>([
            {
                item: new PanningState(this._ctx, randFloat(0, 1) < 0.5 ? MODE.EATERY : MODE.FOOD),
                weight: 0.33333,
            },
            { item: new ViewEateryDetailState(this._ctx), weight: 0.33333 },
            { item: new ViewHistoricalDataState(this._ctx), weight: 0.33333 },
        ]);
        this._transitionTo = nextState;
        sleep(randFloat(ViewEateryDetailState.DETAIL_VIEW_SECONDS.min, ViewEateryDetailState.DETAIL_VIEW_SECONDS.max));
    }
    override onRun(): void {}
    override getName(): StateName {
        return StateName.VIEW_EATERY_DETAIL;
    }

    private fetchEateryDetail(eateryId: string): RefinedResponse<"text"> {
        const url = `${API_BASE_URL}/api/eateries/${eateryId}`;
        return http.get(url, { tags: { name: ViewEateryDetailState.TAG_EATERY_DETAIL } }) as RefinedResponse<"text">;
    }
}

class ViewHistoricalDataState extends State {
    static readonly TAG_HISTORICAL_DATA = "/api/food-entries/historical-data/:foodEntryId";
    static readonly HISTORICAL_VIEW_SECONDS = { min: 5.0, max: 12.0 };
    static readonly HISTORICAL_START_DAYS_AGO = 30;
    static readonly HISTORICAL_REPEAT_PROB = 0.35;

    constructor(ctx: SharedContext) {
        super(ctx);
    }
    override onEnter(): void {
        const foodEntryId = randomFromArray(this._ctx.lastFoodEntryIds);
        if (!foodEntryId) {
            const nextState = getRandomWeightedItem<State>([
                {
                    item: new PanningState(this._ctx, randFloat(0, 1) < 0.5 ? MODE.EATERY : MODE.FOOD),
                    weight: 0.6,
                },
                { item: new SearchState(this._ctx), weight: 0.4 },
            ]);
            this._transitionTo = nextState;
            sleep(1.5);
            return;
        }

        const historicalRes = this.fetchHistoricalFoodEntry(foodEntryId);
        check(historicalRes, { "view historical data 200": (r) => r.status === 200 });

        const nextState = getRandomWeightedItem<State>([
            {
                item: new PanningState(this._ctx, randFloat(0, 1) < 0.5 ? MODE.EATERY : MODE.FOOD),
                weight: 0.33333,
            },
            { item: new ViewEateryDetailState(this._ctx), weight: 0.33333 },
            { item: new ViewHistoricalDataState(this._ctx), weight: 0.33333 },
        ]);
        this._transitionTo = nextState;
        sleep(
            randFloat(
                ViewHistoricalDataState.HISTORICAL_VIEW_SECONDS.min,
                ViewHistoricalDataState.HISTORICAL_VIEW_SECONDS.max,
            ),
        );
    }
    override onRun(): void {}
    override getName(): StateName {
        return StateName.VIEW_HISTORICAL_DATA;
    }

    private fetchHistoricalFoodEntry(foodEntryId: string): RefinedResponse<"text"> {
        const startDate = new Date(Date.now() - ViewHistoricalDataState.HISTORICAL_START_DAYS_AGO * 24 * 60 * 60 * 1000)
            .toISOString()
            .slice(0, 10);
        const url = `${API_BASE_URL}/api/food-entries/historical-data/${foodEntryId}?startDate=${startDate}`;
        return http.get(url, {
            tags: { name: ViewHistoricalDataState.TAG_HISTORICAL_DATA },
        }) as RefinedResponse<"text">;
    }
}
//======================================================================================================
const doRandomActivity = (): void => {
    const ctx = new SharedContext();
    let state: State = new InitialState(ctx);

    state.onEnter();
    while (!(state instanceof ExitState)) {
        state.onRun();

        const nextState = state.transitionTo();
        if (nextState != null && nextState !== state) {
            ctx.prevState = state.getName();
            state = nextState;
            state.onEnter();
        }
    }
};

export default doRandomActivity;
