import { generateRandomCoordBounds, getRandomWeightedItem, randomFromArray, randFloat, CoordBounds } from "./utils";
import { EATERY_SEARCH_TERMS, FOOD_SEARCH_TERMS, USER_EMAILS, USER_PASSWORD } from "./constants";
import { StatusCodes } from "http-status-codes";
import type { RefinedResponse } from "k6/http";
import { check, sleep } from "k6";
import http from "k6/http";

const API_BASE_URL = "http://localhost:8081";

type EateryMapItem = {
    eateryId: string;
};

type FoodPreview = {
    foodEntryId: string;
};

type UserSession = {
    email: string;
    jwt: string;
};

enum Mode {
    EATERY,
    FOOD,
}

const USE_SHARED_LOGIN = __ENV.SHARED_LOGIN === "true";
const SHARED_LOGIN_EMAIL = __ENV.SHARED_LOGIN_EMAIL ?? USER_EMAILS[0];

const pickLoginEmail = (): string => {
    if (USE_SHARED_LOGIN) {
        return SHARED_LOGIN_EMAIL;
    }
    return randomFromArray(USER_EMAILS) ?? SHARED_LOGIN_EMAIL;
};

const pickRandomMode = (): Mode => (Math.random() < 0.5 ? Mode.EATERY : Mode.FOOD);

const pickVoteValue = (): boolean | null => {
    const roll = Math.random();
    if (roll < 0.45) {
        return true;
    }
    if (roll < 0.9) {
        return false;
    }
    return null;
};
//======================================================================================================
abstract class State {
    _transitionTo: State | null = null;

    abstract onEnter(): void;
    abstract onRun(): void;

    transitionTo(): State | null {
        return this._transitionTo;
    }
}

abstract class SessionState extends State {
    protected readonly session: UserSession;

    constructor(session: UserSession) {
        super();
        this.session = session;
    }
}

class InitialState extends State {
    override onEnter(): void {}
    override onRun(): void {
        this._transitionTo = new LoginState();
    }
}

class LoginState extends State {
    override onEnter(): void {
        const email = pickLoginEmail();
        const payload = JSON.stringify({ usernameOrEmail: email, password: USER_PASSWORD });
        const res = http.post(`${API_BASE_URL}/api/auth/login`, payload, {
            headers: { "Content-Type": "application/json" },
            tags: { name: "/api/auth/login" },
        });

        const ok = check(res, { "auth login 200": (r) => r.status === StatusCodes.OK });
        if (!ok) {
            this._transitionTo = new LoginState();
            sleep(1);
            return;
        }

        const data = res.json() as { jwt?: string };
        if (!data.jwt) {
            this._transitionTo = new LoginState();
            sleep(1);
            return;
        }

        const session: UserSession = { email, jwt: data.jwt };
        this._transitionTo =
            pickRandomMode() === Mode.EATERY ? new EateryPanningState(session) : new FoodPanningState(session);
    }

    override onRun(): void {}
}

class ExitState extends State {
    override onEnter(): void {}
    override onRun(): void {}
}
//======================================================================================================
class PauseState extends SessionState {
    static readonly PAUSE_DURATION_SECONDS = { min: 1.0, max: 3.0 };

    override onEnter(): void {
        this._transitionTo =
            pickRandomMode() === Mode.EATERY
                ? new EateryPanningState(this.session)
                : new FoodPanningState(this.session);

        const desiredPauseDurationSeconds = randFloat(
            PauseState.PAUSE_DURATION_SECONDS.min,
            PauseState.PAUSE_DURATION_SECONDS.max,
        );
        sleep(desiredPauseDurationSeconds);
    }
    override onRun(): void {}
}

class EateryPanningState extends SessionState {
    static readonly TAG_EATERY_QUERY = "/api/eateries/within-bounds";
    static readonly QUERY_DEBOUNCE_TIME_SECONDS = 0.3;
    static readonly PANNING_DURATION_SECONDS = { min: 3.0, max: 15.0 };

    private startedPanningAt = 0;
    private desiredPanDurationSeconds = 0;
    private lastEateryIds: string[] = [];

    override onEnter(): void {
        this.startedPanningAt = Date.now();
        this.desiredPanDurationSeconds = randFloat(
            EateryPanningState.PANNING_DURATION_SECONDS.min,
            EateryPanningState.PANNING_DURATION_SECONDS.max,
        );
    }

    override onRun(): void {
        const timeElapsedSeconds = (Date.now() - this.startedPanningAt) / 1000;
        if (timeElapsedSeconds >= this.desiredPanDurationSeconds) {
            const choices: { item: State; weight: number }[] = [
                { item: new PauseState(this.session), weight: 0.3 },
                { item: new EaterySearchState(this.session), weight: 0.4 },
                { item: new FoodPanningState(this.session), weight: 0.1 },
            ];

            const eateryId = randomFromArray(this.lastEateryIds);
            if (eateryId) {
                choices.push({
                    item: new ViewEateryDetailState(this.session, eateryId, this.lastEateryIds),
                    weight: 0.2,
                });
            }

            const nextState = getRandomWeightedItem(choices);
            this._transitionTo = nextState ?? new PauseState(this.session);
            return;
        }

        const bounds = generateRandomCoordBounds();
        const refreshRes = this.fetchEateriesWithinBounds(bounds);
        check(refreshRes, { "eateries | within-bounds 200": (r) => r.status === StatusCodes.OK });

        const ids = this.extractEateryIds(refreshRes);
        if (ids.length > 0) {
            this.lastEateryIds = ids;
        }

        sleep(EateryPanningState.QUERY_DEBOUNCE_TIME_SECONDS);
    }

    private fetchEateriesWithinBounds(bounds: CoordBounds): RefinedResponse<"text"> {
        const url =
            `${API_BASE_URL}/api/eateries/within-bounds?` +
            `minLat=${bounds.minLat}&maxLat=${bounds.maxLat}&minLon=${bounds.minLon}&maxLon=${bounds.maxLon}`;

        return http.get(url, { tags: { name: EateryPanningState.TAG_EATERY_QUERY } }) as RefinedResponse<"text">;
    }

    private extractEateryIds(res: RefinedResponse<"text">): string[] {
        if (res.status !== StatusCodes.OK) {
            return [];
        }
        const data = res.json() as unknown;
        if (!Array.isArray(data)) {
            return [];
        }
        return data.filter((item) => this.isEateryMapItem(item)).map((item) => item.eateryId);
    }

    private isEateryMapItem(value: unknown): value is EateryMapItem {
        if (typeof value !== "object" || value === null) {
            return false;
        }
        const candidate = value as { eateryId?: unknown };
        return typeof candidate.eateryId === "string";
    }
}

class FoodPanningState extends SessionState {
    static readonly TAG_FOOD_QUERY = "/api/food-entries/within-bounds";
    static readonly QUERY_DEBOUNCE_TIME_SECONDS = 0.3;
    static readonly PANNING_DURATION_SECONDS = { min: 3.0, max: 15.0 };

    private startedPanningAt = 0;
    private desiredPanDurationSeconds = 0;

    override onEnter(): void {
        this.startedPanningAt = Date.now();
        this.desiredPanDurationSeconds = randFloat(
            FoodPanningState.PANNING_DURATION_SECONDS.min,
            FoodPanningState.PANNING_DURATION_SECONDS.max,
        );
    }

    override onRun(): void {
        const timeElapsedSeconds = (Date.now() - this.startedPanningAt) / 1000;
        if (timeElapsedSeconds >= this.desiredPanDurationSeconds) {
            const nextState = getRandomWeightedItem<State>([
                { item: new PauseState(this.session), weight: 0.45 },
                { item: new FoodSearchState(this.session), weight: 0.35 },
                { item: new EateryPanningState(this.session), weight: 0.2 },
            ]);
            this._transitionTo = nextState ?? new PauseState(this.session);
            return;
        }

        const bounds = generateRandomCoordBounds();
        const refreshRes = this.fetchFoodEntriesWithinBounds(bounds);
        check(refreshRes, { "food-entries | within-bounds 200": (r) => r.status === StatusCodes.OK });

        sleep(FoodPanningState.QUERY_DEBOUNCE_TIME_SECONDS);
    }

    private fetchFoodEntriesWithinBounds(bounds: CoordBounds): RefinedResponse<"text"> {
        const url =
            `${API_BASE_URL}/api/food-entries/within-bounds?` +
            `minLat=${bounds.minLat}&maxLat=${bounds.maxLat}&minLon=${bounds.minLon}&maxLon=${bounds.maxLon}`;

        return http.get(url, { tags: { name: FoodPanningState.TAG_FOOD_QUERY } }) as RefinedResponse<"text">;
    }
}

class EaterySearchState extends SessionState {
    static readonly SEARCH_KEYSTROKE_DELAY_SECONDS = 0.1;

    private searchTerm = "Maxwell Food Centre";

    override onEnter(): void {
        this.searchTerm = randomFromArray(EATERY_SEARCH_TERMS) ?? "Maxwell Food Centre";
    }

    override onRun(): void {
        for (let i = 1; i <= this.searchTerm.length; i += 1) {
            const query = this.searchTerm.slice(0, i);
            const searchRes = this.fetchSearch(query);
            check(searchRes, {
                "search eateries 200/204": (r) => r.status === StatusCodes.OK || r.status === StatusCodes.NO_CONTENT,
            });
            sleep(EaterySearchState.SEARCH_KEYSTROKE_DELAY_SECONDS);
        }
        this._transitionTo = new PauseState(this.session);
    }

    private fetchSearch(query: string): RefinedResponse<"text"> {
        const url = `${API_BASE_URL}/api/eateries/search?search=${encodeURIComponent(query)}`;
        return http.get(url, { tags: { name: "/api/eateries/search" } }) as RefinedResponse<"text">;
    }
}

class FoodSearchState extends SessionState {
    static readonly SEARCH_KEYSTROKE_DELAY_SECONDS = 0.1;

    private searchTerm = "Chicken Rice";

    override onEnter(): void {
        this.searchTerm = randomFromArray(FOOD_SEARCH_TERMS) ?? "Chicken Rice";
    }

    override onRun(): void {
        for (let i = 1; i <= this.searchTerm.length; i += 1) {
            const query = this.searchTerm.slice(0, i);
            const searchRes = this.fetchSearch(query);
            check(searchRes, {
                "search foods 200/204": (r) => r.status === StatusCodes.OK || r.status === StatusCodes.NO_CONTENT,
            });
            sleep(FoodSearchState.SEARCH_KEYSTROKE_DELAY_SECONDS);
        }
        this._transitionTo = new PauseState(this.session);
    }

    private fetchSearch(query: string): RefinedResponse<"text"> {
        const url = `${API_BASE_URL}/api/foods/search?search=${encodeURIComponent(query)}`;
        return http.get(url, { tags: { name: "/api/foods/search" } }) as RefinedResponse<"text">;
    }
}

class ViewEateryDetailState extends SessionState {
    static readonly TAG_EATERY_DETAIL = "/api/eateries/:eateryId";
    static readonly DETAIL_VIEW_SECONDS = { min: 5.0, max: 12.0 };
    static readonly DETAIL_TO_HISTORICAL_WEIGHT = 0.2;
    static readonly DETAIL_TO_VOTE_WEIGHT = 0; // TODO

    private readonly eateryId: string;
    private readonly availableEateryIds: string[];

    constructor(session: UserSession, eateryId: string, availableEateryIds: string[]) {
        super(session);
        this.eateryId = eateryId;
        this.availableEateryIds = availableEateryIds;
    }

    override onEnter(): void {
        if (!this.eateryId) {
            this._transitionTo = this.buildFallbackState();
            sleep(1);
            return;
        }

        const detailRes = this.fetchEateryDetail(this.eateryId);
        const ok = check(detailRes, { "view eatery panel 200": (r) => r.status === StatusCodes.OK });
        if (!ok) {
            this._transitionTo = this.buildFallbackState();
            sleep(1);
            return;
        }

        const foodEntryIds = this.extractFoodEntryIds(detailRes);
        this._transitionTo = this.buildNextState(foodEntryIds);
        sleep(randFloat(ViewEateryDetailState.DETAIL_VIEW_SECONDS.min, ViewEateryDetailState.DETAIL_VIEW_SECONDS.max));
    }

    override onRun(): void {}

    private buildNextState(foodEntryIds: string[]): State {
        const choices: { item: State; weight: number }[] = [
            { item: new EateryPanningState(this.session), weight: 0.55 },
        ];

        const anotherEateryId = this.pickAnotherEateryId();
        if (anotherEateryId) {
            choices.push({
                item: new ViewEateryDetailState(this.session, anotherEateryId, this.availableEateryIds),
                weight: 0.2,
            });
        }

        const historicalId = randomFromArray(foodEntryIds);
        if (historicalId) {
            choices.push({
                item: new ViewHistoricalDataState(this.session, historicalId, foodEntryIds),
                weight: ViewEateryDetailState.DETAIL_TO_HISTORICAL_WEIGHT,
            });
            choices.push({
                item: new VoteState(this.session, historicalId, new PauseState(this.session)),
                weight: ViewEateryDetailState.DETAIL_TO_VOTE_WEIGHT,
            });
        }

        return getRandomWeightedItem(choices) ?? new EateryPanningState(this.session);
    }

    private buildFallbackState(): State {
        const nextState = getRandomWeightedItem<State>([
            { item: new EateryPanningState(this.session), weight: 0.4 },
            { item: new FoodPanningState(this.session), weight: 0.2 },
            { item: new PauseState(this.session), weight: 0.2 },
            { item: new EaterySearchState(this.session), weight: 0.1 },
            { item: new FoodSearchState(this.session), weight: 0.1 },
        ]);
        return nextState ?? new PauseState(this.session);
    }

    private pickAnotherEateryId(): string | null {
        if (this.availableEateryIds.length <= 1) {
            return null;
        }
        const candidates = this.availableEateryIds.filter((id) => id !== this.eateryId);
        return randomFromArray(candidates);
    }

    private fetchEateryDetail(eateryId: string): RefinedResponse<"text"> {
        const url = `${API_BASE_URL}/api/eateries/${eateryId}`;
        return http.get(url, { tags: { name: ViewEateryDetailState.TAG_EATERY_DETAIL } }) as RefinedResponse<"text">;
    }

    private extractFoodEntryIds(res: RefinedResponse<"text">): string[] {
        if (res.status !== StatusCodes.OK) {
            return [];
        }
        const data = res.json() as unknown;
        if (typeof data !== "object" || data === null) {
            return [];
        }
        const candidate = data as { foodPreviews?: unknown };
        if (!Array.isArray(candidate.foodPreviews)) {
            return [];
        }
        return candidate.foodPreviews.filter((item) => this.isFoodPreview(item)).map((item) => item.foodEntryId);
    }

    private isFoodPreview(value: unknown): value is FoodPreview {
        if (typeof value !== "object" || value === null) {
            return false;
        }
        const candidate = value as { foodEntryId?: unknown };
        return typeof candidate.foodEntryId === "string";
    }
}

class ViewHistoricalDataState extends SessionState {
    static readonly TAG_HISTORICAL_DATA = "/api/food-entries/historical-data/:foodEntryId";
    static readonly HISTORICAL_VIEW_SECONDS = { min: 5.0, max: 12.0 };
    static readonly HISTORICAL_START_DAYS_AGO = 30;
    static readonly HISTORICAL_REPEAT_PROB = 0.35;
    static readonly HISTORICAL_TO_VOTE_WEIGHT = 0.0; // TODO

    private readonly foodEntryId: string;
    private readonly availableFoodEntryIds: string[];

    constructor(session: UserSession, foodEntryId: string, availableFoodEntryIds: string[]) {
        super(session);
        this.foodEntryId = foodEntryId;
        this.availableFoodEntryIds = availableFoodEntryIds;
    }

    override onEnter(): void {
        if (!this.foodEntryId) {
            this._transitionTo = this.buildFallbackState();
            sleep(1);
            return;
        }

        const historicalRes = this.fetchHistoricalFoodEntry(this.foodEntryId);
        const ok = check(historicalRes, { "view historical data 200": (r) => r.status === StatusCodes.OK });
        if (!ok) {
            this._transitionTo = this.buildFallbackState();
            sleep(1);
            return;
        }

        this._transitionTo = this.buildNextState();
        sleep(
            randFloat(
                ViewHistoricalDataState.HISTORICAL_VIEW_SECONDS.min,
                ViewHistoricalDataState.HISTORICAL_VIEW_SECONDS.max,
            ),
        );
    }

    override onRun(): void {}

    private buildNextState(): State {
        const choices: { item: State; weight: number }[] = [];

        const shouldRepeat =
            this.availableFoodEntryIds.length > 1 && Math.random() < ViewHistoricalDataState.HISTORICAL_REPEAT_PROB;
        if (shouldRepeat) {
            const nextId = this.pickAnotherFoodEntryId();
            if (nextId) {
                return new ViewHistoricalDataState(this.session, nextId, this.availableFoodEntryIds);
            }
        }

        choices.push({
            item:
                pickRandomMode() === Mode.EATERY
                    ? new EateryPanningState(this.session)
                    : new FoodPanningState(this.session),
            weight: 0.8,
        });

        choices.push({
            item: new VoteState(this.session, this.foodEntryId, new PauseState(this.session)),
            weight: ViewHistoricalDataState.HISTORICAL_TO_VOTE_WEIGHT,
        });

        return getRandomWeightedItem(choices) ?? new PauseState(this.session);
    }

    private buildFallbackState(): State {
        const nextState = getRandomWeightedItem<State>([
            { item: new EateryPanningState(this.session), weight: 0.4 },
            { item: new FoodPanningState(this.session), weight: 0.2 },
            { item: new PauseState(this.session), weight: 0.2 },
            { item: new EaterySearchState(this.session), weight: 0.1 },
            { item: new FoodSearchState(this.session), weight: 0.1 },
        ]);
        return nextState ?? new PauseState(this.session);
    }

    private pickAnotherFoodEntryId(): string | null {
        if (this.availableFoodEntryIds.length <= 1) {
            return null;
        }
        const candidates = this.availableFoodEntryIds.filter((id) => id !== this.foodEntryId);
        return randomFromArray(candidates);
    }

    private fetchHistoricalFoodEntry(foodEntryId: string): RefinedResponse<"text"> {
        const startDate = new Date(Date.now() - ViewHistoricalDataState.HISTORICAL_START_DAYS_AGO * 24 * 60 * 60 * 1000)
            .toISOString()
            .slice(0, 10);
        const url = `${API_BASE_URL}/api/food-entries/historical-data/${foodEntryId}?startDate=${startDate}`;
        return http.get(url, {
            tags: { name: ViewHistoricalDataState.TAG_HISTORICAL_DATA },
            headers: { Authorization: `Bearer ${this.session.jwt}` },
        }) as RefinedResponse<"text">;
    }
}

class VoteState extends SessionState {
    static readonly TAG_VOTE = "/api/food-entries/:foodEntryId/vote";

    private readonly foodEntryId: string;
    private readonly nextState: State;

    constructor(session: UserSession, foodEntryId: string, nextState: State) {
        super(session);
        this.foodEntryId = foodEntryId;
        this.nextState = nextState;
    }

    override onEnter(): void {
        const payload = JSON.stringify({ isUpvote: pickVoteValue() });
        const res = http.post(`${API_BASE_URL}/api/food-entries/${this.foodEntryId}/vote`, payload, {
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${this.session.jwt}`,
            },
            tags: { name: VoteState.TAG_VOTE },
        });

        check(res, { "vote 200": (r) => r.status === StatusCodes.OK });
        this._transitionTo = this.nextState;
    }

    override onRun(): void {}
}
//======================================================================================================
const doRandomActivity = (): void => {
    let state: State = new InitialState();

    state.onEnter();
    while (!(state instanceof ExitState)) {
        state.onRun();

        const nextState = state.transitionTo();
        if (nextState != null && nextState !== state) {
            state = nextState;
            state.onEnter();
        }
    }
};

export default doRandomActivity;
