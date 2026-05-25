import doRandomActivity from "./user";

const TEST_MODE = __ENV.TEST_MODE ?? "quick";

const quickScenario = {
    executor: "constant-vus",
    vus: 10,
    duration: "40s",
    gracefulStop: "5s",
};

const rpsScenario = {
    executor: "ramping-arrival-rate",
    startRate: 0,
    timeUnit: "1s",
    stages: [
        { target: 800, duration: "40s" },
        { target: 1000, duration: "30s" },
        { target: 1000, duration: "5m" },
        { target: 0, duration: "50s" },
    ],
    gracefulStop: "5s",
};

const stagedScenario = {
    executor: "ramping-vus",
    startVUs: 0,
    stages: [
        { target: 500, duration: "40s" },
        { target: 500, duration: "1.5m" },
        { target: 0, duration: "30s" },
    ],
    gracefulStop: "5s",
};

const selectedScenario = (() => {
    switch (TEST_MODE) {
        case "staged":
            return stagedScenario;
        case "rps":
            return rpsScenario;
        case "quick":
            return quickScenario;
        default:
            return quickScenario;
    }
})();

export const options = {
    scenarios: {
        default: selectedScenario,
    },
    thresholds: {
        http_req_duration: ["p(95)<200", "p(99)<500"],
        http_req_failed: ["rate<0.01"], // Errors must be less than 1%
    },
};

export default function () {
    doRandomActivity();
}
